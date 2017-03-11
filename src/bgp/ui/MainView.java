package bgp.ui;

import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;

import bgp.simulation.SimulatorState;
import bgp.simulation.tasks.SimulationTask;
import bgp.simulation.tasks.SimulationTask.SimulationTaskType;

public class MainView extends JFrame {
	

	private static final long serialVersionUID = 1L;
	
	private static final int WINDOW_WIDTH = 1200;
	private static final int WINDOW_HEIGHT = 900;
	private static final int BUTTON_CONTAINER_HEIGHT = 100;
	
	private final NetworkViewer networkViewer;
	private Viewer viewerComponent;
	
	private JPanel taskContainer;
	
	
	private JPanel controlButtonContainer;
	private JComboBox newTaskDropdown;
	private JButton startButton;
	private JButton stopButton;
	
	public MainView() {
		super();
		this.networkViewer = new NetworkViewer();
		buildUI();
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
		taskContainer.setSize(WINDOW_WIDTH - WINDOW_HEIGHT, WINDOW_HEIGHT - BUTTON_CONTAINER_HEIGHT);
		pane.add(taskContainer);
		
		// Control buttons
		controlButtonContainer = new JPanel();
		controlButtonContainer.setSize(WINDOW_WIDTH - WINDOW_HEIGHT, BUTTON_CONTAINER_HEIGHT);
		controlButtonContainer.setLocation(WINDOW_HEIGHT, WINDOW_HEIGHT - BUTTON_CONTAINER_HEIGHT);
		
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
				
				popup.setVisible(true);	
			}
		});
		controlButtonContainer.add(newTaskDropdown);
		
		startButton = new JButton("Start simulation");
		startButton.addActionListener(e -> {
			SimulatorState.startSimulation(500, tasks, networkViewer);
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

		pane.add(controlButtonContainer);
		
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
			tasks.add(task);
			break;
		case PAUSED:
			return;
		case STARTED:
			task.run();
			break;
		
		}
	}

	public static void main(String[] args) {
		MainView v = new MainView();
		v.setVisible(true);
    }
}
