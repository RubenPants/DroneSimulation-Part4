package swing_components;

import java.awt.Color;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import control.WorldManager;
import entities.Airport;

public class PackageAdderPanel extends JPanel {

	private JLabel label;
	private JComboBox<String> fromA;
	private JComboBox<String> fromG;
	private JComboBox<String> toA;
	private JComboBox<String> toG;
	private JButton btn;
	private JButton randomBtn;
	private JButton automaticBtn;
	
	private WorldManager manager;
	
	public PackageAdderPanel(){
		setLayout(new GridBagLayout());
		
		label = new JLabel("Request a new package: ");
		fromA = new JComboBox<>();
		fromG = new JComboBox<>();
		fromG.addItem("0");
		fromG.addItem("1");
		toA = new JComboBox<>();
		toG = new JComboBox<>();
		toG.addItem("0");
		toG.addItem("1");
		
		btn = new JButton("Request");
		randomBtn = new JButton("Random Package");
		automaticBtn = new JButton("Automatic ON");
		
		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weighty = 1;
		c.weightx = 0.5;
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 0;
		add(label, c);
		////////////////////////////////////////////////////////////////////////
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.5;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 1;
		add(new JLabel("From Airport: "), c);
		////////////////////////////////////////////////////////////////////////

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.5;
		c.gridx = 1;
		c.gridy = 1;
		add(fromA, c);
		////////////////////////////////////////////////////////////////////////
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 2;
		add(new JLabel("From Gate: "), c);
		////////////////////////////////////////////////////////////////////////

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.5;
		c.gridx = 1;
		c.gridy = 2;
		add(fromG, c);		
		
		////////////////////////////////////////////////////////////////////////
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 3;
		add(new JLabel("To Airport: "), c);
		////////////////////////////////////////////////////////////////////////

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.5;
		c.gridx = 1;
		c.gridy = 3;
		add(toA, c);
		////////////////////////////////////////////////////////////////////////
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 4;
		add(new JLabel("To Gate: "), c);
		////////////////////////////////////////////////////////////////////////

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.5;
		c.gridx = 1;
		c.gridy = 4;
		add(toG, c);
		////////////////////////////////////////////////////////////////////////
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.0;
		c.gridx = 1;
		c.gridy = 5;
		add(btn, c);
		////////////////////////////////////////////////////////////////////////
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.0;
		c.gridx = 1;
		c.gridy = 6;
		add(randomBtn, c);
		////////////////////////////////////////////////////////////////////////
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.0;
		c.gridx = 1;
		c.gridy = 7;
		add(automaticBtn, c);
	}
	
	public entities.Package getPackage(){
        String fromGtext = (String)fromG.getSelectedItem();
        String toGtext = (String)toG.getSelectedItem();
        int fA = manager.getAirport(fromA.getSelectedIndex()).getId();
        int fG = 0;
        int tA = manager.getAirport(toA.getSelectedIndex()).getId();
        int tG = 0;
		try {
			fG = Integer.parseInt(fromGtext);
			tG = Integer.parseInt(toGtext);
		} catch (NumberFormatException e) {
		}
        return new entities.Package(fA, fG, tA, tG);
	}
	
	public void addPackageListener(ActionListener listener){
		btn.addActionListener(listener);
	}
	
	public void addRandomListener(ActionListener listener) {
		randomBtn.addActionListener(listener);
	}
	
	public void toggleAutomaticListener(ActionListener listener) {
		automaticBtn.addActionListener(listener);
	}
	
	public void toggleAutomatic(boolean flag) {
		automaticBtn.setText((flag ? "Automatic OFF" : "Automatic ON"));
	}
	
	public void addManager(WorldManager manager) {
		this.manager = manager;
	}
	
	public void fireData() {
		if(fromA.getItemCount()<manager.getAirports().size()) {
			fromA.removeAllItems();
			toA.removeAllItems();
			for(Airport port: manager.getAirports()) {
				fromA.addItem(port.getName());
				toA.addItem(port.getName());
			}
		}
	}
}