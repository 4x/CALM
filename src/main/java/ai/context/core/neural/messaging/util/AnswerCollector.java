package ai.context.core.neural.messaging.util;

import ai.context.core.neural.messaging.information.Answer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class AnswerCollector {

    private HashMap<String, LinkedList<Answer>> answers = new HashMap<>();

    public synchronized void addAnswer(Answer answer) {
        int i = 0;
        if (!answers.containsKey(answer.getqID())) {
            answers.put(answer.getqID(), new LinkedList<Answer>());
        }
        LinkedList<Answer> list = answers.get(answer.getqID());
        synchronized (list) {
            for (Answer existing : list) {
                if (answer.getConfidence() > existing.getConfidence()) {
                    break;
                }
                i++;
            }
            list.add(i, answer);
        }
    }

    public List<Answer> getTopAnswers(String questionID, int number) {
        LinkedList<Answer> answers = this.answers.remove(questionID);
        if (answers == null) {
            return new LinkedList<>();
        }

        return answers.subList(0, Math.min(number, answers.size() - 1));
    }
}
