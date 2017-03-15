package bgp.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.NumberFormatter;

import bgp.simulation.LinkingOrder;
import bgp.simulation.SimulatorState;
import bgp.simulation.SimulatorState.SimulationState;
import bgp.simulation.tasks.AddClientsTask;
import bgp.simulation.tasks.ChangeLocalPrefTask;
import bgp.simulation.tasks.ChangeTrustTask;
import bgp.simulation.tasks.ConnectRoutersTask;
import bgp.simulation.tasks.CreateRouterTask;
import bgp.simulation.tasks.DeleteRouterTask;
import bgp.simulation.tasks.DisconnectRoutersTask;
import bgp.simulation.tasks.GenerateNetworkTask;
import bgp.simulation.tasks.SimulationTask;
import bgp.simulation.tasks.SimulationTask.SimulationTaskType;
import bgp.simulation.tasks.StartGeneratingTrafficTask;
import bgp.simulation.tasks.StopGeneratingTrafficTask;

@SuppressWarnings("serial")
public class TaskCreationPopup extends JFrame {

	private static final long serialVersionUID = 1L;
	
	private final SimulationTaskType type;
	private SimulationTask task;
	private TaskEditor editor;
	
	public TaskCreationPopup(SimulationTaskType type) {
		super();
		this.type = type;
		buildUI();
	}
	
	private void buildUI() {
		setTitle("Create task: " + type.uiText);
		setSize(400, 400);
		
		Container c = getContentPane();
		c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
		
		editor = getEditor();
		c.add(editor);
		
		c.add(Box.createVerticalGlue());
		
		JButton save = new JButton("Create task");
		save.addActionListener(e -> {
			extractTask();
			close();
		});
		
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> close());
		
		JPanel buttonContainer = new JPanel();
		buttonContainer.add(save);
		buttonContainer.add(cancel);
		
		c.add(buttonContainer);
		
