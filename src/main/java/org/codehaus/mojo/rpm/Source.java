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

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A description of a location where files to be packaged can be found.
 * 
 * @author Bob Allison
 * @author Carlos
 * @author Brett Okken
 * @version $Id$
 */
public class Source
{
    // // // Properties

    /** The source location. */
    private String location;

    /** The list of inclusions. */
    private List includes;

    /** The list of exclusions. */
    private List excludes;
    
    /**
     * Optional destination name for the file identified by {@link #location}.<br/>
     * 
     * <b>NOTE:</b> This is only applicable if the {@link #location} is a {@link File#isFile() file},
     * not a {@link File#isDirectory() directory}.
     */
    private String destination;

    /** <code>true</code> to omit the default exclusions. */
    private boolean noDefaultExcludes;
    
    /**
     * A {@link Pattern regular expression} that, if populated, this indicates that the files defined are only
     * applicable if this value matches the <code>RPMMojo.needarch</code> value.
     */
    private String targetArchitecture;

    /**
     * {@link Pattern} compiled from {@link #targetArchitecture}.
     * @since 2.0-beta-3
     */
    private Pattern targetArchitecturePattern;

    /**
     * A {@link Pattern regular expression} that, if populated, indicates that the files defined are only applicable if
     * the expression {@link Pattern#matches(String, CharSequence) matches } the <code>RPMMojo.needOS</code> value.
     * @since 2.0-beta-3
     */
    private String targetOSName;
    
    /**
     * {@link Pattern} compiled from {@link #targetOSName}.
     * @since 2.0-beta-3
     */
    private Pattern targetOSNamePattern;
    
    /**
     * Indicates if the source should be filtered.
     * @since 2.0
     */
    private boolean filter;
    
    /**
     * The {@link #location} with any/all macros {@link AbstractRPMMojo#evaluateMacro(String) evaluated}.
     * @since 2.1-alpha-1
     */
    private String macroEvaluatedLocation;
    
    /**
     * Retrieve the location holding the file(s) to install.
     * 
     * @return The location holding the file(s) to install.
     */
    public String getLocation()
    {
        return location;
    }

    /**
     * Set the location holding the file(s) to install.
     * 
     * @param loc The new location holding the file(s) to install.
     */
    public void setLocation( String loc )
    {
        location = loc;
    }

    /**
     * Retrieve the list of files to include in the package.
     * 
     * @return The list of files to include in the package.
     */
    public List getIncludes()
    {
        return includes;
    }

    /**
     * Set the list of files to include in the package.
     * 
     * @param incl The new list of files to include in the package.
     */
    public void setIncludes( List incl )
    {
        includes = incl;
    }

    /**
     * Retrieve the list of files to exclude from the package.
     * 
     * @return The list of files to exclude from the package.
     */
    public List getExcludes()
    {
        return excludes;
    }

    /**
     * Set the list of files to exclude from the package.
     * 
     * @param excl The new list of files to exclude from the package.
     */
    public void setExcludes( List excl )
    {
        excludes = excl;
    }

    /**
     * Retrieve the default exclude status.
     * 
     * @return <code>true</code> if the default excludes should be omitted.
     */
    public boolean getNoDefaultExcludes()
    {
        return noDefaultExcludes;
    }

    /**
     * Set the default exclude status.
     * 
     * @param noDefExcl <code>true</code> if the default excludes should be omitted.
     */
    public void setNoDefaultExcludes( boolean noDefExcl )
    {
        noDefaultExcludes = noDefExcl;
    }

    // // // Public methods

    /**
     * @return Returns the {@link #destination}.
     * @see #setDestination(String)
     */
    public String getDestination()
    {
        return this.destination;
    }

    /**
     * Sets the destination file name.
     * <p>
     * <b>NOTE:</b> This is only applicable if the {@link #getLocation() location} is a {@link File#isFile() file},
     * not a {@link File#isDirectory() directory}.
     * </p>
     * 
     * @param destination The destination that the {@link #getLocation() location} should be in the final rpm.
     */
    public void setDestination( String destination )
    {
        this.destination = destination;
    }

