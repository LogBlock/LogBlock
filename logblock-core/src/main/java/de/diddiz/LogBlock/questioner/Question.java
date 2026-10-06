package de.diddiz.LogBlock.questioner;

import org.bukkit.entity.Player;

public class Question {
    private String answer;
    private final String[] answers;
    private final String questionMessage;
    private final Player respondent;
    private final long start;

    public Question(Player respondent, String questionMessage, String[] answers) {
        this.start = System.currentTimeMillis();
        this.respondent = respondent;
        this.questionMessage = questionMessage;
        this.answers = answers;
    }

    public synchronized String ask() {
        StringBuilder options = new StringBuilder();
        for (String ans : this.answers) {
            options.append("/" + ans + ", ");
        }
        options.delete(options.length() - 2, options.length());
        this.respondent.sendMessage(this.questionMessage);
        this.respondent.sendMessage("- " + options + "?");
        while (answer == null) {
            try {
                wait();
            } catch (InterruptedException ex) {
                if (answer == null) {
                    answer = "interrupted";
                }
            }
        }
        return this.answer;
    }

    public synchronized boolean isExpired(boolean forceExpire) {
        if (forceExpire || System.currentTimeMillis() - this.start > 120000L || this.answer != null) {
            if (answer == null) {
                answer = "timed out";
            }
            notifyAll();
            return true;
        }
        return false;
    }

    public boolean returnAnswer(String answer) {
        return returnAnswer(answer, false);
    }

    public synchronized boolean returnAnswer(String answer, boolean forceReturn) {
        if (forceReturn) {
            if (this.answer == null) {
                this.answer = answer;
            }
            notifyAll();
            return true;
        }
        for (String s : answers) {
            if (s.equalsIgnoreCase(answer)) {
                if (this.answer == null) {
                    this.answer = s;
                }
                notifyAll();
                return true;
            }
        }
        return false;
    }
}
