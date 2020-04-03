package com.vectorcast.plugins.vectorcastexecution.common;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class VcastUtils
{
    public static Optional< String > getVersion()
    {
        Optional< String > Version = Optional.empty();
	try {
	    File file = new File( URLDecoder.decode( Utils.class.getProtectionDomain().getCodeSource().getLocation().getPath(), "utf-8" ) );
            JarFile jarfile = new JarFile( file );
            Version = Optional.ofNullable( jarfile.getManifest().getMainAttributes().getValue( "Plugin-Version" ) );
            
        } catch ( IOException e ) {
	    e.printStackTrace();
	}

	return Version;
    }    
}
