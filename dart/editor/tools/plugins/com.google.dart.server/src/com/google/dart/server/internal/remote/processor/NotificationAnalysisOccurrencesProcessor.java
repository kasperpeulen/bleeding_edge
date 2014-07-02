/*
 * Copyright (c) 2014, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.server.internal.remote.processor;

import com.google.common.collect.Lists;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.Element;
import com.google.dart.server.Occurrences;
import com.google.dart.server.internal.OccurrencesImpl;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Iterator;
import java.util.List;

/**
 * Processor for "analysis.occurrences" notification.
 * 
 * @coverage dart.server.remote
 */
public class NotificationAnalysisOccurrencesProcessor extends NotificationProcessor {

  public NotificationAnalysisOccurrencesProcessor(AnalysisServerListener listener) {
    super(listener);
  }

  /**
   * Process the given {@link JsonObject} notification and notify {@link #listener}.
   */
  @Override
  public void process(JsonObject response) throws Exception {
    JsonObject paramsObject = response.get("params").getAsJsonObject();
    String file = paramsObject.get("file").getAsString();
    JsonArray occurrencesJsonArray = paramsObject.get("occurrences").getAsJsonArray();
    // compute occurrences and notify listener
    getListener().computedOccurrences(file, constructOccurrencesArray(occurrencesJsonArray));
  }

  private Occurrences[] constructOccurrencesArray(JsonArray occurrencesJsonArray) {
    Iterator<JsonElement> occurrencesIterator = occurrencesJsonArray.iterator();
    List<Occurrences> occurrencesList = Lists.newArrayList();
    while (occurrencesIterator.hasNext()) {
      JsonObject occurrencesObject = occurrencesIterator.next().getAsJsonObject();
      JsonObject elementObject = occurrencesObject.get("element").getAsJsonObject();
      Element element = constructElement(elementObject);
      int length = occurrencesObject.get("length").getAsInt();
      int[] offsets = constructIntArray(occurrencesObject.get("offsets").getAsJsonArray());
      occurrencesList.add(new OccurrencesImpl(element, length, offsets));
    }

    // create outline object
    return occurrencesList.toArray(new Occurrences[occurrencesList.size()]);
  }
}
