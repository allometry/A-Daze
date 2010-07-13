import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.quirlion.script.Constants;
import com.quirlion.script.Script;
import com.quirlion.script.types.Camera;
import com.quirlion.script.types.Interface;
import com.quirlion.script.types.InventoryItem;
import com.quirlion.script.types.NPC;

public class ADaze extends Script {
	private boolean isUsingAlchemy = false, isCameraRotating = false, isStopping = false;
	private int curseCasts = 0, monkZamorakID = 189, spellInterfaceID = 0, startingMagicLevel = 0, startingMagicXP = 0;
	private long startTime = 0, timeout = 0;
	
	//private Antiban antiban;
	private InventoryItem alchemyInventoryItem;
	private Image cursorImage, sumImage, timeImage, wandImage;
	private ImageObserver observer;
	private Interface highAlchemyInterface = null, spellInterface = null;
	private NPC monkZamorak = null;
	//private Thread antibanThread;
	
	@Override
	public void onStart() {
		try {
			cursorImage = ImageIO.read(new URL("http://scripts.allometry.com/app/webroot/img/cursors/cursor-01.png"));
			sumImage = ImageIO.read(new URL("http://scripts.allometry.com/icons/sum.png"));
			timeImage = ImageIO.read(new URL("http://scripts.allometry.com/icons/time.png"));
			wandImage = ImageIO.read(new URL("http://scripts.allometry.com/icons/wand.png"));
		} catch (IOException e) {
			logStackTrace(e);
		} 
		
		startingMagicLevel = skills.getCurrentSkillLevel(Constants.STAT_MAGIC);
		startingMagicXP = skills.getCurrentSkillXP(Constants.STAT_MAGIC);
		
		ADazeGUI aDazeGUI = new ADazeGUI();
		
		if(startingMagicLevel < 3) {
			log("You do not have a magic skill level of 3 or greater. You can't use this script yet! Exiting...");
			stopScript();
		}
		
		if(startingMagicLevel < 11) {
			aDazeGUI.weakenRadioButton.setEnabled(false);
			aDazeGUI.curseRadioButton.setEnabled(false);
			aDazeGUI.vulnerabilityRadioButton.setEnabled(false);
			aDazeGUI.enfeebleRadioButton.setEnabled(false);
			aDazeGUI.stunRadioButton.setEnabled(false);
		}
		
		if(startingMagicLevel < 19) {
			aDazeGUI.curseRadioButton.setEnabled(false);
			aDazeGUI.vulnerabilityRadioButton.setEnabled(false);
			aDazeGUI.enfeebleRadioButton.setEnabled(false);
			aDazeGUI.stunRadioButton.setEnabled(false);
		}
		
		if(startingMagicLevel < 66) {
			aDazeGUI.vulnerabilityRadioButton.setEnabled(false);
			aDazeGUI.enfeebleRadioButton.setEnabled(false);
			aDazeGUI.stunRadioButton.setEnabled(false);
		}
		
		if(startingMagicLevel < 73) {
			aDazeGUI.enfeebleRadioButton.setEnabled(false);
			aDazeGUI.stunRadioButton.setEnabled(false);
		}
		
		if(startingMagicLevel < 80) {
			aDazeGUI.stunRadioButton.setEnabled(false);
		}
		
		aDazeGUI.inventoryItemsComboxBox.setEnabled(false);
		for(InventoryItem item : inventory.getItemArray()) {
			aDazeGUI.inventoryItemsComboxBox.addItem(new Item(item));
		}
		
		aDazeGUI.setVisible(true);
		while(aDazeGUI.isVisible()) { wait(1); };
		
		switch(aDazeGUI.selectedSpell) {
			case ADazeGUI.CONFUSE:
				spellInterfaceID = Constants.SPELL_CONFUSE;
			break;
			
			case ADazeGUI.WEAKEN:
				spellInterfaceID = Constants.SPELL_WEAKEN;
			break;
			
			case ADazeGUI.CURSE:
				spellInterfaceID = Constants.SPELL_CURSE;
			break;
			
			case ADazeGUI.VULNERABILITY:
				spellInterfaceID = Constants.SPELL_VULNERABILITY;
			break;
			
			case ADazeGUI.ENFEEBLE:
				spellInterfaceID = Constants.SPELL_ENFEEBLE;
			break;
			
			case ADazeGUI.STUN:
				spellInterfaceID = Constants.SPELL_STUN;
			break;
			
			default:
				log("No spell selected, exiting...");
				stopScript();
			break;
		}
		
		if(aDazeGUI.alchemyCheckBox.isSelected()) {
			isUsingAlchemy = true;
			alchemyInventoryItem = ((Item)aDazeGUI.inventoryItemsComboxBox.getSelectedItem()).getInventoryItem();
		}
		
		aDazeGUI.dispose();
		aDazeGUI = null;
		
		//antiban = new Antiban();
		//antibanThread = new Thread(antiban);
		//antibanThread.start();
		
		startTime = System.currentTimeMillis();
	}
	
