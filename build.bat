set OLDCLASSPATH=%CLASSPATH%
set CLASSPATH=.
javac -d . src/spores/*.java
copy spores.jpg spores
jar cvfm spores.jar spores-manifest.mf spores
set CLASSPATH=%OLDCLASSPATH%
set OLDCLASSPATH=

