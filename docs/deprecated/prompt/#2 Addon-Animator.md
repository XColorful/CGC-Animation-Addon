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
