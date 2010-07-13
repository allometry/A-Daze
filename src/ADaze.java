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
import javax.swing.JList;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.quirlion.script.Constants;
import com.quirlion.script.Script;
import com.quirlion.script.types.Interface;
import com.quirlion.script.types.InventoryItem;
import com.quirlion.script.types.Magic;
import com.quirlion.script.types.NPC;

public class ADaze extends Script {
	private boolean isUsingAlchemy = false, isUsingHighAlchemy = false, isUsingLowAlchemy = false, isCameraRotating = false;
	private int alchemyCasts = 0, curseCasts = 0, curseNPCID = 0, spellInterfaceID = 0, startingMagicLevel = 0, startingMagicXP = 0;
	private long startTime = 0, timeout = 0;
	
	private InventoryItem alchemyInventoryItem;
	private Image asteriskImage, cursorImage, sumImage, timeImage, wandImage;
	private ImageObserver observer;
	private Interface spellInterface = null;
	private NPC curseNPC = null;
	
	@Override
	public void onStart() {
		try {
			asteriskImage = ImageIO.read(new URL("http://scripts.allometry.com/icons/asterisk_orange.png"));
			cursorImage = ImageIO.read(new URL("http://scripts.allometry.com/app/webroot/img/cursors/cursor-01.png"));
			sumImage = ImageIO.read(new URL("http://scripts.allometry.com/icons/sum.png"));
			timeImage = ImageIO.read(new URL("http://scripts.allometry.com/icons/time.png"));
			wandImage = ImageIO.read(new URL("http://scripts.allometry.com/icons/wand.png"));
		} catch (IOException e) {
			logStackTrace(e);
		} 
		
		startingMagicLevel = skills.getCurrentSkillLevel(Constants.STAT_MAGIC);
		startingMagicXP = skills.getCurrentSkillXP(Constants.STAT_MAGIC);
		
		ArrayList<Monster> monsters = new ArrayList<Monster>();
		for(NPC npc : npcs.getArray())
			if(npc.getName() != "" || !npc.getName().contains("null"))
				monsters.add(new Monster(npc));
		
		ADazeGUI aDazeGUI = new ADazeGUI(monsters.toArray());
		
		if(startingMagicLevel < 3) {
			log("You do not have a magic skill level of 3 or greater. You can't use this script yet! Exiting...");
			stopScript();
		}
		
		if(startingMagicLevel >= 3) aDazeGUI.confuseRadioButton.setEnabled(true);
		if(startingMagicLevel >= 11) aDazeGUI.weakenRadioButton.setEnabled(true);
		
		if(startingMagicLevel >= 19) {
			aDazeGUI.curseRadioButton.setEnabled(true);
			aDazeGUI.lowAlchemyCheckBox.setEnabled(true);
		}
		
		if(startingMagicLevel >= 55) aDazeGUI.highAlchemyCheckBox.setEnabled(true);
		if(startingMagicLevel >= 66) aDazeGUI.vulnerabilityRadioButton.setEnabled(true);
		if(startingMagicLevel >= 73) aDazeGUI.enfeebleRadioButton.setEnabled(true);
		if(startingMagicLevel >= 80) aDazeGUI.stunRadioButton.setEnabled(true);
		
		for(InventoryItem item : inventory.getItemArray())
			aDazeGUI.inventoryComboBox.addItem(new Item(item));
		
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
		
		if(aDazeGUI.lowAlchemyCheckBox.isSelected()) {
			isUsingAlchemy = true;
			isUsingLowAlchemy = true;
			alchemyInventoryItem = ((Item)aDazeGUI.inventoryComboBox.getSelectedItem()).getInventoryItem();
		}
		
		if(aDazeGUI.highAlchemyCheckBox.isSelected()) {
			isUsingAlchemy = true;
			isUsingHighAlchemy = true;
			alchemyInventoryItem = ((Item)aDazeGUI.inventoryComboBox.getSelectedItem()).getInventoryItem();
		}
		
		curseNPCID = ((Monster)aDazeGUI.monsterList.getSelectedValue()).getMonster().getID();

		aDazeGUI.dispose();
		aDazeGUI = null;
		
		startTime = System.currentTimeMillis();
	}
	
