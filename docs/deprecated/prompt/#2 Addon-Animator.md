> #2 提示词

# 

```
现在要将TaCZ的com.tacz.guns.compat.playeranimator移植到CGC Animation Addon的dev.xcolorful.cgcanimation.client.animation.shooter.animator
- Custom Gun Continued和CGC Animation Addon都有./docs-tacz/TaCZ Migration Mapping.md，已经确定移植后的类结构

注意:
- TaCZ的com.tacz.guns.compat.playeranimator.animation.PlayerAnimatorAssetManager已经在dev.xcolorful.customgun.client.resource._AllAssetsManager.playerAnimationManager
- @SubscribeEvent已经改为CGC封装的注册方式，见dev.xcolorful.cgcanimation.client.animation.shooter.animator.legacy.AnimationManager
- CGC Animation Addon还引入了dev.xcolorful.cgcanimation.client.animation.shooter.animator.AddonShooterAnimator，用于替换原本的DefaultShooterAnimator位置
- PlayerAnimatorCompat里诸如loadAnimationFromZip等方法在TaCZ里已经零调用，这些方法不需要移植

先保持原功能和编排进行移植，但API调用改成CGC的
```

# 

I'll start by reading the migration mapping docs and exploring both source and target packages.

Now let me explore the source and target packages in parallel.

Target paths didn't match. Let me find the actual structure.

There's a prompt doc for this task. Let me read it plus the target files.

Now I need to map TaCZ APIs to CGC equivalents. Let me explore both projects' relevant APIs in parallel.

Let me read the CGC stub classes and find TaCZ's call sites for the compat facade.

Let me read the remaining context: DefaultShooterAnimator, the addon's first prompt doc, and the mixin config.

Let me read CGC's AnimationManager and the BedrockAnimation type to resolve the asset type question.

This doc was unrelated. Let me check the CGC docs for playeranimator integration and the addon's git branches.

Let me locate the playeranimator jar to verify its API.

I've mapped the full picture. There's one genuine architectural fork I need your call on before writing code.

The problem: TaCZ's `AnimationManager` plays `playeranimator`'s `KeyframeAnimation` (via `KeyframeAnimationPlayer`). But CGC's `playerAnimationManager` stores CGC's own `BedrockAnimation` pojo, and `KeyframeAnimation`'s constructor is package-private — the only public way to make one is `AnimationSerializing.deserializeAnimation(JSON)`. So consuming CGC's manager needs an adaptation layer.

Good — that clarifies it: port TaCZ's asset manager into the addon as its own `PlayerAnimationManager`, use CGC's `AssetsFolderType` for the folder, and delegate `AddonShooterAnimator` to `DefaultShooterAnimator`. Let me gather the remaining facts.

A `PlayerAnimationManager` stub now exists in the addon. Let me read it and the current state.

Now let me read the CGC shooter events and the event-registration API.

The design is clear now. Let me verify the remaining CGC API details before writing.

All APIs verified. Let me track the work and implement.

Now let me compile.

