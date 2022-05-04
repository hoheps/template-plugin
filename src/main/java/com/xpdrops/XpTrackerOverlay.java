package com.xpdrops;

import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;

public class XpTrackerOverlay extends Overlay {

    protected static final int TOTAL_LEVEL_ICON = 898;
    protected static final String pattern = "#,###,###,###";
    protected static final DecimalFormat xpFormatter = new DecimalFormat(pattern);
    protected static final BufferedImage[] STAT_ICONS = new BufferedImage[Skill.values().length - 1];

    protected CustomizableXpDropsPlugin plugin;
    protected XpDropsConfig config;

    protected String lastFont = "";
    protected int lastFontSize = 0;
    protected boolean useRunescapeFont = true;
    protected XpDropsConfig.FontStyle lastFontStyle = XpDropsConfig.FontStyle.DEFAULT;
    protected Font font = null;
    protected boolean firstRender = true;
    protected long lastFrameTime = 0;

    @Inject
    private Client client;

    private Long overallXp;
    private int skillXp;
    private int icon;
    private Skill currentSkill;

    @Inject
    protected XpTrackerOverlay(CustomizableXpDropsPlugin plugin, XpDropsConfig config)
    {
        this.plugin = plugin;
        this.config = config;
        setLayer(OverlayLayer.UNDER_WIDGETS);
        setPosition(OverlayPosition.TOP_RIGHT);
    }

    /**
     * Font provided by config menu item
     * @param graphics
     */
    protected void handleFont(Graphics2D graphics)
    {
        if( font != null)
        {
            graphics.setFont(font);
            if( useRunescapeFont)
            {
                graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            }
        }
    }

    protected void lazyInit()
    {
        if (firstRender)
        {
            //If the user is using the MOST_RECENT, default to showing overall xp for the first render
            if(config.xpTrackerSkill().equals(XpTrackerSkills.MOST_RECENT))
            {
                currentSkill = Skill.OVERALL;
            }

            firstRender = false;
            initIcons();
        }
        if (lastFrameTime <= 0)
        {
            lastFrameTime = System.currentTimeMillis() - 20;
        }
    }

    protected void initIcons()
    {
        for (int i = 0; i < STAT_ICONS.length; i++)
        {
            STAT_ICONS[i] = plugin.getSkillIcon(Skill.values()[i]);
        }
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (config.useXpTracker())
        {
            lazyInit();
            update();

            setLayer(OverlayLayer.UNDER_WIDGETS);
            setPosition(OverlayPosition.TOP_RIGHT);

            FontMetrics fontMetrics = graphics.getFontMetrics();

            int width = drawXpTracker(graphics);
            int height = fontMetrics.getHeight();
            height += Math.abs(config.xpTrackerFontSize() - fontMetrics.getHeight());

            lastFrameTime = System.currentTimeMillis();
            return new Dimension(width, height);
        }
        return new Dimension(0,0);
    }

    protected int drawXpTracker(Graphics2D graphics)
    {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        String text = "";
        boolean isOverall = false;
        handleFont(graphics);

        int width = graphics.getFontMetrics().stringWidth(pattern);
        int height = graphics.getFontMetrics().getHeight();

        if (currentSkill.equals(Skill.OVERALL))
        {
            text = xpFormatter.format(overallXp);
            isOverall = true;
        }
        else
        {
            text = xpFormatter.format(skillXp);
        }

        int textY = height + graphics.getFontMetrics().getMaxAscent() - graphics.getFontMetrics().getHeight();
        int textX = width - (width - graphics.getFontMetrics().stringWidth(text));

        int imageY = textY - graphics.getFontMetrics().getMaxAscent();

        //Adding 5 onto image width to give a little space in between icon and text
        int imageWidth = drawIcons(graphics, 0, imageY, 0xff, isOverall) + 5;

        drawText(graphics, text, imageWidth, textY);

        return textX + imageWidth;
    }

    private int drawIcons(Graphics2D graphics, int x, int y, float alpha, boolean isOverallXp)
    {
        int width = 0;
        int iconSize = graphics.getFontMetrics().getHeight();
        BufferedImage image;

        if (config.showIconsXpTracker())
        {
            //if the skill we're tracking is not OverallXp, get the icon from the array of bufferedImages using the icon ID
            if(!isOverallXp)
            {
                image = STAT_ICONS[icon];
            }
            //If we're tracking OverallXp, get the Skills Tab icon by getting the icon from the spriteList
            else
            {
                image = plugin.getIcon(TOTAL_LEVEL_ICON, 0);
            }
            int _iconSize = Math.max(iconSize, 18);
            int iconWidth = image.getWidth() * _iconSize / 25;
            int iconHeight = image.getHeight() * _iconSize / 25;
            Dimension dimension = drawIcon(graphics, image, x, y, iconWidth, iconHeight, alpha / 0xff);

            width += dimension.getWidth();
            return width;
        }
        return width;
    }

    private Dimension drawIcon(Graphics2D graphics, BufferedImage image, int x, int y, int width, int height, float alpha)
    {
        int yOffset = graphics.getFontMetrics().getHeight() / 2 - height / 2;
        int xOffset = width;

        Composite composite = graphics.getComposite();
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        graphics.drawImage(image, x, y + yOffset, width, height, null);
        graphics.setComposite(composite);
        return new Dimension(width, height);
    }

