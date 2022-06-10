README

Spores

Copyright 2003 by Tom Portegys, all rights reserved.
-------------------------------------------------------------------------------

DESCRIPTION

Spores is a push and pull peer-to-peer method of file sharing and
storage, making use of publicly available space on the network.
A user stores a file by pushing it to a set of peers. The
file then becomes visible and available to remote peers that
search for it. Spores allows the exchange of folders as well as
individual files.

Private and shared files and folders:

Private files/folders are visible only to the local peer. Shared
files/folders are stored by remote peers and are visible and available
for download by peers that search for them. Shared and private files
may be copied between private and shared space at will. You may
also choose where to store private and shared files.

Uniqueness code:

To ensure a search or download references a desired file or folder,
you may optionally provide a uniqueness code to further qualify the
search or download. The target file or folder must then match not only
the given name, but also the code.

Properties:

You can specify how many shared files can be stored as well as how
much space is available for them. You can also specify shared file
extensions that will be accepted or excluded. For example, if you
want to only accept .mp3 and .wav files, you would enter .mp3,.wav
in the text input field. Conversely if you want to accept all files
except .jpg and .mpeg files, you would enter -.jpg,-.mpeg

Connections:

You can edit the initial connections that Spores makes
to find other Spores on the web.

To run manually:
java -classpath spores.jar spores.Spores

Files:
peer.txt - list of initial peer addresses (format=IP address:port).
webcache.txt - list of gwebcache URLs (see www.gnucleus.com/gwebcache/).
webcache-discovery.txt - list of discovered gwebcache URLs.
properties.txt - properties file.

Folders/directories:
private-files - private file folder.
shared-files - shared file folder.
transfer-files - transfer file folder.
javadoc - Java documentation.
src - Java source code.

IMPORTANT NOTE:
If Spores does not run, you probably do not have Java installed.
Spores requires Java 1.3, which is freely available at www.java.com

-------------------------------------------------------------------------------

RELEASE INFORMATION
-------------------------------------------------------------------------------
v0.1

Reference:
T.E. Portegys, "Spores: a Push and Pull Peer-to-Peer File Sharing Approach",
The 2004 International Conference on Parallel and Distributed Processing Techniques and Applications (PDPTA'04)
https://www.researchgate.net/publication/2940771_Spores_a_Push_and_Pull_Peer-to-Peer_File_Sharing_Approach



