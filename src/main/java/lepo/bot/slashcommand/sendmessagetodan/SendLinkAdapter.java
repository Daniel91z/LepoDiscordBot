package lepo.bot.slashcommand.sendmessagetodan;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static lepo.bot.slashcommand.sendmessagetodan.SendLinkCommand.SEND_LINK_COMMAND;
import static lepo.bot.slashcommand.sendmessagetodan.SendLinkCommand.SEND_LINK_DESCRIPTION;


@Component
@Slf4j
public abstract class SendLinkAdapter extends ListenerAdapter {

    private final String adminId;
    private static final String LINK = "link";

    protected SendLinkAdapter(String adminId) {
        this.adminId = adminId;
    }

    public SlashCommandData sendLink(String commandName, String description) {
        log.info("Loading command {}", commandName);
        return new CommandDataImpl(commandName, description)
                .addOptions(new OptionData(OptionType.STRING, LINK, SEND_LINK_DESCRIPTION, true))
                .setGuildOnly(true);
    }

    /**
     * Sends a message to the admin.
     * inputMessage is the message sent in the event.
     * memberName is the name of the user sending the link.
     * effectiveMemberName is the effective name of the user sending the link.
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals(SEND_LINK_COMMAND)) {
            String inputMessage = Optional.ofNullable(event.getOption(LINK))
                    .map(OptionMapping::getAsString)
                    .orElse("");
            String memberName = Optional.ofNullable(event.getMember())
                    .map(Member::getUser)
                    .map(User::getName)
                    .orElse("");
            String effectiveMemberName = Optional.ofNullable(event.getMember())
                    .map(Member::getUser)
                    .map(User::getEffectiveName)
                    .orElse("");
            Optional.ofNullable(event.getJDA().getUserById(adminId))
                    .ifPresentOrElse(
                            admin -> admin.openPrivateChannel()
                                    .flatMap(channel -> channel.sendMessage(String.format("(%s) %s propone l'audio %s", effectiveMemberName, memberName, inputMessage)))
                                    .queue(
                                            success -> event.reply("Messaggio inviato").setEphemeral(true).queue(setInteractionTimeout()),
                                            error -> event.reply("Non sono riuscito a inviare il messaggio").setEphemeral(true).queue(setInteractionTimeout())
                                    ),
                            () -> event.reply("Impossibile inviare messaggio").setEphemeral(true).queue(setInteractionTimeout())
                    );
        }
    }

    private Consumer<InteractionHook> setInteractionTimeout() {
        return interactionHook -> {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.schedule(() -> interactionHook.deleteOriginal().queue(), 3, TimeUnit.SECONDS);
        };
    }

}