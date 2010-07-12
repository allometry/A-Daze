import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import javax.imageio.ImageIO;

import com.quirlion.script.Constants;
import com.quirlion.script.Script;
import com.quirlion.script.types.Interface;
import com.quirlion.script.types.NPC;

public class ADaze extends Script {
	private boolean isCameraRotating = false;
	private int curseCasts = 0, monkZamorakID = 189, startingMagicLevel = 0, startingMagicXP = 0;
	private long startTime = 0;
	
	private Image cursorImage, sumImage, timeImage, wandImage;
	private ImageObserver observer;
	private Interface enfeebleSpell = null;
	private NPC monkZamorak = null;
	
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
			
			enfeebleSpell = interfaces.get(Constants.INTERFACE_TAB_MAGIC, Constants.SPELL_ENFEEBLE);
			while(!isMouseInArea(enfeebleSpell.getArea()))
				input.moveMouse(enfeebleSpell.getRealX() + 8, enfeebleSpell.getRealY() + 8);
			
			if(enfeebleSpell.click()) {
				monkZamorak.hover();
				
				int magicXP = skills.getCurrentSkillXP(Constants.STAT_MAGIC);
				monkZamorak.click();
				
				long timeout = System.currentTimeMillis() + 3000;
				while(magicXP == skills.getCurrentSkillXP(Constants.STAT_MAGIC) && System.currentTimeMillis() < timeout) {
					wait(1);
				}
				
				if(magicXP != skills.getCurrentSkillXP(Constants.STAT_MAGIC)) curseCasts++;
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
	
	public boolean isMouseInArea(Rectangle inArea) {
		int x = input.getBotMousePosition().x, y = input.getBotMousePosition().y;
		
		return (x > inArea.getX() && x < (inArea.getX() + inArea.getWidth()) && y > inArea.getY() && y < (inArea.getY() + inArea.getHeight()));
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
