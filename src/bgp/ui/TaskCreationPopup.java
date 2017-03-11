package bgp.ui;

import java.awt.Container;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;
import java.util.Optional;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.NumberFormatter;

import bgp.simulation.LinkingOrder;
import bgp.simulation.tasks.AddClientsTask;
import bgp.simulation.tasks.ChangeLocalPrefTask;
import bgp.simulation.tasks.GenerateNetworkTask;
import bgp.simulation.tasks.SimulationTask;
import bgp.simulation.tasks.SimulationTask.SimulationTaskType;

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
		setSize(300, 400);
		
		Container c = getContentPane();
		c.setLayout(new BoxLayout(c, BoxLayout.Y_AXIS));
		
		editor = getEditor();
		editor.setSize(300, 300);
		c.add(editor);
		
		
		JButton save = new JButton("Create task");
		save.addActionListener(e -> {
			extractTask();
			close();
		});
		
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> close());
		
		JPanel buttonContainer = new JPanel();
		buttonContainer.setSize(300, 100);
		buttonContainer.add(save);
		buttonContainer.add(cancel);
		
		c.add(buttonContainer);
	}
	
	private TaskEditor getEditor() {
		switch (type) {
		case ADD_CLIENTS:
			return new AddClientsEditor();
		case CHANGE_LOCAL_PREF:
			return new ChangeLocalPrefEditor();
		case CHANGE_TRUST:
			break;
		case CONNECT_ROUTERS:
			break;
		case CREATE_ROUTER:
			break;
		case DELETE_ROUTER:
			break;
		case DISCONNECT_ROUTERS:
			break;
		case GENERATE_NETWORK:
			return new GenerateNetworkEditor();
		case START_GENERATING_TRAFFIC:
			break;
		case STOP_GENERATING_TRAFFIC:
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
		}
		
		protected abstract SimulationTask getTask();
	}
	
	private class AddClientsEditor extends TaskEditor {
		private NumberFieldWithTitle routerIdField = new NumberFieldWithTitle("ID of the connecting router");
		private NumberFieldWithTitle amountOfClientsField = new NumberFieldWithTitle("Amount of clients to connect");
		
		public AddClientsEditor() {
			super();
			add(routerIdField);
			add(amountOfClientsField);
		}
		

		@Override
		protected SimulationTask getTask() {
			return new AddClientsTask(routerIdField.getValue(), amountOfClientsField.getValue(), delayField.getValue());
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
		private final JComboBox<LinkingOrder> topologySelector;
		private final NumberFieldWithTitle networkSize;
		
		public GenerateNetworkEditor() {
			super();
			topologySelector = new JComboBox(LinkingOrder.values());
			add(topologySelector);
			
			networkSize = new NumberFieldWithTitle("Amount of routers");
			add(networkSize);
		}

		@Override
		protected SimulationTask getTask() {
			return new GenerateNetworkTask(topologySelector.getItemAt(topologySelector.getSelectedIndex()), networkSize.getValue());
		}
		
	}
	
	class NumberFieldWithTitle extends JPanel {
		private static final long serialVersionUID = 1L;
		private final NumberFormatter formatter;
		private final JLabel label;
		private final JFormattedTextField field;
		
		public NumberFieldWithTitle(String title) {
			label = new JLabel(title);
			
			formatter = new NumberFormatter(NumberFormat.getInstance());
			formatter.setValueClass(Integer.class);
			formatter.setMinimum(0);
			formatter.setMaximum(Integer.MAX_VALUE);
			formatter.setAllowsInvalid(false);
			formatter.setCommitsOnValidEdit(true);
			
			
			field = new JFormattedTextField(formatter);
			field.setColumns(8);
			
			add(label);
			add(field);
		}
		
		public int getValue() {
			return (Integer) field.getValue();
		}
	}
}