    protected void drawText(Graphics2D graphics, String text, int textX, int textY)
    {
        Color _color = config.xpTrackerColor();
        Color backgroundColor = new Color(0,0,0);
        graphics.setColor(backgroundColor);
        graphics.drawString(text, textX + 1, textY + 1);
        graphics.setColor(_color);
        graphics.drawString(text, textX, textY);
    }

    private void update()
    {
        updateFont();
        updateXpTracker();
    }

    private void updateFont()
    {
        //only perform anything within this function if any settings related to the font have changed
        if(!lastFont.equals(config.fontName()) || lastFontSize != config.xpTrackerFontSize() || lastFontStyle != config.fontStyle())
        {
            lastFont = config.fontName();
            lastFontSize = config.xpTrackerFontSize();
            lastFontStyle = config.fontStyle();

            //use runescape font as default
            if (config.fontName().equals(""))
            {
                if (config.xpTrackerFontSize() < 16)
                {
                    font = FontManager.getRunescapeSmallFont();
                }
                else if (config.fontStyle() == XpDropsConfig.FontStyle.BOLD || config.fontStyle() == XpDropsConfig.FontStyle.BOLD_ITALICS)
                {
                    font = FontManager.getRunescapeBoldFont();
                }
                else
                {
                    font = FontManager.getRunescapeFont();
                }

                if (config.xpTrackerFontSize() > 16)
                {
                    font = font.deriveFont((float)config.xpTrackerFontSize());
                }

                if (config.fontStyle() == XpDropsConfig.FontStyle.BOLD)
                {
                    font = font.deriveFont(Font.BOLD);
                }
                if (config.fontStyle() == XpDropsConfig.FontStyle.ITALICS)
                {
                    font = font.deriveFont(Font.ITALIC);
                }
                if (config.fontStyle() == XpDropsConfig.FontStyle.BOLD_ITALICS)
                {
                    font = font.deriveFont(Font.ITALIC | Font.BOLD);
                }

                useRunescapeFont = true;
                return;
            }

            int style = Font.PLAIN;
            switch (config.fontStyle())
            {
                case BOLD:
                    style = Font.BOLD;
                    break;
                case ITALICS:
                    style = Font.ITALIC;
                    break;
                case BOLD_ITALICS:
                    style = Font.BOLD | Font.ITALIC;
                    break;
            }

            font = new Font(config.fontName(), style, config.xpTrackerFontSize());
            useRunescapeFont = false;
        }
    }

    private void updateXpTracker()
    {
        if (config.xpTrackerSkill().equals(XpTrackerSkills.MOST_RECENT))
        {
            for (XpDrop xpDrop : plugin.getQueue())
            {
                currentSkill = selectSkill(xpDrop.getSkill().getName().toUpperCase());
                break;
            }
        }
        else
        {
            currentSkill = selectSkill(config.xpTrackerSkill().toString());
        }

        if (currentSkill.equals(Skill.OVERALL))
        {
            overallXp = client.getOverallExperience();
        }
        else
        {
            skillXp = client.getSkillExperience(currentSkill);
        }
    }

    private Skill selectSkill(String xpTrackerSkills) {
        switch (xpTrackerSkills)
        {
            case "OVERALL":
                return Skill.OVERALL;
            case "ATTACK":
                icon = 0;
                return Skill.ATTACK;
            case "DEFENCE":
                icon = 1;
                return Skill.DEFENCE;
            case "STRENGTH":
                icon = 2;
                return Skill.STRENGTH;
            case "HITPOINTS":
                icon = 3;
                return Skill.HITPOINTS;
            case "RANGED":
                icon = 4;
                return Skill.RANGED;
            case "PRAYER":
                icon = 5;
                return Skill.PRAYER;
            case "MAGIC":
                icon = 6;
                return Skill.MAGIC;
            case "COOKING":
                icon = 7;
                return Skill.COOKING;
            case "WOODCUTTING":
                icon = 8;
                return Skill.WOODCUTTING;
            case "FLETCHING":
                icon = 9;
                return Skill.FLETCHING;
            case "FISHING":
                icon = 10;
                return Skill.FISHING;
            case "FIREMAKING":
                icon = 11;
                return Skill.FIREMAKING;
            case "CRAFTING":
                icon = 12;
                return Skill.CRAFTING;
            case "SMITHING":
                icon = 13;
                return Skill.SMITHING;
            case "MINING":
                icon = 14;
                return Skill.MINING;
            case "HERBLORE":
                icon = 15;
                return Skill.HERBLORE;
            case "AGILITY":
                icon = 16;
                return Skill.AGILITY;
            case "THEIVING":
                icon = 17;
                return Skill.THIEVING;
            case "SLAYER":
                icon = 18;
                return Skill.SLAYER;
            case "FARMING":
                icon = 19;
                return Skill.FARMING;
            case "RUNECRAFT":
                icon = 20;
                return Skill.RUNECRAFT;
            case "HUNTER":
                icon = 21;
                return Skill.HUNTER;
            case "CONSTRUCTION":
                icon = 22;
                return Skill.CONSTRUCTION;
        }
        return null;
    }
}