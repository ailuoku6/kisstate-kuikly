
# kisstate for Kuikly

**kisstate** 是一个遵循 **KISS（Keep It Simple, Stupid）原则** 的轻量级响应式状态管理库，
专为 **Kuikly** 框架量身打造。

它提供了类似 **Vue / React Hooks** 的响应式能力，通过 `kisstate`、`computed`、`watch` 等 API，
让你可以用**极低的心智负担**完成状态管理、派生状态和副作用监听。

> 🎯 设计目标
> **简单、直观、可控、不踩坑**

---

## 特性 ✨

* **极简 API**

   * `kisstate`
   * `kisstateList`
   * `computed`
   * `watch`
* **自动依赖追踪**

   * 状态变化 → 依赖自动更新 → UI 自动刷新
* **Kuikly 深度适配**

   * 与 `vfor` / `vif` / Page 生命周期无缝协作
* **无侵入式设计**

   * 不需要继承基类
   * 不污染业务模型
* **显式生命周期管理**

   * 通过 `KisContext` 统一管理，避免内存泄漏

---

## 安装 📦

```kotlin
implementation("io.github.ailuoku6.kisstate:lib:1.0.3")
```

> 版本号以实际发布为准

---

## 核心概念 🧠

### 1️⃣ 基础响应式状态（`kisstate`）

使用 `kisstate` 声明一个**可观察的基础状态**：

```kotlin
class Counter {
    var count by kisstate(0)
}
```

说明：

* 自动追踪 `count` 的读取与修改
* 在 UI / computed / watch 中使用都会建立依赖
* 状态变化会自动触发相关更新

---

### 2️⃣ 响应式列表（`kisstateList`）

列表是 Kuikly 场景中最常见的状态形式：

```kotlin
class Counter {
    var list by kisstateList<String>()
}
```

支持能力：

* `add / remove / clear` 等常见列表操作
* 自动触发依赖更新
* 可被 `computed` / `watch` 依赖
* 原生支持 `vfor` 等场景

#### ⚠️ vfor 使用注意事项（非常重要）

> **在 `vfor` 中使用 `kisstateList` 时，必须调用 `toObservableList()`**

```kotlin
vfor({ counter.list.toObservableList() }) { item ->
    Text {
        attr { text(item) }
    }
}
```

原因说明：

* `kisstateList` 是一个响应式代理对象
* `vfor` 依赖的是 Kuikly 内部的 `ObservableList`
* `toObservableList()` 用于返回其内部维护的原始 `ObservableList`

❗ **不调用无法编译成功**

---

### 3️⃣ 计算属性（`computed`）

`computed` 用于声明**派生状态**，只在依赖变化时重新计算：

```kotlin
val kisContext = KisContext()

val doubleCount by computed(kisContext) {
    counter.count * 2
}

val listLen by computed(kisContext) {
    counter.list.size
}
```

特性：

* 自动依赖收集
* 具备缓存能力
* 仅在依赖变化时重新计算

⚠️ **所有 `computed` 必须绑定到 `KisContext`**

---

### 4️⃣ 副作用监听（`watch`）

使用 `watch` 监听状态变化并执行副作用逻辑。

支持监听：

* `kisstate`
* `kisstateList`
* `computed`
* 多个依赖组合

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

            // ⚠️ vfor 中必须使用 toObservableList()
            vfor({ counter.list.toObservableList() }) { item ->
                Text {
                    attr { text(item) }
                }
            }
        }
    }

    override fun created() {
        super.created()
        initEffect()
    }

    /**
     * 副作用监听统一在这里初始化
     */
    private fun initEffect() {

        // 1️⃣ 监听 kisstate
        watch(kisContext, { counter.count }) { newV, oldV ->
            println("kissLog count change: $oldV -> $newV")
        }

        // 2️⃣ 监听 kisstateList
        watch(kisContext, { counter.list }) { newV, oldV ->
            println("kissLog list change: $newV")
        }

        // 3️⃣ 监听 computed
        watch(kisContext, { doubleCount }) { newV, oldV ->
            println("kissLog doubleCount change: $oldV -> $newV")
        }

        // 4️⃣ 同时监听多个依赖
        watch(
            kisContext,
            {
                listOf(
                    counter.count,
                    counter.list
                )
            }
        ) { newV, oldV ->
            println("kissLog count or list changed")
        }
    }

    override fun pageWillDestroy() {
        super.pageWillDestroy()
        // ⚠️ 非常重要：释放所有 computed / watch
        kisContext.dispose()
    }
}
```

---

## 监听多个依赖 📌

```kotlin
watch(kisContext, {
    listOf(counter.count, counter.list)
}) { newV, oldV ->
    // 任意一个依赖变化都会触发
}
```

说明要点：

* `watch` 的第一个 lambda **只用于依赖收集**
* 返回值本身并不重要，通常使用 `listOf(...)`
* 只要其中任意一个依赖发生变化，回调就会执行

适用场景：

* 多个状态共同决定一个副作用
* 只关心“是否变化”，不关心具体是哪一个变了
* 用于合并多个 `watch` 的场景

---

## 生命周期管理 ⚠️（非常重要）

`computed` 和 `watch` **不会自动销毁**，
必须在 Page / Component 销毁时手动释放，例如：

```kotlin
override fun pageWillDestroy() {
    kisContext.dispose()
    super.pageWillDestroy()
}

override fun viewDestroyed() {
    kisContext.dispose()
    super.viewDestroyed()
}
```

否则可能导致：

* 内存泄漏
* 页面已销毁但仍然响应状态变化

---

## 设计理念 🔧

* **显式依赖，而非隐式魔法**
* **Context 即作用域**
* **状态 ≠ UI**
* **响应式，但必须可控**

kisstate 不试图“接管一切”，
它只专注一件事：

> **状态变化 → 精准通知 → 可预测更新**

---

## 最佳实践 ✅

1. **业务状态单例化**

   ```kotlin
   val counter = Counter()
   ```

2. **在 Page / Component 内创建 `KisContext`**

3. **所有 `computed / watch` 必须绑定到 context**

4. **生命周期结束时及时 `dispose()`**

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
kisstate 不追求“全能”，
只追求 **好理解、好使用、不踩坑**。