	@Override
	public int loop() {
		if(isCameraRotating) return 1;
		
		try {
			if(monkZamorak == null) {
				if(npcs.getNearestByID(monkZamorakID) != null) {
					monkZamorak = npcs.getNearestByID(monkZamorakID);
					
					return 1;
				} else {
					log("Could not find Monk, stopping script...");
					stopScript();
				}
			}
			
			if(tabs.getCurrentTab() != Constants.TAB_MAGIC) tabs.openTab(Constants.TAB_MAGIC);
			
			highAlchemyInterface = interfaces.get(Constants.INTERFACE_TAB_MAGIC, Constants.SPELL_HIGH_LEVEL_ALCHEMY);
			spellInterface = interfaces.get(Constants.INTERFACE_TAB_MAGIC, spellInterfaceID);
			
			while(!isMouseInArea(spellInterface.getArea()))
				input.moveMouse(spellInterface.getRealX() + 8, spellInterface.getRealY() + 8);
			
			if(spellInterface.click()) {
				monkZamorak.hover();
				
				int magicXP = skills.getCurrentSkillXP(Constants.STAT_MAGIC);
				
				if(monkZamorak.click()) {
					if(isUsingAlchemy) {
						//TODO: Add in check to make sure stack sizes are lareg enough to cast high alch.
						
						while(!isMouseInArea(highAlchemyInterface.getArea()))
							input.moveMouse(highAlchemyInterface.getRealX() + 8, highAlchemyInterface.getRealY() + 8);
						
						while(tabs.getCurrentTab() != Constants.TAB_INVENTORY)
							highAlchemyInterface.click();
						
						int x = inventory.findItem(alchemyInventoryItem.getID()).getLocation().x;
						int y = inventory.findItem(alchemyInventoryItem.getID()).getLocation().y;
						Rectangle item = new Rectangle(x, y, 16, 16);
						
						while(!isMouseInArea(item))
							input.moveMouse(x + 8, y + 8);
						
						while(tabs.getCurrentTab() != Constants.TAB_MAGIC) {
							if(inventory.findItem(alchemyInventoryItem.getID()).click()) break;
							wait(1000);
						}
					} else {
						timeout = System.currentTimeMillis() + 3000;
						while(magicXP == skills.getCurrentSkillXP(Constants.STAT_MAGIC) && System.currentTimeMillis() < timeout) wait(1);
					}
				}
				
				if(magicXP != skills.getCurrentSkillXP(Constants.STAT_MAGIC)) curseCasts++;
				
				while(players.getCurrent().getAnimation() > 0) {
					wait(1);
				}
			}
			
		} catch(Exception e) {
			logStackTrace(e);
			stopScript();
		}
		
		return 1;
	}
	
	@Override
	public void onStop() {
		
	}
	
