/**
 * Spores file storage simulation.
 */

package spores;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.math.BigInteger;

// SporesSim.
public class SporesSim extends Canvas
{
    // Peer network.
    SporesPeer[] network;
    int numActive;
    int[] peerCache;
    int filePushes;

    // Buffered display.
    static final int PEER_DISPLAY_RADIUS = 5;
    private Dimension displaySize;
    private Graphics graphics;
    private Image image;
    private Graphics imageGraphics;
    private double scale;

    // Message and font.
    private String message;
    private Font font = new Font("Helvetica", Font.BOLD, 12);
    private FontMetrics fontMetrics;
    private int fontAscent;
    private int fontWidth;
    private int fontHeight;

    // Random numbers.
    private Random random;

    // Constructor.
    public SporesSim(int networkSize, int numActive,
        int peerCacheSize, int filePushes, Dimension displaySize)
    {
        // Create network and peer cache.
        network = new SporesPeer[networkSize];
        for (int i = 0; i < networkSize; i++)
        {
            network[i] = new SporesPeer();
        }
        this.numActive = numActive;
        peerCache = new int[peerCacheSize];
        for (int i = 0; i < peerCache.length; i++)
        {
            peerCache[i] = -1;
        }
        this.filePushes = filePushes;

        // Configure display.
        this.displaySize = displaySize;
        setBounds(0, 0, displaySize.width, displaySize.height);
        scale =
            ((double)displaySize.width * (double)displaySize.height) /
            (double)network.length;

        // Random numbers.
        random = new Random(new Date().getTime());
    }

    // Reset.
    public void reset()
    {
        for (int i = 0; i < network.length; i++)
        {
            network[i].removeFile();
            network[i].disconnect();
        }
        for (int i = 0; i < peerCache.length; i++)
        {
            peerCache[i] = -1;
        }
    }

    // Set random number seed.
    public void setRandomSeed(long randomSeed)
    {
        random = new Random(randomSeed);
    }

    // Step simulation.
    public void step()
    {
        // Disconnect peers.
        for (int i = 0; i < network.length; i++)
        {
            network[i].disconnect();
        }

        // Clear peer cache.
        for (int i = 0; i < peerCache.length; i++)
        {
            peerCache[i] = -1;
        }

        // Randomly connect peers.
        int p = 0;
        for (int i = 0; i < numActive; i++)
        {
            if (i == network.length) break;
            int j = random.nextInt(network.length);
            while (network[j].isConnected()) j = (j + 1) % network.length;
            int[] connectedPeers = new int[peerCache.length];
            for (int k = 0; k < peerCache.length; k++)
            {
                connectedPeers[k] = peerCache[k];
            }
            network[j].connect(connectedPeers);
            peerCache[p] = j;
            p = (p + 1) % peerCache.length;
        }
    }

    // Upload simulated file.
    public void upload()
    {
        int filesPushed;
        LinkedList triedList,currentList,pushList,tempList;
        int size,peer;
        int[] connected;

        // Push file to peers.
        triedList = new LinkedList();
        currentList = new LinkedList();
        for (int i = 0; i < peerCache.length; i++)
        {
            if (peerCache[i] != -1)
            {
                currentList.add(new Integer(peerCache[i]));
            }
        }
        filesPushed = 0;
        while (filesPushed < filePushes && currentList.size() > 0)
        {
            pushList = new LinkedList();
            for (int i = 0; i < currentList.size(); i++)
            {
                peer = ((Integer)currentList.get(i)).intValue();
                pushList.add(new Integer(peer));
            }
            while ((size = pushList.size()) > 0)
            {
                // Pick random peer.
                peer = -1;
                try
                {
                    int i = random.nextInt(size);
                    peer = ((Integer)pushList.get(i)).intValue();
                    pushList.remove(i);
                } catch (IndexOutOfBoundsException e) {}
                if (peer == -1) continue;

                // Attempt push to peer.
                if (!network[peer].hasFile())
                {
                    network[peer].storeFile();
                    filesPushed++;
                }
            }

            // Record tried peers.
            for (int i = 0; i < currentList.size(); i++)
            {
                peer = -1;
                try
                {
                    peer = ((Integer)currentList.get(i)).intValue();
                } catch (IndexOutOfBoundsException e) {}
                if (peer == -1) continue;
                triedList.add(new Integer(peer));
            }

            // Expand to next tier of peers.
            for (int i = 0; i < currentList.size(); i++)
            {
                peer = -1;
                try
                {
                    peer = ((Integer)currentList.get(i)).intValue();
                } catch (IndexOutOfBoundsException e) {}
                if (peer == -1) continue;
                tempList = new LinkedList();
                connected = network[peer].getConnected();
                for (int j = 0; j < connected.length; j++)
                {
                    if (connected[j] != -1)
                    {
                        tempList.add(new Integer(connected[j]));
                    }
                }
                for (int j = 0; j < tempList.size(); j++)
                {
                    peer = -1;
                    try
                    {
                        peer = ((Integer)tempList.get(j)).intValue();
                    } catch (IndexOutOfBoundsException e) {}
                    if (peer == -1) continue;
                    int k;
                    for (k = 0; k < triedList.size(); k++)
                    {
                        if (peer == ((Integer)triedList.get(k)).intValue()) break;
                    }
                    if (k == triedList.size())
                    {
                        for (k = 0; k < pushList.size(); k++)
                        {
                            if (peer == ((Integer)pushList.get(k)).intValue()) break;
                        }
                        if (k == pushList.size())
                        {
                            pushList.add(new Integer(peer));
                        }
                    }
                }
            }
            currentList = pushList;
        }
    }