    /**
     * @return Returns the {@link #targetArchitecture}.
     */
    public String getTargetArchitecture()
    {
        return this.targetArchitecture;
    }

    /**
     * Sets a {@link Pattern regular expression} that indicates that the files defined are only applicable if
     * the expression {@link Pattern#matches(String, CharSequence) matches } the architecture.
     * <p>
     * In order to be backwards compatible, the <i>targetArch</i> will be converted to
     * {@link String#toLowerCase() lower case} for the purpose of comparison.
     * </p>
     * 
     * @param targetArch The target architecture to set.
     */
    public void setTargetArchitecture( String targetArch )
    {
        this.targetArchitecture = targetArch;
        this.targetArchitecturePattern =
            targetArch == null ? null : Pattern.compile( targetArch.toLowerCase( Locale.ENGLISH ) );
    }
    
    /**
     * Indicates if the {@link #getTargetArchitecture()} matches the <i>archicture</i>.
     * 
     * @param architecture The target architecture for the rpm.
     * @return if the {@link #getTargetArchitecture()} {@link java.util.regex.Matcher#matches() matches} the <i>archicture</i>.
     */
    boolean matchesArchitecture( String architecture )
    {
        return targetArchitecturePattern == null ? true : targetArchitecturePattern.matcher( architecture ).matches();
    }

    /**
     * @return Returns the {@link #targetOSName}.
     * @since 2.0-beta-3
     */
    public String getTargetOSName()
    {
        return this.targetOSName;
    }

    /**
     * Sets a {@link Pattern regular expression} that indicates that the files defined are only applicable if
     * the expression {@link Pattern#matches(String, CharSequence) matches } the operating system name.
     * 
     * @param targetOSName The {@link #targetOSName} to set.
     * @since 2.0-beta-3
     */
    public void setTargetOSName( String targetOSName )
    {
        this.targetOSName = targetOSName;
        this.targetOSNamePattern = targetOSName != null ? Pattern.compile( targetOSName ) : null;
    }
    
    /**
     * @return Returns the {@link #filter}.
     * @since 2.0
     */
    public boolean isFilter()
    {
        return this.filter;
    }

    /**
     * @param filter The {@link #filter} to set.
     * @since 2.0
     */
    public void setFilter( boolean filter )
    {
        this.filter = filter;
    }

    /**
     * Indicates if the target OS name matches <i>osName</i>.
     * @param osName The name of the os to match against the {@link #getTargetOSName()}.
     * @return if {@link #getTargetOSName()} {@link java.util.regex.Matcher#matches() matches} <i>osName</i>.
     * @since 2.0-beta-3
     */
    boolean matchesOSName( String osName )
    {
        return targetOSNamePattern == null ? true : targetOSNamePattern.matcher( osName ).matches();
    }

    /**
     * @return Returns the {@link #macroEvaluatedLocation}.
     * @since 2.1-alpha-1
     */
    final String getMacroEvaluatedLocation()
    {
        return this.macroEvaluatedLocation;
    }

    /**
     * @param macroEvaluatedLocation The {@link #macroEvaluatedLocation} to set.
     * @since 2.1-alpha-1
     */
    final void setMacroEvaluatedLocation( String macroEvaluatedLocation )
    {
        this.macroEvaluatedLocation = macroEvaluatedLocation;
    }

    /** {@inheritDoc} */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append( "{" );

        if ( location == null )
        {
            sb.append( "nowhere" );
        }
        else
        {
            sb.append( "\"" + location + "\"" );
        }

        if ( includes != null )
        {
            sb.append( " incl:" + includes );
        }

        if ( excludes != null )
        {
            sb.append( " excl:" + excludes );
        }
        
        if ( destination != null )
        {
            sb.append( " destination: " );
            sb.append( destination );
        }
        
        sb.append( " filter: " + Boolean.toString( filter ) );

        if ( noDefaultExcludes )
        {
            sb.append( " [no default excludes]" );
        }
        
        if ( targetArchitecture != null )
        {
            sb.append( " targetArch: " );
            sb.append( targetArchitecture );
        }

        sb.append( "}" );
        return sb.toString();
    }
}
