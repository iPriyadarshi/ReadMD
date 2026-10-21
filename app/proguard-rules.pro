# Keep file names and line numbers so crash stack traces stay readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# JLatexMath (LaTeX rendering) looks up its fonts, symbols and macros by name at run time.
-keep class org.scilab.forge.jlatexmath.** { *; }
-keep class ru.noties.jlatexmath.** { *; }
-dontwarn org.scilab.forge.jlatexmath.**
-dontwarn ru.noties.jlatexmath.**

# Markwon and commonmark register node visitors and renderers by class; their own consumer
# rules are enough, but silence optional integrations that are not on the class path.
-dontwarn io.noties.markwon.**
-dontwarn org.commonmark.**

# Prism4j loads the generated grammar locator and each grammar by name.
-keep class io.noties.prism4j.** { *; }
-keep class * implements io.noties.prism4j.GrammarLocator { *; }
-dontwarn io.noties.prism4j.**