	@Override
	public void paint(Graphics g2) {
		if(!players.getCurrent().isLoggedIn() || players.getCurrent().isInLobby() || startTime == 0) return ;
		
		Graphics2D g = (Graphics2D)g2;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		//Draw Mouse
		g.drawImage(cursorImage, input.getBotMousePosition().x - 16, input.getBotMousePosition().y - 16, observer);
		
		//Draw Scoreboard
		NumberFormat number = NumberFormat.getIntegerInstance(Locale.US);
		int sum = skills.getCurrentSkillXP(Constants.STAT_MAGIC) - startingMagicXP;
		
		Scoreboard leftScoreboard = new Scoreboard(Scoreboard.TOP_LEFT, 128, 5);
		Scoreboard rightScoreboard = new Scoreboard(Scoreboard.TOP_RIGHT, 128, 5);
		
		leftScoreboard.addWidget(new ScoreboardWidget(wandImage, number.format(curseCasts)));
		
		rightScoreboard.addWidget(new ScoreboardWidget(timeImage, millisToClock(System.currentTimeMillis() - startTime)));
		
		int lvlDif = skills.getCurrentSkillLevel(Constants.STAT_MAGIC) - startingMagicLevel;
		if(lvlDif == 0)
			rightScoreboard.addWidget(new ScoreboardWidget(sumImage, number.format(sum)));
		else
			rightScoreboard.addWidget(new ScoreboardWidget(sumImage, number.format(sum) + " " + "(+" + lvlDif + ")"));
		
		leftScoreboard.drawScoreboard(g);
		rightScoreboard.drawScoreboard(g);
		
		RoundRectangle2D progressBackground = new RoundRectangle2D.Float(
				Scoreboard.gameCanvasRight - 128,
				rightScoreboard.getHeight() + 30,
				128,
				8,
				5,
				5);
		
		Double percentToWidth = Math.floor(128 * (skills.getPercentToNextLevel(Constants.STAT_MAGIC) / 100));
		
		RoundRectangle2D progressBar = new RoundRectangle2D.Float(
				Scoreboard.gameCanvasRight - 128,
				rightScoreboard.getHeight() + 31,
				percentToWidth.intValue(),
				7,
				5,
				5);
		
		g.setColor(new Color(0, 0, 0, 127));
		g.draw(progressBackground);
		
		g.setColor(new Color(0, 0, 200, 191));
		g.fill(progressBar);
	}
	
	public boolean isMouseInArea(Rectangle inArea) {
		int x = input.getBotMousePosition().x, y = input.getBotMousePosition().y;
		
		return (x > inArea.getX() && x < (inArea.getX() + inArea.getWidth()) && y > inArea.getY() && y < (inArea.getY() + inArea.getHeight()));
	}
	
	private class Antiban implements Runnable {
		@Override
		public void run() {
			while(!isStopping) {
				switch(random(1, 11) % 2) {
				case 1:					
					if(cam.getCameraAngle() >= 45 && cam.getCameraAngle() <= 122) {
						//spin right
						int randomAngle = random(122, 290) - cam.getCameraAngle();
						int deltaAngle = cam.getCameraAngle() - randomAngle;
						
						isCameraRotating = true;
						cam.spinCamera(deltaAngle, Camera.LEFT);
						
						isCameraRotating = false;
					}
					
					if(cam.getCameraAngle() >= 290 && cam.getCameraAngle() <= 122) {
						//spin left
						int randomAngle = random(45, 122);
						int deltaAngle = cam.getCameraAngle() - randomAngle;
						
						isCameraRotating = true;
						cam.spinCamera(deltaAngle, Camera.RIGHT);
						
						isCameraRotating = false;
					}
					
					long c1Timeout = System.currentTimeMillis() + random(30000, 60000);
					while(System.currentTimeMillis() < c1Timeout && !isStopping) {}
					
					break;

				default:					
					long c2Timeout = System.currentTimeMillis() + random(30000, 60000);
					while(System.currentTimeMillis() < c2Timeout && !isStopping) {}
					
					break;
				}
			}
			
			log("Antiban: Shutting down...");
		}
	}
	
