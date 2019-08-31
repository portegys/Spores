/**
 * Spores simulated peer.
 */

package spores;

import java.util.*;

// Spores peer.
public class SporesPeer
{
    boolean connected;
    int[] connectedPeers;
    boolean hasFile;
 
    // Constructor.
    public SporesPeer()
    {
        connected = false;
        hasFile = false;
    }
    
    // Connect to network, setting connected peers.
    void connect(int[] connectedPeers)
    {
        this.connectedPeers = connectedPeers;
        connected = true;
    }
    
    // Disconnect.
    void disconnect()
    {
        connected = false;
    }
    
    // Get connection state.
    boolean isConnected()
    {
        return connected;
    }
    
    // Get connected list.
    int[] getConnected()
    {
        return connectedPeers;
    }
   
    // Store file.
    void storeFile()
    {
        hasFile = true;
    }
   
    // Remove file.
    void removeFile()
    {
        hasFile = false;
    }
    
    // Peer has file?
    boolean hasFile()
    {
        return hasFile;
    }
}
