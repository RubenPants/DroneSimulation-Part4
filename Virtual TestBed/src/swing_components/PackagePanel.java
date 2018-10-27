package swing_components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import control.AppManager;
import control.WorldManager;
import entities.Drone;

public class PackagePanel extends JPanel{
	
	private JTable packageTable;
	private PackageTableModel packagesModel;
	private JPopupMenu popupCubeMenu;
	
	private PackageAdderPanel packAdder;
	
	private WorldManager manager;
		
	public PackagePanel() {

		packagesModel = new PackageTableModel();
		packageTable = new JTable(packagesModel);
		packageTable.getTableHeader().setReorderingAllowed(false);
		packageTable.getColumnModel().getColumn(0).setPreferredWidth(30);
		packageTable.getColumnModel().getColumn(2).setPreferredWidth(30);
		packageTable.getColumnModel().getColumn(4).setPreferredWidth(30);

		packageTable.addComponentListener(new ComponentAdapter() {
		    public void componentResized(ComponentEvent e) {
		    	packageTable.scrollRectToVisible(packageTable.getCellRect(packageTable.getRowCount()-1, 0, true));
		    }
		});
		
		packageTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
	        public void valueChanged(ListSelectionEvent e) {
	            int selectedRow = packageTable.getSelectedRow();
	            if(selectedRow==-1) return;
	            Drone drone = manager.getPackages().get(selectedRow).carriedBy;
	            if(drone==null) return;
	            int[] drones = AppManager.getFocussedDrones();
	            drones[0] = drone.getID();
	            AppManager.setFocussedDrones(drones);
	        }
	    });
		
		packageTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int row = packageTable.rowAtPoint(e.getPoint());
				packageTable.getSelectionModel().setSelectionInterval(row, row);

				if(e.getButton() == MouseEvent.BUTTON3) {
					popupCubeMenu.show((Component) e.getSource(),e.getX(),e.getY());
				}
			}
		});

		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				packageTable.getSelectionModel().clearSelection();
			}
		});
		JScrollPane scrollPane2 = new JScrollPane(packageTable);
		
		scrollPane2.setPreferredSize(new Dimension(450,350));

		JLabel errorLabel = new JLabel();
		errorLabel.setPreferredSize(new Dimension(280, 30));
		errorLabel.setMinimumSize(new Dimension(280, 30));
		
		packAdder = new PackageAdderPanel();
		packAdder.addPackageListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					manager.addNewPackage(packAdder.getPackage());
					errorLabel.setText("");
				} catch (IllegalArgumentException e1) {
					errorLabel.setText(e1.getMessage());
				}
				addPackageToTable();
			}
		});
		packAdder.addRandomListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				manager.createRandomRequest();
				addPackageToTable();
			}
		});
		
		packAdder.toggleAutomaticListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				packAdder.toggleAutomatic(manager.toggleAutomaticAdder());
			}
		});
		
		setLayout(new GridBagLayout());
		GridBagConstraints gc = new GridBagConstraints();
		
		/////////////////////////////////////////
		gc.gridx = 0;
		gc.gridy = 0;
		gc.weighty = 1;
		gc.anchor = GridBagConstraints.CENTER;
		add(scrollPane2, gc);		
		
		//////////////////////////////////////////
		gc.gridy++;
		gc.anchor = GridBagConstraints.CENTER;
		add(packAdder, gc);
		
		//////////////////////////////////////////
		gc.gridy++;
		gc.anchor = GridBagConstraints.CENTER;
		add(errorLabel, gc);
	}

	
	
	public void setWorldManager(WorldManager manager) {
		this.manager = manager;
		packagesModel.setWorldManager(manager);
		packAdder.addManager(manager);
	}
	
	public void refreshTables() {
		packagesModel.fireTableRowsUpdated(0, packagesModel.getRowCount()-1);
		packAdder.fireData();
	}
	
	public void fireData() {
		packAdder.fireData();
	}
	
	public void addPackageToTable() {
		int row = packageTable.getSelectedRow();
		packagesModel.fireTableDataChanged();
		if(row!=-1)
			packageTable.setRowSelectionInterval(row, row);
		

	}
}