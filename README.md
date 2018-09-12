Phosphor: Dynamic Taint Tracking for the JVM
========


Phosphor is a system for performing dynamic taint analysis in the JVM, on commodity JVMs (e.g. Oracle's HotSpot or OpenJDK's IcedTea). This repository contains the source for Phosphor. For more information about how Phosphor works and what it could be useful for, please refer to our [OOPSLA 2014 paper](http://jonbell.net/publications/phosphor), [ISSTA 2015 Tool Demo ](http://mice.cs.columbia.edu/getTechreport.php?techreportID=1601) or email [Jonathan Bell](mailto:jbell@cs.columbia.edu). José Cambronero also maintains a [series of examples on using Phosphor](https://github.com/josepablocam/phosphor-examples/).

Note - for those interested in reproducing our OOPSLA 2014 experiments, please find a [VM Image with all relevant files here](http://academiccommons.columbia.edu/catalog/ac%3A182689), and a [README with instructions for doing so here](https://www.dropbox.com/s/dmebj6k8izams6p/artifact-63-phosphor.pdf?dl=0).


Running
-------
Phosphor works by modifying your application's bytecode to perform data flow tracking. To be complete, Phosphor also modifies the bytecode of JRE-provided classes, too. The first step to using Phosphor is generating an instrumented version of your runtime environment. We have tested Phosphor with versions 7 and 8 of both Oracle's HotSpot JVM and OpenJDK's IcedTea JVM.

The instrumenter takes two primary arguments: first a path containing the classes to instrument, and then a destination for the instrumented classes. You can also specify to track taint tags through control flow, to use objects as tags (instead of integers), or to automatically perform taint marking in particular methods using the various options as shown by invoking Phosphor with the "-help" option.


```
usage: java -jar phosphor.jar [OPTIONS] [input] [output]
 -controlTrack                  Enable taint tracking through control flow
 -help                          print this message
 -multiTaint                    Support for 2^32 tags instead of just 32
 -taintSinks <taintSinks>       File with listing of taint sinks to use to
                                check for auto-taints
 -taintSources <taintSources>   File with listing of taint sources to
                                auto-taint
 -withoutDataTrack              Disable taint tracking through data flow
                                (on by default)
```
Phosphor now should be configured to correctly run JUnit tests (with taint tracking) in most environments (Mac + Linux... sorry Windows!). Running `mvn verify` should cause Phosphor to generate several different instrumented JRE's (for multitaint use, int-tag taint use, and control track use) into the project's `target` directory, then run unit tests in that JRE that are automatically tracked. You take a look at the test cases to see some example usage. Test cases that end in `IntTagITCase` are executed with Phosphor configured for integer tags, tests that end in `ObjTagITCase` are executed with Phosphor configured for object tags (multi tainting), and `ImplicitITCase` tests run in the control tracking mode.
 
We'll assume that in all of the code examples below, we're in the same directory (which has a copy of Phosphor-0.0.3-SNAPSHOT.jar, which you generated by downloading Phosphor, and running `mvn package`), and that the JRE is located here: `/Library/Java/JavaVirtualMachines/jdk1.7.0_45.jdk/Contents/Home/jre` (modify this path in the commands below to match your environment).

Then, to instrument the JRE we'll run:
`java -jar Phosphor-0.0.3-SNAPSHOT.jar /Library/Java/JavaVirtualMachines/jdk1.7.0_45.jdk/Contents/Home/jre jre-inst`

After you do this, make sure to chmod +x the binaries in the new folder, e.g. `chmod +x jre-inst/bin/*`

The next step is to instrument the code which you would like to track. This time when you run the instrumenter, pass your entire (compiled) code base to Phosphor for instrumentation, and specify an output folder for that.

We can now run the instrumented code using our instrumented JRE, as such:
`JAVA_HOME=jre-inst/ $JAVA_HOME/bin/java  -Xbootclasspath/a:Phosphor-0.0.3-SNAPSHOT.jar -javaagent:Phosphor-0.0.3-SNAPSHOT.jar -cp path-to-instrumented-code your.main.class`

Note: It is not 100% necessary to instrument your application/library code in advance - the javaagent will detect any uninstrumented class files as they are being loaded into the JVM and instrument them as necessary. If you want to do this, then you may want to add the flag `-javaagent:Phosphor-0.0.3-SNAPSHOT.jar=cacheDir=someCacheFolder` and Phosphor will cache the generated files in `someCacheFolder` so they aren't regenerated every run. If you take a look at the execution of Phosphor's JUnit tests, you'll notice that this is how they are instrumented. It's always necessary to instrument the JRE in advance though for bootstrapping.

Interacting with Phosphor
-----
Phosphor exposes a simple API to allow to marking data with tags, and to retrieve those tags. Key functionality is implemented in two different classes, one for interacting with integer taint tags (``edu.columbia.cs.psl.phosphor.runtime.Tainter``), and one for interacting with object tags (used for the multi-taint mode: (``edu.columbia.cs.psl.phosphor.runtime.MultiTainter``)). To get or set the taint tag of a primitive type, developers call the taintedX or getTaint(X) method (replacing X with each of the primitive types, e.g. taintedByte, etc.).
Ignore the methods ending with the suffix $$PHOSPHOR, they are used internally.
To get or set the taint tag of an object, developers first cast that object to the interface TaintedWithIntTag or TaintedWithObjTag (Phosphor changes all classes to implement this interface), and use the get and set methods.

In the case of integer tags, developers can determine if a variable is derived from a particular tainted source by checking the bit mask of that variable's tag (since tags are combined by bitwise OR'ing them).
In the case of multi-tainting, developers can determine if a variable is derived from a particular tainted source by examining the dependencies of that variable's tag.

You *can* detaint variables with Phosphor - to do so, simply use the `Tainter` or `MultiTainter` interface (as appropriate) to set the taint on a value to `0` (or `null`).

Building
------
Phosphor is a maven project. You can generate the jar with a simple `mvn package`. You can run the tests with `mvn verify` (which also generates the jar). Phosphor requires Java >= 8 to build and run its tests - but can still be used with Java 7 (there are now tests included for Phosphor's functionality with lambdas). If you are making changes to Phosphor and running the tests, you will want to make sure that Phosphor regenerates the instrumented JRE between test runs (because you are changing the instrumentation process). To do so, simply do `mvn clean verify` instead. If you would like to develop Phosphor in eclipse, use `mvn eclipse:eclipse` to generate eclipse project files, then import the project into Eclipse.

Support for Android
----
We have preliminary results that show that we can apply Phosphor to Android devices running the Dalvik VM, by instrumenting all Android libraries prior to dex'ing them, then dex'ing them, and deploying to the device. Note that we have only evaluated this process with several small benchmarks, and the process for using Phosphor on the Dalvik VM is far from streamlined. It is not suggested that the faint of heart try this.

In principle, we could pull .dex files off of a device, de-dex, instrument, and re-dex, but at time of writing, no de-dex'ing tool is sufficiently complete (for example, [dex2jar does not currently support the int-to char opcode](https://code.google.com/p/dex2jar/issues/detail?id=214&can=1&q=i2c)). Therefore, to use Phosphor on an Android device, you will need to [compile the Android OS](https://source.android.com) yourself, so that you get the intermediate .class files to instrument.

Once you have compiled Android, the next step will be to instrument all of the core framework libraries. For each library *X*, the classses are found within the file `out/target/common/obj/JAVA_LIBRARIES/X_intermediates/classes.jar`. Copy each of these jar's to a temporary directory, renaming them to *X.jar*. Then instrument each of these jar's following the instructions from the section above, *Running*. Then, use the `dx` tool provided with the Android SDK to convert each jar to dex:
`dx -JXmx3g --dex --core-library --output=dex/X.jar instrumented-jars/X.jar`. For the file, *framework.jar*, you will find that the jar now contains too many classes. Use the flag `--multi-dex` here, and assemble all of the output classes.dex files into a single jar.  

Next, use `dx` to also convert phosphor.jar to a dex file, and then copy all of the completed .dex files to the Android device. Use the same procedure to instrument each application. Then, using an `adb shell`, change the boot classpath to point to the instrumented core libraries, and invoke your application using the `dalvikvm` command.


For more information on applying Phosphor to Dalvik, please contact us.

Questions, concerns, comments
----
Please email [Jonathan Bell](mailto:jbell@cs.columbia.edu) with any feedback. This project is still under heavy development, and we are working on many extensions (for example, tagging data with arbitrary types of tags, rather than just integer tags), and would very much welcome any feedback.

License
-------
This software is released under the MIT license.

Copyright (c) 2013, by The Trustees of Columbia University in the City of New York.

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Acknowledgements
--------
This project makes use of the following libraries:
* [ASM](http://asm.ow2.org/license.html), (c) 2000-2011 INRIA, France Telecom, [license](http://asm.ow2.org/license.html)
* [Apache Harmony](https://harmony.apache.org), (c) The Apache Software Foundation, [license](http://www.apache.org/licenses/LICENSE-2.0)

Phosphor's performance tuning is made possible by [JProfiler, the java profiler](https://www.ej-technologies.com/products/jprofiler/overview.html).

The authors of this software are [Jonathan Bell](http://jonbell.net) and [Gail Kaiser](http://www.cs.columbia.edu/~kaiser/). Jonathan Bell is funded in part by NSF CCF-1763822. Gail Kaiser directs the [Programming Systems Laboratory](http://www.psl.cs.columbia.edu/), funded in part by NSF CCF-1161079, NSF CNS-0905246, and NIH U54 CA121852.

[![githalytics.com alpha](https://cruel-carlota.pagodabox.com/ae2f03ebde27be607b8ffe5a9911293d "githalytics.com")](http://githalytics.com/Programming-Systems-Lab/phosphor)
