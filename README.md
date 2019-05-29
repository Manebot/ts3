<p align="center">
  <br/>
  <a href="https://discord.gg/qJPzQX3"><img height="28" src="https://img.shields.io/discord/563010101254815776.svg?label=Discord&logo=discord&style=for-the-badge"></a> <img height="28" src="https://img.shields.io/github/issues/manevolent/manebot-discord.svg?style=for-the-badge">
</p>

This is the reference implementation of the **Teamspeak3** platform for **Manebot**, my multi-platform (where platform means chat platform) chatbot framework. You can use this plugin to get Manebot to interact with your Teamspeak3 server(s) The integration is completely seamless; simply install the Teamspeak3 plugin to Manebot, add some Teamspeak3 servers, and watch your existing plugins/features auto-magically work on the Teamspeak platform!  This plugin fully supports the Manebot audio system, meaning you can turn Manebot into a Teamspeak3 music bot with this plugin.

The support for Teamspeak3 is provided through **ts3j**: https://github.com/Manevolent/ts3j

## Manebot

Manebot is a really neat plugin-based Java chatbot framework that allows you to write one "bot", and host it across several platforms. This plugin provides the **Discord** "*Platform*" to Manebot, which allows Manebot to seamlessly interact with Discord and provide all of the features your Manebot instance is set up to provide on Discord.

#### How do I make a bot with this?

You don't have to do anything specifically to make a bot for Teampseak3 with Manebot; the objective of Manebot is to act as middleware, abstracting the Teamspeak3 platform away from you as a developer and to provide you a platform-agnostic API to seamlessly port (or simoultaneously host) your bot in other platforms, such as Slack.

In summary, simply follow the guides on making a bot with Manebot, and just install Teamspeak3 when you're ready to test that platform!

## Installation

Manebot uses the **Maven** repository system to coordinate plugin and dependency installation. Because of this, you can easily install the Teamspeak3 platform plugin without interacting with your filesystem at all.

```
plugin install ts3
```

After you've installed Teamspeak3, you should enable it:

```
plugin enable ts3
```

... and that's it! Teamspeak3 will automatically start with Manebot, and even re-install itself if you "accidentally" the associated JAR files. It's got your back.

**Uninstall**

```
plugin uninstall ts3
```

You should restart Manebot too to make sure it's totally unplugged. You can clean up any no longer needed plugins it required with:

```
plugin autoremove
```

### Properties

| Property          	| Default 	| Required 	| Description                                                                                                                                                                                                                                        	|
|-------------------	|---------	|----------	|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------	|
| identity           	| (none)  	| No      	| Teamspeak3 Base64 identity string.  If you don't supply one, an identity is generated for you!                                                                                                                  	|
| keyOffset          	| 0       	| No       	| Teamspeak3 identity key offset, used to facilitate the **level**/proof-of-work system.  Automatically set as needed.                                                                      	|
| securityLevel      	| 10       	| No       	| Teamspeak3 target security level.  Identities are automatically improved to this value when needed.         	|

### Dependencies

The **Teamspeak3** plugin requires the following plugins at least be *installed*. Don't worry, if you don't have them installed, Manebot will automatically install them for you.  Maven magic.

* io.manebot.plugin:audio (or just "audio")
* io.manebot.plugin:media (or just "media")

## Supported Features

This plugin supports the following essential Teamspeak3 features:

* TS3DNS ready, connecting directly to Teamspeak3 as a single, full client.  No ServerQuery clients needed!
* Teamspeak3 audio system (audio broadcast as stereo Opus Music, capable of receiving Opus Voice/Music from clients for other plugins).
* Teamspeak3 user system, via user unique identity, associated with the global Manebot user & permission system.
* Automatic user registration via the default user registration implementation.
* *Markdown* text in chat (bold, italics, underline, strikethrough).
* Server, voice channel, and private chat channels.
* Lobby (bot default/sleep) and Away (inactive) channels, with idle timeout to move inactive clients.
* Connection events allow other plugins to audit & vett new client connections, including validating IP addresses.  Check out automatic ban plugins that utilize DNS blacklists to stop clients on VPNs from connecting to your servers, ban entire CIDRs, etc.