	public class Item {
		private InventoryItem inventoryItem;
		
		public Item(InventoryItem inventoryItem) {
			this.inventoryItem = inventoryItem;
		}
		
		public InventoryItem getInventoryItem() {
			return inventoryItem;
		}
		
		public String toString() {
			return inventoryItem.getName().replaceAll("<(.|\n)*?>", "");
		}
	}
	
	public class Scoreboard {
		public static final int TOP_LEFT = 1, TOP_RIGHT = 2, BOTTOM_LEFT = 3, BOTTOM_RIGHT = 4;
		public static final int gameCanvasTop = 25, gameCanvasLeft = 25, gameCanvasBottom = 309, gameCanvasRight = 487;
		
		private ImageObserver observer = null;
		
		private int scoreboardLocation, scoreboardX, scoreboardY, scoreboardWidth, scoreboardHeight, scoreboardArc;
		
		private ArrayList<ScoreboardWidget> widgets = new ArrayList<ScoreboardWidget>();
		
		public Scoreboard(int scoreboardLocation, int width, int arc) {
			this.scoreboardLocation = scoreboardLocation;
			scoreboardHeight = 10;
			scoreboardWidth = width;
			scoreboardArc = arc;
			
			switch(scoreboardLocation) {
				case 1:
					scoreboardX = gameCanvasLeft;
					scoreboardY = gameCanvasTop;
				break;
				
				case 2:
					scoreboardX = gameCanvasRight - scoreboardWidth;
					scoreboardY = gameCanvasTop;
				break;
				
				case 3:
					scoreboardX = gameCanvasLeft;
				break;
				
				case 4:
					scoreboardX = gameCanvasRight - scoreboardWidth;
				break;
			}
		}
		
		public void addWidget(ScoreboardWidget widget) {
			widgets.add(widget);
		}
		
		public boolean drawScoreboard(Graphics2D g) {
			try {
				for (ScoreboardWidget widget : widgets) {
					scoreboardHeight += widget.getWidgetImage().getHeight(observer) + 4;
				}
				
				if(scoreboardLocation == 3 || scoreboardLocation == 4) {
					scoreboardY = gameCanvasBottom - scoreboardHeight;
				}
				
				RoundRectangle2D scoreboard = new RoundRectangle2D.Float(
					scoreboardX,
					scoreboardY,
					scoreboardWidth,
					scoreboardHeight,
					scoreboardArc,
					scoreboardArc
				);
				
				g.setColor(new Color(0, 0, 0, 127));
				g.fill(scoreboard);
				
				int x = scoreboardX + 5;
				int y = scoreboardY + 5;
				for (ScoreboardWidget widget : widgets) {
					widget.drawWidget(g, x, y);
					y += widget.getWidgetImage().getHeight(observer) + 4;
				}
				
				return true;
			} catch(Exception e) {}
			
			return false;
		}
		
		public int getHeight() {
			return scoreboardHeight;
		}
	}
	
	public class ScoreboardWidget {
		private ImageObserver observer = null;
		private Image widgetImage;
		private String widgetText;
		
		public ScoreboardWidget(Image widgetImage, String widgetText) {
			this.widgetImage = widgetImage;
			this.widgetText = widgetText;
		}
		
		public Image getWidgetImage() {
			return widgetImage;
		}
		
		public String getWidgetText() {
			return widgetText;
		}
		
		public void drawWidget(Graphics2D g, int x, int y) {
			g.setColor(Color.white);
			g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
			
			g.drawImage(widgetImage, x, y, observer);
			g.drawString(widgetText, x + widgetImage.getWidth(observer) + 4, y + 12);
		}
	}
	
