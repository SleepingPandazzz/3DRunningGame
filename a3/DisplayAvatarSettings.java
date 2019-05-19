package a3;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.TitledBorder;

public class DisplayAvatarSettings extends DisplaySettingsDialog {

	private int pickCharacter = -1;

	private JRadioButton pickChar1;
	private JRadioButton pickChar2;

	private MyGame game;

	private Object frame;

	public DisplayAvatarSettings(GraphicsDevice theDevice, MyGame g) {
		super(theDevice);
		this.game = g;
		setTitle("Select Options");
		setSize(600, 200);
		doCharacterLayout();
	}

	private void doCharacterLayout() {
		JPanel screenPanel = new JPanel();
		screenPanel.setBorder(new TitledBorder("Select Character Avatar Models:  "));
		Box screenButtonBox = new Box(BoxLayout.Y_AXIS);

		pickChar1 = new JRadioButton("Character1", true);
		pickChar2 = new JRadioButton("Character2", false);

		ButtonGroup screenModeButtonGroup = new ButtonGroup();
		screenModeButtonGroup.add(pickChar1);
		screenModeButtonGroup.add(pickChar2);

		screenButtonBox.add(pickChar1);
		screenButtonBox.add(pickChar2);

		screenPanel.add(screenButtonBox);

		this.add(screenPanel, "Center");
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == "OK") {
			if (pickChar1.isSelected()) {
				// character model 1 is selected
				this.pickCharacter = 1;
				game.setPlayerModel(1);
			} else {
				this.pickCharacter = 2;
				game.setPlayerModel(2);
			}
		}
		setVisible(false);
	}
}
