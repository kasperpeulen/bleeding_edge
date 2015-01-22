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
package com.google.dart.server.internal.remote.utilities;

import com.google.common.annotations.VisibleForTesting;
import com.google.dart.server.generated.types.AddContentOverlay;
import com.google.dart.server.generated.types.AnalysisError;
import com.google.dart.server.generated.types.AnalysisOptions;
import com.google.dart.server.generated.types.ChangeContentOverlay;
import com.google.dart.server.generated.types.Location;
import com.google.dart.server.generated.types.RefactoringOptions;
import com.google.dart.server.generated.types.RemoveContentOverlay;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A utilities class for generating the {@link String} analysis server json requests.
 * 
 * @coverage dart.server.remote
 */
public class RequestUtilities {
  private static final String CONTEXT_ROOT = "contextRoot";
  private static final String FILE = "file";
  private static final String ID = "id";
  private static final String LENGTH = "length";
  private static final String METHOD = "method";
  private static final String OFFSET = "offset";
  private static final String PARAMS = "params";
  private static final String SELECTION_LENGTH = "selectionLength";
  private static final String SELECTION_OFFSET = "selectionOffset";
  private static final String SUBSCRIPTIONS = "subscriptions";
  private static final String URI = "uri";

  // Server domain
  private static final String METHOD_SERVER_GET_VERSION = "server.getVersion";
  private static final String METHOD_SERVER_SHUTDOWN = "server.shutdown";
  private static final String METHOD_SERVER_SET_SUBSCRIPTIONS = "server.setSubscriptions";

  // Analysis domain
  private static final String METHOD_ANALYSIS_GET_ERRORS = "analysis.getErrors";
  private static final String METHOD_ANALYSIS_GET_HOVER = "analysis.getHover";
  private static final String METHOD_ANALYSIS_GET_LIBRARY_DEPENDENCIES = "analysis.getLibraryDependencies";
  private static final String METHOD_ANALYSIS_GET_NAVIGATION = "analysis.getNavigation";
  private static final String METHOD_ANALYSIS_REANALYZE = "analysis.reanalyze";
  private static final String METHOD_ANALYSIS_SET_ROOTS = "analysis.setAnalysisRoots";
  private static final String METHOD_ANALYSIS_SET_PRIORITY_FILES = "analysis.setPriorityFiles";
  private static final String METHOD_ANALYSIS_SET_SUBSCRIPTIONS = "analysis.setSubscriptions";
  private static final String METHOD_ANALYSIS_UPDATE_CONTENT = "analysis.updateContent";
  private static final String METHOD_ANALYSIS_UPDATE_OPTIONS = "analysis.updateOptions";

  // Edit domain
  private static final String METHOD_EDIT_FORMAT = "edit.format";
  private static final String METHOD_EDIT_GET_ASSISTS = "edit.getAssists";
  private static final String METHOD_EDIT_GET_AVAILABLE_REFACTORING = "edit.getAvailableRefactorings";
  private static final String METHOD_EDIT_GET_FIXES = "edit.getFixes";
  private static final String METHOD_EDIT_GET_REFACTORING = "edit.getRefactoring";
  private static final String METHOD_EDIT_SORT_MEMBERS = "edit.sortMembers";

  // Code Completion domain
  private static final String METHOD_COMPLETION_GET_SUGGESTIONS = "completion.getSuggestions";

  // Search domain
  private static final String METHOD_SEARCH_FIND_ELEMENT_REFERENCES = "search.findElementReferences";
  private static final String METHOD_SEARCH_FIND_MEMBER_DECLARATIONS = "search.findMemberDeclarations";
  private static final String METHOD_SEARCH_FIND_MEMBER_REFERENCES = "search.findMemberReferences";
  private static final String METHOD_SEARCH_FIND_TOP_LEVEL_DECLARATIONS = "search.findTopLevelDeclarations";
  private static final String METHOD_SEARCH_GET_TYPE_HIERARCHY = "search.getTypeHierarchy";

