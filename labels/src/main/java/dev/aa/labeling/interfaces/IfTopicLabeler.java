package dev.aa.labeling.interfaces;

import dev.aa.labeling.model.Topic;

public interface IfTopicLabeler {
    void processTopic(Topic topic);
    boolean isStopped();
}
