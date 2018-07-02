多线程编程是时下生产环境下应用最为普遍的编程环境，但是我们知道，操作系统创建新线程要额外增加系统资源的开销，
CPU在多线程之间切换也要耗费资源，消耗时间，那为什么我们还是要用多线程呢？
### 1. 提高CPU利用率
我们知道，同一时刻同一CPU核心只能执行一条程序指令，这条程序指令当然只能来自于一个线程。
现代服务器基本上都采用多核CPU，单线程程序在同一时间点只能使用多核CPU的一个核心，其他核心处于闲置状态，
而多线程程序可以同时使用多个CPU核心，提高CPU的利用率。
### 2. 减少阻塞
当一个线程执行的任务需要花费较长时间时，单线程环境下，这个任务执行完毕之前，后续任务只能等待，程序就会阻塞；
而多线程环境下，其他线程还是可以正常执行，返回结果。    
例如当一个线程在等待网络IO同步返回结果时，CPU处于空闲状态，当前线程处于阻塞状态，单线程程序只能等待网络IO返回结果继续执行。
多线程程序就可以利用CPU执行其他任务，既减少了程序阻塞时间，有提高了CPU的利用率。
### 3. 并行执行任务，减少处理时间
举例说明，当我们处理一个1G大小的日志文件时，单线程处理需要10s，如果我们把日志文件拆成10份，分别交给10个线程处理，
再将每个线程得到的结果合并起来，那么也许我们只需要1-2s时间。    
这种拆分的前提是拆分之后的任务互不依赖，也不会有资源竞争。