  // Execution domain
  private static final String METHOD_EXECUTION_CREATE_CONTEXT = "execution.createContext";
  private static final String METHOD_EXECUTION_DELETE_CONTEXT = "execution.deleteContext";
  private static final String METHOD_EXECUTION_MAP_URI = "execution.mapUri";
  private static final String METHOD_EXECUTION_SET_SUBSCRIPTIONS = "execution.setSubscriptions";

  @VisibleForTesting
  public static JsonElement buildJsonElement(Object object) {
    if (object instanceof Boolean) {
      return new JsonPrimitive((Boolean) object);
    } else if (object instanceof Number) {
      return new JsonPrimitive((Number) object);
    } else if (object instanceof String) {
      return new JsonPrimitive((String) object);
    } else if (object instanceof List<?>) {
      List<?> list = (List<?>) object;
      JsonArray jsonArray = new JsonArray();
      for (Object item : list) {
        JsonElement jsonItem = buildJsonElement(item);
        jsonArray.add(jsonItem);
      }
      return jsonArray;
    } else if (object instanceof Map<?, ?>) {
      Map<?, ?> map = (Map<?, ?>) object;
      JsonObject jsonObject = new JsonObject();
      for (Entry<?, ?> entry : map.entrySet()) {
        Object key = entry.getKey();
        // prepare string key
        String keyString;
        if (key instanceof String) {
          keyString = (String) key;
        } else {
          throw new IllegalArgumentException("Unable to convert to string: " + getClassName(key));
        }
        // prepare JsonElement value
        Object value = entry.getValue();
        JsonElement valueJson = buildJsonElement(value);
        // put a property into the JSON object
        if (keyString != null && valueJson != null) {
          jsonObject.add(keyString, valueJson);
        }
      }
      return jsonObject;
    } else if (object instanceof AnalysisError) {
      return buildJsonObjectAnalysisError((AnalysisError) object);
    } else if (object instanceof AddContentOverlay) {
      return ((AddContentOverlay) object).toJson();
    } else if (object instanceof ChangeContentOverlay) {
      return ((ChangeContentOverlay) object).toJson();
    } else if (object instanceof RemoveContentOverlay) {
      return ((RemoveContentOverlay) object).toJson();
    } else if (object instanceof AnalysisOptions) {
      return ((AnalysisOptions) object).toJson();
    } else if (object instanceof Location) {
      return buildJsonObjectLocation((Location) object);
    }
    throw new IllegalArgumentException("Unable to convert to JSON: " + object);
  }

  /**
   * Generate and return a {@value #METHOD_ANALYSIS_GET_ERRORS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.getErrors"
   *   "params": {
   *     "file": FilePath
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateAnalysisGetErrors(String idValue, String file) {
    JsonObject params = new JsonObject();
    params.addProperty(FILE, file);
    return buildJsonObjectRequest(idValue, METHOD_ANALYSIS_GET_ERRORS, params);
  }

  /**
   * Generate and return a {@value #METHOD_ANALYSIS_GET_HOVER} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.getHover"
   *   "params": {
   *     "file": FilePath
   *     "offset": int
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateAnalysisGetHover(String idValue, String file, int offset) {
    JsonObject params = new JsonObject();
    params.addProperty(FILE, file);
    params.addProperty(OFFSET, offset);
    return buildJsonObjectRequest(idValue, METHOD_ANALYSIS_GET_HOVER, params);
  }

  /**
   * Generate and return a {@value #METHOD_ANALYSIS_GET_LIBRARY_DEPENDENCIES} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.getLibraryDependencies"
   * }
   * </pre>
   */
  public static JsonObject generateAnalysisGetLibraryDependencies(String id) {
    return buildJsonObjectRequest(id, METHOD_ANALYSIS_GET_LIBRARY_DEPENDENCIES);
  }

