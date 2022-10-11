/* **************************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License 2.0 which is available at http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ************************************************************************************** */
package org.calypsonet.terminal.reader.selection;

import org.calypsonet.terminal.reader.CardCommunicationException;
import org.calypsonet.terminal.reader.CardReader;
import org.calypsonet.terminal.reader.ObservableCardReader;
import org.calypsonet.terminal.reader.ReaderCommunicationException;
import org.calypsonet.terminal.reader.selection.spi.CardSelection;

/**
 * Service dedicated to card selection, based on the preparation of a card selection scenario.
 *
 * <p>A card selection scenario consists of one or more selection cases based on a {@link
 * CardSelection}.<br>
 * A card selection case targets a specific card. <br>
 * Optionally, additional commands can be defined to be executed after the successful selection of
 * the card. <br>
 *
 * <p>If a card selection case fails, the service will try with the next card selection case defined
 * in the scenario, until there are no further card selection cases available.
 *
 * <p>If a card selection case succeeds:
 *
 * <ul>
 *   <li>By default, the service stops at the first successful card selection.
 *   <li>If the multiple selection mode is set (disabled by default), the service will execute the
 *       next selection. This multiple selection mode force the execution of all card selection
 *       cases defined in the scenario. This method can be enabled using the {@link
 *       CardSelectionManager#setMultipleSelectionMode()} method
 * </ul>
 *
 * <p>The logical channel established with the card can be left open (default) or closed after card
 * selection (by using the {@link CardSelectionManager#prepareReleaseChannel()} method).
 *
 * <p>This service allows to:
 *
 * <ul>
 *   <li>Prepare the card selection scenario.
 *   <li>Make an explicit selection of a card (when the card is already present).
 *   <li>Schedule the execution of the selection as soon as a card is presented to an observable
 *       reader.
 * </ul>
 *
 * @since 1.0.0
 */
public interface CardSelectionManager {

  /**
   * Sets the multiple selection mode to process all selection cases even in case of a successful
   * selection.
   *
   * <p>The multiple selection mode is disabled by default.
   *
   * @since 1.0.0
   */
  void setMultipleSelectionMode();

  /**
   * Appends a card selection case to the card selection scenario.
   *
   * <p>The method returns the index giving the current position of the selection in the selection
   * scenario (0 for the first application, 1 for the second, etc.). This index will be used to
   * retrieve the corresponding result in the {@link CardSelectionResult} object.
   *
   * @param cardSelection The card selection.
   * @return A non-negative int.
   * @throws IllegalArgumentException If the provided card selection is null.
   * @since 1.0.0
   */
  int prepareSelection(CardSelection cardSelection);

  /**
   * Requests the closing of the physical channel at the end of the execution of the card selection
   * request.
   *
   * <p>It is thus possible to chain several selections on the same card selection scenario by
   * restarting the card connection sequence.
   *
   * @since 1.0.0
   */
  void prepareReleaseChannel();

  /**
   * Exports the current prepared card selection scenario to a string in JSON format.
   *
   * <p>This string can be imported into the same or another card selection manager via the method
   * {@link #importCardSelectionScenario(String)}.
   *
   * @param detectionMode The card detection mode to use when searching for a card.
   * @param notificationMode The card notification mode to use when a card is detected.
   * @return A not null JSON string.
   * @see #importCardSelectionScenario(String)
   * @since 1.1.0
   */
  String exportCardSelectionScenario(
      ObservableCardReader.DetectionMode detectionMode,
      ObservableCardReader.NotificationMode notificationMode);

  /**
   * Imports a card selection scenario provided as a string in JSON format.
   *
   * <p>The string must have been exported from a card selection manager via the method {@link
   * #exportCardSelectionScenario(ObservableCardReader.DetectionMode,
   * ObservableCardReader.NotificationMode)}.
   *
   * <p>The included card detection and notification modes will be used during the scheduling
   * process if they are not overridden.
   *
   * @param cardSelectionScenario The string in JSON format containing the card selection scenario.
   * @return The index of the last imported selection in the card selection scenario.
   * @throws IllegalArgumentException If the string is null or malformed.
   * @see #exportCardSelectionScenario(ObservableCardReader.DetectionMode,
   *     ObservableCardReader.NotificationMode)
   * @since 1.1.0
   */
  int importCardSelectionScenario(String cardSelectionScenario);

  /**
   * Explicitly executes a previously prepared card selection scenario and returns the card
   * selection result.
   *
   * @param reader The reader to communicate with the card.
   * @return A non-null reference.
   * @throws IllegalArgumentException If the provided reader is null.
   * @throws ReaderCommunicationException If the communication with the reader has failed.
   * @throws CardCommunicationException If communication with the card has failed or if the status
   *     word check is enabled in the card request and the card has returned an unexpected code.
   * @throws InvalidCardResponseException If the card returned invalid data during the selection
   *     process.
   * @since 1.0.0
   */
  CardSelectionResult processCardSelectionScenario(CardReader reader);

  /**
   * Schedules the execution of the prepared card selection scenario as soon as a card is presented
   * to the provided {@link ObservableCardReader}.
   *
   * <p>{@link org.calypsonet.terminal.reader.CardReaderEvent} are pushed to the observer according
   * to the specified notification mode.
   *
   * <p>The reader's behavior at the end of the card processing is defined by the specified {@link
   * ObservableCardReader.DetectionMode}.
   *
   * <p>The result of the scenario execution will be analyzed by {@link
   * #parseScheduledCardSelectionsResponse(ScheduledCardSelectionsResponse)}.
   *
   * @param observableCardReader The reader with which the card communication is carried out.
   * @param detectionMode The card detection mode to use when searching for a card.
   * @param notificationMode The card notification mode to use when a card is detected.
   * @throws IllegalArgumentException If one of the parameters is null.
   * @since 1.0.0
   */
  void scheduleCardSelectionScenario(
      ObservableCardReader observableCardReader,
      ObservableCardReader.DetectionMode detectionMode,
      ObservableCardReader.NotificationMode notificationMode);

  /**
   * Invokes the method {@link #scheduleCardSelectionScenario(ObservableCardReader,
   * ObservableCardReader.DetectionMode, ObservableCardReader.NotificationMode)} using the current
   * detection and notification modes.
   *
   * <p>If the preparation of the card selection scenario was done locally, without being imported,
   * then the default values used are {@link ObservableCardReader.DetectionMode#REPEATING} and
   * {@link ObservableCardReader.NotificationMode#ALWAYS}.
   *
   * @param observableCardReader The reader with which the card communication is carried out.
   * @throws IllegalArgumentException If the observable card reader is null.
   * @since 1.1.0
   */
  void scheduleCardSelectionScenario(ObservableCardReader observableCardReader);

  /**
   * Analyzes the responses provided by a {@link org.calypsonet.terminal.reader.CardReaderEvent}
   * following the insertion of a card and the execution of the card selection scenario.
   *
   * @param scheduledCardSelectionsResponse The card selection scenario execution response.
   * @return A non-null reference.
   * @throws IllegalArgumentException If the provided card selection response is null.
   * @throws InvalidCardResponseException If the data returned by the card could not be interpreted.
   * @since 1.0.0
   */
  CardSelectionResult parseScheduledCardSelectionsResponse(
      ScheduledCardSelectionsResponse scheduledCardSelectionsResponse);
}
