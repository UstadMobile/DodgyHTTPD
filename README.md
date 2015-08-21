DodgyHTTPD: The intentionally dodgy HTTP Server

DodgyHTTPD is a horrible server: it will slow down and cut out on
command.  It's purpose in life is to help test if code is robust
enough to handle interrupted connections, slow connections, loss of 
connectivity etc.

It's built on the excellent NanoHTTPD.  It has one master server that
you use to run actual servers; and then tell it if you'd like it to 
mess with the requests.

```
java -jar <pathtojar> -p <startingPort> -d <rootDirectory>
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
GET http://localhost:8060/?action=startnewserver
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
















