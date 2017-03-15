package bgp.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;

import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;

import bgp.core.BGPRouter;
import bgp.core.network.ASConnection;
import bgp.core.network.fsm.State;
import bgp.core.routing.SubnetNode;
import bgp.simulation.LogMessage.LogMessageType;
import bgp.simulation.Logger;
import bgp.simulation.SimulatorState;
import bgp.simulation.tasks.SimulationTask;
import bgp.simulation.tasks.SimulationTask.SimulationTaskType;
import bgp.simulation.tasks.SimulationTask.TaskState;

public class MainView extends JFrame {
	
	private static final long serialVersionUID = 1L;
	
	private static final int WINDOW_WIDTH = 1200;
	private static final int WINDOW_HEIGHT = 900;
	private static final int SIDEPANEL_WIDTH = WINDOW_WIDTH - WINDOW_HEIGHT;
	private static final int BUTTON_CONTAINER_HEIGHT = 100;
	private static final int LOGGER_WIDTH = 700;
	private static final int LOGGER_HEIGHT = 500;
	
	private final NetworkViewer networkViewer;
	private Viewer viewerComponent;
	
	private JPanel taskContainer;
	
	
	private JPanel controlButtonContainer;
	private JComboBox<SimulationTaskType> newTaskDropdown;
	private JComboBox<Integer> routingTableSelector;
	private JButton startButton;
	private JButton stopButton;
	
	public MainView() {
		super();
		this.networkViewer = new NetworkViewer();
		buildUI();
		buildLogger();
	}
	
