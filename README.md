DodgyHTTPD: The intentionally dodgy HTTP Server

DodgyHTTPD is a horrible server: it will slow down and cut out on
command.  It's purpose in life is to help test if code is robust
enough to handle interrupted connections, slow connections, loss of 
connectivity etc.

There are plenty of nice http mock interfaces to test interacting with
APIs: there seems to be an underlying assumption you can reach the 
remote end.  I needed some quick to test when that's not the case, 
including with large(ish) file downloads etc.

It's built on the excellent NanoHTTPD.  It has one master server that
you use to run actual servers; and then tell it if you'd like it to 
mess with the requests.

```
mvn compile
mvn exec:java -Dexec.mainClass="com.ustadmobile.dodgyhttpd.DodgyHTTPDServer" -Dexec.args='-d "/path/to/filesdir" -p 8065'
```

Optional arguments
```
-p --port Main control listening port (default 8065)
-d --dir Base directory for assets that get served over http
-r --resultdir Directory to save result logs and raw socket logs to
-a -- rawport the start opening raw sockets on (default 3330)
```

Where starting port is the port number you want the control server to
listen to and rootDirectory is the path from which to serve files. Each
new server created will be listening on startingPort+1, +2, etc.

Many servers can run simultaneously.  The idea is that each test run can
makes it own test server and run without interferring with any others;
useful when using androids connected test runner that runs simultaneously
on all available emulators etc.

startingPort default is 8060

Start a new server:

```
GET http://localhost:8060/?action=newserver
```
Returns
```
{'status': 'OK', 'port': 8061}
```
port: the port on which the new server will be running

Stop a server:

Stop a server to see how code copes when the server is gone...

```
GET http://localhost:8060/?action=stopserver&port=8061
```
port: the port on which the server is running

returns:
```
{'stopped' : true}
```

Start a server:

Bring back a server that was previously stopped

example:
```
GET http://localhost:8060/?action=stopserver&port=8061
```
port: the port on which the server is running

returns:
```
{'started' : true}
```

Slow down responses:

example:
```
GET http://localhost:8060/?action=setparams&speedlimit=16000&forceerrorafter=400000&port=8061
```
speedlimit: The maximum download speed in bytes per second (e.g. 16K)
forceerrorafter: After serving this number of bytes the server will cut out.  This is useful to test resume abilities with unreliable connections.
port: the port on which the server is running

Save a test result:

example:
```
POST http://localhost:8060 with parameters
 action - "saveresults" - Required
 numPass - Number tests passed
 numFail - Number of tests failed
 logtext - Complete log text to be saved
 device - (optional) device name to be used with filename of results to be saved
```

Will save (devicename-)result which will contain PASS or FAIL depending on if numFail > 0 and
(devicename)-testresults.txt which will contain the logtext sent

Will be saved to the directory specified by -r or --resultdir in the command line arguments


Open a raw socket listener+logger

This can be handy for devices (e.g. J2ME) that lack their own real debug logging

example:
```
GET http://localhost:8060?action=newrawserver&client=nokia5000
```

returns:
```
{'port' : 3330, 'status' : 'OK'}
```

A socket will be opened on the port in the json response.  Whatever is sent to
that socket will be logged to the result save directory under raw-(client).log.

The client name must include only a-z,  A-Z, 0-9 or -.

