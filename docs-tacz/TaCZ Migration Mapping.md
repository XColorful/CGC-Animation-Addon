Reference mapping between [TaCZ](https://github.com/MCModderAnchor/TACZ) 1.1.8 and the refactored [CGC Animation Addon](https://github.com/XColorful/CGC-Animation-Addon).

Use this document to:
- locate the corresponding implementation in the refactored project
- understand architectural changes introduced during the refactor
- assist source code navigation and migration

Use original TaCZ package names or type names to find their corresponding implementation in CGC Animation Addon.

This document is intentionally maintained as a single file to simplify searching for both developers and AI agents.

Notation:
- `...` — Java type (`.java` file)
- plain text — package, namespace, field or method
- `*` — wildcard
- _Deprecated_ — already deprecated in TaCZ or intentionally removed in the refactored implementation

## Common

### Mod Compat
> ```java
> package com.tacz.guns.compat;
> ```

|com.tacz.guns.compat|dev.xcolorful.cgcanimation.core.api.resource|
|---|---|
|playeranimator.`AnimationName`|assets.animation.`BedrockAnimationTag`|
