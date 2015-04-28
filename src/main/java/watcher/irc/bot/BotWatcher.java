package watcher.irc.bot;

import com.google.common.collect.Lists;
import fr.vter.xdcc.infrastructure.persistence.mongo.MongoLinkContext;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import watcher.worker.BotWorker;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

public class BotWatcher extends PircBot {

  @Inject
  public BotWatcher(MongoLinkContext mongoLinkContext, BotWorker botWorker) {
    this.mongoLinkContext = mongoLinkContext;
    this.botWorker = botWorker;

    setName("xdcc-botwatcher");
    setLogin("xdcc-botwatcher");
    setAutoNickChange(true);
  }

  @Override
  protected void onConnect() {
    LOGGER.debug("{} connected to server", getClass().getSimpleName());
  }

  @Override
  protected void onDisconnect() {
    LOGGER.debug("{} disconnected from server", getClass().getSimpleName());
    dispose();
  }

  @Override
  protected void onUserList(String channel, User[] channelUsers) {
    List<User> users = Lists.newArrayList(channelUsers);
    List<String> namesOfBotsInChannel = users.stream()
        .filter(this::isABot)
        .map(bot -> stripChannelPrefix(bot.getNick()))
        .collect(Collectors.toList());

    if (!namesOfBotsInChannel.isEmpty()) {
      try {
        mongoLinkContext.beforeExecution();
        botWorker.updateAvailableBots(namesOfBotsInChannel);
        mongoLinkContext.afterExecution();
      } catch (Throwable e) {
        mongoLinkContext.onError();
        LOGGER.error("Error updating available bots", e);
      } finally {
        mongoLinkContext.ultimately();
      }
    } else {
      LOGGER.warn("No bot found on {}", channel);
    }

    disconnect();
  }

  private boolean isABot(User user) {
    return botPrefixes.stream().anyMatch(prefix -> user.getNick().startsWith(prefix));
  }

  private String stripChannelPrefix(String nickname) {
    return nickname.substring(1);
  }

  private final MongoLinkContext mongoLinkContext;
  private final BotWorker botWorker;
  private static final List<String> botPrefixes = Lists.newArrayList("%[SeriaL]", "%[DarksiDe]", "%[Darkside]", "%iNFEXiOUS");
  private static final Logger LOGGER = LoggerFactory.getLogger(BotWatcher.class);
}