		pack();
	}
	
	private TaskEditor getEditor() {
		switch (type) {
		case ADD_CLIENTS:
			return new AddClientsEditor();
		case CHANGE_LOCAL_PREF:
			return new ChangeLocalPrefEditor();
		case CHANGE_TRUST:
			return new ChangeTrustEditor();
		case CONNECT_ROUTERS:
			return new ConnectRoutersEditor();
		case CREATE_ROUTER:
			return new CreateRouterTaskEditor();
		case DELETE_ROUTER:
			return new DeleteRouterTaskEditor();
		case DISCONNECT_ROUTERS:
			return new DisconnectRoutersTaskEditor();
		case GENERATE_NETWORK:
			return new GenerateNetworkEditor();
		case START_GENERATING_TRAFFIC:
			return new StartGeneratingTrafficTaskEditor();
		case STOP_GENERATING_TRAFFIC:
			return new StopGeneratingTrafficTaskEditor();
		case NONE:
		default:
			break;
		}
		return null;
	}
	
	private void extractTask() {
		this.task = editor.getTask();
	}
	
	private void close() {
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}
	
	public Optional<SimulationTask> getTask() {
		return Optional.ofNullable(this.task);
	}
	
	private abstract class TaskEditor extends JPanel {
		
		protected final NumberFieldWithTitle delayField = new NumberFieldWithTitle("Delay from start of simulation (ms)");
		
		private TaskEditor() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			add(delayField);
			delayField.setVisible(SimulatorState.getSimulationState() != SimulationState.STARTED);
		}
		
		protected abstract SimulationTask getTask();
	}
	
	private class AddClientsEditor extends TaskEditor {
		private FieldWithTitle routerIdField = new FieldWithTitle("<html>IDs of the connecting routers<br>(comma separated, empty to populate all)</html>");
		private NumberFieldWithTitle amountOfClientsField = new NumberFieldWithTitle("Amount of clients to connect");
		
		public AddClientsEditor() {
			super();
			add(routerIdField);
			add(amountOfClientsField);
		}
		

		@Override
		protected SimulationTask getTask() {
			List<Integer> routerIds = new ArrayList<>();
			for (String id : routerIdField.getValue().split(",")) {
				try {
					routerIds.add(Integer.parseInt(id.trim()));
				} catch (Exception e) {
					
				}
			}
			if (routerIds.isEmpty()) {
				return new AddClientsTask(amountOfClientsField.getValue(), delayField.getValue());
			} else {
				return new AddClientsTask(routerIds, amountOfClientsField.getValue(), delayField.getValue());
			}
		}
		
	}
	
	private class ChangeLocalPrefEditor extends TaskEditor {
		private NumberFieldWithTitle changingRouterIdField = new NumberFieldWithTitle("ID of the changing router");
		private NumberFieldWithTitle targetIdField = new NumberFieldWithTitle("ID of the target router");
		private NumberFieldWithTitle newLocalPrefField = new NumberFieldWithTitle("New LOCAL_PREF");
		
		public ChangeLocalPrefEditor() {
			super();
			add(changingRouterIdField);
			add(targetIdField);
		}
		
		@Override
		protected SimulationTask getTask() {
			return new ChangeLocalPrefTask(changingRouterIdField.getValue(), targetIdField.getValue(), newLocalPrefField.getValue(), delayField.getValue());
		}
		
	}
	
	private class GenerateNetworkEditor extends TaskEditor {
		private final JComboBox<LinkingOrder> topologySelector = new JComboBox<>(LinkingOrder.values());;
		private final NumberFieldWithTitle networkSize = new NumberFieldWithTitle("Amount of routers");;
		
		public GenerateNetworkEditor() {
			super();
			add(topologySelector);
			add(networkSize);
		}

		@Override
		protected SimulationTask getTask() {
			return new GenerateNetworkTask(topologySelector.getItemAt(topologySelector.getSelectedIndex()), networkSize.getValue());
		}
	}
	
	private class ChangeTrustEditor extends TaskEditor {
		private NumberFieldWithTitle changingRouterIdField = new NumberFieldWithTitle("ID of the changing router");
		private NumberFieldWithTitle targetIdField = new NumberFieldWithTitle("ID of the target router");
		private NumberFieldWithTitle trustDeltaField = new NumberFieldWithTitle("Change in trust", -127, 128);
		
		
		public ChangeTrustEditor() {
			super();
			add(changingRouterIdField);
			add(targetIdField);
			add(trustDeltaField);
		}
		
		@Override
		protected SimulationTask getTask() {
			return new ChangeTrustTask(changingRouterIdField.getValue(),
					targetIdField.getValue(), trustDeltaField.getValue(),
					1, 0, delayField.getValue());
		}
		
	}
	
	private class ConnectRoutersEditor extends TaskEditor {
		private NumberFieldWithTitle router1IdField = new NumberFieldWithTitle("ID of the first router");
		private NumberFieldWithTitle router2IdField = new NumberFieldWithTitle("ID of the second router");
		
		public ConnectRoutersEditor() {
			super();
			add(router1IdField);
			add(router2IdField);
		}

		@Override
		protected SimulationTask getTask() {
			return new ConnectRoutersTask(router1IdField.getValue(), router2IdField.getValue(), delayField.getValue());
		}
		
	}
	
	private class CreateRouterTaskEditor extends TaskEditor {
		private NumberFieldWithTitle routerIdField = new NumberFieldWithTitle("ID of the router");
		private FieldWithTitle subnetField = new FieldWithTitle("Subnet of the router (CIDR notation)");

		public CreateRouterTaskEditor() {
			super();
			subnetField.setText("0.0.0.0/0");
			add(routerIdField);
			add(subnetField);
		}
		
		@Override
		protected SimulationTask getTask() {
			return new CreateRouterTask(routerIdField.getValue(), subnetField.getValue(), delayField.getValue());
		}
		
	}
	
	private class DeleteRouterTaskEditor extends TaskEditor {
		private NumberFieldWithTitle routerIdField = new NumberFieldWithTitle("ID of the router");
		
		public DeleteRouterTaskEditor() {
			super();
			add(routerIdField);
		}

		@Override
		protected SimulationTask getTask() {
			return new DeleteRouterTask(routerIdField.getValue(), delayField.getValue());
		}
		
	}
	
	private class DisconnectRoutersTaskEditor extends TaskEditor {
		private NumberFieldWithTitle router1IdField = new NumberFieldWithTitle("ID of the first router");
		private NumberFieldWithTitle router2IdField = new NumberFieldWithTitle("ID of the second router");
		
		public DisconnectRoutersTaskEditor() {
			super();
			add(router1IdField);
			add(router2IdField);
		}

		@Override
		protected SimulationTask getTask() {
			return new DisconnectRoutersTask(router1IdField.getValue(),
					router2IdField.getValue(), delayField.getValue());
		}
		
	}
	
	private class StartGeneratingTrafficTaskEditor extends TaskEditor {
		private NumberFieldWithTitle routerIdField = new NumberFieldWithTitle("ID of the source router");
		private FieldWithTitle targetIdsField = new FieldWithTitle("Target network ID's (comma separated)");
		private NumberFieldWithTitle intervalField = new NumberFieldWithTitle("Time between pings (ms)");
		
		public StartGeneratingTrafficTaskEditor() {
			super();
			add(routerIdField);
			add(targetIdsField);
			add(intervalField);
		}

		@Override
		protected SimulationTask getTask() {
			List<Integer> targets = new ArrayList<>();
			for (String id : targetIdsField.getValue().split(",")) {
				try {
					targets.add(Integer.parseInt(id.trim()));
				} catch (Exception e) {
					
				}
			}
			StartGeneratingTrafficTask task = new StartGeneratingTrafficTask(routerIdField.getValue(), targets, intervalField.getValue(), delayField.getValue());
			generatorTasks.add(task);
			return task;
		}
		
	}
	
	public static List<StartGeneratingTrafficTask> generatorTasks = new ArrayList<>();
	
	private class StopGeneratingTrafficTaskEditor extends TaskEditor {
		private JComboBox<StartGeneratingTrafficTask> taskToStop = new JComboBox<>();
		
		public StopGeneratingTrafficTaskEditor() {
			generatorTasks.forEach(task -> taskToStop.addItem(task));
			add(taskToStop);
		}

		@Override
		protected SimulationTask getTask() {
			return new StopGeneratingTrafficTask(taskToStop.getItemAt(taskToStop.getSelectedIndex()), delayField.getValue());
		}
		
	}
	
	class FieldWithTitle extends JPanel {
		private static final long serialVersionUID = 1L;
		private final JLabel label;
		private final JTextField field;
		
		public FieldWithTitle(String title) {
			super();
			label = new JLabel(title);
			
			field = new JTextField();
			field.setColumns(8);
			
			add(label);
			add(field);
		}
		
		public void setText(String text) {
			field.setText(text);
		}
		
		public String getValue() {
			return field.getText();
		}
	}
	
	class NumberFieldWithTitle extends JPanel {
		private static final long serialVersionUID = 1L;
		private final NumberFormatter formatter;
		private final JLabel label;
		private final JFormattedTextField field;
		

		public NumberFieldWithTitle(String title) {
			this(title, 0, Integer.MAX_VALUE);
		}
		
		public NumberFieldWithTitle(String title, int minValue, int maxValue) {
			super();
			label = new JLabel(title);
			
			formatter = new NumberFormatter(NumberFormat.getInstance());
			formatter.setValueClass(Integer.class);
			formatter.setMinimum(minValue);
			formatter.setMaximum(maxValue);
			formatter.setAllowsInvalid(false);
			formatter.setCommitsOnValidEdit(true);
			
			
			field = new JFormattedTextField(formatter);
			field.setText("0");
			field.setColumns(8);
			
			add(label);
			add(field);
		}
		
		public int getValue() {
			return (Integer) field.getValue();
		}
	}
}
