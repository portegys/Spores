/*
 * This software is provided under the terms of the GNU General
 * Public License as published by the Free Software Foundation.
 * Copyright (c) 2003 by Tom Portegys, All Rights Reserved.
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.
 */
package spores;

import java.awt.*;


/**
 * Spores parameters.
 */
public interface Parameters {
   /**
    * Version.
    */
   static final double VERSION = 0.1;

   /**
    * Spores primary web addresses.
    */
   static final String SPORES_WWW_0 = "www.spores.com";
   static final String SPORES_WWW_1 = "itklinux.itk.ilstu.edu";

   // Control tabs.
   static final int PRIVATE_FILES     = 0;
   static final int SHARED_FILES      = 1;
   static final int TRANSFER          = 2;
   static final int SPORES_PROPERTIES = 3;
   static final int CONNECTIONS       = 4;
   static final int READ_ME           = 5;

   // Operations.
   static final int SEARCH   = 0;
   static final int DOWNLOAD = 1;
   static final int UPLOAD   = 2;
   static final int NOP      = -1;

   // File types.
   static final byte FILE   = (byte)0;
   static final byte FOLDER = (byte)1;

   /**
    * Window size.
    */
   static final Dimension WINDOW_SIZE = new Dimension(485, 525);

   /**
    * Default private file folder.
    */
   static final String DEFAULT_PRIVATE_FOLDER = "private-files";

   /**
    * Default shared file folder.
    */
   static final String DEFAULT_SHARED_FOLDER = "shared-files";

   /**
    * Default maximum number of shared files.
    */
   static final int DEFAULT_MAX_SHARED_FILES = 1000;

   /**
    * Default maximum shared files total size (kbytes).
    */
   static final int DEFAULT_MAX_SHARED_SIZE = 10000;

   /**
    * Transfer file folder.
    */
   static final String TRANSFER_FOLDER = "transfer-files";

   /**
    * Maximum transferred file size (bytes).
    */
   static final int MAX_TRANSFER_FILE_SIZE = 500000000;

   /**
    * Maximum line size.
    */
   static final int MAX_LINE_SIZE = 500;

   /**
    * Data block size (bytes).
    */
   static final int BLOCK_SIZE = 4096;

   /**
    * Peer address file.
    */
   static final String PEER_FILE = "peer.txt";

   /**
    * Maximum number of pending peers.
    */
   static final int MAX_PENDING_PEERS = 100;

   /**
    * Maximum number of connected peers.
    */
   static final int MAX_CONNECTED_PEERS = 10;

   /**
    * Maximum number of transmitted peers.
    */
   static final int MAX_TRANSMITTED_PEERS = 10;

   /**
    * Web cache address file.
    */
   static final String WEB_CACHE_FILE = "webcache.txt";

   /**
    * Discovered web cache address file.
    */
   static final String WEB_CACHE_DISCOVERY_FILE = "webcache-discovery.txt";

   /**
    * Maximum number of discovered web caches.
    */
   static final int MAX_WEB_CACHE_DISCOVERED = 10;

   /**
    * Properties save file.
    */
   static final String PROPERTIES_FILE = "properties.txt";

   /**
    * Spores port.
    */
   static final int SPORES_PORT = 8944;

   /**
    * Spores gnutella vendor code.
    */
   static final String SPORES_VENDOR_CODE = "SPOR";

   /**
    * Request code for peers.
    */
   static final String PEER_REQUEST = "PEER_REQ";

   /**
    * Request code for search.
    */
   static final String SEARCH_REQUEST = "SEARCH_REQ";

   /**
    * Request code for download.
    */
   static final String DOWNLOAD_REQUEST = "DOWNLOAD_REQ";

   /**
    * Request code for upload.
    */
   static final String UPLOAD_REQUEST = "UPLOAD_REQ";

   /**
    * Request code for confirmation.
    */
   static final String CONFIRMATION_REQUEST = "CONFIRM_REQ";

   /**
    * Positive response.
    */
   static final byte POSITIVE_RESPONSE = (byte)1;

   /**
    * Negative response.
    */
   static final byte NEGATIVE_RESPONSE = (byte)0;

   /**
    * Transfer thread delay (ms).
    */
   static final long TRANSFER_DELAY = 100;

   /**
    * Socket time-out (ms).
    */
   static final int SOCKET_TIME_OUT = 5000;

   /**
    * Maximum network search depth.
    */
   static final long MAX_SEARCH_DEPTH = 10;

   /**
    * Search time-out (ms).
    */
   static final int SEARCH_TIME_OUT = 60000;

   /**
    * Maximum upload attempts.
    */
   static final long MAX_UPLOAD_ATTEMPTS = 1000;

   /**
    * Connection check frequency (ms).
    */
   static final int CONNECTION_CHECK_FREQ = 5000;

   /**
    * Peer confirmation frequency (ms).
    */
   static final int CONFIRMATION_FREQ = 10000;

   /**
    * Version notice display time (ms).
    */
   static final int VERSION_NOTICE_TIME = 5000;
}
;
