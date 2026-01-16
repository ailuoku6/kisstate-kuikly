
# kisstate for Kuikly

**kisstate** 是一个遵循 **KISS（Keep It Simple, Stupid）原则** 的轻量级响应式状态管理库，
为 **Kuikly** 框架量身设计。

它提供了类似 **Vue / React Hooks** 的响应式能力，通过 `kisstate`、`computed`、`watch` 等 API，
让你可以用**极少的心智负担**完成状态管理、派生状态和副作用监听。

---

## 特性 ✨

* **极简 API**：`kisstate / kisstateList / computed / watch`
* **响应式自动追踪**：状态变化 → UI 自动刷新
* **Kuikly 深度适配**：与 `vfor` `vif` 等指令 、Page 生命周期无缝协作
* **无侵入式设计**：不需要继承基类，不污染业务模型
* **显式生命周期管理**：通过 `KisContext` 避免内存泄漏

---

## 安装 📦

```kotlin
implementation("com.ailuoku6:kuikly-kisstate:latest-version")
```

> 版本号以实际发布为准

---

## 核心概念 🧠

### 1️⃣ 响应式状态（kisstate）

使用 `kisstate` 声明一个**可观察的基础状态**：

```kotlin
class Counter {
    var count by kisstate(0)
}
```

* 自动追踪 `count` 的读取与修改
* 在 UI、computed、watch 中使用都会建立依赖

---

### 2️⃣ 响应式列表（kisstateList）

列表是 Kuikly 场景中最常见的状态之一，使用 `kisstateList`：

```kotlin
class Counter {
    var list by kisstateList<String>()
}
```

支持：

* `add / remove / clear` 等数组方法
* 自动触发 `vfor` 刷新
* 可被 `computed` / `watch` 依赖

在 `vfor` 中使用时：

```kotlin
vfor({ counter.list.toObservableList() }) { item ->
    Text {
        attr { text(item) }
    }
}
```

---

### 3️⃣ 计算属性（computed）

`computed` 用于声明**派生状态**，只在依赖变化时重新计算。

```kotlin
val kisContext = KisContext()

val doubleCount by computed(kisContext) {
    counter.count * 2
}

val listLen by computed(kisContext) {
    counter.list.size
}
```

特点：

* 自动依赖收集
* 具备缓存能力
* 依赖变化才重新计算

⚠️ `computed` **必须绑定到 `KisContext`**

---

### 4️⃣ 副作用监听（watch）

使用 `watch` 监听状态变化并执行副作用逻辑：

```kotlin
// 监听kisstate
watch(kisContext, { counter.count }) { newV, preV ->
   println("kissLog count change: ${newV}")
}
// 监听kisstateList
watch(kisContext, { counter.list }){ newV, preV ->
   println("kissLog list change: ${newV}")
}
// 监听computed值
watch(kisContext, { doudleCount }){ newV, preV ->
   println("kissLog doudleCount change: ${newV}")
}
// 监听多个变量
watch(kisContext, {
   listOf(counter.count ,counter.list)
}) { newV, preV ->
   println("kissLog count or list change: ${newV}")
}
```

* 支持监听 `kisstate` 
* 支持监听 `kisstateList`
* 支持监听 `computed`
* 提供 `newValue / oldValue`

---

## 完整示例 🚀

```kotlin
class Counter {
    var count by kisstate(0)
    var list by kisstateList<String>()
}

val counter = Counter()

@Page("router", supportInLocal = true)
internal class RouterPage : BasePager() {

    private val kisContext = KisContext()

    val doubleCount by computed(kisContext) {
        counter.count * 2
    }

    val listLen by computed(kisContext) {
        counter.list.size
    }

    override fun body(): ViewBuilder = {
        View {
            attr {
                flex(1f)
                allCenter()
            }

            Text { attr { text("count: ${counter.count}") } }
            Text { attr { text("double count: $doubleCount") } }
            Text { attr { text("list length: $listLen") } }

            Button {
                attr { titleAttr { text("add") } }
                event {
                    click {
                        counter.list.add("item: ${counter.count}")
                        counter.count++
                    }
                }
            }

            Text { attr { text("list:") } }

           // 需要注意的是，在 vfor 中使用kisstateList，需要调用toObservableList返回原始的ObservableList对象
            vfor({ counter.list.toObservableList() }) { item ->
                Text { attr { text(item) } }
            }
        }
    }

   fun initEffect() {
      // 监听kisstate
      watch(kisContext, { counter.count }) { newV, preV ->
         println("kissLog count change: ${newV}")
      }
      // 监听kisstateList
      watch(kisContext, { counter.list }){ newV, preV ->
         println("kissLog list change: ${newV}")
      }
      // 监听computed值
      watch(kisContext, { doudleCount }){ newV, preV ->
         println("kissLog doudleCount change: ${newV}")
      }
      // 监听多个变量
      watch(kisContext, {
         listOf(counter.count ,counter.list)
      }) { newV, preV ->
         println("kissLog count or list change: ${newV}")
      }
   }

    override fun created() {
        super.created()
        initEffect()
    }

    override fun pageWillDestroy() {
        super.pageWillDestroy()
        // ⚠️ 非常重要：释放所有 computed / watch
        kisContext.dispose()
    }
}
```

---

## 生命周期管理 ⚠️（非常重要）

`computed` 和 `watch` **不会自动销毁**，
必须在 Page / Component 销毁时手动释放：

```kotlin
override fun pageWillDestroy() {
    kisContext.dispose()
    super.pageWillDestroy()
}
```

否则可能导致：

* 内存泄漏
* 已销毁页面仍然响应状态变化

---

## 设计理念 🔧

* **显式依赖，而非魔法**
* **Context 即作用域**
* **状态 ≠ UI**
* **响应式但可控**

kisstate 不试图“接管一切”，
它只负责 **状态 → 响应 → 通知** 这一件事。

---

## 最佳实践 ✅

1. **业务状态单例化**

   ```kotlin
   val counter = Counter()
   ```

2. **Page / Component 内创建 KisContext**

3. **computed / watch 全部绑定到 context**

4. **生命周期结束及时 dispose**

5. **状态与 UI 解耦，方便复用与测试**

---

## API 一览 📖

| API            | 说明           |
| -------------- | ------------ |
| `kisstate`     | 基础响应式状态      |
| `kisstateList` | 响应式列表        |
| `computed`     | 计算属性         |
| `watch`        | 状态监听         |
| `KisContext`   | 作用域 & 生命周期管理 |

---

## License

MIT © ailuoku6

---

🎉 **让 Kuikly 的状态管理回归简单！**
kisstate 不追求“全能”，只追求**好理解、好用、不坑**。
