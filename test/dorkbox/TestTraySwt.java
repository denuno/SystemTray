/*
 * Copyright 2015 dorkbox, llc
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

package dorkbox;

import dorkbox.systemTray.MenuEntry;
import dorkbox.systemTray.SystemTray;
import dorkbox.systemTray.SystemTrayMenuAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.net.URL;

/**
 * Icons from 'SJJB Icons', public domain/CC0 icon set
 *
 * Needs SWT to run
 */
public
class TestTraySwt {

    public static final URL BLACK_MAIL = TestTraySwt.class.getResource("mail.000000.24.png");
    public static final URL GREEN_MAIL = TestTraySwt.class.getResource("mail.39AC39.24.png");
    public static final URL LT_GRAY_MAIL = TestTraySwt.class.getResource("mail.999999.24.png");

    public static
    void main(String[] args) {
        // make sure JNA jar is on the classpath!
        System.setProperty("SWT_GTK3", "0"); // Necessary for us to work with SWT

        new TestTraySwt();
    }

    private SystemTray systemTray;
    private SystemTrayMenuAction callbackGreen;
    private SystemTrayMenuAction callbackGray;

    public
    TestTraySwt() {
        final Display display = new Display ();
        final Shell shell = new Shell(display);

        Text helloWorldTest = new Text(shell, SWT.NONE);
        helloWorldTest.setText("Hello World SWT  .................  ");
        helloWorldTest.pack();


        this.systemTray = SystemTray.getSystemTray();
        if (systemTray == null) {
            throw new RuntimeException("Unable to load SystemTray!");
        }

        this.systemTray.setTooltipText("A useful tooltip");
        this.systemTray.setIcon(LT_GRAY_MAIL);

        systemTray.setStatus("No Mail");

        callbackGreen = new SystemTrayMenuAction() {
            @Override
            public
            void onClick(final SystemTray systemTray, final MenuEntry menuEntry) {
                systemTray.setStatus("Some Mail!");
                systemTray.setIcon(GREEN_MAIL);

                menuEntry.setCallback(callbackGray);
                menuEntry.setImage(BLACK_MAIL);
                menuEntry.setText("Delete Mail");
//                systemTray.removeMenuEntry(menuEntry);
            }
        };

        callbackGray = new SystemTrayMenuAction() {
            @Override
            public
            void onClick(final SystemTray systemTray, final MenuEntry menuEntry) {
                systemTray.setStatus(null);
                systemTray.setIcon(BLACK_MAIL);

                menuEntry.setCallback(null);
//                systemTray.setStatus("Mail Empty");
                systemTray.removeMenuEntry(menuEntry);
                System.err.println("POW");
            }
        };

        this.systemTray.addMenuEntry("Green Mail", GREEN_MAIL, callbackGreen);

        systemTray.addMenuEntry("Quit", new SystemTrayMenuAction() {
            @Override
            public
            void onClick(final SystemTray systemTray, final MenuEntry menuEntry) {
                systemTray.shutdown();

                display.asyncExec(new Runnable() {
                    public void run() {
                        shell.dispose();
                    }
                });

                //System.exit(0);  not necessary if all non-daemon threads have stopped.
            }
        });



        shell.pack();
        shell.open();

        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }

        display.dispose();
    }
}
