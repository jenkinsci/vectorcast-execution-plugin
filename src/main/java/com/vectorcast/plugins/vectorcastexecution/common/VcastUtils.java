/*
 * The MIT License
 *
 * Copyright 2020 Vector Software, East Greenwich, Rhode Island USA
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.vectorcast.plugins.vectorcastexecution.common;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.Optional;
import java.util.jar.JarFile;
import java.io.FileNotFoundException;

/** Utility class for VectorCAST. */

public class VcastUtils {
    /**
     * Gets the version of the plugins.
     * @return Optional returns the version
     */
    public static Optional<String> getVersion() {
        Optional<String> version = Optional.empty();
        try {
            File file = new File(URLDecoder.decode(
                    VcastUtils.class.getProtectionDomain().getCodeSource()
                    .getLocation().getPath(), "utf-8"));
            JarFile jarfile = new JarFile(file);
            version = Optional.ofNullable(jarfile.getManifest()
                    .getMainAttributes().getValue("Plugin-Version"));
            jarfile.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return version;
    }

    protected VcastUtils() {
        // prevents calls from subclass
        throw new UnsupportedOperationException();
    }

}
