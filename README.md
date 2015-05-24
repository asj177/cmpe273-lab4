cmpe273-lab6
============

CMPE 273 Lab 6 Baseline

Change in the Server Module :
Previously the server was not able to modify ,hence updated the InMemoryCache file from putIfAbsent to put ,so that it updates the values .

Change in Client Moddule:

Added a CRDTClient file which has the functionality to read-repair and put the key value pairs in all the 3 servers .
DistributedCacheService which does the asynchronous get and put .
Have used the CountDownLatch for synchoization purpose so that the main threadexecutes only when the values are written into all the 3 servers .(First initialized to 3 ,and then everytime a get or put occurs decrement happens )


Sample Output :


Starting Cache Client...
Write operation...

value put on serverhttp://localhost:3002

value put on serverhttp://localhost:3001

value put on serverhttp://localhost:3000

put (1 => a)

value put on serverhttp://localhost:3002

value put on serverhttp://localhost:3001

Update failed on http://localhost:3000

put (1 => b)
Server http://localhost:3002 has value b
Server http://localhost:3001 has value b
Server http://localhost:3000 has value a
hi this is in completed
value put on serverhttp://localhost:3000
Step 3: get(1) => b
Exiting Client...


