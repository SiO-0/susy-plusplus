@echo off
rem ============================================================================
rem  低内存启动脚本（15 GB 笔记本 / 220 mods 整合包）
rem
rem  背景：run/hs_err_pid*.log 的崩溃全部是 JVM *原生* 内存不足：
rem    - hs_err_pid28468: Native memory allocation (mmap) failed ... G1 virtual space
rem    - hs_err_pid32636: Native memory allocation (malloc) failed ... Chunk::new (arena.cpp)
rem  实测本机：物理内存 15.8 GB，Commit 上限只有 19.4 GB（页面文件仅 3.5 GB）。
rem  游戏 4 GB 堆 + 220 mods 的 Metaspace/CodeCache/JIT arena + 其它程序，
rem  很容易把提交内存用满 —— 这与模组代码无关，需要「加大页面文件 + 限制原生内存」。
rem
rem  本脚本做两件事：
rem    1) JAVA_TOOL_OPTIONS 注入原生内存上限（对 Gradle 与游戏进程都生效，
rem       比 extra_jvm_args 更可靠 —— 后者实测只把 -Xms/-Xmx 传到了游戏）；
rem    2) 先停掉 Gradle 守护进程（每个约占 3 GB），再启动游戏。
rem
rem  ⚠ 仍崩溃的话：把 Windows 虚拟内存（页面文件）调大到 初始 8192 / 最大 24576 MB，
rem    并关掉多余的 VS Code 窗口 / 浏览器（当前 VS Code 多开约占 3.1 GB）。
rem ============================================================================
setlocal

set "JAVA_TOOL_OPTIONS=-XX:MaxMetaspaceSize=1G -XX:ReservedCodeCacheSize=256m -XX:CICompilerCount=2"

call gradlew.bat --stop
call gradlew.bat runClient %*

endlocal
