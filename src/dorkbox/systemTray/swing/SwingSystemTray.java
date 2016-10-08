/*
 * Copyright 2014 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.systemTray.swing;

import dorkbox.systemTray.ImageUtil;
import dorkbox.systemTray.MenuEntry;
import dorkbox.systemTray.SystemTrayMenuAction;
import dorkbox.systemTray.SystemUtils;
import dorkbox.util.ScreenUtil;
import dorkbox.util.SwingUtil;

import javax.imageio.ImageIO;
import javax.swing.JMenuItem;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Class for handling all system tray interaction, via SWING
 */
public
class SwingSystemTray extends dorkbox.systemTray.SystemTray {
    volatile SwingSystemTrayMenuPopup menu;

    volatile JMenuItem connectionStatusItem;
    private volatile String statusText = null;
    private volatile String tooltipText = null;

    volatile SystemTray tray;
    volatile TrayIcon trayIcon;

    volatile boolean isActive = false;

    /**
     * Creates a new system tray handler class.
     */
    public
    SwingSystemTray() {
        super();
        SwingUtil.invokeAndWait(new Runnable() {
            @Override
            public
            void run() {
                SwingSystemTray.this.tray = SystemTray.getSystemTray();
                if (SwingSystemTray.this.tray == null) {
                    logger.error("The system tray is not available");
                }
            }
        });
    }


    @Override
    public
    void shutdown() {
        SwingUtil.invokeAndWait(new Runnable() {
            @Override
            public
            void run() {
                SwingSystemTray tray = SwingSystemTray.this;
                synchronized (tray) {
                    tray.tray.remove(tray.trayIcon);

                    for (MenuEntry menuEntry : tray.menuEntries) {
                        menuEntry.remove();
                    }
                    tray.menuEntries.clear();

                    tray.connectionStatusItem = null;
                }
            }
        });
    }

    @Override
    public
    String getStatus() {
        return this.statusText;
    }

    protected
    void dispatch(Runnable runnable) {
        SwingUtil.invokeLater(runnable);
    }

    @Override
    public
    void setStatus(final String statusText) {
        this.statusText = statusText;
    }

    @Override
    public
    void setTooltipText(final String tooltipText) {
        this.tooltipText = tooltipText;
    }

    public
    String getTooltipText() {
        return this.tooltipText;
    }

    @Override
    protected
    void setIcon_(final String iconPath) {
        dispatch(new Runnable() {
            @Override
            public
            void run() {
                SwingSystemTray tray = SwingSystemTray.this;
                synchronized (tray) {
                    if (!isActive) {
                        // here we init. everything
                        isActive = true;

                        menu = new SwingSystemTrayMenuPopup();
                        Image trayImage = getResizedImage(iconPath);
                        trayImage.flush();
                        trayIcon = new TrayIcon(trayImage, getTooltipText());

                        // appindicators don't support this, so we cater to the lowest common denominator
                        // trayIcon.setToolTip(SwingSystemTray.this.appName);

                        trayIcon.addMouseListener(new MouseAdapter() {
                            @Override
                            public
                            void mousePressed(MouseEvent e) {
                                Dimension size = menu.getPreferredSize();

                                Point point = e.getPoint();
                                Rectangle bounds = ScreenUtil.getScreenBoundsAt(point);

                                int x = point.x;
                                int y = point.y;

                                if (y < bounds.y) {
                                    y = bounds.y;
                                }
                                else if (y + size.height > bounds.y + bounds.height) {
                                    // our menu cannot have the top-edge snap to the mouse
                                    // so we make the bottom-edge snap to the mouse
                                    y -= size.height; // snap to edge of mouse
                                }

                                if (x < bounds.x) {
                                    x = bounds.x;
                                }
                                else if (x + size.width > bounds.x + bounds.width) {
                                    // our menu cannot have the left-edge snap to the mouse
                                    // so we make the right-edge snap to the mouse
                                    x -= size.width; // snap to edge of mouse
                                }

                                // voodoo to get this to popup to have the correct parent
                                // from: http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6285881
                                menu.setInvoker(menu);
                                menu.setLocation(x, y);
                                menu.setVisible(true);
                                menu.setFocusable(true);
                                menu.requestFocusInWindow();
                            }
                        });

                        try {
                            SwingSystemTray.this.tray.add(trayIcon);
                        } catch (AWTException e) {
                            logger.error("TrayIcon could not be added.", e);
                        }
                    } else {
                        Image trayImage = getResizedImage(iconPath);
                        trayImage.flush();
                        tray.trayIcon.setImage(trayImage);
                    }
                }
            }
        });
    }

    /**
     * Will add a new menu entry, or update one if it already exists
     */
    private
    void addMenuEntry_(final String menuText, final String imagePath, final SystemTrayMenuAction callback) {
        if (menuText == null) {
            throw new NullPointerException("Menu text cannot be null");
        }

        dispatch(new Runnable() {
            @Override
            public
            void run() {
                SwingSystemTray tray = SwingSystemTray.this;
                synchronized (tray) {
                    synchronized (menuEntries) {
                        MenuEntry menuEntry = getMenuEntry(menuText);

                        if (menuEntry != null) {
                            throw new IllegalArgumentException("Menu entry already exists for given label '" + menuText + "'");
                        }
                        else {
                            menuEntry = new SwingMenuEntry(menu, menuText, imagePath, callback, tray);
                            menuEntries.add(menuEntry);
                        }
                    }
                }
            }
        });
    }

    @Override
    public
    void addMenuEntry(String menuText, final String imagePath, final SystemTrayMenuAction callback) {
        if (imagePath == null) {
            addMenuEntry_(menuText, null, callback);
        }
        else {
            addMenuEntry_(menuText, ImageUtil.iconPath(imagePath), callback);
        }
    }

    @Override
    public
    void addMenuEntry(final String menuText, final URL imageUrl, final SystemTrayMenuAction callback) {
        if (imageUrl == null) {
            addMenuEntry_(menuText, null, callback);
        }
        else {
            addMenuEntry_(menuText, ImageUtil.iconPath(imageUrl), callback);
        }
    }

    @Override
    public
    void addMenuEntry(final String menuText, final String cacheName, final InputStream imageStream, final SystemTrayMenuAction callback) {
        if (imageStream == null) {
            addMenuEntry_(menuText, null, callback);
        }
        else {
            addMenuEntry_(menuText, ImageUtil.iconPath(cacheName, imageStream), callback);
        }
    }

    @Override
    @Deprecated
    public
    void addMenuEntry(final String menuText, final InputStream imageStream, final SystemTrayMenuAction callback) {
        if (imageStream == null) {
            addMenuEntry_(menuText, null, callback);
        }
        else {
            addMenuEntry_(menuText, ImageUtil.iconPathNoCache(imageStream), callback);
        }
    }
    
    // using this fixes weird getScaledImage() size variance on Windows
    private BufferedImage getResizedImage(String imagePath) {
        if(SystemUtils.IS_OS_WINDOWS && !SystemUtils.IS_OS_WINDOWS_10) {
            TRAY_SIZE = 16;
        }
        BufferedImage originalImage, resizedImage = null;
        try {
            originalImage = ImageIO.read(new File(imagePath));
            int type = originalImage.getType() == 0? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
            resizedImage = new BufferedImage(TRAY_SIZE, TRAY_SIZE, type);
            Graphics2D g = resizedImage.createGraphics();
            g.drawImage(originalImage, 0, 0, TRAY_SIZE, TRAY_SIZE, null);
            g.dispose();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resizedImage;
    }
}
