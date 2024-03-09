Launch all servers BEFORE starting client. Client will not connect to anything without.

Note: current working directory must be parent directory.

1. Start database server, specify port.
java DatabaseServer.DatabaseServer <Port>

2. Start user account server, port is set to 8081.
java UserAccountServer.UserAccountServer

3. Start game server, specify IP address, port and database port.
java GameServer.Game <localhost> <Port> <Database Port>

4. Start client, specify game port.