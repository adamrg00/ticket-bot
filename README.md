# ticket-bot
- A Discord Bot written in java, utilizing the [JavaCord](https://javacord.org/) library.
- Provides a ticket-based system, allowing members of a server to create a variety of types of ***customisable and reconfigurable*** channels to speak with the staff team of that server in a streamlined fashion.
- Includes a plethora of commands to use in tickets for maximum efficiency.
- Current iteration has been optimised for the Raze Network Discord server, but it is easily adaptable to many different needs.

## Setting up the bot for your server

- Make sure that you have a MongoDB Server running : can download [here](https://www.mongodb.com/docs/manual/installation/) or change the URL in the Mongo class if using an atlas deployment.
- Create a [discord application](https://discord.com/developers/applications) -> then create a bot inside of your application
- Add your bot's token to the BOTTOKEN environment variable AND to a .env file in the root of the project
- Set up the Ticket objects to suit your server, making sure to include the correct role-ids for your server
- Adjust role based priviledges to however you need
- Add the `[server_id] : channel id` to `serverTicketSavedChannels` in `transcript.js` for the channel to send transcripts / ticket close messages to.
- Run Main.main()
  - This should print the invite link needed to invite your bot to your server, if you havent done that already.
