# ticket-bot
- A Discord Bot written in java, utilizing the JavaCord https://javacord.org/ library.
- Provides a ticket system which allows members of a server to create a variety of types of channels to speak with the staff team of that server in a streamlined and efficient way.

## Setting up the bot for your server

- Create an application -> bot in the discord developer portal https://discord.com/developers/applications
- Add your bot's token to the BOTTOKEN environment variable AND to a .env file in the root of the project
- Set up the Ticket objects to suit your server, making sure to include the correct role-ids for your server
- Adjust role based priviledges to however you need
- Add the `[server_id] : channel id` to `serverTicketSavedChannels` in `transcript.js` for the channel to send transcripts / ticket close messages to.
- Run Main.main()
  - This should print the invite link needed to invite your bot to your server, if you havent done that already.
