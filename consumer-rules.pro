# MeshLink consumer ProGuard rules
# Preserve the public API surface used by Android consumers.
-keep class ch.trancee.meshlink.api.** { *; }
-keep class ch.trancee.meshlink.engine.MeshEngine { *; }
-keep class ch.trancee.meshlink.transport.AndroidBleTransport { *; }