	@Override
	public int loop() {
		if(isCameraRotating) return 1;
		
		try {
			if(curseNPC == null) {
				if(npcs.getNearestByID(curseNPCID) != null) {
					curseNPC = npcs.getNearestByID(curseNPCID);
					
					return 1;
				} else {
					log("Could not find monster, stopping script...");
					stopScript();
				}
			}
			
			if(tabs.getCurrentTab() != Constants.TAB_MAGIC) tabs.openTab(Constants.TAB_MAGIC);
			
			spellInterface = interfaces.get(Constants.INTERFACE_TAB_MAGIC, spellInterfaceID);
			
			while(!isMouseInArea(spellInterface.getArea()))
				input.moveMouse(spellInterface.getRealX() + 8, spellInterface.getRealY() + 8);
			
			if(spellInterface.click()) {
				Point npcLocation = calculations.tileToScreen(curseNPC.getLocation());
				
				while(input.getBotMousePosition().equals(npcLocation)) {
					input.hopMouse(npcLocation.x, npcLocation.y);
					npcLocation = calculations.tileToScreen(curseNPC.getLocation());
				}
				
				if(curseNPC.click()) {
					timeout = System.currentTimeMillis() + 3000;
					int magicXP = skills.getCurrentSkillXP(Constants.STAT_MAGIC);
					while(magicXP == skills.getCurrentSkillXP(Constants.STAT_MAGIC) && System.currentTimeMillis() < timeout) wait(1);
					if(magicXP != skills.getCurrentSkillXP(Constants.STAT_MAGIC)) curseCasts++;
				}

				if(isUsingHighAlchemy || isUsingLowAlchemy) {
					Magic.Spell alchemySpell;
					
					if(isUsingHighAlchemy)
						alchemySpell= magic.REGULAR.HIGH_LEVEL_ALCHEMY;
					else
						alchemySpell = magic.REGULAR.LOW_LEVEL_ALCHEMY;
					
					if(alchemySpell.canCast()) {
						if(alchemySpell.castOn(alchemyInventoryItem.getID())) {
							timeout = System.currentTimeMillis() + 3000;
							int magicXP = skills.getCurrentSkillXP(Constants.STAT_MAGIC);
							while(magicXP == skills.getCurrentSkillXP(Constants.STAT_MAGIC) && System.currentTimeMillis() < timeout) wait(1);
							if(magicXP != skills.getCurrentSkillXP(Constants.STAT_MAGIC)) alchemyCasts++;
						}
					} else {
						isUsingHighAlchemy = false;
						isUsingLowAlchemy = false;
					}
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
		
		if(isUsingAlchemy)
			leftScoreboard.addWidget(new ScoreboardWidget(asteriskImage, number.format(alchemyCasts)));
		
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
	
	public class Monster {
		private NPC monsterNPC;
		
		public Monster(NPC monsterNPC) {
			this.monsterNPC = monsterNPC;
		}
		
		public NPC getMonster() {
			return monsterNPC;
		}
		
		public String toString() {
			return monsterNPC.getName();
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
		public int selectedSpell = 0;
		
		private static final long serialVersionUID = 6280429693064089142L;
		
		public ADazeGUI(Object[] monsterObjects) {
			initComponents(monsterObjects);
		}
		
		private void updateBeans() {
			if(confuseRadioButton.isSelected()) selectedSpell = CONFUSE;
			if(weakenRadioButton.isSelected()) selectedSpell = WEAKEN;
			if(curseRadioButton.isSelected()) selectedSpell = CURSE;
			if(vulnerabilityRadioButton.isSelected()) selectedSpell = VULNERABILITY;
			if(enfeebleRadioButton.isSelected()) selectedSpell = ENFEEBLE;
			if(stunRadioButton.isSelected()) selectedSpell = STUN;
			
			if(selectedSpell > 0) {
				monsterList.setEnabled(true);
				startButton.setEnabled(true);
			}
			
			if(lowAlchemyCheckBox.isSelected() || highAlchemyCheckBox.isSelected())
				inventoryComboBox.setEnabled(true);
			else
				inventoryComboBox.setEnabled(false);
		}

		private void confuseRadioButtonStateChanged(ChangeEvent e) {
			updateBeans();
		}

		private void weakenRadioButtonStateChanged(ChangeEvent e) {
			updateBeans();
		}

		private void curseRadioButtonStateChanged(ChangeEvent e) {
			updateBeans();
		}

		private void vulnerabilityRadioButtonStateChanged(ChangeEvent e) {
			updateBeans();
		}

		private void enfeebleRadioButtonStateChanged(ChangeEvent e) {
			updateBeans();
		}

		private void stunRadioButtonStateChanged(ChangeEvent e) {
			updateBeans();
		}

		private void noAlchemyCheckBoxStateChanged(ChangeEvent e) {
			updateBeans();
		}

		private void lowAlchemyCheckBoxStateChanged(ChangeEvent e) {
			updateBeans();
		}

		private void highAlchemyCheckBoxStateChanged(ChangeEvent e) {
			updateBeans();
		}

		private void startButtonActionPerformed(ActionEvent e) {
			this.setVisible(false);
		}

		private void initComponents(Object[] objects) {
			aDazeConfigurationLabel = new JLabel();
			topSeparator = new JSeparator();
			curseSpellLabel = new JLabel();
			confuseRadioButton = new JRadioButton();
			weakenRadioButton = new JRadioButton();
			curseRadioButton = new JRadioButton();
			vulnerabilityRadioButton = new JRadioButton();
			enfeebleRadioButton = new JRadioButton();
			stunRadioButton = new JRadioButton();
			topMiddleSeparator = new JSeparator();
			monsterLabel = new JLabel();
			monsterScrollPane = new JScrollPane();
			monsterList = new JList(objects);
			bottomMiddleSeparator = new JSeparator();
			alchemySpellLabel = new JLabel();
			noAlchemyCheckBox = new JCheckBox();
			lowAlchemyCheckBox = new JCheckBox();
			highAlchemyCheckBox = new JCheckBox();
			inventoryLabel = new JLabel();
			inventoryComboBox = new JComboBox();
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

			curseSpellLabel.setText("Select a Curse Spell...");
			contentPane.add(curseSpellLabel);
			curseSpellLabel.setBounds(15, 50, 200, curseSpellLabel.getPreferredSize().height);

			confuseRadioButton.setText("Confuse (3)");
			confuseRadioButton.setEnabled(false);
			confuseRadioButton.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					confuseRadioButtonStateChanged(e);
				}
			});
			contentPane.add(confuseRadioButton);
			confuseRadioButton.setBounds(15, 75, 200, confuseRadioButton.getPreferredSize().height);

			weakenRadioButton.setText("Weaken (11)");
			weakenRadioButton.setEnabled(false);
			weakenRadioButton.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					weakenRadioButtonStateChanged(e);
				}
			});
			contentPane.add(weakenRadioButton);
			weakenRadioButton.setBounds(15, 100, 200, 23);

			curseRadioButton.setText("Curse (19)");
			curseRadioButton.setEnabled(false);
			curseRadioButton.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					curseRadioButtonStateChanged(e);
				}
			});
			contentPane.add(curseRadioButton);
			curseRadioButton.setBounds(15, 125, 200, 23);

			vulnerabilityRadioButton.setText("Vulnerability (66)");
			vulnerabilityRadioButton.setEnabled(false);
			vulnerabilityRadioButton.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					vulnerabilityRadioButtonStateChanged(e);
				}
			});
			contentPane.add(vulnerabilityRadioButton);
			vulnerabilityRadioButton.setBounds(15, 150, 200, 23);

			enfeebleRadioButton.setText("Enfeeble (73)");
			enfeebleRadioButton.setEnabled(false);
			enfeebleRadioButton.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					enfeebleRadioButtonStateChanged(e);
				}
			});
			contentPane.add(enfeebleRadioButton);
			enfeebleRadioButton.setBounds(15, 175, 200, 23);

			stunRadioButton.setText("Stun (80)");
			stunRadioButton.setEnabled(false);
			stunRadioButton.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					stunRadioButtonStateChanged(e);
				}
			});
			contentPane.add(stunRadioButton);
			stunRadioButton.setBounds(15, 200, 200, 23);
			contentPane.add(topMiddleSeparator);
			topMiddleSeparator.setBounds(5, 225, 220, 12);

			monsterLabel.setText("Select a Nearby Monster...");
			contentPane.add(monsterLabel);
			monsterLabel.setBounds(15, 240, 200, 16);
			{
				monsterList.setEnabled(false);
				monsterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				monsterScrollPane.setViewportView(monsterList);
			}
			contentPane.add(monsterScrollPane);
			monsterScrollPane.setBounds(15, 265, 200, 55);
			contentPane.add(bottomMiddleSeparator);
			bottomMiddleSeparator.setBounds(5, 325, 220, 12);

			alchemySpellLabel.setText("Select Alchemy Bonus...");
			contentPane.add(alchemySpellLabel);
			alchemySpellLabel.setBounds(15, 340, 200, 16);

			noAlchemyCheckBox.setText("No Alchemy Bonus");
			noAlchemyCheckBox.setSelectedIcon(null);
			noAlchemyCheckBox.setSelected(true);
			noAlchemyCheckBox.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					noAlchemyCheckBoxStateChanged(e);
				}
			});
			contentPane.add(noAlchemyCheckBox);
			noAlchemyCheckBox.setBounds(15, 365, 200, 23);

			lowAlchemyCheckBox.setText("Use Low Alchemy (19)");
			lowAlchemyCheckBox.setSelectedIcon(null);
			lowAlchemyCheckBox.setEnabled(false);
			lowAlchemyCheckBox.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					lowAlchemyCheckBoxStateChanged(e);
				}
			});
			contentPane.add(lowAlchemyCheckBox);
			lowAlchemyCheckBox.setBounds(15, 390, 200, 23);

			highAlchemyCheckBox.setText("Use High Alchemy (55)");
			highAlchemyCheckBox.setEnabled(false);
			highAlchemyCheckBox.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					highAlchemyCheckBoxStateChanged(e);
				}
			});
			contentPane.add(highAlchemyCheckBox);
			highAlchemyCheckBox.setBounds(15, 415, 200, highAlchemyCheckBox.getPreferredSize().height);

			inventoryLabel.setText("Select an Inventory Item...");
			contentPane.add(inventoryLabel);
			inventoryLabel.setBounds(15, 445, 200, 16);

			inventoryComboBox.setEnabled(false);
			contentPane.add(inventoryComboBox);
			inventoryComboBox.setBounds(15, 470, 200, inventoryComboBox.getPreferredSize().height);
			contentPane.add(bottomSeparator);
			bottomSeparator.setBounds(5, 500, 220, 12);

			startButton.setText("Start");
			startButton.setEnabled(false);
			startButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					startButtonActionPerformed(e);
				}
			});
			contentPane.add(startButton);
			startButton.setBounds(new Rectangle(new Point(150, 515), startButton.getPreferredSize()));

			contentPane.setPreferredSize(new Dimension(230, 570));
			setSize(230, 570);
			setLocationRelativeTo(null);

			ButtonGroup curseSpellGroup = new ButtonGroup();
			curseSpellGroup.add(confuseRadioButton);
			curseSpellGroup.add(weakenRadioButton);
			curseSpellGroup.add(curseRadioButton);
			curseSpellGroup.add(vulnerabilityRadioButton);
			curseSpellGroup.add(enfeebleRadioButton);
			curseSpellGroup.add(stunRadioButton);

			ButtonGroup alchemySpellGroup = new ButtonGroup();
			alchemySpellGroup.add(noAlchemyCheckBox);
			alchemySpellGroup.add(lowAlchemyCheckBox);
			alchemySpellGroup.add(highAlchemyCheckBox);
		}

		private JLabel aDazeConfigurationLabel;
		private JSeparator topSeparator;
		private JLabel curseSpellLabel;
		private JRadioButton confuseRadioButton;
		private JRadioButton weakenRadioButton;
		private JRadioButton curseRadioButton;
		private JRadioButton vulnerabilityRadioButton;
		private JRadioButton enfeebleRadioButton;
		private JRadioButton stunRadioButton;
		private JSeparator topMiddleSeparator;
		private JLabel monsterLabel;
		private JScrollPane monsterScrollPane;
		private JList monsterList;
		private JSeparator bottomMiddleSeparator;
		private JLabel alchemySpellLabel;
		private JCheckBox noAlchemyCheckBox;
		private JCheckBox lowAlchemyCheckBox;
		private JCheckBox highAlchemyCheckBox;
		private JLabel inventoryLabel;
		private JComboBox inventoryComboBox;
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
