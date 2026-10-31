# 开发环境：启动崩溃排查（原生 OOM）

> 现象：`gradlew runClient` 跑一段时间后游戏直接消失，**没有** Java 异常栈，
> 只在仓库根目录留下 `run/hs_err_pid*.log`（以及 `run/replay_pid*.log`）。

---

## 1. 怎么区分「模组崩溃」和「JVM 原生 OOM」

| 证据 | 含义 |
| --- | --- |
| `run/crash-reports/crash-*.txt` 里有 `java.lang.NoSuchMethodError` / `Exception in ...` | Java 异常，**是代码问题** |
| `run/hs_err_pid*.log` 开头是 `There is insufficient memory for the Java Runtime Environment` | JVM 原生内存不足，**不是代码问题** |
| 日志最后停在主菜单 / 贴图重载，没有任何异常 | 同上（被 OS 拒绝分配内存） |

本仓库出现过的两类实例：

```text
# run/hs_err_pid28468.log  (12:13)
Native memory allocation (mmap) failed to map 218103808 bytes. Error detail: G1 virtual space
Command Line: -Xms1G -Xmx6G

# run/hs_err_pid32636.log  (12:33)
Native memory allocation (malloc) failed to allocate 8847512 bytes. Error detail: Chunk::new
Out of Memory Error (arena.cpp:186)          # C2 JIT arena
Command Line: -Xms1G -Xmx4G
```

> `Chunk::new` / `arena.cpp` 是 **C2 编译器**在申请原生内存；
> 降到 4G 堆之后仍然失败，说明问题不在堆大小，而在**系统的提交内存（commit）上限**。

---

## 2. 本机实测数据（为什么是页面文件太小）

```text
RAM total      : 15.8 GB
RAM free       :  6.3 GB
Commit limit   : 19.4 ~ 20.4 GB   ← 物理 15.8G + 页面文件 3.6 ~ 4.7G
Commit free    :  3.8 ~ 5.9 GB
Pagefile       : C:\pagefile.sys  allocated 3616 ~ 4705 MB

占用前列（空闲时）：VS Code 多开 ≈ 3.1 GB，QQ/msedge/若干 java ≈ 1 GB
```

游戏进程需要 **4 GB 堆 + 约 2–3 GB 原生内存**
（220 个 mod 的 Metaspace、CodeCache、C2 arena、LWJGL/OpenGL 驱动）。
加上 VS Code 多开的 3 GB，19.4 GB 的提交上限很容易被用满 →
`mmap/malloc` 被 OS 拒绝 → JVM 直接终止。

---

## 3. 解决办法（按顺序做）

### 3.1 加大 Windows 页面文件（最关键）

「系统属性 → 高级 → 性能设置 → 高级 → 虚拟内存 → 更改」：

- 取消勾选「自动管理所有驱动器的分页文件大小」；
- 选 `C:`（或剩余空间大的盘）→「自定义大小」→
  **初始大小 8192 MB / 最大值 24576 MB** → 设置 → 确定 → **重启**。

命令行方式（管理员 CMD，重启生效）：

```bat
wmic pagefileset where name="C:\\pagefile.sys" set InitialSize=8192,MaximumSize=24576
```

### 3.2 直接用 `gradlew runClient` 即可（已自动注入）

[`gradle/scripts/extra.gradle`](../gradle/scripts/extra.gradle) 里给
`runClient / runServer / runObfClient / runObfServer` 设了环境变量：

```groovy
tasks.matching { it.name in ['runClient', ...] }.configureEach {
    environment 'JAVA_TOOL_OPTIONS',
            '-XX:MaxMetaspaceSize=1024m -XX:ReservedCodeCacheSize=256m -XX:CICompilerCount=2'
}
```

> 为什么用环境变量而不是 `extra_jvm_args`：实测那条路径只有 `-Xms/-Xmx`
> 会被转发到真正的游戏子进程（对比 `hs_err` 里的 `Command Line`），`-XX` 会被丢掉；
> 而 `JAVA_TOOL_OPTIONS` 会被所有 JVM（包括 Cleanroom 重新拉起的子进程）继承，
> 启动后日志里应出现 `Picked up JAVA_TOOL_OPTIONS: ...` 以确认生效。

另外堆已从 6G 降到 **3G**（`gradle.properties` 的
`extra_jvm_args = -Xms1G -Xmx3G`）。

也可以继续用 [`run-client.bat`](../run-client.bat)：它会先 `gradlew --stop`
（释放约 3 GB 的 Gradle 守护进程）再启动。

> 为什么不用 `gradle.properties` 的 `extra_jvm_args` 传这些 `-XX`？
> 实测这条路径只把 `-Xms/-Xmx` 送到了游戏进程（对比 hs_err 的 `Command Line`），
> 所以原生内存上限改用环境变量注入。

### 3.3 其它

- 关掉多余的 VS Code 窗口 / 浏览器（当前多开约占 3.1 GB）；
- 游戏内图形设置可适当降低（OptiFine 的 render distance / mipmap 会占原生内存）；
- 需要「绝对不再 OOM」时，把堆降到 `-Xmx3G`（`gradle.properties` 的 `extra_jvm_args`）。

---

## 4. 已经修掉的另一类崩溃（Java 异常，非原生 OOM）

`NoSuchMethodError: EntityPlayer.getHeldItemMainhand()` 等 8 份 crash-report
是**重混淆工具链**问题：这些方法声明在 `EntityLivingBase` / `Entity` 上，
RFG reobf 不沿继承链查映射，接收者写成 `EntityPlayer` 时调用名不会被映射。

现已统一走 [`PlayerHeld`](../src/main/java/com/susy/plusplus/util/PlayerHeld.java)，
并在重混淆后的 jar 里逐一核对过（`func_184614_ca` / `func_184592_cb` /
`func_184586_b` / `func_70093_af`）。详见
[`steam_pump_and_pipes.md`](steam_pump_and_pipes.md) §6.1。
