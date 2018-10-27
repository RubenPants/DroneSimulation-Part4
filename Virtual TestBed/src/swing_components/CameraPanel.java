package swing_components;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.Border;

import control.AppManager;
import control.WorldManager;
import entities.Drone;

public class CameraPanel extends JPanel {
	
	private JComboBox<Drone> topLeft;
	private JComboBox<Drone> topRight;
	private JComboBox<Drone> bottomLeft;
	private JComboBox<Drone> bottomRight;
	
	private WorldManager manager;
	
	public CameraPanel() {
		Dimension dim = getPreferredSize();
		dim.width = 400;
		dim.height = 200;
		setPreferredSize(dim);
		
		topLeft = new JComboBox<>();
		topLeft.setMaximumSize(topLeft.getPreferredSize());
		topRight = new JComboBox<>();
		topRight.setMaximumSize(topRight.getPreferredSize());
		bottomLeft = new JComboBox<>();
		bottomLeft.setMaximumSize(bottomLeft.getPreferredSize());
		bottomRight = new JComboBox<>();
		bottomRight.setMaximumSize(bottomRight.getPreferredSize());
		
		Border innerBorder = BorderFactory.createTitledBorder("CameraViews");
		Border outerBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
		setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
		
		setLayout(new GridBagLayout());
		
		GridBagConstraints gc = new GridBagConstraints();
		
		//////////////////////////////////////////////////////////////
		gc.gridy = 0;
		gc.gridx = 0;
		gc.weighty = 1;
		gc.ipadx = 30;
		gc.ipady = 30;
		gc.anchor = GridBagConstraints.CENTER;
		add(topLeft, gc);
		//////////////////////////////////////////////////////////////
		gc.gridx++;
		add(topRight, gc);
		//////////////////////////////////////////////////////////////
		gc.gridy++;
		gc.gridx = 0;
		add(bottomLeft, gc);
		//////////////////////////////////////////////////////////////
		gc.gridx = 1;
		add(bottomRight, gc);
	}
	
	public void setManager(WorldManager manager) {
		this.manager = manager;
		fireData();
		ActionListener listener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AppManager.setFocussedDrones(getValues());
			}
		};
		
		topLeft.addActionListener(listener);
		topRight.addActionListener(listener);
		bottomLeft.addActionListener(listener);
		bottomRight.addActionListener(listener);
	}
	
	public void fireData() {
		if(topLeft.getItemCount()<manager.getDrones().size()) {
			topLeft.removeAllItems();
			topRight.removeAllItems();
			bottomLeft.removeAllItems();
			bottomRight.removeAllItems();
			for(Drone drone: manager.getDrones()) {
				topLeft.addItem(drone);
				topRight.addItem(drone);
				bottomLeft.addItem(drone);
				bottomRight.addItem(drone);
			}
		}
		int[] drones = AppManager.getFocussedDrones();
		topLeft.setSelectedIndex(drones[0]);
		topRight.setSelectedIndex(drones[1]);
		bottomLeft.setSelectedIndex(drones[2]);
		bottomRight.setSelectedIndex(drones[3]);
	}
	
	private int[] getValues() {
		return new int[] {topLeft.getSelectedIndex(), topRight.getSelectedIndex(),
				bottomLeft.getSelectedIndex(), bottomRight.getSelectedIndex()};
	}
}
