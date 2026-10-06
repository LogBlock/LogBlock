package de.diddiz.LogBlock.questioner;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import de.diddiz.LogBlock.LogBlock;

public class Questioner {
    private final LogBlock logBlock;
    private final ConcurrentHashMap<UUID, Question> questions = new ConcurrentHashMap<>();

    public Questioner(LogBlock logBlock) {
        this.logBlock = logBlock;
        logBlock.getServer().getPluginManager().registerEvents(new QuestionerListener(), logBlock);
        logBlock.getServer().getScheduler().scheduleSyncRepeatingTask(logBlock, new QuestionsReaper(), 600, 600);
    }

    public String ask(Player respondent, String questionMessage, String... answers) {
        if (Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("This method may not be called from the primary thread");
        }
        Question question = new Question(respondent, questionMessage, answers);
        Question oldQuestion = this.questions.put(respondent.getUniqueId(), question);
        if (oldQuestion != null) {
            oldQuestion.returnAnswer("no", true);
            // wait a little time to let the other thread continue
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return question.ask();
    }

    private class QuestionsReaper implements Runnable {
        @Override
        public void run() {
            if (questions.isEmpty()) {
                return;
            }
            Iterator<Entry<UUID, Question>> it = questions.entrySet().iterator();
            while (it.hasNext()) {
                Entry<UUID, Question> e = it.next();
                Question question = e.getValue();
                if (question.isExpired(logBlock.getServer().getPlayer(e.getKey()) == null)) {
                    it.remove();
                }
            }
        }
    }

    private class QuestionerListener implements Listener {
        @EventHandler(ignoreCancelled = true)
        public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
            UUID player = event.getPlayer().getUniqueId();
            Question question;
            question = questions.get(player);
            if (question != null) {
                String answer = event.getMessage().substring(1).toLowerCase();
                if (question.returnAnswer(answer)) {
                    questions.remove(player, question);
                    event.setCancelled(true);
                }
            }
        }
    }
}
