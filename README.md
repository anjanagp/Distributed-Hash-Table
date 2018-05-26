# Distributed-Hash-Table

Designed a simple Distributed Hash Table (DHT) using Chord protocol. 

Chord is a distributed lookup protocol that helps efficiently locate the node that stores a particular data item in peer to peer 
(P2P) applications.  Chord is scalable, with communication cost
and the state maintained by each node scaling logarithmically with
the number of Chord node

Implemented 1) ID space partitioning/re-partitioning, 2) Ring-based routing, and 3) Node joins

The content provider implements all DHT functionalities and support insert and query operations. Thus, if you run multiple instances of the app, all content provider instances form a Chord ring and serve insert/query requests in a distributed fashion according to the Chord protocol.

