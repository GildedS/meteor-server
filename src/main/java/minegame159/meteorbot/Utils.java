package minegame159.meteorbot;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import minegame159.meteorbot.database.Db;
import minegame159.meteorbot.database.documents.JoinStats;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;

import java.awt.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Updates.inc;

public class Utils {
    private static final Color EMBED_COLOR = new Color(204, 0, 0);

    public static EmbedBuilder embed(String format, Object... args) {
        return new EmbedBuilder()
                .setColor(EMBED_COLOR)
                .setDescription(String.format(format, args));
    }

    public static int count(String str, Pattern pattern) {
        Matcher matcher = pattern.matcher(str);
        int count = 0;

        while (matcher.find()) count++;

        return count;
    }

    public static void updateMemberCount(Guild guild, boolean joined) {
        VoiceChannel channel = findMemberCount(guild);
        if (channel != null) channel.getManager().setName("Member Count: " + guild.getMemberCount()).queue();

        String date = getDateString();
        JoinStats joinStats = Db.JOIN_STATS.get(date);

        if (joinStats != null) {
            if (joined) {
                Db.JOIN_STATS.update(date, inc("joins", 1));
            } else {
                Db.JOIN_STATS.update(date, inc("leaves", 1));
            }
        } else {
            Db.JOIN_STATS.add(new JoinStats(date, joined ? 1 : 0, joined ? 0 : 1));
        }
    }

    public static String getDateString() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTime(new Date());

        return String.format("%d-%02d-%02d", calendar.get(Calendar.YEAR), calendar.get(Calendar.DATE), calendar.get(Calendar.MONTH) + 1);
    }

    private static VoiceChannel findMemberCount(Guild guild) {
        for (VoiceChannel channel : guild.getVoiceChannelCache()) {
            if (channel.getName().startsWith("Member Count: ")) return channel;
        }

        return null;
    }

    public static String getMcUuid(String username) {
        HttpResponse<JsonNode> response = Unirest
                .post("https://api.mojang.com/profiles/minecraft")
                .header("Content-Type", "application/json")
                .body(new JSONArray().put(username))
                .asJson();

        try {
            return response.getBody().getArray().getJSONObject(0).getString("id");
        } catch (Exception ignored) {
            return null;
        }
    }

    public static String getMcUsername(String uuid) {
        HttpResponse<JsonNode> response = Unirest
                .get("https://api.mojang.com/user/profiles/" + uuid + "/names")
                .asJson();

        try {
            String username = null;
            long time = 0;

            JSONArray array = response.getBody().getArray();
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);

                if (!o.has("changedToAt")) o.put("changedToAt", 1);

                if (o.getLong("changedToAt") > time) {
                    username = o.getString("name");
                    time = o.getLong("changedToAt");
                }
            }

            return username;
        } catch (Exception ignored) {
            return null;
        }
    }
}
