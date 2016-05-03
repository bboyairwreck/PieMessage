# PieMessage - iMessage on Android
This is project allows Android clients to communicate using iMessage.

## Video Demonstration
<a href="http://www.youtube.com/watch?feature=player_embedded&v=rcoX-uiDNs4
" target="_blank"><img src="http://img.youtube.com/vi/rcoX-uiDNs4/0.jpg" 
alt="IMAGE ALT TEXT HERE" width="240" height="180" border="10" /></a>

## Disclaimer
This project is a super alpha prototype. I am releasing it because I need your help. :)

## About

There are 4 parts to the PieMessage project.
- **messages.applescript**
- **Java Web Server (JWS)**
- **OSX Client**
- **Android Client**

The [messages.applescript](./messages.applescript) is arguably the most important part of the project. It is what makes sending iMessages possible. This script is what sends an iMessage message.

The [OSX Client](./PieOSXClient) & [JWS](./JavaWebServer) run on any OSX machine (Macbook, Mac, etc.).

If the [OSX Client](./PieOSXClient) detects any changes to the "Messages" sqlite database file where a new message has been received, it will send the JWS a socket *'incoming'* JSON message. Incoming messages are detected from a change from the sqlite chat.db of the 'Messages' app whose default location is ~/Library/Messages/chat.db. I have provided a database schema to help visualize the database in the pdf, [MessagesSchema.pdf](./MessagesSchema.pdf). 

The [JWS](./JavaWebServer) is what connects the OSX Client to the Android client. If the JWS receives a socket 'outgoing' JSON message from the Android client, it will pass it to the OSX Client to tell it to send the iMessage that was requested from the Android. If the JWS recieves a socket *'incoming'* JSON message from the OSX Client, it means the OSX Client has detected a new message and wants the JWS to notify the Android client.

The [Android client](./PieMessage-Android/) connects to a socket that whose IP address is of the OSX device that is running the JWS and OSX Client. It then sends JSON messages to the JWS using that socket. It also receives JSON to show in list of any new incoming iMessages.

## Requirements
- OSX device
- Public IP for OSX device
- iCloud account w/ iMessage enabled
- Java JDK
- Android device (4.0+)

#### Optional Requirement
- IntelliJ IDEA
- Android Studio

## Set up
### On OSX Device
1. Open the Messages application and add your iCloud account in Messages > Preferences > Accounts.
2. Clone the PieMessage project onto your OSX Device.
3. Move [messages.applescript](./messages.applescript) to your ~ home directory (/Users/<username>).
4. Open the [JavaWebServer/](./JavaWebServer) as a project in IntelliJ. Run the [Server](./JavaWebServer/src/Server.java) class.
5. Open the [PieOSXClient/](./PieOSXClient) as a project in IntelliJ.
6. Edit the *socketAddress* value in [PieOSXClient/src/Constants.java](./PieOSXClient/src/Constants.java) to your public IP address that is linked to your OSX device.
7. Run [PieOSXClient](PieOSXClient/src/PieOSXClient.java) class.

### On Android device
1. Open [PieMessage-Android/](./PieMessage-Android/) as a project in Android Studio.
2. Edit the *socketAddress* value in [/app/.../Constants.java](./PieMessage-Android/app/src/main/java/com/ericchee/bboyairwreck/piemessage/Constants.java) to your public IP address that is linked to your OSX device.
3. Compile apk to any Android device.

## WishList
Since I've moved onto other projects and haven't had time to finish this, there are few things that wanted to implement. It would be nice to combine the OSX Client and the JWS. Also the OSX Client sometimes timesout and loses socket connection over a 2 hour+ period. I'm not sure if this is my own internet, the OSX it self.

Also it is possible to recieve group messaging, just not send it. Unfortunately I couldnt figure out an applescript to send to multiple clients in a single conversation thread. You definitely can send multiple *individual* messages at once but that still isn't in the same conversation thread. The only reason why we can receive is because it's just a chat table in the sqlite database on the OSX device.

Photo/Video messages are definitely possible too. There is a place in the SQLite table named *message_attachment_join* and *attachment*. It just has to link that in some kind of protocol for the OSX, JWS, and Android client to implement.

There can be clients for any platform like Windows, web, BlackBerry OS, Windows Phone, a toaster, etc. Since the JWS, and OSX Client just take in a JSON to send & receive, one just has to implement a client that is similar to the way the Android client communicates with it.