	private void buildUI() {
		setTitle("BGP Simulation");
		setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		Container pane = getContentPane();
		pane.setLayout(null);
		
		// Viewer
		viewerComponent = new Viewer(networkViewer, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
		viewerComponent.enableAutoLayout();
		
		ViewPanel p = viewerComponent.addDefaultView(false);
		p.setSize(WINDOW_HEIGHT, WINDOW_HEIGHT);
		pane.add(p);
		
		// Task list
		taskContainer = new JPanel();
		taskContainer.setLayout(new BoxLayout(taskContainer, BoxLayout.Y_AXIS));
		taskContainer.setSize(SIDEPANEL_WIDTH, WINDOW_HEIGHT - BUTTON_CONTAINER_HEIGHT);
		taskContainer.setLocation(WINDOW_HEIGHT, 0);
		pane.add(new JScrollPane(taskContainer));
		
		// Control buttons
		controlButtonContainer = new JPanel();
		controlButtonContainer.setSize(SIDEPANEL_WIDTH, BUTTON_CONTAINER_HEIGHT);
		controlButtonContainer.setLocation(WINDOW_HEIGHT, WINDOW_HEIGHT - BUTTON_CONTAINER_HEIGHT);
		

		routingTableSelector = new JComboBox<>();
		routingTableSelector.addActionListener(e -> {
			if (routingTableSelector.getSelectedIndex() > 0) {
				showRoutingTable(routingTableSelector.getItemAt(routingTableSelector.getSelectedIndex()));
			}
		});
		controlButtonContainer.add(routingTableSelector);
		
		newTaskDropdown = new JComboBox<>(SimulationTaskType.values());
		
		newTaskDropdown.addActionListener(e -> {
			SimulationTaskType type = (SimulationTaskType) newTaskDropdown.getSelectedItem();
			if (type != SimulationTaskType.NONE) {
				TaskCreationPopup popup = new TaskCreationPopup(type);
				popup.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent e) {
						popup.getTask().ifPresent(task -> processNewTask(task));
					}
				});
				popup.setLocation(WINDOW_WIDTH, LOGGER_HEIGHT);
				popup.setVisible(true);	
			}
		});
		controlButtonContainer.add(newTaskDropdown);
		
		startButton = new JButton("Start simulation");
		startButton.addActionListener(e -> {
			SimulatorState.startSimulation(500, tasks, this, networkViewer);
			stopButton.setEnabled(true);
			startButton.setEnabled(false);
		});
		controlButtonContainer.add(startButton);
		
		stopButton = new JButton("Stop simulation");
		stopButton.setEnabled(false);
		stopButton.addActionListener(e -> {
			SimulatorState.stopSimulation();
			startButton.setEnabled(true);
			stopButton.setEnabled(false);
		});
		controlButtonContainer.add(stopButton);
		
		JButton t = new JButton("Magic");
		t.addActionListener(e -> {
			Map<State, Integer> counters = new HashMap<>();
			for (int rid : SimulatorState.getReservedIds()) {
				for (ASConnection conn : SimulatorState.getRouter(rid).getAllConnections()) {
					counters.put(conn.getCurrentState(), counters.getOrDefault(conn.getCurrentState(),0)+1);
				}
			}
			
			for (State s : State.values()) {
				System.out.println(s + " " + counters.getOrDefault(s, 0));
			}
		});
		controlButtonContainer.add(t);

		pane.add(controlButtonContainer);
	}
	
	private Map<LogMessageType, Boolean> logFilters = new EnumMap<>(LogMessageType.class);
	
	private void buildLogger() {
		for (LogMessageType lmt : LogMessageType.values()) {
			logFilters.put(lmt, true);
		}
		JFrame logWindow = new JFrame("Log");
		logWindow.setSize(LOGGER_WIDTH, LOGGER_HEIGHT);
		logWindow.setLocation(WINDOW_WIDTH, 0);
		logWindow.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		logWindow.getContentPane().setLayout(new BorderLayout());
		
		JTextArea logArea = new JTextArea();
		logArea.setEditable(false);
		logWindow.add(new JScrollPane(logArea), BorderLayout.CENTER);
		
		Logger.setLogHandler(m -> {
			if (!logFilters.getOrDefault(m.type, false)) {
				return;
			}
			logArea.append(m.toString());
			logArea.setCaretPosition(logArea.getDocument().getLength());
			logWindow.validate();
		});
		
		JPanel buttons = new JPanel();
		for (LogMessageType t : LogMessageType.values()) {
			logFilters.put(t, true);
			buttons.add(getFilterToggleButton(t));
		}
		logWindow.add(buttons, BorderLayout.SOUTH);
		
		logWindow.setVisible(true);
	}
	
	private JButton getFilterToggleButton(LogMessageType type) {
		Color enabled = new Color(0, 128, 0);
		Color disabled = new Color(200, 30, 30);
		JButton button = new JButton(type
				+ (logFilters.getOrDefault(type, false) ? " visible" : " hidden"));
		button.setForeground(logFilters.get(type) ? enabled : disabled);
		button.addActionListener(e -> {
			logFilters.put(type, !logFilters.get(type));
			button.setText(type
					+ (logFilters.get(type) ? " visible" : " hidden"));
			button.setForeground(logFilters.get(type) ? enabled : disabled);
		});
		
		return button;
	}
	
	public synchronized void refreshRouterList() {
		routingTableSelector.removeAllItems();
		SimulatorState.getReservedIds().forEach(id -> routingTableSelector.addItem(id));
	}
	
	private JFrame routingTableWindow;
	private void showRoutingTable(int routerId) {
		if (routingTableWindow != null) {
			routingTableWindow.dispose();
		}
		BGPRouter r = SimulatorState.getRouter(routerId);
		
		String[] columns = {"Subnet", "First hop", "Path length"};
		
		List<SubnetNode> info = r.getRoutingEngine().getRoutingTable();
		Object[][] data = new Object[info.size()][3];
		for (int i = 0; i < info.size(); i++) {
			SubnetNode n = info.get(i);
			data[i][0] = n.getSubnet().toString();
			data[i][1] = n.getFirstHop();
			data[i][2] = n.getLength();
		}
		
		JTable table = new JTable(data, columns);
		table.setAutoCreateRowSorter(true);
		JScrollPane pane = new JScrollPane(table);
		
		routingTableWindow = new JFrame("Routing table for router " + routerId);
		routingTableWindow.add(pane);
		routingTableWindow.setLocation(WINDOW_WIDTH, LOGGER_HEIGHT);
		routingTableWindow.pack();
		
		routingTableWindow.setVisible(true);
	}
	
	private final List<SimulationTask> tasks = new ArrayList<>();
	
	private void processNewTask(SimulationTask task) {
		switch (SimulatorState.getSimulationState()) {
		case ERROR:
			JOptionPane.showMessageDialog(this, "Error in simulation, task not executed");
			return;
		case FINISHED:
			JOptionPane.showMessageDialog(this, "Simulation finished, task not executed");
			return;
		case NOT_STARTED:
			addTaskPanel(task);
			tasks.add(task);
			break;
		case PAUSED:
			return;
		case STARTED:
			addTaskPanel(task);
			tasks.add(task);
			SimulatorState.runTaskNow(task);
			if (task.getState() == TaskState.FAILED) {
				JOptionPane.showMessageDialog(this, "Task failed");
			}
			break;
		
		}
	}
	
	private void addTaskPanel(SimulationTask task) {
		int index = (int) tasks.stream()
				.map(t -> t.getDelay())
				.filter(delay -> delay < task.getDelay())
				.count();
		
		taskContainer.add(new TaskPanel(task), index);
		taskContainer.invalidate();
	}
	
	private class TaskPanel extends JPanel {
		private JLabel info;
		private JButton cancelButton;
		private String prefix, suffix;
		
		public TaskPanel(SimulationTask task) {
			super();
			
			prefix = "<html><b>" + task.getType().toString() + "</b> (" + task.getDelay() + " ms)<br>Status: ";
			suffix = "</html>";
			info = new JLabel(prefix + task.getState() + suffix);
			cancelButton = new JButton("Cancel ");
			cancelButton.addActionListener(e -> {
				cancelButton.setEnabled(false);
				task.cancelTask();
			});

			add(info);
			add(cancelButton);
			
			task.addStateChangeListener(() -> {
				updateStatus(task.getState());
				if (task.getState() != TaskState.WAITING) {
					cancelButton.setEnabled(false);
				}
			});
			
			this.setSize(SIDEPANEL_WIDTH, 200);
		}
		
		public void updateStatus(TaskState newState) {
			info.setText(prefix + newState + suffix);
		}
	}
	

	public static void main(String[] args) {
		MainView v = new MainView();
		v.setVisible(true);
    }
}
