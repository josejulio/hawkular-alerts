/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.notifiers.api.log;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

/**
 * Common log for INFO, WARN, ERROR and FATAL messages.
 *
 * @author Lucas Ponce
 */
@MessageLogger(projectCode = "HAWKALERT")
@ValidIdRange(min = 240000, max = 249999)
public interface MsgLogger extends BasicLogger {
    MsgLogger LOGGER = Logger.getMessageLogger(MsgLogger.class, MsgLogger.class.getPackage().getName());

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 240001, value = "Plugin [%s] has received a notification message: [%s]")
    void infoNotificationReceived(String notifierType, String msg);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 240002, value = "Plugin [%s] has received a notifier Id: [%s] registration request: [%s]")
    void infoNotifierRegistrationReceived(String notifierType, String notifierId, String msg);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 240003, value = "Plugin [%s] hast sent a registration request: [%s]")
    void infoPluginRegistration(String notifierType, String msg);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 240004, value = "Plugin [%s] has been unregistered")
    void infoPluginUnregistered(String notifierType);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 240005, value = "Plugin [%s] cannot send a message to the bus. Error: [%s]")
    void errorCannotSendMessage(String notifierType, String msg);

}
