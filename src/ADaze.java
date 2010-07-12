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
import com.quirlion.script.types.NPC;

public class ADaze extends Script {
	private boolean isCameraRotating = false, isStopping = false;
	private int curseCasts = 0, monkZamorakID = 189, spellInterfaceID = 0, startingMagicLevel = 0, startingMagicXP = 0;
	private long startTime = 0;
	
	private Antiban antiban;
	private Image cursorImage, sumImage, timeImage, wandImage;
	private ImageObserver observer;
	private Interface spellInterface = null;
	private NPC monkZamorak = null;
	private Thread antibanThread;
	
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
		
		ADazeGUI aDazeGUI = new ADazeGUI();
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
		
		aDazeGUI.dispose();
		aDazeGUI = null;
		
		antiban = new Antiban();
		antibanThread = new Thread(antiban);
		antibanThread.start();
		
		startingMagicLevel = skills.getCurrentSkillLevel(Constants.STAT_MAGIC);
		startingMagicXP = skills.getCurrentSkillXP(Constants.STAT_MAGIC);
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
			
			spellInterface = interfaces.get(Constants.INTERFACE_TAB_MAGIC, spellInterfaceID);
			while(!isMouseInArea(spellInterface.getArea()))
				input.moveMouse(spellInterface.getRealX() + 8, spellInterface.getRealY() + 8);
			
			if(spellInterface.click()) {
				monkZamorak.hover();
				
				int magicXP = skills.getCurrentSkillXP(Constants.STAT_MAGIC);
				monkZamorak.click();
				
				long timeout = System.currentTimeMillis() + 3000;
				while(magicXP == skills.getCurrentSkillXP(Constants.STAT_MAGIC) && System.currentTimeMillis() < timeout) {
					wait(1);
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
		if(!players.getCurrent().isLoggedIn() || players.getCurrent().isInLobby()) return ;
		
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
						
						isCameraRotating = true;
						while(cam.getCameraAngle() != randomAngle)
							cam.spinCamera(1, Camera.RIGHT);
						
						isCameraRotating = false;
					}
					
					if(cam.getCameraAngle() >= 290 && cam.getCameraAngle() <= 122) {
						//spin left
						int randomAngle = random(45, 122);
						
						isCameraRotating = true;
						while(cam.getCameraAngle() != randomAngle)
							cam.spinCamera(1, Camera.LEFT);
						
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
		private static final long serialVersionUID = 6280429693064089142L;
		public static final int CONFUSE = 1, WEAKEN = 2, CURSE = 3, VULNERABILITY = 4, ENFEEBLE = 5, STUN = 6;
		public int selectedSpell;
		
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

		private void stunRadioButtonStateChanged(ChangeEvent e) {
			if(!startButton.isEnabled()) startButton.setEnabled(true);
		}

		private void enfeebleRadioButtonStateChanged(ChangeEvent e) {
			if(!startButton.isEnabled()) startButton.setEnabled(true);
		}

		private void vulnerabilityRadioButtonStateChanged(ChangeEvent e) {
			if(!startButton.isEnabled()) startButton.setEnabled(true);
		}

		private void startButtonActionPerformed(ActionEvent e) {
			if(confuseRadioButton.isSelected()) selectedSpell = CONFUSE;
			if(weakenRadioButton.isSelected()) selectedSpell = WEAKEN;
			if(curseRadioButton.isSelected()) selectedSpell = CURSE;
			if(vulnerabilityRadioButton.isSelected()) selectedSpell = VULNERABILITY;
			if(enfeebleRadioButton.isSelected()) selectedSpell = ENFEEBLE;
			if(stunRadioButton.isSelected()) selectedSpell = STUN;
			
			setVisible(false);
		}

		private void initComponents() {
			confuseRadioButton = new JRadioButton();
			aDazeConfigurationLabel = new JLabel();
			topSeparator = new JSeparator();
			weakenRadioButton = new JRadioButton();
			curseRadioButton = new JRadioButton();
			stunRadioButton = new JRadioButton();
			enfeebleRadioButton = new JRadioButton();
			vulnerabilityRadioButton = new JRadioButton();
			startButton = new JButton();
			bottomSeparator = new JSeparator();

			setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
			setResizable(false);
			setTitle("A. Daze");
			Container contentPane = getContentPane();
			contentPane.setLayout(null);

			confuseRadioButton.setText("Confuse (3)");
			confuseRadioButton.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					confuseRadioButtonStateChanged(e);
				}
			});
			contentPane.add(confuseRadioButton);
			confuseRadioButton.setBounds(15, 50, 200, confuseRadioButton.getPreferredSize().height);

			aDazeConfigurationLabel.setText("A. Daze Configuration");
			aDazeConfigurationLabel.setHorizontalAlignment(SwingConstants.CENTER);
			contentPane.add(aDazeConfigurationLabel);
			aDazeConfigurationLabel.setBounds(15, 15, 200, aDazeConfigurationLabel.getPreferredSize().height);
			contentPane.add(topSeparator);
			topSeparator.setBounds(5, 35, 220, topSeparator.getPreferredSize().height);

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

			stunRadioButton.setText("Stun (80)");
			stunRadioButton.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					stunRadioButtonStateChanged(e);
				}
			});
			contentPane.add(stunRadioButton);
			stunRadioButton.setBounds(15, 175, 200, 23);

			enfeebleRadioButton.setText("Enfeeble (73)");
			enfeebleRadioButton.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					enfeebleRadioButtonStateChanged(e);
				}
			});
			contentPane.add(enfeebleRadioButton);
			enfeebleRadioButton.setBounds(15, 150, 200, 23);

			vulnerabilityRadioButton.setText("Vulnerability (66)");
			vulnerabilityRadioButton.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					vulnerabilityRadioButtonStateChanged(e);
				}
			});
			contentPane.add(vulnerabilityRadioButton);
			vulnerabilityRadioButton.setBounds(15, 125, 200, 23);

			startButton.setText("Start");
			startButton.setEnabled(false);
			startButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					startButtonActionPerformed(e);
				}
			});
			contentPane.add(startButton);
			startButton.setBounds(new Rectangle(new Point(150, 215), startButton.getPreferredSize()));
			contentPane.add(bottomSeparator);
			bottomSeparator.setBounds(5, 200, 220, 12);

			contentPane.setPreferredSize(new Dimension(230, 275));
			setSize(230, 275);
			setLocationRelativeTo(null);

			ButtonGroup spellGroup = new ButtonGroup();
			spellGroup.add(confuseRadioButton);
			spellGroup.add(weakenRadioButton);
			spellGroup.add(curseRadioButton);
			spellGroup.add(stunRadioButton);
			spellGroup.add(enfeebleRadioButton);
			spellGroup.add(vulnerabilityRadioButton);
		}
		
		private JRadioButton confuseRadioButton;
		private JLabel aDazeConfigurationLabel;
		private JSeparator topSeparator;
		private JRadioButton weakenRadioButton;
		private JRadioButton curseRadioButton;
		private JRadioButton stunRadioButton;
		private JRadioButton enfeebleRadioButton;
		private JRadioButton vulnerabilityRadioButton;
		private JButton startButton;
		private JSeparator bottomSeparator;
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
