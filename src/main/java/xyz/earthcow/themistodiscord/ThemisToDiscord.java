package xyz.earthcow.themistodiscord;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.exception.HttpException;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class ThemisToDiscord extends JavaPlugin {
    public static ThemisToDiscord instance;
    public static WebhookClient client;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        WebhookClient.setDefaultErrorHandler((client, message, throwable) -> {
            getLogger().severe(String.format("[%s] %s%n", client.getId(), message));

            if (throwable instanceof HttpException ex && ex.getCode() == 404) {
                client.close();
                return;
            }

            if (throwable != null)
                throwable.printStackTrace();
        });

        TtdCommand ttdCommand = new TtdCommand();
        Objects.requireNonNull(getCommand("ttd")).setExecutor(ttdCommand);
        Objects.requireNonNull(getCommand("ttd")).setTabCompleter(ttdCommand);

        String webhookUrl = getConfig().getString("webhookUrl");

        if (isInvalidWebhookUrl(webhookUrl)) {
            getLogger().warning("Webhook url is missing or invalid! Set one using /ttd url <url>");
            return;
        }

        initializeWebhook(webhookUrl);

    }

    @Override
    public void onDisable() {
        if (client != null && !client.isShutdown()) {
            client.close();
        }
    }

    public static boolean isInvalidWebhookUrl(@Nullable String url) {
        if (url == null) return true;
        return !WebhookClientBuilder.WEBHOOK_PATTERN.matcher(url).matches();
    }

    public static void initializeWebhook(String webhookUrl) {
        if (isInvalidWebhookUrl(webhookUrl)) return;

        WebhookClientBuilder builder = new WebhookClientBuilder(webhookUrl);
        builder.setThreadFactory((job) -> {
            Thread thread = new Thread(job);
            thread.setName("ThemisWebhookThread");
            thread.setDaemon(true);
            return thread;
        });
        builder.setWait(true);

        if (client != null) {
            client.close();
        }

        client = builder.build();
    }
}