	public class ADazeGUI extends JFrame {
		public static final int CONFUSE = 1, WEAKEN = 2, CURSE = 3, VULNERABILITY = 4, ENFEEBLE = 5, STUN = 6;
		public int selectedSpell;
		
		private static final long serialVersionUID = 6280429693064089142L;
		
		public ADazeGUI() {
			initComponents();
		}

		private void confuseRadioButtonStateChanged(ChangeEvent e) {
			if(!startButton.isEnabled()) startButton.setEnabled(true);
		}

		private void weakenRadioButtonStateChanged(ChangeEvent e) {
			if(!startButton.isEnabled()) startButton.setEnabled(true);
		}

		private void curseRadioButtonStateChanged(ChangeEvent e) {
			if(!startButton.isEnabled()) startButton.setEnabled(true);
		}
		
		private void vulnerabilityRadioButtonStateChanged(ChangeEvent e) {
			if(!startButton.isEnabled()) startButton.setEnabled(true);
		}

		private void enfeebleRadioButtonStateChanged(ChangeEvent e) {
			if(!startButton.isEnabled()) startButton.setEnabled(true);
		}
		
		private void stunRadioButtonStateChanged(ChangeEvent e) {
			if(!startButton.isEnabled()) startButton.setEnabled(true);
		}
		
		private void alchemyCheckBoxStateChanged(ChangeEvent e) {
			if(alchemyCheckBox.isSelected())
				inventoryItemsComboxBox.setEnabled(true);
			else
				inventoryItemsComboxBox.setEnabled(false);
		}

		private void startButtonActionPerformed(ActionEvent e) {
			if(confuseRadioButton.isSelected()) selectedSpell = CONFUSE;
			if(weakenRadioButton.isSelected()) selectedSpell = WEAKEN;
			if(curseRadioButton.isSelected()) selectedSpell = CURSE;
			if(stunRadioButton.isSelected()) selectedSpell = STUN;
			if(enfeebleRadioButton.isSelected()) selectedSpell = ENFEEBLE;
			if(vulnerabilityRadioButton.isSelected()) selectedSpell = VULNERABILITY;
			
			setVisible(false);
		}

