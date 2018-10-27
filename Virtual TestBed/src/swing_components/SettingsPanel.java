package swing_components;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class SettingsPanel extends JPanel{
	
	private JLabel label;
	private JSlider slider;
	private JTextField field;
	
	private float scale;
	
	private ChangeListener changeListener;
	
	public SettingsPanel(String name, int orientation, int min, int max, int value, float scale) {
		setLayout(new GridBagLayout());
		
		this.scale = scale;
		
		label = new JLabel(name);
		label.setMinimumSize(new Dimension(120, 20));
		label.setPreferredSize(new Dimension(120, 20));
		label.setMaximumSize(new Dimension(120, 20));
		
	    slider = new JSlider(orientation, min, max, value);
	    slider.setMajorTickSpacing(max/2);
	    slider.setMinorTickSpacing(max/10);
	    slider.setPaintTicks(true);
	    slider.setPaintLabels(true);
	    slider.addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
		        JSlider slider = (JSlider) e.getSource();
		        int value = slider.getValue();
				field.setText(Float.toString(value));
				if (changeListener != null)
					changeListener.stateChanged(null);;
			}
	    });
	    
		field = new JTextField(Float.toString(value), 10);
		field.addKeyListener(new KeyAdapter(){
		    @Override
		    public void keyReleased(KeyEvent ke) {
		    	String typed = field.getText();
		        float value = 100;
				try {
					value = Float.parseFloat(typed);
				} catch (NumberFormatException e) {
					System.out.println("not a float");
				}
		        slider.setValue((int) (value));
		   }
		});

		GridBagConstraints gc = new GridBagConstraints();
		
		//////////////////////////////////////////////////////////////
		gc.gridy = 0;
		gc.gridx = 0;
		gc.weightx = 1;
		gc.anchor = GridBagConstraints.CENTER;
		add(label, gc);
		//////////////////////////////////////////////////////////////
		gc.gridx++;
		gc.weightx = 5;
		add(slider, gc);
		//////////////////////////////////////////////////////////////
		gc.gridx++;
		gc.weightx = 1;
		add(field, gc);
		
	}
	
	public float getValue() {
		return slider.getValue()*scale;	
	}
	
	public void addChangeListener(ChangeListener listener) {
		this.changeListener = listener;
	}
}