  /**
   * Generate and return a {@value #METHOD_ANALYSIS_GET_NAVIGATION} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.getNavigation"
   *   "params": {
   *     "file": FilePath
   *     "offset": int
   *     "length": int
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateAnalysisGetNavigation(String idValue, String file, int offset,
      int length) {
    JsonObject params = new JsonObject();
    params.addProperty(FILE, file);
    params.addProperty(OFFSET, offset);
    params.addProperty(LENGTH, length);
    return buildJsonObjectRequest(idValue, METHOD_ANALYSIS_GET_NAVIGATION, params);
  }

  /**
   * Generate and return a {@value #METHOD_ANALYSIS_REANALYZE} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.reanalyze"
   * }
   * </pre>
   */
  public static JsonObject generateAnalysisReanalyze(String id) {
    return buildJsonObjectRequest(id, METHOD_ANALYSIS_REANALYZE);
  }

  /**
   * Generate and return a {@value #METHOD_ANALYSIS_SET_ROOTS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.setAnalysisRoots"
   *   "params": {
   *     "included": List&lt;FilePath&gt;
   *     "excluded": List&lt;FilePath&gt;
   *     "packageRoots": optional Map&lt;FilePath, FilePath&gt;
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateAnalysisSetAnalysisRoots(String id, List<String> included,
      List<String> excluded, Map<String, String> packageRoots) {
    JsonObject params = new JsonObject();
    params.add("included", buildJsonElement(included));
    params.add("excluded", buildJsonElement(excluded));
    if (packageRoots != null) {
      params.add("packageRoots", buildJsonElement(packageRoots));
    }
    return buildJsonObjectRequest(id, METHOD_ANALYSIS_SET_ROOTS, params);
  }

  /**
   * Generate and return a {@value #METHOD_ANALYSIS_SET_PRIORITY_FILES} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.setPriorityFiles"
   *   "params": {
   *     "files": List&lt;FilePath&gt;
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateAnalysisSetPriorityFiles(String id, List<String> files) {
    JsonObject params = new JsonObject();
    params.add("files", buildJsonElement(files));
    return buildJsonObjectRequest(id, METHOD_ANALYSIS_SET_PRIORITY_FILES, params);
  }

  /**
   * Generate and return a {@value #METHOD_ANALYSIS_SET_SUBSCRIPTIONS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.setSubscriptions"
   *   "params": {
   *     "subscriptions": Map&gt;AnalysisService, List&lt;FilePath&gt;&gt;
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateAnalysisSetSubscriptions(String id,
      Map<String, List<String>> subscriptions) {
    JsonObject params = new JsonObject();
    params.add(SUBSCRIPTIONS, buildJsonElement(subscriptions));
    return buildJsonObjectRequest(id, METHOD_ANALYSIS_SET_SUBSCRIPTIONS, params);
  }

  /**
   * Generate and return a {@value #METHOD_ANALYSIS_UPDATE_CONTENT} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.updateContent"
   *   "params": {
   *     "files": Map&lt;FilePath, ContentChange&gt;
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateAnalysisUpdateContent(String idValue, Map<String, Object> files) {
    JsonObject params = new JsonObject();
    params.add("files", buildJsonElement(files));
    return buildJsonObjectRequest(idValue, METHOD_ANALYSIS_UPDATE_CONTENT, params);
  }

  /**
   * Generate and return a {@value #METHOD_ANALYSIS_UPDATE_OPTIONS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.updateOptions"
   *   "params": {
   *     "options": AnalysisOptions
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateAnalysisUpdateOptions(String idValue, AnalysisOptions options) {
    JsonObject params = new JsonObject();
    params.add("options", buildJsonElement(options));
    return buildJsonObjectRequest(idValue, METHOD_ANALYSIS_UPDATE_OPTIONS, params);
  }

  /**
   * Generate and return a {@value #METHOD_COMPLETION_GET_SUGGESTIONS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "completion.getSuggestions"
   *   "params": {
   *     "file": FilePath
   *     "offset": int
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateCompletionGetSuggestions(String idValue, String file, int offset) {
    JsonObject params = new JsonObject();
    params.addProperty(FILE, file);
    params.addProperty(OFFSET, offset);
    return buildJsonObjectRequest(idValue, METHOD_COMPLETION_GET_SUGGESTIONS, params);
  }

  /**
   * Generate and return a {@value #METHOD_EDIT_FORMAT} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "edit.format"
   *   "params": {
   *     "file": FilePath
   *     "selectionOffset": int
   *     "selectionLength": int
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateEditFormat(String idValue, String file, int offset, int length) {
    JsonObject params = new JsonObject();
    params.addProperty(FILE, file);
    params.addProperty(SELECTION_OFFSET, offset);
    params.addProperty(SELECTION_LENGTH, length);
    return buildJsonObjectRequest(idValue, METHOD_EDIT_FORMAT, params);
  }

  /**
   * Generate and return a {@value #METHOD_EDIT_GET_ASSISTS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "edit.getAssists"
   *   "params": {
   *     "file": FilePath
   *     "offset": int
   *     "length": int
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateEditGetAssists(String idValue, String file, int offset,
      int length) {
    JsonObject params = new JsonObject();
    params.addProperty(FILE, file);
    params.addProperty(OFFSET, offset);
    params.addProperty(LENGTH, length);
    return buildJsonObjectRequest(idValue, METHOD_EDIT_GET_ASSISTS, params);
  }

  /**
   * Generate and return a {@value #METHOD_EDIT_GET_AVAILABLE_REFACTORING} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "edit.getAvailableRefactorings"
   *   "params": {
   *     "file": FilePath
   *     "offset": int
   *     "length": int
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateEditGetAvaliableRefactorings(String idValue, String file,
      int offset, int length) {
    JsonObject params = new JsonObject();
    params.addProperty(FILE, file);
    params.addProperty(OFFSET, offset);
    params.addProperty(LENGTH, length);
    return buildJsonObjectRequest(idValue, METHOD_EDIT_GET_AVAILABLE_REFACTORING, params);
  }

  /**
   * Generate and return a {@value #METHOD_EDIT_GET_FIXES} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "edit.getFixes"
   *   "params": {
   *     "file": FilePath
   *     "offset": int
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateEditGetFixes(String idValue, String file, int offset) {
    JsonObject params = new JsonObject();
    params.addProperty(FILE, file);
    params.addProperty(OFFSET, offset);
    return buildJsonObjectRequest(idValue, METHOD_EDIT_GET_FIXES, params);
  }

  /**
   * Generate and return a {@value #METHOD_REFACTORING} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "edit.getRefactoring"
   *   "params": {
   *     "kind": RefactoringKind
   *     "file": FilePath
   *     "offset": int
   *     "length": int
   *     "validateOnly": bool
   *     "options": optional object
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateEditGetRefactoring(String idValue, String kind, String file,
      int offset, int length, boolean validateOnly, RefactoringOptions options) {
    JsonObject params = new JsonObject();
    params.addProperty("kind", kind);
    params.addProperty(FILE, file);
    params.addProperty(OFFSET, offset);
    params.addProperty(LENGTH, length);
    params.addProperty("validateOnly", validateOnly);
    if (options != null) {
      params.add("options", options.toJson());
    }
    return buildJsonObjectRequest(idValue, METHOD_EDIT_GET_REFACTORING, params);
  }

  /**
   * Generate and return a {@value #METHOD_EDIT_SORT_MEMBERS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "edit.sortMembers"
   *   "params": {
   *     "file": FilePath
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateEditSortMembers(String idValue, String file) {
    JsonObject params = new JsonObject();
    params.addProperty(FILE, file);
    return buildJsonObjectRequest(idValue, METHOD_EDIT_SORT_MEMBERS, params);
  }

  /**
   * Generate and return a {@value #METHOD_EXECUTION_CREATE_CONTEXT} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "execution.createContext"
   *   "params": {
   *     "contextRoot": FilePath
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateExecutionCreateContext(String idValue, String contextRoot) {
    JsonObject params = new JsonObject();
    params.addProperty(CONTEXT_ROOT, contextRoot);
    return buildJsonObjectRequest(idValue, METHOD_EXECUTION_CREATE_CONTEXT, params);
  }

  /**
   * Generate and return a {@value #METHOD_EXECUTION_DELETE_CONTEXT} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "execution.deleteContext"
   *   "params": {
   *     "id": ExecutionContextId
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateExecutionDeleteContext(String idValue, String contextId) {
    JsonObject params = new JsonObject();
    params.addProperty(ID, contextId);
    return buildJsonObjectRequest(idValue, METHOD_EXECUTION_DELETE_CONTEXT, params);
  }

  /**
   * Generate and return a {@value #METHOD_EXECUTION_MAP_URI} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "execution.mapUri"
   *   "params": {
   *     "id": ExecutionContextId
   *     "file": FilePath
   *     "uri": String
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateExecutionMapUri(String idValue, String contextId, String file,
      String uri) {
    JsonObject params = new JsonObject();
    params.addProperty(ID, contextId);
    if (file == null) {
      if (uri == null) {
        throw new IllegalArgumentException("Exactly one of 'file' and 'uri' must be non-null");
      }
      params.addProperty(URI, uri);
    } else {
      if (uri != null) {
        throw new IllegalArgumentException("Exactly one of 'file' and 'uri' must be non-null");
      }
      params.addProperty(FILE, file);
    }
    return buildJsonObjectRequest(idValue, METHOD_EXECUTION_MAP_URI, params);
  }

  /**
   * Generate and return a {@value #METHOD_EXECUTION_SET_SUBSCRIPTIONS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "execution.setSubscriptions"
   *   "params": {
   *     "subscriptions": List<ExecutionService>
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateExecutionSetSubscriptions(String idValue,
      List<String> subscriptions) {
    JsonObject params = new JsonObject();
    params.add(SUBSCRIPTIONS, buildJsonElement(subscriptions));
    return buildJsonObjectRequest(idValue, METHOD_EXECUTION_SET_SUBSCRIPTIONS, params);
  }

  /**
   * Generate and return a {@value #METHOD_SEARCH_FIND_ELEMENT_REFERENCES} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "search.findElementReferences"
   *   "params": {
   *     "file": FilePath
   *     "offset": int
   *     "includePotential": boolean
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateSearchFindElementReferences(String idValue, String file,
      int offset, boolean includePotential) {
    JsonObject params = new JsonObject();
    params.addProperty(FILE, file);
    params.addProperty(OFFSET, offset);
    params.addProperty("includePotential", includePotential);
    return buildJsonObjectRequest(idValue, METHOD_SEARCH_FIND_ELEMENT_REFERENCES, params);
  }

  /**
   * Generate and return a {@value #METHOD_SEARCH_FIND_MEMBER_DECLARATIONS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "search.findMemberDeclarations"
   *   "params": {
   *     "name": String
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateSearchFindMemberDeclarations(String idValue, String name) {
    JsonObject params = new JsonObject();
    params.addProperty("name", name);
    return buildJsonObjectRequest(idValue, METHOD_SEARCH_FIND_MEMBER_DECLARATIONS, params);
  }

  /**
   * Generate and return a {@value #METHOD_SEARCH_FIND_MEMBER_REFERENCES} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "search.findMemberReferences"
   *   "params": {
   *     "name": String
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateSearchFindMemberReferences(String idValue, String name) {
    JsonObject params = new JsonObject();
    params.addProperty("name", name);
    return buildJsonObjectRequest(idValue, METHOD_SEARCH_FIND_MEMBER_REFERENCES, params);
  }

  /**
   * Generate and return a {@value #METHOD_SEARCH_FIND_TOP_LEVEL_DECLARATIONS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "search.findTopLevelDeclarations"
   *   "params": {
   *     "name": String
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateSearchFindTopLevelDeclarations(String idValue, String pattern) {
    JsonObject params = new JsonObject();
    params.addProperty("pattern", pattern);
    return buildJsonObjectRequest(idValue, METHOD_SEARCH_FIND_TOP_LEVEL_DECLARATIONS, params);
  }

  /**
   * Generate and return a {@value #METHOD_SEARCH_GET_TYPE_HIERARCHY} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "search.getTypeHierarchy"
   *   "params": {
   *     "file": FilePath
   *     "offset": int
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateSearchGetTypeHierarchy(String id, String file, int offset) {
    JsonObject params = new JsonObject();
    params.addProperty(FILE, file);
    params.addProperty(OFFSET, offset);
    return buildJsonObjectRequest(id, METHOD_SEARCH_GET_TYPE_HIERARCHY, params);
  }

  /**
   * Generate and return a {@value #METHOD_SERVER_GET_VERSION} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "server.getVersion"
   * }
   * </pre>
   */
  public static JsonObject generateServerGetVersion(String idValue) {
    return buildJsonObjectRequest(idValue, METHOD_SERVER_GET_VERSION);
  }

