package org.codehaus.mojo.rpm;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/**
 * A {@link Source} that simply indicates that the {@link Source#getLocation()} is to be linked to the
 * {@link Mapping#getDirectory()}.
 *
 * @author Brett Okken Cerner Corp.
 * @since 2.0-beta-3
 */
public class SoftlinkSource
    extends Source
{
    /**
     * The {@link Mapping} this instance is defined in.
     */
    private Mapping sourceMapping;

    /**
     * @return Returns the {@link #sourceMapping}.
     */
    Mapping getSourceMapping()
    {
        return this.sourceMapping;
    }

    /**
     * @param sourceMapping The {@link #sourceMapping} to set.
     */
    void setSourceMapping( Mapping sourceMapping )
    {
        this.sourceMapping = sourceMapping;
    }
}
