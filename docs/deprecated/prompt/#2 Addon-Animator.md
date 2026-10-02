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
