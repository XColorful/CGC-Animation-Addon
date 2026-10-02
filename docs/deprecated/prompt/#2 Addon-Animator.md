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

# 

```
现在接着CGC Animation Addon的#2 Addon-Animator.md，还是没解决，并且只装了必要mod，日志更新在D:\Temp-File\debug-1.21.1.log
继续排查
```

# 

I'll start by locating the issue doc and reading the fresh log.

Now let me analyze the fresh log.

Let me look at the actual addon code and its 1.20.1 counterpart.

Let me compare CGC's render-side hookup between the two branches.

Let me inspect the actual playeranimator 2.0.1 implementation.

Let me inspect the 1.21.1 vanilla `PlayerModel.setupAnim` to locate playeranimator's injection point.

Let me systematically diff the addon against TaCZ's original and against its own 1.20.1 branch.

I've been reading code; let me now read the log in full — it's small enough.

Let me check whether I can actually run the client here to observe it directly.

Found a promising lead — both playeranimator and CGC mixin into `ItemInHandLayer`. Let me compare.

Let me search for known issues with this combination.

Let me compare playeranimator 1.0.2 (1.20.1) against 2.0.1 (1.21.1) — the model apply path specifically.

Let me check the actual released CGC jar the addon compiles against — that's what the user runs.

Promising lead — let me check whether CGC calls `renderHand` (which trips playeranimator's first-person flag) during third-person rendering.

Let me check CGC's third-person gun renderer for `renderHand` — that would trip playeranimator's first-person flag.

Let me check whether the client player's tick actually reaches `Player.tick()` — that's what drives the animation clock.

I've now verified the port is faithful and the state is healthy; the remaining unknown is whether the applied pose is smooth per frame. Let me add a decisive probe.

Adding two more cheap probes to make this round conclusive: explicit loop-replace logging and `stopAllAnimation` frequency.

我把这轮能静态验证的都验证完了，结论是**没找到根因**，但把范围压到了一个之前从未被测量过的点上，并为此加好了探针、编译并打出 jar。下面按事实—核对—下一步说。

## 一、新日志（极简实例）确立的事实

| 观测 | 结论 |
|---|---|
| 有 mixin 的 mod 只有 `customgun` / `cgccpackloader` / `playeranimator` / `cgcanimation` | ModernFix、Iris、Sodium **确实都不在了** → 上一轮的 ModernFix 假说被实测否掉 |
| 全程只有 1 次 `Reloading ResourceManager` | 幂等注册守卫确实与本病无关 |
| `player_animator loaded: 4 file(s), 84 animation(s)` | 资源装配正常 |
| `loop replace` 全会话只打 1 条、`probe` 全部 `active=true` | 各层稳定、未被每帧重建 |
| **无 bendy-lib** | playeranimator 的「上下身分两段渲染」路径（`Helper.isBendEnabled()`）根本没走，可以排除 |

另外我把 probe 的 `tick` 序列重算了一遍（8,0,12,0,8,1,7,25,5,10,3,9,1,2,6）。按每次采样间隔 0.6–1.5s 反推，**它和「≈12 tick 的循环动画 + 20Hz 推进」完全自洽**——即动画时钟也是正常的，我上一轮对它的怀疑可以撤回。

## 二、这轮核对的代码（都没发现差异）

- **反编译了你实际在跑的发布 jar** `custom-gun-continued-1653554-8953760`：`ShooterAnimationManager.setRotationAnglesHead` 里确实有 `playAnimation(...) == true → return`，不会和 CGC 自家 animator 双写。
- addon ↔ TaCZ-1.21.1 的 playeranimator 包逐方法比对，仍然只差 `AnimationCodecs` 一处。
- **关键补充**：1.20.1 用的 `playeranimator 1.0.2-rc1+1.20` 和 1.21.1 编译用的 `2.0.4+1.21.1`，在「把姿态写进模型」这条路径上**结构完全相同**——`setEmote` 都注入在 `PlayerModel#setupAnim` 的第一个 `copyFrom`；两个 MC 版本的 `PlayerModel#setupAnim` 都是 `super.setupAnim()` + 5 个 `copyFrom`，顺序一致；`HumanoidModel#setupAnim` 结尾都是 `hat.copyFrom(head)`。**写入时机、渲染顺序、层优先级（93–96）三方都没差异。**

## 三、剩下唯一没被测量过的环节

之前所有探针读的都是**动画自己的数据**（tickDelta / tick / extraData name）——它们只能证明**输入**平滑，证明不了**输出**平滑。中间还隔着 `AnimationStack.isActive()` → `setEmote` → 淡化链 → `AbstractFadeModifier`。

所以这轮加的是直接读**最终 ModelPart** 的探针（已编译、已验证进 jar）：

| 探针 | 位置 | 读法 |
|---|---|---|
| `pose #N limb= armX= legX= legY= headX=` | `PlayerModel#setupAnim` 的 TAIL（playeranimator 写完姿态之后、这帧真正画出来之前），每 300 帧连打 12 帧 | 12 帧的值**连续变化** ⇒ 姿态本身平滑，问题在渲染侧；**armX 在动画值与≈0 的僵直值之间来回跳** ⇒ `setEmote` 被隔帧跳过；**值长时间不动** ⇒ 时钟没走 |
| `loop replace #N` 改为**前 60 次全打** | `AnimationManager` | 之前 `%120==1` 只能告诉你「少于 121 次」，现在能看出是每秒 1 次还是每秒 30 次 |
| `stopAllAnimation #N` 前 60 次全打 | `PlayerAnimatorCompatMixin` | CGC 若每帧都在停层，同样会抽搐，这条以前从没测过 |

改动文件（都在 CGC-Animation-Addon-1.21.xneoforge）：
- [PlayerPoseDiagMixin.java](core/src/main/java/dev/xcolorful/cgcanimation/client/mixin/diag/PlayerPoseDiagMixin.java)（新增）
- [cgcanimationclient.mixins.json](core/src/main/resources/cgcanimationclient.mixins.json)
- [AnimationManager.java](core/src/main/java/dev/xcolorful/cgcanimation/client/animation/shooter/animator/legacy/AnimationManager.java)
- [PlayerAnimatorCompatMixin.java](core/src/main/java/dev/xcolorful/cgcanimation/client/mixin/playeranimator/PlayerAnimatorCompatMixin.java)

`:core:compileJava` + `:neoforge-compat:compileJava` + `:neoforge-compat:jar` 全部 EXIT=0，jar 在 `neoforge-compat/build/libs/cgcanimation-0.0.0-neoforge-1.21.x.jar`（已验证 `diag.PlayerPoseDiagMixin` 与配置都在包里）。改动未提交。

```
cd D:/Github/CGC-Animation-Addon-1.21.xneoforge && ./gradlew :neoforge-compat:jar
```

## 四、顺便必须问清的一点

**TaCZ-1.21.1 那次「不抽搐」时，你确认看到的确实是枪包里的 `player_animator` 动画吗？** 判据是腿在走路时播 `*_lower` 的半蹲/走姿、而不是原版摆腿。如果那边 `hasPlayerAnimator3rd` 其实是 false，你看到的是 TaCZ 自己的第三人称动画，那「TaCZ 不抽 ⇒ 病不在移植代码里」这条推理就不成立，前面的排除要重来。

跑一次把 `pose #` / `loop replace #` / `stopAllAnimation #` 三段给我即可，诊断代码确认完我一次性删干净。

# 

```
1.21.1的日志更新了，CGC用的是当前CGC1.21.1最新编译的，不是传CurseForge的
1.21.1tacz用的是player animator的动画
```

# 

The diag mixin failed to apply (`@Shadow` can't reach inherited fields, no refmap) — but the log now has 10 loop-replace lines. Let me read them first.

The diag mixin failed to apply (`@Shadow` can't reach fields declared in `HumanoidModel`, and NeoForge loads no refmap). The loop-replace data is clean, though — let me fix the probe.

新日志读完了 —— 三条结论：一条彻底排除、一条彻底排除、探针本身挂了（已修）。

## 1. 对照组现在成立

你确认 TaCZ-1.21.1 走的确实是枪包里的 player animator 动画 ⇒ 「同一份移植代码在 TaCZ 上不抽」成立 ⇒ **病不在移植代码里**，而在 CGC 侧或两边装配方式的差异。这条我记下了。

## 2. 这次日志新增的证据

**`loop replace` 前 10 次全打出来了，全部对应真实状态切换，没有一次是每帧重建：**

| # | 时间 | 层 | 切换 |
|---|---|---|---|
| 1,2 | 19.766 / 19.782 | lower / loop_upper | `<inactive>` → `hold_*`（进世界首次） |
| 3,4 | 20.170 / 20.171 | lower / loop_upper | `hold_*` → `run_*` |
| 5 | 21.705 | loop_upper | `run_upper` → `aim_upper` |
| 6 | 21.728 | lower | `run_lower` → `walk_lower` |
| 7 | 22.458 | lower | `walk_lower` → `run_lower` |
| 8 | 22.613 | loop_upper | `aim_upper` → `run_upper` |
| 9,10 | 24.828 / 24.829 | lower / loop_upper | `run_*` → `walk_*` |

5 秒 10 次（≈2/s），全是走/跑/瞄准的真实切换 ⇒ **「守卫失效导致每帧重建」彻底排除。**

**`stopAllAnimation` 全程一次都没调用**（前 60 次计数为 0）⇒ **「CGC 每帧停层」也排除。**

**姿态探针没生效**，日志里明确报错：

```
Mixin apply for mod cgcanimation failed ... diag.PlayerPoseDiagMixin -> net.minecraft.client.model.PlayerModel:
@Shadow field head was not located in the target class net.minecraft.client.model.PlayerModel. No refMap loaded.
```

`head`/`rightArm`/`leftLeg` 声明在 `HumanoidModel`，不在 `PlayerModel`；NeoForge 不加载 refmap，`@Shadow` 只在目标类里找。**已改成不 Shadow、直接 `(PlayerModel) (Object) this` 取模型实例**（这也解释了为什么 CGC 自己的 `PlayerModelMixin` 能 Shadow `leftSleeve` —— 那两个字段确实声明在 `PlayerModel` 里）。混入失败只记 WARN、游戏照常跑，`PlayerAnimatorCompatMixin` 的 4 个注入点不受影响。

## 3. 一个下次值得留意的观察

每次 `loop replace` 都是 `new KeyframeAnimationPlayer(...)` ⇒ **动画从 tick 0 重头开始，只靠 8 tick 交叉淡化**。这次 5 秒里你做了 run↔walk、aim↔run 共 4 次切换，腿的循环每次都被打回起点。

- 若搐动是「隔一小会儿腿猛地弹一下」，那就是这个：状态在阈值边缘来回翻（`limbSwingAmount > 0.05` 与 `isSprinting()` 两个阈值），每次都重打回起点。
- **判据：下次看 `loop replace` 是不是成串出现**（一秒内 5 条以上）。成串 ⇒ 状态抖动，不是动画本身的问题。

## 4. 新 jar（探针已修好）

```
cd D:/Github/CGC-Animation-Addon-1.21.xneoforge && ./gradlew :neoforge-compat:jar
```

产物：`neoforge-compat/build/libs/cgcanimation-0.0.0-neoforge-1.21.x.jar`（`:core:compileJava` / `:neoforge-compat:jar` 均 EXIT=0，已确认 jar 内配置含 `diag.PlayerPoseDiagMixin`）。

**跑的时候请连续走一段路、再 ADS 一下**（别站着不动），然后给我 `pose #` 段落。每 150 帧连打 20 帧，格式：

```
pose #N limb=<摆幅> sprint=<bool> pose=<枚举> armX= legX= legY= headX=
```

读法：

| 看到什么 | 结论 |
|---|---|
| 相邻帧的 `legX`/`armX` 连续变化 | 最终姿态是平滑的 ⇒ 病在渲染侧，我转去查 CGC 的第三人称渲染链 |
| `armX` 在 ≈-1.5（动画值）与 ≈0（僵直值）之间来回跳 | `setEmote` 被隔帧跳过 ⇒ 锁定 `PlayerModel` 的 `firstPersonNext` 分支 |
| 值长时间不动、再突然跳一大格 | 动画时钟没走 / 循环在重打回起点 |
| `loop replace` 成串刷 | 状态阈值抖动，不是动画问题 |

改动仍全部未提交（addon 1.21.1 分支：新增 `PlayerPoseDiagMixin` + 混入配置，`AnimationManager` 的 replace 计数改为前 60 条全打，`PlayerAnimatorCompatMixin` 加 `stopAllAnimation` 计数），诊断代码确认完一次性删干净。

# 

```
1.21.1日志更新，这个抽搐不是在切换的时候有，是播放动画的时候就一直有抽搐
```

# 

The pose data is decisive — let me look at it closely and compare against the animation's own keyframes.

The pose data is the breakthrough — let me look at it against the actual keyframes.

The pose data is out of range for any leg animation — that's decisive. Let me add a probe that reads the animation's raw value and the playeranimator active flag at the same instant.

这轮拿到了**决定性数据**：写进模型的腿角度超出了该动画本身能产生的范围。

## 1. 实测值 vs 动画键值

```
walk_lower  left_leg.x 键值范围 : -0.4099 … +0.7413 rad   (14 tick 循环)
run_lower   left_leg.x 键值范围 : -0.4884 … +0.9159 rad   (12 tick 循环)
实测 leftLeg.xRot              : -0.662  … +1.243  rad
```

**两边都对不上。** 而且 `+1.243` 正好等于原版摆腿幅度 `1.4 × limbSwingAmount`（该段 `limb=0.881` → 1.233），`hold_lower` 更是只有 −0.087 的常量。

## 2. 更关键的：分段形状

拿 burst 600–619（sprint=false，走路）逐帧看：

```
600 -0.2536
601  0.10891634   ← 正好等于 walk_lower 第 3 tick 的键值 0.1089（动画生效）
602  0.17117172   ┐
603  0.21959257   │ 斜率 +0.05/帧，与 walk_lower 上升段一致
604  0.26109618   │
605  0.3441034    ┘
606 -0.63814795   ┐
607 -0.5966444    │ 同一斜率 +0.05，但整段被平移出动画范围
608 -0.5482236    │
609 -0.49288547   │
610 -0.44100595   ┘
611  0.7507789    ← 又回到动画峰值附近（walk_lower 第 7 tick = 0.7413）
612  0.832988     ┐ 再次整体上移，超出动画上限
613  0.8709307    │
614  0.91519696   │
615  0.96894896   │
616  1.0163773    ┘
617 -0.20094845
618 -0.14403445
619 -0.08712058
```

**每段内部的斜率（+0.05 rad/帧）就是动画自己的斜率**（walk_lower 上升段 1.151 rad / 7 tick，@75fps ≈ 0.0439 rad/帧），**但整段每隔 4–6 帧被整体挪位一次**。

⇒ 这不是「原版动画漏出来」（原版坡度是 ±1.23 的余弦，斜率约 0.22/帧，对不上），而是**同一个动画信号被周期性错位** —— 最可能是动画的 `currentTick` 在跳。这正好就是"一直抽"。

## 3. 已排除的（这轮日志）

- `loop replace` 全程 **11 次**，逐条都是真实状态切换（hold→run、run→aim、aim→run、run→walk…）⇒ 不是每帧重建。
- `stopAllAnimation` **0 次** ⇒ 不是 CGC 在停层。
- `limb` / `sprint` / `pose` 在各段内不变 ⇒ 不是状态抖动导致的换动画。

## 4. 新探针（已编译，jar 已重打）

同一行里同时打出**动画原始值**和**模型最终值**，一次跑就能把原因劈开：

```
pose #N limb=… sprint=… armX=… legX=… legY=… | active=… rawLegX=… tick=… td=… playing=…
```

| 现象 | 结论 |
|---|---|
| `rawLegX` 也在跳 | 动画求值层的问题 ⇒ 我去查 `currentTick` / `returnToTick` 那条链 |
| `rawLegX` 平滑、`legX` 跳 | 写入被打断（`setEmote` 被跳过）⇒ 看 `active=` |
| `active=false` 的帧 | `AnimationStack` 失活导致整帧不写动画 |
| `tick` 在 burst 内非单调 | 动画时钟被反复重置 — 直接对应上面的错位 |

产物路径不变：

```
cd D:/Github/CGC-Animation-Addon-1.21.xneoforge && ./gradlew :neoforge-compat:jar
```

`neoforge-compat/build/libs/cgcanimation-0.0.0-neoforge-1.21.x.jar`（EXIT=0）。跑的时候**一直走一段路**（别停），给我几段 `pose #` 就行。改动仍未提交。

# 

```
1.21.1日志更新，继续排查
```

# 

`rawLegX == legX` exactly, `active=true` every frame, `tick` monotonic, `td` smooth — yet the animation's own value function jumps out of its keyframe range. That points at the parsed animation data. Let me compare with how TaCZ-1.21.1 parses the same files.

Let me get the parsed keyframe data straight from the running game — that will show whether the data or the evaluation is wrong.

Let me dump the parsed keyframe data straight from the game — that separates bad data from bad evaluation.

Let me check the other gun packs' leg animations — the display might reference a different one.

这轮探针把范围收得非常干净了。

## 1. 已经证明「不是写入被打断」

```
#150 tick=3 td=0.04  legX=1.1256807   rawLegX=1.12568
#151 tick=3 td=0.32  legX=1.1821676   rawLegX=1.18217
#155 tick=4 td=0.24  legX=-0.24614823 rawLegX=-0.24615
#162 tick=6 td=0.04  legX=0.48866972  rawLegX=0.48867
```

- `rawLegX` **逐帧与 `legX` 完全相等** ⇒ `setEmote` 每帧都忠实写入，我上一轮"写入被打断"的猜测**推翻**。
- `active=true` **全程为真** ⇒ `AnimationStack` 没有失活。
- `tick` 单调推进（3,3,3,3,3,4,4,4,4,5,5,5,6…），`td` 每 tick 内 0.04→1.00 平滑走完 ⇒ **动画时钟完全正常**。
- `hold_lower`（常量）**求值完全正确**：全程 `-0.08718391`，正好是 JSON 里的 `-4.99527°` 换算结果。

## 2. 真正的问题：动画自己返回的值跑出了键值范围

| | 键值范围（从 JSON 算出） | 实测 `rawLegX` |
|---|---|---|
| `run_lower` sprint=true | −0.4884 … **+0.9159** | 到 **+1.31935** |
| `walk_lower` 走路 | −0.4099 … **+0.7413** | 到 **+1.243** |

而且是在**同一 tick 内**跨出去的：

```
#153 tick=3 td=0.76  rawLegX= 1.27093
#154 tick=3 td=1.00  rawLegX= 1.31935
#155 tick=4 td=0.24  rawLegX=-0.24615    ← 0.24 tick 内跳 -1.565
#165 tick=6 td=0.82  rawLegX= 0.32533
#166 tick=7 td=0.10  rawLegX= 1.10435    ← 跳 +0.779
```

**但同一段里的斜率是对的**：`+0.2018/tick` 正是 `run_lower` 第 2→6 tick 段的斜率，`−0.2094/tick` 正是第 6→8 tick 段——**说明选段没错，整段被平移了**。

我还把三个枪包（rifle / pistol / minigun）的 `walk_lower`/`run_lower` 都算了一遍，没有任何一个能到 ±1.32 ⇒ **不是"播了别的动画"**。

⇒ 结论：**`KeyframeAnimationPlayer` 在手头这份数据上求值越界**。而 TaCZ-1.21.1 用的是逐字节相同的解析与求值代码。所以要么这份 `KeyframeAnimation` 的数据和 TaCZ 拿到的不一样，要么求值环节有我没看到的东西。

## 3. 新探针：直接把解析结果打出来

每个动画只打一次：

```
ANIM run_lower begin= end= stop= ret= inf= easeBefore=
  pitch (tick,value,ease)(tick,value,ease)… enabled=true
  yaw   …   enabled=
  roll  …   enabled=
```

| 打出来的键帧 | 结论 |
|---|---|
| 就是 JSON 的 `0/2/6/8/10/12` 与对应弧度值 | 数据没问题 ⇒ 求值环节，我去查 `getValueFromKeyframes` 的 `tickBefore/tickAfter/returnToTick` 分支 |
| tick 或值对不上（例如 tick 变成 0/1/3/4/5/6 = 秒数没乘 20） | 解析/编解码环节，我改 `PlayerAnimationManager` 的解析路径 |
| `ret=` / `inf=` / `easeBefore=` 异常 | 同上 |

顺带 `hold_lower` 也会打出来做对照（它的值已经证明是对的）。

```
cd D:/Github/CGC-Animation-Addon-1.21.xneoforge && ./gradlew :neoforge-compat:jar
```

产物：`neoforge-compat/build/libs/cgcanimation-0.0.0-neoforge-1.21.x.jar`（EXIT=0）。跑的时候**站着别动几秒、再走一段、再冲刺一段**，这样 `hold_lower`/`walk_lower`/`run_lower` 三份数据都会被 dump 出来。改动仍未提交。

# 

```
日志更新，继续排查
```

# 

**Found the data.** The parsed keyframes are exactly right — but every keyframe's ease is `CATMULLROM`, while the JSON says `INOUTSINE`. Let me verify whether that reproduces the observed values.

**Root cause found.** Let me confirm by comparing `Easing.catmullRom` between the 1.20.1 and 1.21.1 playeranimator versions.

**Root cause confirmed by arithmetic.** Let me verify the exact match and find which parser produced `CATMULLROM`.

**Simulation reproduces the observed values exactly.** Let me pin down which parser assigns `CATMULLROM`.

**Everything is explained.** Let me confirm the last link — 1.0.2's `Ease` has no `CATMULLROM`, so the old pack's `catmullrom` degrades to LINEAR there.

找到了，**根因已用数值逐位对上**。

## 根因：playeranimator 2.0.x 的 `Easing.catmullRom` 是坏的

`dev.kosmx.playerAnim.core.util.Easing`（2.0.x）：

```java
public static float catmullRom(float n) {
    return (0.5f * (2.0f*(n + 1) + ((n + 2) - n) * 1
            + (2.0f*n - 5.0f*(n + 1) + 4.0f*(n + 2) - (n + 3)) * 1
            + (3.0f*(n + 1) - n - 3.0f*(n + 2) + (n + 3)) * 1));
}
```

把后面三项展开，系数全部抵消，**整个函数等于 `n + 2`**。而 `Ease.CATMULLROM = easeInOut(Easing::catmullRom)`，于是：

| f | ease(f) |
|---|---|
| 0.4999 | **+1.4999** |
| 0.5000 | **−0.5000** |

⇒ **在每段键帧的正中间有一个 2.0 的跳变。** `getValueFromKeyframes` 做的是 `lerp(ease(f), before, after)`，所以**每段插值参数被整体 ±1 平移，并在段中点弹跳 `2×(after−before)`**。

拿 `run_lower` 的 left_leg 第 2→6 tick 段验算（before=0.10892, after=0.91587, Δ=0.80696）：

```
f 略小于 0.5 → ease=1.5  → 0.10892 + 1.5*0.80696  = +1.31936   实测 +1.31935 ✔
f = 0.56     → ease=-0.44 → 0.10892 - 0.44*0.80696 = -0.24614   实测 -0.24615 ✔
```

我把整条 `getValueAtCurrentTick`+`getValueFromKeyframes` 用 node 复刻后跑你日志里的 12 个点，**10 个点小数点后 5 位完全一致**（另外 2 个是我模拟在整数 tick 边界选段的偏差）。`hold_lower` 之所以看起来正常，是因为它段内前后值相等，`2×(after−before)=0`，平移被抵消。

## 为什么只有 1.21.1 中招

| | playeranimator | `Ease` 里有没有 `CATMULLROM` | 老枪包的 `"lerp_mode": "catmullrom"` 解析成 |
|---|---|---|---|
| 1.20.1 addon | **1.0.2** | **没有**（javap 确认） | `valueOf` 抛异常 → 兜底 **`LINEAR`** → 平滑 |
| 1.21.1 addon | **2.0.4** | 有 | `Ease.CATMULLROM` → **上面那个坏函数** → 抽搐 |

## 为什么 TaCZ-1.21.1 是好的 —— 它把包改了

```
D:/Github/TACZ/…/rifle_default.player_animation.json        →  1140 处 "lerp_mode": "catmullrom"
D:/Github/TACZ-1.21.1/…/rifle_default.player_animation.json →  1140 处 "lerp_mode": "INOUTSINE"
```

TaCZ-1.21.1 自己的枪包已经被改写成 `INOUTSINE`，所以你那次对照测不到这个 bug。而你的 CGC 实例走 `cgccpackloader` 加载的是**老包**（`catmullrom`），撞上了 2.0.x 的坏实现。日志里那份 dump 全被解析成 `CATMULLROM`，就是铁证。

## 修法（需要你定一个）

| 方案 | 做法 | 影响面 |
|---|---|---|
| **A 换包** | 把枪包的 `catmullrom` 改成 `inoutsine`（= TaCZ-1.21.1 的做法） | 零代码，但**所有**老枪包/第三方包仍会中招 |
| **B addon 侧数据归一** | 在 `PlayerAnimationManager` 里把 `lerp_mode` 解析成 `CATMULLROM` 的键帧改写成 `linear` 再交给 `AnimationCodecs` | 只动 addon，覆盖所有老包；等价于 1.20.1 的实际行为 |
| **C addon 侧 mixin 修库** | `@Inject` 到 `Easing.catmullRom` 让它返回 `n`（等价 LINEAR） | 一行，但会改掉**整个实例里所有 mod** 的行为 |
| **D 上游** | 报给 playeranimator（`Easing.catmullRom` 实现本身就是错的） | 正解，但周期长 |

我倾向 **B**：可解释、可移除、只影响本 addon，且与 1.20.1 的表现完全一致（1.20.1 实际就是退化成 LINEAR）。

**在动手前建议先做一次一分钟、零编译的验证**：把你 CGC 实例里 `tacz_default_gun` 的 `rifle_default.player_animation.json` 的 `"catmullrom"` 全替换成 `"inoutsine"`，进游戏看抽搐是否消失。消失了就 100% 坐实，我再按你选的方案改。

诊断代码还在（`PlayerPoseDiagMixin` + `AnimationManager`/`PlayerAnimatorCompatMixin` 计数），确认完一次性删干净。改动仍未提交。

# 

```
确实生效，按B修
并且git查一下1.21.1TaCZ当时的commit描述
```

# 

Root cause confirmed. Let me first check that TaCZ commit, then implement B.

That's the commit: **`0c884f33` "fix \`playeranimator\` compat, I guess"** (MUKSC, 2025-01-16) — a pure `"lerp_mode": "catmullrom"` → `"INOUTSINE"` rewrite, 1140 lines in rifle + 1124 in pistol, no other change. Let me now check my working tree and implement B.

Now implementing fix B and stripping all diagnostics.

Fix compiles. Let me validate the transform against the real (old) gun pack offline.

已按 B 修好，诊断代码全部移除。

## 你要的那条 commit

| | |
|---|---|
| 仓库 | `XColorful/TACZ-1.21.1`（1.21.1 非官方移植） |
| commit | `0c884f335ac2795db25068a221ef59a8eff7fcd0` |
| 作者 | MUKSC `<68376185+MUKSC@users.noreply.github.com>` |
| 日期 | Thu Jan 16 19:36:22 2025 +0900 |
| 描述 | `fix \`playeranimator\` compat, I guess`（正文为空，就这一行） |
| 改动 | 只有 `pistol_default.player_animation.json` 和 `rifle_default.player_animation.json`，2264 行 +/− 完全 1:1 —— 就是把 1140（rifle）+ 1124（pistol）处 `"lerp_mode": "catmullrom"` 改成 `"lerp_mode": "INOUTSINE"`，没有任何别的改动 |

也就是他当年也是靠**改包**绕开的，跟我们实测出来的机制完全对上。

## 改动（1.21.1 addon，未提交）

只动一个文件：[PlayerAnimationManager.java](core/src/main/java/dev/xcolorful/cgcanimation/client/resources/assets/PlayerAnimationManager.java)

- `prepare()` 里先把文件读成 `byte[]`，过一遍 `fixBrokenEase(...)` 再交给 `AnimationCodecs`（用 `ByteArrayInputStream` 供货，两个 json codec 各拿一份新流）。
- 新增 `fixBrokenEase / fixAnimations / fixKeyFrame`：走 `animations → <动画> → bones → <骨骼> → <通道> → <时间> → 键帧`，把 `lerp_mode`/`easing` 里大小写不敏感等于 `catmullrom` 的值改写成 `linear`；**没有命中就直接返回原字节**，不动其它文件，也不重新序列化。
- 归一成 `linear` 是为了和 **1.20.1 分支的实际行为**一致（1.0.2 的 `Ease` 没这个枚举值，`easeFromString` 抛异常兜底成 `LINEAR`）。如果你要的是和 **TaCZ-1.21.1 视觉一致**，把 `FALLBACK_EASE` 改成 `"inoutsine"` 即可，就这一个常量。

**移除的诊断**（全部还原到 HEAD）：`diag.PlayerPoseDiagMixin` 及其包、`cgcanimationclient.mixins.json` 的注册、`AnimationManager` 的 `_debugLogLoopReplace`/`_debugProbeTickDelta`、`PlayerAnimatorCompatMixin` 的 stop 计数与探针调用。`git status` 现在只剩上面那一个文件（它还带着上一轮那个 `AnimationSerializing → AnimationCodecs` 的对齐改动，两者是一套的）。

## 验证

- `:core:compileJava` + `:neoforge-compat:compileJava` + `:neoforge-compat:jar` 全 EXIT=0，产物 `neoforge-compat/build/libs/cgcanimation-0.0.0-neoforge-1.21.x.jar`。
- 把 `fixBrokenEase` 的遍历逻辑用脚本跑在真包上：`rifle_default` 命中 **1140** 处、`pistol_default` **1124**、`minigun` **355**，且这三个包里 ease 值**只有** `catmullrom` 一种；TaCZ-1.21.1 那份（`INOUTSINE`）一处都不会被改。数字和 `0c884f33` 的 diff 完全吻合。

**没动 1.20.1 分支**：那边 catmullrom 本来就被兜底成 LINEAR，加这段是纯冗余。如果你想让两边代码对称，说一声我补过去。

改动未提交，等你 review。建议进游戏确认一次（走 / 跑 / 瞄准各来一段），确认后我再把 `#2` 那份文档补上这次结论。
