const discordTranscripts = require('discord-html-transcripts');
const {Client, GatewayIntentBits} = require('discord.js');
const Console = require("console");
const dotenv = require('dotenv').config();
const serverId = process.argv[2];
const channelId = process.argv[3];
const serverTicketSavedChannels = {
    ["1030491410954199071"] : "1034496481329172623",// Test Server
    ["776550060732317737"] : "1016467420833140766" // Raze Server
}
if(!channelId || !serverId) {
    console.log("No channel provided");
    process.exit()
}

const client = new Client({ intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildPresences] });
client.login(process.env.BOTTOKEN)

client.on("ready", async c => {
    console.log(c.user.tag + " Logged in")
    const guild = c.guilds.cache.get(serverId)
    const ticketChannel = guild.channels.cache.get(channelId)
    const attachment = await discordTranscripts.createTranscript(ticketChannel);
    const savedTicketsChannel = (c.guilds.cache.get(serverId)).channels.cache.get(serverTicketSavedChannels[serverId])
    savedTicketsChannel.send({
        content: "transcript " + channelId,
        files: [attachment],
    }).then(() => {
        process.exit()
    })
});
