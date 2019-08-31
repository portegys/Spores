/*
 * Spores simulator application.
 *
 * Usage:
 * java spores.SporesSimApp
 *   [-networkSize <network size>]
 *   [-numActive <number of active peers>]
 *   [-peerCacheSize <peer cache size]
 *   [-filePushes <number of file pushes>
 *   [-screenSize <width> <height>]
 */

package spores;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.net.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.*;

// Spores simulator application.
public class SporesSimApp implements Runnable
{
    // Display frequency (ms).
    static final int DISPLAY_FREQUENCY = 100;
    Thread displayThread;

    // Default parameters.
    static final int DEFAULT_NETWORK_SIZE = 1000;
    static final int DEFAULT_ACTIVE_PEERS = 10;
    static final int DEFAULT_PEER_CACHE_SIZE = 5;
    static final int DEFAULT_FILE_PUSHES = 10;
    static final Dimension DEFAULT_SCREEN_SIZE = new Dimension(600, 700);

    // Settings.
    static int networkSize = DEFAULT_NETWORK_SIZE;
    static int numActive = DEFAULT_ACTIVE_PEERS;
    static int peerCacheSize = DEFAULT_PEER_CACHE_SIZE;
    static int filePushes = DEFAULT_FILE_PUSHES;
    static Dimension screenSize = new Dimension(DEFAULT_SCREEN_SIZE);

    // Spores simulator.
    SporesSim sporesSim;

    // Control panel.
    class Controls extends JPanel implements ActionListener
    {
        // Components.
        JButton stepButton;
        JButton uploadButton;
        JButton searchButton;
        JButton resetButton;
        JTextField outputText;

        // Constructor.
        Controls()
        {
            setLayout(new GridLayout(3, 1));
            setBorder(BorderFactory.createRaisedBevelBorder());
            JPanel panel = new JPanel();
            stepButton = new JButton("Step");
            stepButton.addActionListener(this);
            panel.add(stepButton);
            uploadButton = new JButton("Upload");
            uploadButton.addActionListener(this);
            panel.add(uploadButton);
            searchButton = new JButton("Search");
            searchButton.addActionListener(this);
            panel.add(searchButton);
            resetButton = new JButton("Reset");
            resetButton.addActionListener(this);
            panel.add(resetButton);
            add(panel);
            panel = new JPanel();
            panel.add(new JLabel("Status: "));
            outputText = new JTextField("", 40);
            outputText.setEditable(false);
            panel.add(outputText);
            add(panel);
        }

        // Button listener.
        public void actionPerformed(ActionEvent evt)
        {
            outputText.setText("");

            // Step?
            if (evt.getSource() == (Object)stepButton)
            {
                sporesSim.step();
                sporesSim.display();
                return;
            }

            // Upload?
            if (evt.getSource() == (Object)uploadButton)
            {
                sporesSim.upload();
                sporesSim.display();
                return;
            }

            // Search?
            if (evt.getSource() == (Object)searchButton)
            {
                if (sporesSim.search())
                {
                    outputText.setText("File found, saturation = " +
                        sporesSim.fileCount() + "/" + networkSize +
                        " Probability = " + sporesSim.probability());
                } else {
                    outputText.setText("File not found, saturation = " +
                        sporesSim.fileCount() + "/" + networkSize +
                        " Probability = " + sporesSim.probability());
                }
                sporesSim.display();
                return;
            }

            // Reset?
            if (evt.getSource() == (Object)resetButton)
            {
                sporesSim.reset();
                return;
            }
        }
    }

    // Controls.
    Controls controls;

    // Constructor.
    public SporesSimApp()
    {
        // Set up screen.
        JFrame screen = new JFrame("Spores Simulator");
        screen.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { System.exit(0); }
        });
        screen.setSize(screenSize);
        screen.getContentPane().setLayout(new BorderLayout());

        // Create display.
        Dimension displaySize = new Dimension((int)((double)screenSize.width * .99),
        (int)((double)screenSize.height * .80));
        sporesSim = new SporesSim(networkSize, numActive,
            peerCacheSize, filePushes, displaySize);
        screen.getContentPane().add(sporesSim, BorderLayout.NORTH);

        // Create controls.
        controls = new Controls();
        screen.getContentPane().add(controls);

        // Make screen visible.
        screen.setVisible(true);

        // Start display thread.
        displayThread = new Thread(this);
        displayThread.start();
    }

    // Display.
    public void run()
    {
        // Lower thread's priority.
        if (Thread.currentThread() == displayThread)
        {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
        }

        // Display loop.
        while (Thread.currentThread() == displayThread && !displayThread.isInterrupted())
        {
            sporesSim.display();
            try
            {
                Thread.sleep(DISPLAY_FREQUENCY);
            } catch(InterruptedException e) { break; }
        }
    }

    // Main.
    public static void main(String[] args)
    {
        // Get arguments.
        for (int i = 0; i < args.length;)
        {
            if (args[i].equals("-networkSize")) {
                i++;
                networkSize = -1;
                if (i < args.length)
                {
                    networkSize = Integer.parseInt(args[i]);
                    i++;
                }
                if (networkSize <= 0)
                {
                    System.err.println("Invalid network size");
                    System.exit(1);
                }
            } else if (args[i].equals("-screenSize"))
            {
                i++;
                screenSize.width = screenSize.height = -1;
                if (i < args.length)
                {
                    screenSize.width = Integer.parseInt(args[i]);
                    i++;
                }
                if (i < args.length)
                {
                    screenSize.height = Integer.parseInt(args[i]);
                    i++;
                }
                if (screenSize.width <= 0 || screenSize.height <= 0)
                {
                    System.err.println("Invalid screen size");
                    System.exit(1);
                }
            } else if (args[i].equals("-numActive"))
            {
                i++;
                numActive = -1;
                if (i < args.length)
                {
                    numActive = Integer.parseInt(args[i]);
                    i++;
                }
                if (numActive < 0)
                {
                    System.err.println("Invalid number of active peers");
                    System.exit(1);
                }
            } else if (args[i].equals("-peerCacheSize"))
            {
                i++;
                peerCacheSize = -1;
                if (i < args.length)
                {
                    peerCacheSize = Integer.parseInt(args[i]);
                    i++;
                }
                if (peerCacheSize <= 0)
                {
                    System.err.println("Invalid peer cache size");
                    System.exit(1);
                }
            } else if (args[i].equals("-filePushes"))
            {
                i++;
                filePushes = -1;
                if (i < args.length)
                {
                    filePushes = Integer.parseInt(args[i]);
                    i++;
                }
                if (filePushes <= 0)
                {
                    System.err.println("Invalid number of file pushes");
                    System.exit(1);
                }
            } else {
                System.err.println("Usage:");
                System.err.println("java spores.SporesSimApp [-networkSize <network size>]");
                System.err.println("\t[-numActive <number of active peers>]");
                System.err.println("\t[-peerCacheSize <peer cache size]");
                System.err.println("\t[-filePushes <number of file pushes>");
                System.err.println("\t[-screenSize <width> <height>]");
                System.exit(1);
            }
        }

        // Print settings.
        System.out.println("Parameters:");
        System.out.println("Network size = " + networkSize);
        System.out.println("Active peers = " + numActive);
        System.out.println("Peer cache size = " + peerCacheSize);
        System.out.println("File pushes = " + filePushes);

        // Create the application.
        new SporesSimApp();
    }
}
