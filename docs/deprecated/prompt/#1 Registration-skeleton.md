> #1 提示词

# 

```
对照CGC-Compat-MachineMax的dev.xcolorful.cgccompat.machinemax.core.config.CgccMMConfig，把CGC-Animation-Addon的dev.xcolorful.cgcanimation.client.config一个字段的写入和保存给补全
- 需要额外加一个save()供CgcAnimationClient调用
```

# 

I'll look at both config classes to understand the pattern.

Now I have the full picture. The addon's `CgcAnimationClient` already calls `CgcAnimationConfig.load(configFile)` and `CgcAnimationConfig.save(configFile)`, but the config class is a skeleton with only the default constant. I'll fill it in mirroring `CgccMMConfig`.

Let me verify the file analyzes cleanly via IDEA.

补全了 [CgcAnimationConfig.java](../CGC-Animation-Addon/core/src/main/java/dev/xcolorful/cgcanimation/client/config/CgcAnimationConfig.java)，对照 `CgccMMConfig` 的写法：

- 新增字段 `public static boolean enableAnimator`（默认 `DEFAULT_ENABLE_ANIMATOR`）
- `load(Path)` — 流式解析，文件不存在/字段缺失走默认值，异常记日志；末尾调用 `save()` 回写，保证缺省键也会出现在文件里
- 新增公开 `save(Path)` — 按固定顺序写 JSON，供 `CgcAnimationClient.saveConfig()` 调用
- `_read(JsonReader)` — 按 `CgcAnimationConfigTag.ENABLE_ANIMATOR` 分发，未知键 `skipValue()`

没有改动 `CgcAnimationClient`（它本来就调了 `load`/`save`，之前 `save` 不存在编译不过，现在补齐）。

验证：IDEA 分析 `CgcAnimationConfig` 与 `CgcAnimationClient` 均无 error；未跑 gradle（依赖来自 CurseForge，未验证实际构建）。

两点需要你确认：
- 字段注释我只写了「是否启用动画」——addon 里目前没有任何消费方，语义我不敢替你定，你补一下准确说明。
- `load()` 回写沿用了 compat 的行为（每次启动规范化配置），如果这个配置允许玩家手改且不希望被回写覆盖，说一声，我把回写挪到 `save()` 里。

未提交，等你 review。

# 
---

```
把CGC Animation Addon PR#1 三个forge类在1.21.xneoforge对照着补充，api调用参考CGC1.21.1neoforge
```

# 

I'll start by exploring the Animation Addon repo and finding PR#1.

PR #1 is the "Registration skeleton" branch, already merged. Let me find the three forge classes it added.

PR#1's three forge classes are the `forgeclient` ones. Let me see the actual directory structure and the PR1 content.

Now I can see the shape. Let me read the three Forge classes from PR#1 and the current NeoForge equivalents.

Now let me read the core classes these depend on.

Key finding: core `CgcAnimationClient.init(Path, Path)` takes two paths, but the NeoForge client calls the no-arg `init()`. Let me check how CGC 1.21.1neoforge handles these NeoForge APIs.

CGC has exact NeoForge analogs. Let me read them plus how paths are obtained.

Let me check the NeoForge module build and verify `FMLPaths` exists in NeoForge 21.1.

Confirmed `FMLPaths` and `EventBusSubscriber` (bus ignored). Let me verify the two event classes and annotation attributes.

Let me check the Forge main class to see how the client init is wired, for consistency.

Let me verify NeoForge's `EventPriority` API exists before writing.

All APIs confirmed. Writing the three NeoForge classes now.

Now updating the NeoForge client entry to pass the paths that core `init(Path, Path)` requires.

Now let me verify via IDEA analysis, then compile.

No IDEA errors. Now compiling the module to confirm.

Compiles clean. Let me review the final diff.

三个 Forge 类已对照补充为 NeoForge 版本，`neoforge-compat` 编译通过（`gradle --offline :neoforge-compat:compileJava` exit 0），IDEA 分析无 error。未提交。

对照关系（PR#1 的三个 forge 类 → 新增/修改的 neoforge 类）：

| PR#1 Forge | 1.21.x NeoForge |
|---|---|
| [CgcAnimationForgeClient.java](forge-compat/src/main/java/dev/xcolorful/cgcanimation/forgeclient/CgcAnimationForgeClient.java) | [CgcAnimationNeoforgeClient.java](neoforge-compat/src/main/java/dev/xcolorful/cgcanimation/neoforgeclient/CgcAnimationNeoforgeClient.java)（改） |
| [ForgeClientCommandRegistry.java](forge-compat/src/main/java/dev/xcolorful/cgcanimation/forgeclient/init/ForgeClientCommandRegistry.java) | [NeoforgeClientCommandRegistry.java](neoforge-compat/src/main/java/dev/xcolorful/cgcanimation/neoforgeclient/init/NeoforgeClientCommandRegistry.java)（新） |
| [ForgeClientSetup.java](forge-compat/src/main/java/dev/xcolorful/cgcanimation/forgeclient/init/ForgeClientSetup.java) | [NeoforgeClientSetup.java](neoforge-compat/src/main/java/dev/xcolorful/cgcanimation/neoforgeclient/init/NeoforgeClientSetup.java)（新） |

API 换算按 CGC 1.21.1neoforge 的 `NeoClientCommandRegistry` / `NeoClientSetup` 写法：`net.neoforged.api.distmarker.Dist`、`net.neoforged.bus.api.SubscribeEvent`、`net.neoforged.fml.common.EventBusSubscriber`、`net.neoforged.neoforge.client.event.RegisterClientCommandsEvent`、`net.neoforged.fml.event.lifecycle.FMLClientSetupEvent`。

顺带修掉一个真实编译错误：core 的 `CgcAnimationClient.init(Path, Path)` 需要两个路径参数，而 `CgcAnimationNeoforgeClient` 原来调的是无参 `init()`。已改成 `FMLPaths.GAMEDIR.get(), FMLPaths.CONFIGDIR.get()`（NeoForge 的 `net.neoforged.fml.loading.FMLPaths` 有同名枚举，与 Forge 一致）。

三个判断点，需要你确认：

1. **类名用 `Neoforge*` 而非 CGC 的 `Neo*`** — 依据是 addon 自身一律用长写法（`neoforgeclient` 包、`CgcAnimationNeoforge`、`CgcAnimationNeoforgeClient`），而 CGC 自己是 `Neo*` 与 `Neoforge` 混用。若你想向 CGC 对齐，我改成 `NeoClientCommandRegistry` / `NeoClientSetup`。
2. **保留了 `@SubscribeEvent(priority = EventPriority.LOW)`**（换算成 `net.neoforged.bus.api.EventPriority.LOW`）——Forge 版有这个语义，CGC 的 NeoForge 版没有。我按"忠实迁移"保留了；要跟 CGC 一致就去掉。
3. **去掉了 `bus = ...Bus.FORGE / Bus.MOD`** —— NeoForge 1.21.1 的 `EventBusSubscriber.bus()` 已 `@Deprecated(forRemoval)` 且被忽略，bus 由事件是否实现 `IModBusEvent` 自动判定，与 CGC 写法一致。

另外注意：`forge-compat/` 不在 `settings.gradle` 里（只 include 了 `core` 和 `neoforge-compat`），所以这次只动了 neoforge 侧。
