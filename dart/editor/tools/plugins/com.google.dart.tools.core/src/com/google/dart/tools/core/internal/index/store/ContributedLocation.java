/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.index.store;

import com.google.dart.tools.core.index.Location;
import com.google.dart.tools.core.index.Resource;

/**
 * Instances of the class <code>ContributedLocation</code> record the resource that was being
 * analyzed when a relationship was recorded.
 */
public class ContributedLocation {
  /**
   * The resource that contributed the relationship.
   */
  private Resource contributor;

  /**
   * The location that is part of the relationship contributed by the contributor.
   */
  private Location location;

  /**
   * Initialize a newly created contributed location with the given information.
   * 
   * @param contributor the resource that contributed the relationship
   * @param location the location that is part of the relationship contributed by the contributor
   */
  public ContributedLocation(Resource contributor, Location location) {
    this.contributor = contributor;
    this.location = location;
  }

  /**
   * Return the resource that contributed the relationship.
   * 
   * @return the resource that contributed the relationship
   */
  public Resource getContributor() {
    return contributor;
  }

  /**
   * Return the location that is part of the relationship contributed by the contributor.
   * 
   * @return the location that is part of the relationship contributed by the contributor
   */
  public Location getLocation() {
    return location;
  }
}