Core compiles. Now forge-compat (which recompiles core's sources plus the mixin annotation processor).

Both modules compile. Let me verify the non-deprecated `GunDisplay` accessors and check the generated refmap.

Refmap is correctly empty. Let me switch to CGC's current (non-deprecated) `getPojo()` accessors, matching its own usage.

Let me get the exact unchecked warning location via IDEA analysis.

Found it — one method (`playRotationAnimation`) lacks the `@SuppressWarnings("unchecked")` its siblings have (TaCZ had the same gap). Let me make it consistent.

Clean. Let me verify the other new files via IDEA and review the full diff.

All clean. Let me review the final diff.

移植完成，`:core:compileJava` 与 `:forge-compat:compileJava` 均通过（EXIT=0），生成的混入 refmap 为空、IDEA 分析无 error。

## 改动（均在 CGC-Animation-Addon，未提交）

| 文件 | 内容 |
|---|---|
| [PlayerAnimationManager.java](../CGC-Animation-Addon/core/src/main/java/dev/xcolorful/cgcanimation/client/resources/assets/PlayerAnimationManager.java) | 端口 `PlayerAnimatorAssetManager`：`SimplePreparableReloadListener<Map<ResourceLocation, HashMap<String, KeyframeAnimation>>>`；目录用 CGC 的 `AssetsFolderType.PLAYER_ANIMATOR.getFolderName()` 交给 `FileToIdConverter` |
| [AnimationManager.java](../CGC-Animation-Addon/core/src/main/java/dev/xcolorful/cgcanimation/client/animation/shooter/animator/legacy/AnimationManager.java) | 补全播放逻辑（lower/loopUpper/rotation、loop/once、stop、isPlayerLie/isFlying）与四个事件处理器；保留原骨架的 register/unregister/handleEvent |
| [AnimationDataRegisterFactory.java](../CGC-Animation-Addon/core/src/main/java/dev/xcolorful/cgcanimation/client/animation/shooter/animator/legacy/AnimationDataRegisterFactory.java) | 注册 93/94/95/96 四个 `ModifierLayer` 工厂，rotation 挂 `AdjustmentYRotModifier` |
| [AdjustmentYRotModifier.java](../CGC-Animation-Addon/core/src/main/java/dev/xcolorful/cgcanimation/client/animation/shooter/animator/legacy/AdjustmentYRotModifier.java) | 原样端口，仅替换 `TimelessAPI.getGunDisplay` → `ClientResourceApi.getGunDisplayInstance` |
| [PlayerAnimatorCompatMixin.java](../CGC-Animation-Addon/core/src/main/java/dev/xcolorful/cgcanimation/client/mixin/playeranimator/PlayerAnimatorCompatMixin.java) | 注入 CGC 的四个注入点：`init`→注册数据工厂；`registerReloadListener`→注册 `PlayerAnimationManager`；`stopAllAnimation`/`playAnimation`→委托 AnimationManager，`playAnimation` 接管时返回 true |
| [AddonShooterAnimator.java](../CGC-Animation-Addon/core/src/main/java/dev/xcolorful/cgcanimation/client/animation/shooter/animator/AddonShooterAnimator.java) | `animateShooter` 委托 `DefaultShooterAnimator.INSTANCE` |
| [cgcanimationclient.mixins.json](../CGC-Animation-Addon/core/src/main/resources/cgcanimationclient.mixins.json) | `client` 数组加入该混入（原来是空，不加不会生效） |
| [forge-compat/build.gradle](../CGC-Animation-Addon/forge-compat/build.gradle) | 加 `compileOnly fg.deobf("${player_animator_maven}")`——forge-compat 会重编 core 的 srcDirs，缺这行会编译不过 |

API 换算：`IGunOperator`→`ILivingShooterGetter.cgc$fromLivingEntity` + `cgc$getSynAimingProgress`；`IGun.getIGunOrNull`→`IGunGetter.fromItemStack`；`TimelessAPI.getGunDisplay(...).ifPresent`→`ClientResourceApi.getGunDisplayInstance(...)` 可空判空；`AnimationName.*`→`BedrockAnimationTag.*`；事件改用 `getLivingShooter()/getGunItem()/getCurrentItem()/getPreviousItem()/getLogicalSide()`；`PlayerAnimatorAssetManager`→`PlayerAnimationManager`。

按你之前的说明，`PlayerAnimatorLoader`（zip/文件加载）与其专属的 `putAnimation`、零调用的 `clearAll` 未移植；`PlayerAnimatorCompat.hasPlayerAnimator3rd` 在 CGC 里已并入 `playAnimation` 且无调用方，故未注入。

## 需要你确认的三点

1. **`enableAnimator` 开关与混入的交互**：`AddonShooterAnimator.init()` 在 `enableAnimator=false` 时会 unregister（换成 DefaultShooterAnimator、退掉 legacy 事件），但混入的 `playAnimation` 是渲染期无条件生效的，会照常接管 playeranimator 动画——即关掉开关也压不住 playeranimator 路径。TaCZ 没有这个开关，我没擅自加 gate，保持原编排。要不要在混入里按 `CgcAnimationConfig.enableAnimator` 跳过？
2. **踩了弃用 API 之外的替换**：CGC 的 `GunDisplayInstance.getPlayerAnimator3rd()/is3rdFixedHand()` 已 `@Deprecated`，我改用了它自己的当前路径 `getPojo().getPlayerAnimatorLocation()/getPlayerAnimatorFixedHand()`（与 `ShooterAnimationManager`/`DefaultShooterAnimator` 的用法一致）。如果你要严格对齐 TaCZ 的调用名，说一声改回。
3. **重复加载**：按你的说明 Addon 自建了 `PlayerAnimationManager`，在 CGC 内置那份被删掉前，`player_animator` 目录会被两边各加载一次（仅浪费，无功能冲突）。

未实机验证——`playAnimation` 接管、趴下/骑乘/瞄准各分支与 93–96 层动画需要进游戏 F5 看：`./gradlew :forge-compat:runClient`。改动未提交，等你 review。

# 

[fork1](#fork1)
[fork2](#fork2)

# fork1

```
/fork 风格调整：
- 看下把刚才commit里所有返回Optional<>类型，都改成直接返回值，函数签名加@Nullable，调用处也先一个局部变量@Nullable，把ifPresent改成if (== null) return;再接着后面的逻辑
- 如果可以改就改，如果playerAnimator强制要求Optional就不用改
```

# 

Let me check the current state and find every `Optional` return in the changed files.

The work is committed. Only two `Optional` returns exist: `AdjustmentYRotModifier` (forced by playerAnimator's `AdjustmentModifier(Function<String, Optional<PartModifier>>)` — leaving as-is) and my `PlayerAnimationManager.getAnimations`. Let me read the current file.

Now let me apply the changes. First `PlayerAnimationManager`.

The user has reformatted this file. Let me read the rest to get the exact current text.

Now I'll convert the three `.ifPresent` call sites.

Compiles clean. Let me verify with IDEA and review the diff.

改完，`:core:compileJava` 与 `:forge-compat:compileJava` 通过（EXIT=0），IDEA 无 error。

## 改动

**改了：**

- [PlayerAnimationManager.java](../CGC-Animation-Addon/core/src/main/java/dev/xcolorful/cgcanimation/client/resources/assets/PlayerAnimationManager.java) — `getAnimations` 由 `Optional<KeyframeAnimation>` 改为 `@Nullable KeyframeAnimation`，去掉 `Optional` import、加 `org.jetbrains.annotations.Nullable`。
- [AnimationManager.java](../CGC-Animation-Addon/core/src/main/java/dev/xcolorful/cgcanimation/client/animation/shooter/animator/legacy/AnimationManager.java) — 三处调用 `playRotationAnimation` / `playLoopAnimation` / `playOnceAnimation` 的 `.ifPresent(lambda)` 都改成 `@Nullable var keyframeAnimation = ...; if (keyframeAnimation == null) return;`，lambda 里的 `return;` 直接提升为方法级 `return;`（原来是方法最后一句，语义不变）。

**没改（playerAnimator 强制要求）：**

- [AdjustmentYRotModifier.java](../CGC-Animation-Addon/core/src/main/java/dev/xcolorful/cgcanimation/client/animation/shooter/animator/legacy/AdjustmentYRotModifier.java) — 它是 `Function<String, Optional<AdjustmentModifier.PartModifier>>`，因为 `AdjustmentModifier(Function<String, Optional<PartModifier>>)` 这个构造器签名就是 `Optional`，改不了。

其余方法本来就已经是直接返回 `@Nullable`（`ClientResourceApi.getGunDisplayInstance`、`IGunGetter.fromItemStack`），无需调整。

改动未提交，等你 review。

# fork2

```
现在disable后仍然是播放player animator动画，按CGC Animation Addon的设计，unregister后所有监听应该都关掉了，哪里还有残余？
- 是否是注册到playerAnimator并由playerAnimator调用所以参与?如果是，那得读config boolean来拦截
```

# 

The user edited `PlayerAnimationManager` to return `@Nullable` instead of `Optional`. Let me read the current state and trace the disable path.

The user committed the port and edited the manager. Let me check the log and the current call sites.

The user has committed and restyled. Let me read the current mixin and manager to answer precisely.

Let me check `ModifierLayer.isActive()` semantics to pick a safe approach.

Now I understand the residual. Let me read the current animator before editing.

Found it. The residual is the mixin itself — I'll gate it on the config and clear already-playing layers on disable.

找到了：**残余不是监听，是混入**。

## 残余在哪

`AddonShooterAnimator.unregister()` 只做了三件事：换回 `DefaultShooterAnimator`、`AnimationManager.unregister()`（退掉 fire/reload/melee/draw 四个 CGC 事件）、写配置 false。

但播放路径根本不走事件：CGC 的 `ShooterAnimationManager.setRotationAnglesHead` **每渲染帧**都会调 `PlayerAnimatorCompat.playAnimation(...)`，而 `PlayerAnimatorCompatMixin` 是类加载时贴上的混入，不随 `unregister` 消失 → 无条件接管并播放。这就是残余。

你猜的「注册到 playerAnimator 由它调用」也对了一半，两层要分开看：

- **驱动层**：混入劫持 `PlayerAnimatorCompat.playAnimation`，每帧把动画重新赋给 `ModifierLayer`。这个不受任何 unregister 影响。
- **tick 层**：`AnimationDataRegisterFactory.registerData()` 把 93–96 四个层注册进 playerAnimator 的 `PlayerAnimationFactory.ANIMATION_DATA_FACTORY`，playerAnimator 建好 `ModifierLayer` 后**每帧自行 tick**。所以层上只要有过动画，就算不再驱动也会继续播——必须显式 `replaceAnimationWithFade(fade, null)` 才会停。

## 改法（按你说的读 config）

| 文件 | 改动 |
|---|---|
| [PlayerAnimatorCompatMixin.java](../CGC-Animation-Addon/core/src/main/java/dev/xcolorful/cgcanimation/client/mixin/playeranimator/PlayerAnimatorCompatMixin.java) | `cgc$playAnimation` 加 `if (!CgcAnimationConfig.enableAnimator) return;`，禁用时不接管，CGC 落到 vanilla animator |
| [AddonShooterAnimator.java](../CGC-Animation-Addon/core/src/main/java/dev/xcolorful/cgcanimation/client/animation/shooter/animator/AddonShooterAnimator.java) | `unregister()` 增加一次性清理：遍历 `Minecraft.getInstance().level.players()` 调 `AnimationManager.stopAllAnimation(player)`，把还在 tick 的层清空 |

**清理为什么放 unregister 而不是混入里每帧调**：反编译 `ModifierLayer.isActive()` 是「有 modifier 就返回 `modifiers.get(0).isActive()`」，fade-out 的 8 tick 内恒为 true。逐帧调 stop 会逐帧重启 fade、永远结束不了；一次性调用才真正落到 null。

**没拦的三处及原因**：
- `init()` 的 `registerData()`——纯工厂注册，拦掉的话之后 `/cgcanimation enable` 时层不会被创建（playerAnimator 只在玩家加入时构建 animation stack）。
- `registerReloadListener` 注册的 `PlayerAnimationManager`——只加载资源、不产生动画；拦掉反而会让 enable 后没数据可用。
- `stopAllAnimation` 混入——只停不播，保留正好能帮着清残留。

`:core:compileJava` + `:forge-compat:compileJava` 通过（EXIT=0），IDEA 无 error。未实机验证——`/cgcanimation disable` 后应立刻回到 vanilla 姿态，`enable` 后应恢复。改动未提交。

# 

```
现在移植到1.21.1，播放动画会抽搐，但是1.20.1不会
检查下是否是刚修改的AdjustmentYRotModifier使用的ClientRenderUtils.getRenderFrameTime()的问题
如果不是也继续排查
```

# 

I'll investigate the twitching. Let me look at both worktrees' state and the `getRenderFrameTime()` implementation.

Key difference found in the wrapper. Let me read both implementations and the addon files.

The 1.21.1 wrapper returns `getGameTimeDeltaPartialTick(false)`. Let me verify what that actually returns in 1.21.1.

`getGameTimeDeltaPartialTick(false)` returns the same tick residual as 1.20.1's `getFrameTime()`, so the wrapper looks equivalent. Let me diff the whole port between versions to find what actually differs.

The port files are byte-identical across versions, so it's version-dependent behavior. Let me verify the 1.20.1 side of the wrapper.

Confirmed the wrapper is semantically equivalent (`partialTick` residual on both). So the partial tick isn't it — let me check the playeranimator version difference.

**playeranimator 2.0.1 on 1.21.1 vs 1.0.2 on 1.20.1** — a major version jump. And there's a sources jar. Let me inspect.

Let me check where 1.21.1 sets `Timer.frozen`, and compare the 1.0.2 fade logic.

Found something important — let me compare `AdjustmentModifier`'s fade logic between 1.0.2 and 2.0.1.

# 

```
可以对比1.21.1非官方移植
```

# 

TaCZ-1.21.1 is the reference port. Let me find its playeranimator compat and compare.

TaCZ-1.21.1 uses the **same** `getGameTimeDeltaPartialTick(false)` — so the wrapper isn't the culprit. Let me see how CGC 1.21.1 calls the compat vs how TaCZ 1.21.1 does.

**Found it.** TaCZ-1.21.1 uses the new `AnimationCodecs.deserialize("json", ...)` path, while our `PlayerAnimationManager` still uses the legacy `AnimationSerializing`. Let me confirm the difference.

Confirmed: `AnimationSerializing` is `@Deprecated(forRemoval=true)` in 2.0.1 and uses a single legacy GSON path, while `AnimationCodecs.deserialize` chains codecs. Let me verify they actually produce different data.

The reference's only functional 1.21.1 change is the deserialization path. Let me confirm the two paths differ.

The rotation layer is replaced **every frame** and `AdjustmentModifier.getFadeIn` divides by `beginTick`. Let me check what `beginTick` is.

Let me check whether CGC's own side differs between versions.

Let me check two remaining version-specific suspects: 1.20.1's `getPartialTick` and `AbstractFadeModifier`.

**Key finding**: 1.20.1's `Minecraft.getPartialTick()` (what TaCZ used) returns Forge's `realPartialTick`, not the raw timer residual. Let me see how it's computed.

One more check before I change anything — does 1.0.2 have `AnimationCodecs`?

Let me check a real gun pack's player_animator JSON to see the animation-name casing.

One last check — what `frozen` means in normal play.

I've exhausted the version-symmetry question. Let me apply the reference's parsing change plus the one concrete defect I found. First, read the 1.21.1 files.

`stopTick` defaults to 0, so the fade-out stays 1 — the guard is safe. Applying both changes.

Compiles clean. Here's what I found.

## 1. `getRenderFrameTime()` 不是原因

三点证据：

- **参照实现用的是同一个调用**：`TACZ-1.21.1` 的 `AdjustmentYRotModifier` 第 33 行就是 `mc.getTimer().getGameTimeDeltaPartialTick(false)`，和我们 1.21.1 的 `ClientRenderUtils.getRenderFrameTime()` 完全一致。
- 我核了三个实现，返回的都是同一个「tick 残差」：
    - 1.20.1 `Minecraft.getFrameTime()` → `timer.partialTick`
    - 1.20.1 `Minecraft.getPartialTick()`（TaCZ 1.20.1 用的）→ Forge 的 `realPartialTick` = `pause ? pausePartialTick : timer.partialTick`
    - 1.21.1 `getGameTimeDeltaPartialTick(false)` → `frozen ? 1.0F : (paused ? pausedDeltaTickResidual : deltaTickResidual)`

  `frozen = !isLevelRunningNormally()`，正常游玩时为 false，所以三者取值相同（只有暂停/`/tick freeze` 才有差别，而实体渲染对玩家传的 `runsNormally` 恒为 true——`TickRateManager.isEntityFrozen` 对 Player 永远返回 false）。

## 2. 与参照实现真正的差异（已改）

把 `TACZ` 1.20.1 与 `TACZ-1.21.1` 的整个 playeranimator 包逐文件 diff，**唯一的功能性改动**只有一处：`PlayerAnimatorAssetManager` 从 `AnimationSerializing.deserializeAnimation(...)` 换成 `AnimationCodecs.deserialize("json", ...)`。我们在 1.21.1 分支上还在用 `AnimationSerializing`——它在 2.x 里已标 `@Deprecated(forRemoval = true)`。

已在 [PlayerAnimationManager.java](../CGC-Animation-Addon-1.21.xneoforge/core/src/main/java/dev/xcolorful/cgcanimation/client/resources/assets/PlayerAnimationManager.java) 按参照改成 `AnimationCodecs` + `instanceof KeyframeAnimation` 过滤（`entry.getValue().open()` 取 InputStream）。**只动 1.21.1 分支**——1.0.2 的 jar 里没有 `AnimationCodecs`/`IPlayable`。

坦白说：静态比对两条路径对基岩格式文件最终都落到 `GeckoLibSerializer.readAnimations`，我没能证明解析结果不同；但这既是参照实现的做法，也去掉了 `forRemoval` 依赖。

## 3. 查到的具体缺陷（很可能是抽搐来源，已修）

[AnimationManager.playRotationAnimation](../CGC-Animation-Addon-1.21.xneoforge/core/src/main/java/dev/xcolorful/cgcanimation/client/animation/shooter/animator/legacy/AnimationManager.java:196) 每帧都新建一个 `KeyframeAnimationPlayer` 覆盖 rotation 层——它的两个兄弟方法 `playLoopAnimation` / `playOnceAnimation` 都有「已在播放就不重建」的守卫，只有它没有。后果：

- playeranimator 该层 8 tick 的淡入每帧重来；
- `AdjustmentModifier.getFadeIn()` 算的是 `(getTick() + tickDelta) / beginTick`。玩家每帧被重建 → `getTick()` 恒为 0；而 `beginTick` 为 0（`GeckoLibSerializer` 从不设置，`AnimationBuilder.beginTick` 默认 0）→ `tickDelta / 0`：正常是 `+Inf → min → 1`，但**渲染 partialTick 恰好为 0 时是 `0/0 = NaN`**。`fade = NaN` → `PartModifier.rotation().scale(NaN)` → body/head/arm 的变换变成 NaN → 那几帧抽一下。

已按 `playOnceAnimation` 的写法加守卫。另核对过 `stopTick` 默认 0，所以 `getFadeOut` 恒为 1，加守卫后调整量维持在满值、不会随时间衰减。

**必须说清楚的不确定点**：两个分支的 addon 六个文件、CGC 的 `PlayerAnimatorCompat`/`ShooterAnimationManager`/`HumanoidModelMixin`、以及 playeranimator 的 `AdjustmentModifier`/`AbstractFadeModifier`/`ModifierLayer` 我逐字节比过，**完全相同**。所以第 3 条是真实缺陷，但我无法证明它就是「只在 1.21.1 出现」的那个触发点。

`:core:compileJava` + `:neoforge-compat:compileJava` 通过（EXIT=0）。**未实机验证**——请进游戏看抽搐是否消失。若还在，下一个嫌疑是 `playLoopUpperAnimation`/`playLowerAnimation` 的守卫：它拿 `extraData["name"]`（原始大小写）和查表用的全小写 key 比较，任一枪包名带大写就会每帧重建循环动画。要不要我把第 3 条也回移到 1.20.1 分支？改动未提交。

# 

```
现在修改后的抽搐更厉害了
TaCZ1.21.1非官方移植测试下是没问题的，再排查下问题
```

# 

The twitch getting *worse* from my guard is a strong clue — it suggests the adjustment is being applied more than once. Let me check how `registerData()`'s factories are stored, in both playeranimator versions.

**Found it.** `FactoryHolder.factories` is a static `List` and `registerFactory` just does `List.add` — **no dedup** in either version. Every `registerData()` call stacks another full set of 4 layers, and each stacked rotation layer carries its own `AdjustmentModifier`. Let me confirm how often our `registerData()` runs.

Confirmed: the event is NeoForge's `RegisterClientReloadListenersEvent`, which fires **per resource reload**, and `registerData()` appends 4 factories each time (no dedup). Let me check whether the animation stack collapses duplicate priorities — this would explain why only 1.21.1 breaks.

`AnimationStack.addAnimLayer` also doesn't dedup — duplicates stack in both versions. Let me see the current file state (you edited `AnimationManager`) and the factory.

The guard clearly made it worse, so re-creating that player each frame is load-bearing — I'll revert it. And I found a real wiring defect: our `registerData()` (unlike TaCZ's one-shot init) runs on every `RegisterClientReloadListenersEvent`, and `FactoryHolder.factories` is a static list with **no dedup**.

编译通过。这轮的进展和一个新发现：

## 1. 已回退我上次的 rotation 守卫

它让抽搐变严重，说明「每帧重建 `KeyframeAnimationPlayer`」在 TaCZ 的设计里是**承重的**——`replaceAnimationWithFade` 每帧把 `AbstractFadeModifier.time` 归零，rotation 层自身 EMPTY 动画的 alpha 一直≈0，只有 `AdjustmentModifier` 生效。加了守卫后 alpha 涨到 1，EMPTY 动画也整个参与进去，反而更糟。已恢复成和 TaCZ 一致的写法。`AnimationCodecs` 那处保留（与参照一致）。

## 2. 新发现：我们的装配方式会把 Adjustment 叠乘

逐层查了 playeranimator 的注册表，两处都**不去重**：

- `PlayerAnimationFactory$FactoryHolder.factories` 是 `private static final List`，`registerFactory` 就是 `List.add`（1.0.2 字节码与 2.0.1 源码我都核过，无去重）；
- `AnimationStack.addAnimLayer` 按优先级插入，同优先级也不合并。

于是 `AnimationDataRegisterFactory.registerData()` **每被调用一次就多出整整 4 个层**；多出来的 rotation 层各自带一个 `AdjustmentModifier`，而 `AnimationStack.get3DTransform` 会逐个 layer 串联，**body/head/arm 的俯仰+偏航修正就被应用了 N 次**——正好是「抽搐」。

而调用次数两边不一样：

| | `registerData()` 的调用点 | 频率 |
|---|---|---|
| TaCZ-1.21.1 | `PlayerAnimatorCompat.init()` ← `FMLClientSetupEvent` | **一次** |
| 我们的 addon | 混入 CGC 的 `PlayerAnimatorCompat.init()` ← `_AllAssetsManager.onAddClientReloadListenerEvent` ← NeoForge `RegisterClientReloadListenersEvent` | **每次资源重载**（启动、F3+T、切资源包、连接后的重载） |

这也解释了为什么加了守卫反而更糟：以前每帧重置淡入把 rotation 层的 alpha 压在 0 附近，把叠乘的修正量掩盖了一部分；守卫一加，N 份修正全量生效。

## 3. 已做的修复

[AnimationDataRegisterFactory.java](../CGC-Animation-Addon-1.21.xneoforge/core/src/main/java/dev/xcolorful/cgcanimation/client/animation/shooter/animator/legacy/AnimationDataRegisterFactory.java) 的 `registerData()` 改为**幂等**（静态 `registered` 标志）。`factories` 是进程级静态 List、永不清空，所以只注册一次是正确语义。

只动 1.21.1 分支。`:core:compileJava` + `:neoforge-compat:compileJava` 通过（EXIT=0），未提交。

## 需要你验证的一点

**请重测。** 我无法在本地启动客户端，也没法确认一次普通会话里到底发生了几次 `RegisterClientReloadListenersEvent`——我推理上确认的是「一旦多注册一次就会叠乘」，而启动/连接/F3+T 任一多触发一次就够了，幂等后这个变量被彻底消除。

- 抽搐消失 → 归因确认，我再把幂等守卫补到 1.20.1 分支（那边 `_AllAssetsManager` 同样每次重载调 `init()`，是同一个隐患，只是你测试时可能没触发重载）。
- 还在抽 → 下一步我建议加一行临时日志打印 `factories.size()`（每次 `registerData()` 后）和玩家构造时的层数，直接看是不是还有别的叠加源；另外 `playLoopUpperAnimation`/`playLowerAnimation` 的守卫拿 `extraData["name"]` 原始大小写和全小写 key 比较，任一枪包名带大写就会每帧重建循环动画，也可以一并查。

# 

```
还在抽搐，跟最初没修改时差不多，继续排查
```

# 

Still twitching with TaCZ-faithful code means the difference is in the surrounding wiring. Let me look at CGC's `PlayerModelMixin`, which the port's arm handling could be fighting.

The dedup guard didn't change it — so duplicates weren't the cause. Let me check one more version-sensitive spot: `KeyframeAnimationPlayer.isActive()`, which `playLoopAnimation`'s guard depends on.

Let me check which playeranimator version TaCZ-1.21.1 actually uses — ours pinned `7389814`.

TaCZ-1.21.1 uses the same playeranimator 2.0.1. One thing I haven't verified: what the **referenced CGC jar** (not the local checkout) actually returns from `getRenderFrameTime()`.

TaCZ-1.21.1's recoil writes the player's `xRot` exactly like CGC's, so that's not it either. I've now verified an enormous amount is identical across the two setups. Let me set up a decisive isolation test.

只剩最后一步没法在本地做——运行客户端。我放了一个临时诊断开关，请你跑一次就能把问题劈成两半。

## 先说结果：幂等守卫没解决

叠加注册**不是**原因。守卫保留（多次重载确实会叠层，是隐患），但它与此抽搐无关。

## 这一轮排除掉的（都验证到字节码/源码级）

| 嫌疑 | 结论 |
|---|---|
| `ClientRenderUtils.getRenderFrameTime()` | **不是**。我反编译了你实际编译用的发布 jar（`custom-gun-continued-1653554-8953760`），字节码就是 `getTimer().getGameTimeDeltaPartialTick(false)`，和 TaCZ-1.21.1 第 33 行完全一致 |
| playeranimator 版本 | 两边都是 `player-animation-lib-forge 2.0.1+1.21.1` |
| `AdjustmentModifier` / `AbstractFadeModifier` / `ModifierLayer` / `KeyframeAnimationPlayer` / `AnimationStack` | 2.0.1 与 1.0.2 我逐个比过，**逻辑完全相同**；`registerFactory` 不去重、`addAnimLayer` 同优先级也不合并 |
| addon 六个文件 / CGC 的 `PlayerAnimatorCompat`·`ShooterAnimationManager`·`HumanoidModelMixin` | 两个分支逐字节相同 |
| TaCZ-1.21.1 的 playeranimator 包 | 与 TaCZ-1.20.1 只差 `AnimationCodecs`、`ResourceLocation` API、`SubscribeEvent` 三处 |
| 后坐力写 `player.setXRot` | 两边都在渲染帧里改本体 xRot，写法一致（TaCZ 在 `CameraSetupEvent`，CGC 在 `GunCameraHelper`） |
| 工厂叠加注册 | 上一轮已排除 |

## 关键推理：抽的是「本体修正」，不是动画层

我上一轮加的守卫（rotation 层不再每帧重建 player）让抽搐**变重**。守卫的作用恰恰是让 `AdjustmentModifier` 不再被每帧新建的 `AbstractFadeModifier`（`time=0`、alpha≈0）冲淡——也就是**让修正量真正显出来**。修正显出来就抽得更凶 ⇒ 抽搐幅度与「本体修正被施加多少」正相关 ⇒ **症状在 `AdjustmentYRotModifier` 的输出里，不在动画层**。

## 请你做的测试

我已在 [AdjustmentYRotModifier.java](../CGC-Animation-Addon-1.21.xneoforge/core/src/main/java/dev/xcolorful/cgcanimation/client/animation/shooter/animator/legacy/AdjustmentYRotModifier.java) 顶部加了一行临时短路（`if (true) return Optional.empty();`，有明显注释），编译已过（EXIT=0）。**重测一次：**

- **不抽了** → 确认是本体修正。下一步我查它的输入源（`xRotO/xRot`、`yBodyRot/yHeadRot` 与插值）和那条 `fadeIn=(getTick()+delta)/beginTick`（`beginTick` 恒为 0）——那行在 2.0.1 里没变，但只有 CGC 这边的调用时序不同。
- **还抽** → 症状在动画层，我去查 `playLoopUpperAnimation`/`playLowerAnimation` 的「已激活就不重建」守卫是否在 2.0.1 下失效（失效就会每帧重建循环动画）。

顺便几个不用改代码的观察，能大幅缩小范围：

1. **只自己抽，还是别的玩家/生物也抽？**（区分「本机玩家特有路径」与「所有玩家的层处理」）
2. **只在开火时抽，还是一直抽？**（后坐力每渲染帧改本体xRot，会让修正的插值抖动）
3. 是否装了 ShoulderSurfing？（CGC 那条分支会改走 SSR 相机）

诊断那行验证完我立刻删掉。当前改动未提交。

# 

```
还在抽搐，并且取消安装ShoulderSurfing也会抽搐；1.21.1TaCZ装了ShoulderSurfing后仍然不抽搐
继续排查
```

# 

The adjustment is innocent — that's a big narrowing. Let me revert that diagnostic and get the next split.

While you test, let me check whether anything else in CGC touches those same animation layers.

诊断还原了，编译通过。工作树现在只剩两处改动（幂等注册 + AnimationCodecs），`AdjustmentYRotModifier` 和 `AnimationManager` 都已回到和 TaCZ 一致的写法。

## 这轮的结论：本体修正无辜

关掉 `AdjustmentYRotModifier` 后**仍然抽搐**，所以抽搐不在修正项里。我上一轮「修正被叠乘」的推论被证伪了。

顺带查到一个很有用的边界：**CGC 整个 `core`/`neoforge-compat`/`forge-compat` 里没有任何一处 `dev.kosmx` 或 `PlayerAnimationAccess` 引用**——那四个层的读写**只有我们 addon 这一处**。所以抽搐只可能来自两个地方：

- 我们的播放层（`playLowerAnimation` / `playLoopUpperAnimation` / `playOnceAnimation` / `playRotationAnimation`）；
- 或 addon 关掉后 CGC 自己接手的 `DefaultShooterAnimator`。

另外两处旁证，和你的实测吻合：TaCZ-1.21.1 的后坐力同样在渲染帧里写本体 xRot（`CameraSetupEvent.java:221`，`player.setXRot(...)`），写法与 CGC 的 `GunCameraHelper:148` 一致；SSR 两边都不影响。

## 请帮我做两个观察（都不用改代码、不用重新编译）

**1. `/cgcanimation disable` 之后还抽吗？** 这是最干净的分界：

- **不抽了** → 确认在我们的播放层，我下一步逐层掐（lower / loop_upper / once）定位；
- **还抽** → 与我们的播放层无关，是 CGC 那边接手后的 `DefaultShooterAnimator`（也就是 `AddonShooterAnimator` 委托过去的那条），我转去查 CGC 侧，不再动 addon。

**2. 抽搐的是哪些部位？**

| 抽的部位 | 对应 |
|---|---|
| 腿 / 下半身 | LOWER 层（93） |
| 手臂 / 上半身 | LOOP_UPPER 层（94） |
| 头 | 修正项（已排除） |

还有两个侧面信息如果能一起看到就更省一轮：是**站着不动也抽**，还是**只在移动/开火时抽**；以及**只自己抽还是别的玩家也抽**。

## 当前工作树

```
core/.../animator/legacy/AnimationDataRegisterFactory.java   +11  （幂等注册，防重载叠层）
core/.../resources/assets/PlayerAnimationManager.java        ±15  （AnimationCodecs，对齐参照）
```

两处都不是抽搐的原因，但第二处与 TaCZ-1.21.1 对齐、第一处是真实隐患，先留着。诊断代码已全部删除，未提交。

# 

```
移动时上下半身都抽搐，静止不动的时候表现为帧数比较低（相较于1.20.1）
先在1.20.1把潜在隐患修了，以及TaCZ是否有这个问题？
```

# 

That "低帧数" detail is the strongest clue yet — it matches CGC's own `DefaultShooterAnimator` (which steps on the 20 Hz synced aim progress), not a playeranimator animation. Let me first do the asked 1.20.1 fix.

1.20.1 的隐患已修并编译通过。

## 1. 1.20.1 已做的修复

[AnimationDataRegisterFactory.java](../CGC-Animation-Addon/core/src/main/java/dev/xcolorful/cgcanimation/client/animation/shooter/animator/legacy/AnimationDataRegisterFactory.java) — `registerData()` 改为幂等（静态 `registered` 标志）。`PlayerAnimationFactory.FactoryHolder.factories` 是进程级静态 List、`registerFactory` 只做 `List.add` 不去重，而我们的混入挂在 CGC 每次 `RegisterClientReloadListenersEvent` 都会调的 `PlayerAnimatorCompat.init()` 上——重载一次就多叠 4 个层，rotation 层的 `AdjustmentModifier` 会叠乘。`:core:compileJava` + `:forge-compat:compileJava` 通过。

**没动**的隐患（故意）：`playRotationAnimation` 每帧重建 `KeyframeAnimationPlayer`（TaCZ 原始写法，1.20.1 现在正常，我在 1.21.1 上加守卫反而更糟），以及 `playLoopAnimation` 拿 `extraData["name"]` 原始大小写比较（默认枪包是全小写，不触发）。

## 2. TaCZ 是否有这个问题

**叠加注册隐患：TaCZ 没有。** 它的 `PlayerAnimatorCompat.init()` 挂在 `FMLClientSetupEvent`，整个进程只跑一次；我们是挂在 CGC 的 `init()` 上。这是 addon 特有的装配差异。

**抽搐本身：TaCZ-1.21.1 的播放代码和我们逐字节相同**（只差一处 `AnimationCodecs` 调用），所以如果抽搐来自这段代码，TaCZ-1.21.1 也该抽。你实测它不抽 ⇒ **抽搐不在这段移植代码里**，而在「谁在驱动这批层」上。

## 3. 新线索指向一个新假说

**「静止不动时表现为帧数比较低」这句非常具体**，它匹配的是 CGC 自己的 `DefaultShooterAnimator`，不是 playeranimator 动画：

- `DefaultShooterAnimator` 用 `cgc$getSynAimingProgress()` 插值手臂，而瞄准进度是**每 tick 同步一次（20 Hz）**的量——你 `#54` 文档里就记过「瞄准进度对客户端是每 tick 跳一档，手臂姿态跟着阶梯推进」。20 Hz 阶梯 = **帧数比较低**。
- 它**只写手臂，不碰腿**。

**假说：1.21.1 上我们的 `playAnimation` 根本没接管，你看到的是 CGC 的 `DefaultShooterAnimator`（`AddonShooterAnimator` 委托过去的那条）。** 这一条能解释**全部**否定结果：

| 已测结果 | 在「我们的层没接管」下的解释 |
|---|---|
| 关掉 `AdjustmentYRotModifier` 仍抽 | 那层没在驱动 |
| 装/不装 SSR 都抽 | 同上 |
| 幂等注册守卫无变化 | 同上 |
| 1.20.1 不抽、1.21.1 抽 | 1.20.1 层接管了，1.21.1 没有 |

## 4. 请按顺序验证（第 1 条不用改代码）

1. **`/cgcanimation enable`**，或看 `config/cgcanimation-client.json` 里的 `enable_animator`。你之前测过 disable，它会把 false **写进配置**；同一个实例后续启动就是关着的 → 混入直接 early-return → 全程走 CGC 的 animator。**这条最可能。**
2. **看腿**：站定/走动时腿有没有播枪包的 `*_lower`（半蹲、走姿）？如果腿只有原版摆动 → 我们的层确实没接管。
3. **`/cgcanimation disable` 前后姿态有没有变化？** 几乎不变 ⇒ 我们的层本来就没在驱动。
4. 日志搜 `Failed to player animation file` 与 playeranimator 报错（`prepare` 解析失败会让 manager 为空 → `hasPlayerAnimator3rd` 恒 false）。

如果是第 1 条，那这不是代码 bug，而是测试状态问题；如果是第 2/3 条，我就去查 1.21.1 的资源装配（`player_animator` 目录是否被 CGC 的枪包加载链路扫到）。改动未提交。

# 

```
disable后正常播放CGC内置动画
enable后移动时上下身都抽搐，瞄准时上半身帧数低，下半身抽搐
日志放在D:\Temp-File\debug.log
```

# 

Log is exactly what I need. Let me search it.

The log shows mass `GunDisplayInstance ... validation failed` warnings. Let me check what that means in CGC.

This is pointing at "our layers may never get data". Let me verify CGC 1.21.1 actually calls the compat hooks.

The log confirms the mixin applied and CGC wires the hooks. I need to know whether our layers actually get data and whether they're re-created per frame. Let me add temporary instrumentation.

**Found something concrete in the log.** CGC *drops* invalid instances (`_DataInstanceManager:79`), and the log shows mass failures:

75 gun displays are dropped as invalid on 1.21.1. Let me add targeted diagnostics so one run answers the remaining questions.

编译通过。日志给了几条实打实的线索，同时我加了两个诊断日志，跑一次就能定性。

## 日志里的发现

**1. 混入确实生效了，但有一条兼容级别警告**
```
Mixing playeranimator.PlayerAnimatorCompatMixin ... into ...PlayerAnimatorCompat
Class version 65 required is higher than the class version supported by the current version of Mixin (JAVA_17 supports class version 61)
```
四个注入点随后都列出来了（`cgc$init` / `cgc$registerReloadListener` / `cgc$stopAllAnimation` / `cgc$playAnimation`，最后一个用到了返回值），所以**注入是成功的**。但我们的混入类是 Java 21（class 65），而 `cgcanimationclient.mixins.json` 里写的是 `"compatibilityLevel": "JAVA_17"`。当前 Mixin 版本只认到 61，所以**不能改成 JAVA_21**（会直接报错）——这条先记着，属于配置与编译目标不一致，不一定是病因。

**2. CGC 侧的钩子都在**
`_AllAssetsManager.onAddClientReloadListenerEvent` 里 `PlayerAnimatorCompat.init()`（113 行）和 `registerReloadListener(event)`（116 行）都在调，内置的 `playerAnimationManager` 已注释掉（和你之前说的一致）。所以我们的 `registerData()` 和资源监听器都装上了。

**3. 没有 `Failed to player animation file`** —— 我们的 `prepare` 没遇到解析异常（但这不等于扫到了文件，见下）。

**4. 75 个 `GunDisplayInstance validation failed`** —— 这条值得注意：
```java
// _DataInstanceManager.buildPojoInstance
I instance = instanceFactory.apply(pojo);
if (instance != null) targetMap.put(location, instance);
else CustomGun.LOGGER.warn("{} {} validation failed", instanceClass, location);   // 直接丢弃
```
失败的实例是**被丢掉**的，所以 `tacz:hk_g3_display`、`tacz:m4a1_display`、`tacz:glock_17_display`、`suffuse:svd_display` 等 75 个（还有 16 个 BlockIndex、2 个 AttachmentIndex）在 1.21.1 上**没有 display 实例**。对这些枪，`ShooterAnimationManager.setRotationAnglesHead` 会在 `if (gunDisplayInstance == null) return;` 直接返回——**既不驱动我们的层，也不调 CGC 的 animator，而且不会停掉上一次的层**，于是上一个枪留下的那些层继续叠在原版姿态上。这天然会表现为"换枪后姿态打架"。

需要你确认一件事：**这 75 个失败在 1.20.1 上有吗？** 如果你手头有 1.20.1 的日志，grep 一下 `validation failed`。1.21.1 独有的话，那它本身就是一个 CGC 侧的移植缺陷。

## 我加的临时诊断（跑一次就够）

```
[cgcanim-debug] player_animator loaded: N file(s), M animation(s) [...]
[cgcanim-debug] loop replace #K layer=<层> want=<目标动画> was=<原动画>
```

怎么读：

- **`loaded: 0 file(s)`** → 我们的资源管理器根本没扫到枪包的 `player_animator`（1.21.1 资源装配差异）→ `hasPlayerAnimator3rd` 恒 false → `playAnimation` 不接管 → 你看到的是 CGC 的 `DefaultShooterAnimator`，而它的瞄准姿态走 20 Hz 同步的 `cgc$getSynAimingProgress()`，正好对应"瞄准时上半身帧数低"。
- **`loop replace #` 每秒涨约 60** → `playLoopAnimation` 的"已激活就不重建"守卫失效，循环动画每帧重建 → 对应"移动时抽搐、静止时帧数低"。
- **两者都正常**（数据有、replace 每秒 ~0）→ 层没问题，我下一步把诊断挪到混入的 `cgc$playAnimation` 上看接管率。

1.20.1 那边只加了幂等注册（已编译通过），1.21.1 这边现在是"幂等注册 + AnimationCodecs + 两个诊断日志"。都未提交。

# 

```
两个版本日志都好了
```

# 

Two hypotheses are now dead: data **does** load (4 files, 84 animations) and the loop guard **holds** (only one replace ever). So the layers are driven correctly — the animation output itself is coarse. Let me compare how each playeranimator version turns ticks into per-frame interpolation.

Data loads and the guard holds — so the coarse output must be interpolation. `KeyframeAnimationPlayer.get3DTransform` ignores its `tickDelta` argument, so the per-frame value must come from `setupAnim`. Let me trace it.

Let me look at playeranimator's own render hook — that's where the per-frame tickDelta comes from.

This is the crux — `ModifierLayer.setupAnim` prefers `modifiers.get(0)`. Let me compare that method across the two playeranimator versions.

Let me trace the one remaining unknown: the per-frame `tickDelta` our layers actually receive.

1.21.1 compiles. Let me put the identical probe on 1.20.1 so the two logs are directly comparable.

两个日志都读到了，直接否掉了前两个假说，同时把病因定位到了一个具体的字段。

## 日志给的事实

**1. 数据没问题**
```
[cgcanim-debug] player_animator loaded: 4 file(s), 84 animation(s)
  [tacz:rifle_default.player_animation, rainforest:rifle_default.player_animation,
   tacz:pistol_default.player_animation, tacz:minigun.player_animation]
```
资源管理器正常扫到并解析成功，`hasPlayerAnimator3rd` 对默认包的枪是 true。

**2. 守卫成立，循环动画没有每帧重建**
```
[cgcanim-debug] loop replace #1 layer=tacz:lower_animation want=hold_lower was=<inactive>
```
计数只在 #1 打（每 120 次才打一条），**整个会话只出现这一次** → `playLoopAnimation` 的「已激活就不重建」守卫一直生效。所以我前两轮的假说（数据没加载 / 每帧重建）**都被日志直接否掉**。

**3. 两个版本的失效 display 数量**
`validation failed`：1.20.1 = **38**，1.21.1 = **86**（75 × GunDisplayInstance、16 × BlockIndex、2 × AttachmentIndex）。1.21.1 多丢了一倍多。这些枪的 display 实例被 `_DataInstanceManager` 丢弃 → `setRotationAnglesHead` 在 `gunDisplayInstance == null` 处直接 return → 既不驱动层也不停层。这是 CGC 1.21.1 侧的移植问题，值得你单独看（但 1.20.1 也有 38 个，不是唯一差异）。

## 机制定位到了具体字段

顺着 playeranimator 的调用链读到底：

```
PlayerRendererMixin.applyBodyTransforms
  → AnimationProcessor.setTickDelta(partialTicks)
        this.tickDelta = tickDelta;
        animation.setupAnim(tickDelta);            // ← 唯一把 tickDelta 送进动画的路径
  → AnimationProcessor.get3DTransform(name, type, value0)
        animation.get3DTransform(name, type, this.tickDelta, value0)
```

而 **`KeyframeAnimationPlayer.get3DTransform(...)` 完全忽略传进来的 `tickDelta`**（1.0.2 字节码与 2.0.1 源码一致），它内部 `BodyPart.getBodyRotation` → `Axis.getValueAtCurrentTick`，用的是被 `setupAnim()` 写进 `this.tickDelta` **字段**的值。

**结论：整条链上只要 `setupAnim(partialTicks)` 没跑到或传了 0，姿态就只会按整数 tick 跳变——正好就是"20 Hz / 帧数低 / 移动时抽搐"。**

我把这条链在 1.0.2 与 2.0.1 上逐段核对过，两版实现完全一致：`ModifierLayer.setupAnim`（优先走 `modifiers.get(0)`）、`AnimationContainer.getAnim/setupAnim`、`AbstractFadeModifier.setupAnim`、`KeyframeAnimationPlayer.get3DTransform`、`AnimationProcessor.setTickDelta` 字节码/源码相同。所以问题不在「谁实现得不一样」，而在「那个调用到底有没有发生、传了什么」。

## 新探针（**两个版本都加了，直接可比**）

在混入的 `cgc$playAnimation` 里每 60 帧探一次 loop 层动画的插值状态：

```
[cgcanim-debug] probe: tickDelta=0.42 tick=37 active=true
```

- **1.20.1 的 `tickDelta` 在 0..1 间连续变化、1.21.1 恒定（尤其恒为 0）** → 插值确实没生效，病因锁定在 `setTickDelta` 这一环：playeranimator 的 `applyBodyTransforms` 注入点在当前渲染路径下没跑到或被重排，我下一步去 CGC 侧的 mixin 配置和渲染链上找是谁挡的。
- **两个版本都连续变化** → 插值是好的，那 20 Hz 另有来源，我再看 `tick` 的增长速率与 `active`。

两个版本都编译通过（EXIT=0），1.20.1 现在只有「幂等注册 + 探针」，1.21.1 是「幂等注册 + AnimationCodecs + 探针」。跑一次把两个日志里的 `probe:` 片段给我即可。未提交。

# 

```
两个日志都好了，再继续排查
```

# 

Both show `tickDelta` varying smoothly in 0..1 — so interpolation is fine on 1.21.1 too. That kills the tickDelta hypothesis. Let me compare the two environments instead.

Both instances use shader mods (1.20.1 → Oculus, 1.21.1 → Iris). Let me check for shader-pipeline activity in both.

两个日志的探针都读到了，结果**推翻了我上一个假说**，但把范围收到了一处结构性差异上。

## 探针结果：插值是好的，两个版本都正常

```
1.20.1: tickDelta=0.92 tick=0 / 0.54 tick=9 / 0.94 tick=6 / 0.74 tick=3 / 0.58 tick=0
1.21.1: tickDelta=0.50 tick=9 / 0.96 tick=0 / 0.84 tick=6 / 0.76 tick=0 / 0.88 tick=5 / 0.18 tick=1
```

`tickDelta` 两边都在 0..1 之间连续变化，`active=true`，`tick` 是正常的循环推进。而且 1.21.1 的探针间隔还略短（~0.87s/60 帧 ≈ 69fps，1.20.1 约 58fps）——**实际帧率没问题，`setupAnim(partialTicks)` 也在正常喂值**。所以「插值没生效」这条也排除了。

## 至此，我们代码里所有可测的量都是健康的

| 项 | 结果 |
|---|---|
| 数据加载 | 4 文件 / 84 动画 ✓ |
| 层是否被驱动 | 是，且守卫成立（全会话仅 1 次重建）✓ |
| 逐帧插值 tickDelta | 0..1 连续变化 ✓ |
| 关掉 `AdjustmentYRotModifier` | 症状不变 ✗（不是它） |
| SSR | 无关 ✗ |
| addon 代码 vs TaCZ | 逐字节相同（仅 codec 一处） |

## 剩下的唯一结构性差异：姿态是在哪一步写进去的

这是目前唯一还没被证伪、且能解释全部现象的点：

- **关掉 addon（"正常"那一次）**：CGC 的 `DefaultShooterAnimator` 在 **`setupAnim`（TAIL 注入）里直接写 `ModelPart`**。
- **打开 addon（抽搐那一次）**：playeranimator 通过自己的 `BipedEntityModelMixin.renderToBuffer(...)` 在 **`renderToBuffer`（`setupAnim` 之后、真正出顶点时）** 改 `ModelPart`。

我核过 jar：`BipedEntityModelMixin` 确实是在 `renderToBuffer` 里应用动画的，`PlayerRendererMixin.applyBodyTransforms` 只负责给 `AnimationProcessor` 喂 tickDelta。**也就是说"正常"和"抽搐"的区别是"姿态在 setupAnim 写"还是"在 renderToBuffer 写"。**

在 Iris/Sodium（以及 1.20.1 那套 Oculus）的**延迟/批处理实体渲染**下，`renderToBuffer` 阶段写进去的姿态有可能被跨帧采样，表现就是抖/抽；而在 `setupAnim` 里写的会随批处理正确捕获。这条也正好解释了为什么"关 addon 就正常"。

这个项目里有过同类先例——之前 1.21.1 的"Sodium 红点 lens"最后查到触发者是 ModernFix 而不是 CGC。

## 需要你配合的两件事

1. **把 shader 相关临时摘掉再测一次**（Iris + Sodium，或先把光影关掉）：抽搐若消失，就锁定为「playeranimator 在 renderToBuffer 写姿态」×「光影的批处理实体渲染」的交互，修复方向不是在移植代码里，而是在渲染层（CGC 侧把动画改成在 `setupAnim` 阶段应用，或与 Iris 的批次对齐），也可以去 Iris 那边确认。
2. **TaCZ-1.21.1 那次"没问题"是在哪个实例测的？** 是不是就是这个 57 个 mod 的包（Iris/Sodium/ModernFix/PlayerRevive/CameraOverhaul/ItemPhysic 都在）？如果不是同一个包，前面的对照就不成立，那这台机器上多出来的这些渲染类 mod 就是首要嫌疑。

诊断探针还留在两个分支里（`_debugProbeTickDelta` / `_debugLogLoopReplace` / `prepare` 的 loaded 日志），确认完我一次性删掉。1.20.1 目前是「幂等注册 + 探针」，1.21.1 是「幂等注册 + AnimationCodecs + 探针」，都未提交。

# 

```
1.21.1tacz装了光影后仍然没问题
1.21.1CGC去了irish和sodium后仍然有问题，日志更新
```

# 

The animation state is provably healthy on 1.21.1 even without Iris/Sodium. Let me check whether anything *else* in CGC writes the model parts after playeranimator applies them.

Only one caller. Let me compare CGC's model/render mixins between the two branches — that's the last unexamined surface.

CGC's mixin *file set* is identical — let me diff their contents, especially the model/renderer ones.

CGC's mixins are effectively identical. Let me check the instances for another mod that might also register playeranimator layers.

Let me see which mods mix into the same classes on 1.21.1.

ModernFix has an `entity_pose_stack` `PlayerRendererMixin` — and this project has a precedent of ModernFix causing a fake CGC visual bug. Let me check which of these mods exist in the 1.20.1 instance.

**Strong lead.** ModernFix is present only in the 1.21.1 instance — and this project already has a precedent of a "CGC visual bug" turning out to be ModernFix. Let me confirm the details.

**找到了一个非常强的嫌疑，而且这个项目里有过一模一样的先例。**

## 关键差异：ModernFix 只存在于 1.21.1 实例

```
modernfix:  1.20.1 = 0 次    1.21.1 = 271 次
```
（`journeymap` / `itemphysic` / `creativecore` / `passablefoliage` / `cameraoverhaul` 同样只在 1.21.1 有，但 ModernFix 是唯一直接改玩家渲染姿态的。）

而 ModernFix 的 mixin 恰好全部落在我们这条链上：

```
bugfix.entity_pose_stack.PoseStackAccessor          → com.mojang.blaze3d.vertex.PoseStack
bugfix.entity_pose_stack.LivingEntityRendererMixin  → net.minecraft.client.renderer.entity.LivingEntityRenderer
bugfix.entity_pose_stack.PlayerRendererMixin        → net.minecraft.client.renderer.entity.player.PlayerRenderer
```

配置在 `E:\Software\Minecraft\neoforge1.21.1\config\modernfix-common.toml`。

## 为什么这条能解释**全部**观测

playeranimator 应用姿态的方式是 **在 `PoseStack` 上做变换**（`PlayerRendererMixin.applyBodyTransforms`）+ 在 `renderToBuffer` 里改 `ModelPart`——**正是 ModernFix `entity_pose_stack` 那个补丁重写的代码**。而：

| 观测 | 解释 |
|---|---|
| 关 addon 就正常 | 那时姿态由 CGC 的 `DefaultShooterAnimator` 在 `setupAnim` 里直接写 `ModelPart`，**完全不碰 PoseStack**，ModernFix 的补丁影响不到 ✓ |
| 1.20.1 不抽 | 那个实例**没有 ModernFix** ✓ |
| 数据/守卫/tickDelta 全部健康 | 状态是好的，坏在姿态应用这一层 —— 与「PoseStack 被改写」完全吻合 ✓ |
| 去掉 Iris/Sodium 仍抽 | ModernFix 的补丁与光影无关 ✓ |

而且**这个项目里已经有过同一个先例**：1.21.1 那个"Sodium 红点 lens"问题最后查到的触发者是 **ModernFix 而不是 CGC**（当时记录在项目记忆里）。

## 请做这个测试（最省事）

**把 ModernFix 从 1.21.1 实例里摘掉**（或先只关掉它的 `entity_pose_stack` bugfix），再测一次 addon：

- **不抽了** → 定位完成，这是 ModernFix 与 playeranimator 在 PoseStack 上的冲突，不是移植缺陷。后续要么在 ModernFix 侧关补丁，要么在 CGC 侧避开（让动画不依赖 `applyBodyTransforms` 的 PoseStack 路径），我可以往下做。
- **还抽** → ModernFix 也排除，我接着查 `journeymap` / `shouldersurfing` 那两组也混进 `ModelPart` 的 mixin。

**另外一个必须确认的点**：**TaCZ-1.21.1 那次"没问题"是在哪个实例测的？** 如果它是在 TaCZ 自己的开发实例（多半没有 ModernFix / 这一百来个 mod），那前面的对照就不成立——这也正好解释了"TaCZ 装了光影也没事"。如果确实是在这个 57 mod 的包里测的，那 ModernFix 这条就要打问号，请告诉我，我换个方向。

诊断探针还在两个分支里（`_debugProbeTickDelta`、`_debugLogLoopReplace`、`prepare` 的 loaded 日志），确认完我一次性删干净。当前改动未提交。
