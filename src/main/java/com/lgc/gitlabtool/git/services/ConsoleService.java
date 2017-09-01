package com.lgc.gitlabtool.git.services;

import java.util.List;

import com.lgc.gitlabtool.git.entities.ConsoleMessage;
import com.lgc.gitlabtool.git.entities.MessageType;
import com.lgc.gitlabtool.git.listeners.updateConsole.UpdateConsoleGenerator;

/**
 * Interface for working with the UI console.
 * It allows add new message definite type for the UI console.
 * Also allows get all messages of console and clear them.
 *
 * All added messages duplicated in the log.
 *
 * @author Lyudmila Lyska
 */
public interface ConsoleService extends UpdateConsoleGenerator{

    /**
     * Adds new message to UI console.
     *
     * @param message the text for console
     * @param type the type of message (ERROR, SUCCESS or SIMPLE)
     */
    void addMessage(String message, MessageType type);

    /**
     * Gets all messages
     *
     * @return a list of messages
     */
    List<ConsoleMessage> getMessages();

    /**
     * Removes all messages and clears UI console.
     */
    void removeAll();
}