		private void initComponents() {
			aDazeConfigurationLabel = new JLabel();
			topSeparator = new JSeparator();
			confuseRadioButton = new JRadioButton();
			weakenRadioButton = new JRadioButton();
			curseRadioButton = new JRadioButton();
			vulnerabilityRadioButton = new JRadioButton();
			enfeebleRadioButton = new JRadioButton();
			stunRadioButton = new JRadioButton();
			middleSeparator = new JSeparator();
			alchemyCheckBox = new JCheckBox();
			inventoryItemsComboxBox = new JComboBox();
			bottomSeparator = new JSeparator();
			startButton = new JButton();
			
			setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
			setResizable(false);
			setTitle("A. Daze");
			Container contentPane = getContentPane();
			contentPane.setLayout(null);

			aDazeConfigurationLabel.setText("A. Daze Configuration");
			aDazeConfigurationLabel.setHorizontalAlignment(SwingConstants.CENTER);
			
			contentPane.add(aDazeConfigurationLabel);
			aDazeConfigurationLabel.setBounds(15, 15, 200, aDazeConfigurationLabel.getPreferredSize().height);
			
			contentPane.add(topSeparator);
			topSeparator.setBounds(5, 35, 220, topSeparator.getPreferredSize().height);
			
			confuseRadioButton.setText("Confuse (3)");
			confuseRadioButton.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					confuseRadioButtonStateChanged(e);
				}
			});
			contentPane.add(confuseRadioButton);
			confuseRadioButton.setBounds(15, 50, 200, confuseRadioButton.getPreferredSize().height);

			weakenRadioButton.setText("Weaken (11)");
			weakenRadioButton.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					weakenRadioButtonStateChanged(e);
				}
			});
			contentPane.add(weakenRadioButton);
			weakenRadioButton.setBounds(15, 75, 200, 23);

			curseRadioButton.setText("Curse (19)");
			curseRadioButton.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					curseRadioButtonStateChanged(e);
				}
			});
			contentPane.add(curseRadioButton);
			curseRadioButton.setBounds(15, 100, 200, 23);

			vulnerabilityRadioButton.setText("Vulnerability (66)");
			vulnerabilityRadioButton.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					vulnerabilityRadioButtonStateChanged(e);
				}
			});
			contentPane.add(vulnerabilityRadioButton);
			vulnerabilityRadioButton.setBounds(15, 125, 200, 23);

			enfeebleRadioButton.setText("Enfeeble (73)");
			enfeebleRadioButton.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					enfeebleRadioButtonStateChanged(e);
				}
			});
			contentPane.add(enfeebleRadioButton);
			enfeebleRadioButton.setBounds(15, 150, 200, 23);
			
			stunRadioButton.setText("Stun (80)");
			stunRadioButton.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					stunRadioButtonStateChanged(e);
				}
			});
			contentPane.add(stunRadioButton);
			stunRadioButton.setBounds(15, 175, 200, 23);
			
			contentPane.add(middleSeparator);
			middleSeparator.setBounds(5, 195, 220, 12);
			
			alchemyCheckBox.setText("Use High Alchemy");
			alchemyCheckBox.setSelected(false);
			alchemyCheckBox.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					alchemyCheckBoxStateChanged(e);
				}
			});
			contentPane.add(alchemyCheckBox);
			alchemyCheckBox.setBounds(new Rectangle(15, 220, 200, 23));
			
			contentPane.add(inventoryItemsComboxBox);
			inventoryItemsComboxBox.setBounds(new Rectangle(15, 245, 200, 23));
			
			contentPane.add(bottomSeparator);
			bottomSeparator.setBounds(5, 265, 220, 12);

			startButton.setText("Start");
			startButton.setEnabled(false);
			startButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					startButtonActionPerformed(e);
				}
			});
			contentPane.add(startButton);
			startButton.setBounds(new Rectangle(new Point(150, 285), startButton.getPreferredSize()));

			contentPane.setPreferredSize(new Dimension(230, 345));
			setSize(230, 345);
			setLocationRelativeTo(null);

			ButtonGroup spellGroup = new ButtonGroup();
			spellGroup.add(confuseRadioButton);
			spellGroup.add(weakenRadioButton);
			spellGroup.add(curseRadioButton);
			spellGroup.add(vulnerabilityRadioButton);
			spellGroup.add(enfeebleRadioButton);
			spellGroup.add(stunRadioButton);
		}
		
		private JLabel aDazeConfigurationLabel;
		private JSeparator topSeparator;
		private JRadioButton confuseRadioButton;
		private JRadioButton weakenRadioButton;
		private JRadioButton curseRadioButton;
		private JRadioButton vulnerabilityRadioButton;
		private JRadioButton enfeebleRadioButton;
		private JRadioButton stunRadioButton;
		private JSeparator middleSeparator;
		private JCheckBox alchemyCheckBox;
		private JComboBox inventoryItemsComboxBox;
		private JSeparator bottomSeparator;
		private JButton startButton;
	}
	
	private String millisToClock(long milliseconds) {
		long seconds = (milliseconds / 1000), minutes = 0, hours = 0;
		
		if (seconds >= 60) {
			minutes = (seconds / 60);
			seconds -= (minutes * 60);
		}
		
		if (minutes >= 60) {
			hours = (minutes / 60);
			minutes -= (hours * 60);
		}
		
		return (hours < 10 ? "0" + hours + ":" : hours + ":")
				+ (minutes < 10 ? "0" + minutes + ":" : minutes + ":")
				+ (seconds < 10 ? "0" + seconds : seconds);
	}
}
