# Spying-Socks-Proxy
Implementation of a limited version of SOCKS v4 proxy server, with an additional feature: grabbing
usernames and password from HTTP Basic Authentication (as mentioned in RFC7617).

Sockspy.java - Create a Socket listening on port 8080 and enable 20 concurrent connections.

Proxy.java - Handle SOCKS CONNECT Request and SOCKS CONNECT Reply. Afterwards, create two threads running RunThread.java class. Finally, close connections.

RunThread.java - Check for HTTP features using Regex - GET request method, password in Authorization header and optional sub-URL. Finally, transfer data.