  /**
   * Generate and return a {@value #METHOD_SERVER_SET_SUBSCRIPTIONS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "server.setSubscriptions"
   *   "params": {
   *     "subscriptions": List&lt;ServerService&gt;
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateServerSetSubscriptions(String idValue, List<String> subscriptions) {
    JsonObject params = new JsonObject();
    params.add(SUBSCRIPTIONS, buildJsonElement(subscriptions));
    return buildJsonObjectRequest(idValue, METHOD_SERVER_SET_SUBSCRIPTIONS, params);
  }

  /**
   * Generate and return a {@value #METHOD_SERVER_SHUTDOWN} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "server.shutdown"
   * }
   * </pre>
   */
  public static JsonObject generateServerShutdown(String idValue) {
    return buildJsonObjectRequest(idValue, METHOD_SERVER_SHUTDOWN);
  }

  private static JsonObject buildJsonObjectAnalysisError(AnalysisError error) {
    JsonObject errorJsonObject = new JsonObject();
    errorJsonObject.addProperty("severity", error.getSeverity());
    errorJsonObject.addProperty("type", error.getType());
    errorJsonObject.add("location", buildJsonObjectLocation(error.getLocation()));
    errorJsonObject.addProperty("message", error.getMessage());
    String correction = error.getCorrection();
    if (correction != null) {
      errorJsonObject.addProperty("correction", correction);
    }
    return errorJsonObject;
  }

  private static JsonObject buildJsonObjectLocation(Location location) {
    JsonObject locationJsonObject = new JsonObject();
    locationJsonObject.addProperty(FILE, location.getFile());
    locationJsonObject.addProperty(OFFSET, location.getOffset());
    locationJsonObject.addProperty(LENGTH, location.getLength());
    locationJsonObject.addProperty("startLine", location.getStartLine());
    locationJsonObject.addProperty("startColumn", location.getStartColumn());
    return locationJsonObject;
  }

  private static JsonObject buildJsonObjectRequest(String idValue, String methodValue) {
    return buildJsonObjectRequest(idValue, methodValue, null);
  }

  private static JsonObject buildJsonObjectRequest(String idValue, String methodValue,
      JsonObject params) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(ID, idValue);
    jsonObject.addProperty(METHOD, methodValue);
    if (params != null) {
      jsonObject.add(PARAMS, params);
    }
    return jsonObject;
  }

  /**
   * Return the name of the given object, may be {@code "null"} string.
   */
  private static String getClassName(Object object) {
    return object != null ? object.getClass().getName() : "null";
  }

  private RequestUtilities() {
  }
}
