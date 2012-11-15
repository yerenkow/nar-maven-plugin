package org.apache.maven.plugin.nar;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Generates a NarSystem class with static methods to use inside the java part of the library.
 * Runs in generate-resources rather than generate-sources to allow the maven-swig-plugin (which runs in
 * generate-sources) to configure the nar plugin and to let it generate a proper system file. 
 * 
 * @goal nar-system-generate
 * @phase generate-resources
 * @requiresProject
 * @author Mark Donszelmann
 */
public class NarSystemMojo
    extends AbstractNarMojo
{

    public final void narExecute()
        throws MojoExecutionException, MojoFailureException
    {
        // get packageName if specified for JNI.
        String packageName = null;
        String narSystemName = null;
        File narSystemDirectory = null;
        boolean jniFound = false;
        for ( Iterator i = getLibraries().iterator(); !jniFound && i.hasNext(); )
        {
            Library library = (Library) i.next();
            if ( library.getType().equals( Library.JNI ) 
		 || library.getType().equals( Library.SHARED )) 
            {
                packageName = library.getNarSystemPackage();
                narSystemName = library.getNarSystemName();
                narSystemDirectory = new File(getTargetDirectory(), library.getNarSystemDirectory());
                jniFound = true;
            }
        }
        
        if ( !jniFound || packageName == null)
        {
	    if ( !jniFound ) {
		getLog().debug("NAR: not building a shared or JNI library, so not generating NarSystem class.");
	    } else {
		getLog().warn(
			      "NAR: no system package specified; unable to generate NarSystem class.");
	    }
            return;
        }

        // make sure destination is there
        narSystemDirectory.mkdirs();

        getMavenProject().addCompileSourceRoot( narSystemDirectory.getPath() );

        File fullDir = new File( narSystemDirectory, packageName.replace( '.', '/' ) );
        fullDir.mkdirs();

        File narSystem = new File( fullDir, narSystemName + ".java" );
        getLog().info("Generating "+narSystem);
        try
        {
            String output = getOutput(true);
            FileOutputStream fos = new FileOutputStream( narSystem );
            PrintWriter p = new PrintWriter( fos );
            p.println( "// DO NOT EDIT: Generated by NarSystemGenerate." );
            p.println( "package " + packageName + ";" );
            p.println( "" );
            p.println( "/**" );
            p.println( " * Generated class to load the correct version of the jni library" );            
            p.println( " *" );            
            p.println( " * @author maven-nar-plugin" );            
            p.println( " */" );            
            p.println( "public final class NarSystem" );
            p.println( "{" );
            p.println( "" );
            p.println( "    private NarSystem() " );
            p.println( "    {" );
            p.println( "    }" );
            p.println( "" );
            p.println( "   /**" );
            p.println( "    * Load jni library: "+ output );            
            p.println( "    *" );            
            p.println( "    * @author maven-nar-plugin" );            
            p.println( "    */" );            
            p.println( "    public static void loadLibrary()" );
            p.println( "    {" );
            p.println( "        System.loadLibrary(\"" + output + "\");" );
            p.println( "    }" );
	    p.println("");
	    p.println("    public static int runUnitTests() {");
	    p.println("	       return new NarSystem().runUnitTestsNative();");
	    p.println("}");
	    p.println("");
	    p.println("    public native int runUnitTestsNative();");
            p.println( "}" );
            p.close();
            fos.close();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Could not write '" + narSystemName + "'", e );
        }
    }
}
