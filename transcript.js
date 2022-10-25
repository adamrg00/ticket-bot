const discordTranscripts = require('discord-html-transcripts');
const {Client, Events, GatewayIntentBits, TextChannel, GuildBasedChannel} = require('discord.js');
const Console = require("console");
const dotenv = require('dotenv').config();
const serverId = process.argv[2];
const channelId = process.argv[3];
if(!channelId || !serverId) {
    console.log("No channel provided");
    process.exit()
}

const client = new Client({ intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildPresences] });
client.login(process.env.BOTTOKEN)

client.on("ready", async c => {
    console.log(c.user.tag + " Logged in")
    const guild = c.guilds.cache.get(serverId)
    const channel = guild.channels.cache.get(channelId)
    const attachment = await discordTranscripts.createTranscript(channel);
    channel.send({
        content: "transcript",
        files: [attachment],
    }).then(() => {
        process.exit()
    })
});
