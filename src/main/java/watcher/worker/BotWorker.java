package watcher.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import watcher.model.RepositoryLocator;
import watcher.model.bot.Bot;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BotWorker {

  public void updateAvailableBots(List<String> namesOfBotsInChannel) {
    List<Bot> botsInRepository = RepositoryLocator.bots().getAllWithoutLoadingPacks();

    addNewBots(botsInRepository, namesOfBotsInChannel);
    removeObsoleteBots(botsInRepository, namesOfBotsInChannel);
  }

  private void addNewBots(List<Bot> botsInRepository, List<String> namesOfBotsInChannel) {
    List<String> namesOfBotsInRepository = botsInRepository.stream()
        .map(Bot::nickname)
        .collect(Collectors.toList());

    Set<Bot> botsToAdd = namesOfBotsInChannel.stream()
        .filter(nickname -> !namesOfBotsInRepository.contains(nickname))
        .map(Bot::new)
        .collect(Collectors.toSet());

    LOGGER.debug("Adding {} newly available bots", botsToAdd.size());
    RepositoryLocator.bots().addAll(botsToAdd);
  }

  private void removeObsoleteBots(List<Bot> botsInRepository, List<String> namesOfBotsInChannel) {
    List<Bot> botsToRemove = botsInRepository.stream()
        .filter(bot -> !namesOfBotsInChannel.contains(bot.nickname()))
        .collect(Collectors.toList());

    LOGGER.debug("Removing {} bots which are not available anymore", botsToRemove.size());
    RepositoryLocator.bots().removeAll(botsToRemove);
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(BotWorker.class);
}