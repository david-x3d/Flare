# OkHttp ships its own consumer rules; Room/DataStore/Compose need none.
# Keep line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# R8 optimization miscompiles suspend lambdas in the speed test engine
# (ART: "Verifier rejected class ... copy-cat1 ... type=Undefined" at runtime).
# Shrinking and obfuscation stay on; only the optimization pass is disabled.
-dontoptimize