    // Search for file.
    public boolean search()
    {
        for (int i = 0; i < network.length; i++)
        {
            if (network[i].isConnected() && network[i].hasFile())
            {
                return true;
            }
        }
        return false;
    }

    // How many peers have file?
    public int fileCount()
    {
        int count = 0;
        for (int i = 0; i < network.length; i++)
        {
            if (network[i].hasFile()) count++;
        }
        return count;
    }

    // What is the probability of finding file?
    public double probability()
    {
        int count = fileCount();
        if (numActive == 0 || count == 0) return 0.0;
        if (network.length - count < numActive) return 1.0;
        BigInteger n =
        	Combinations.choose(network.length - count, numActive).multiply(
                    new BigInteger(new String("100")));
        double p = n.divide(Combinations.choose(
            network.length, numActive)).doubleValue() / 100.0;
        return 1.0 - p;
    }

    // Display network.
    public void display()
    {
        if (graphics == null)
        {
            graphics = getGraphics();
            image = createImage(displaySize.width, displaySize.height);
            imageGraphics = image.getGraphics();
            graphics.setFont(font);
            fontMetrics = graphics.getFontMetrics();
            fontAscent = fontMetrics.getMaxAscent();
            fontWidth = fontMetrics.getMaxAdvance();
            fontHeight = fontMetrics.getHeight();
        }
        if (graphics == null) return;

        // Clear display.
        imageGraphics.setColor(Color.white);
        imageGraphics.fillRect(0,0,displaySize.width,displaySize.height);

        // Draw active peers.
        Point p1,p2;
        int i,j,x,y,w,h;
        int[] connected;
        w = h = PEER_DISPLAY_RADIUS * 2;
        for (i = 0; i < network.length; i++)
        {
            if (!network[i].isConnected()) continue;
            p1 = getPoint(i);
            if (network[i].hasFile())
            {
                imageGraphics.setColor(Color.green);
            } else {
                imageGraphics.setColor(Color.red);
            }
            x = p1.x - PEER_DISPLAY_RADIUS;
            y = p1.y - PEER_DISPLAY_RADIUS;
            imageGraphics.fillOval(x, y, w, h);
            connected = network[i].getConnected();
            for (j = 0; j < connected.length; j++)
            {
                if (connected[j] == -1) continue;
                p2 = getPoint(connected[j]);
                imageGraphics.setColor(Color.black);
                imageGraphics.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }

        // Draw message.
        drawMessage();

        // Refresh display.
        graphics.drawImage(image, 0, 0, this);
    }

    // Get peer display point.
    private Point getPoint(int index)
    {
        int i = (int)((double)index * scale);
        Point p = new Point();
        p.x = i % displaySize.width;
        p.x += PEER_DISPLAY_RADIUS;
        p.y = i / displaySize.width;
        return p;
    }

    // Set message.
    public void setMessage(String s)
    {
        message = s;
    }

    // Draw message.
    private void drawMessage()
    {
        if (message != null && !message.equals(""))
        {
            imageGraphics.setFont(font);
            imageGraphics.setColor(Color.black);
            imageGraphics.drawString(message,
            (displaySize.width - fontMetrics.stringWidth(message)) / 2,
            displaySize.height / 2);
        }
    }
}

