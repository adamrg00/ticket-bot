const discordTranscripts = require('discord-html-transcripts');
const {Client, Events, GatewayIntentBits} = require('discord.js');
const Console = require("console");
const dotenv = require('dotenv').config();
const channelId = process.argv[2];
console.log(channelId)
if(!channelId) {
    console.log("No channel provided");
    process.exit()
}
const client = new Client({ intents: [GatewayIntentBits.Guilds] });
client.login(process.env.BOTTOKEN)

// client.once(Events.ClientReady, c => {
//     console.log(`Client Logged in as ${c.user.tag}`);
//     client.channels.fetch(channelId).then(res => {
//         Console.log(res)
//         Console.log(res.name)
//         process.exit()
//     })
// });
client.on("ready", c => {
    console.log(c)
    console.log(c.guilds)
    process.exit()
});
