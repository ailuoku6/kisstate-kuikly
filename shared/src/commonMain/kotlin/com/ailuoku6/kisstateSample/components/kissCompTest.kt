package com.ailuoku6.kisstateSample.components

import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.ailuoku6.kisstateSample.counter
import com.ailuoku6.kuiklyKisstate.KisContext
import com.ailuoku6.kuiklyKisstate.computed
import com.ailuoku6.kuiklyKisstate.watch
import com.tencent.kuikly.core.base.Color

internal class kissCompTestView: ComposeView<kissCompTestViewAttr, kissCompTestViewEvent>() {

    private val kisContext = KisContext()

    private val doubleCount by computed(kisContext) {
        counter.count * 2
    }

    private fun initEffect() {
        // 只保留一个副作用监听：监听 computed（用于验证销毁后是否已取消监听）
        watch(kisContext, { doubleCount }) { newV, preV ->
            println("kissLog [kissCompTestView] doubleCount change: $newV, $preV")
        }
    }
    
    override fun createEvent(): kissCompTestViewEvent {
        return kissCompTestViewEvent()
    }

    override fun createAttr(): kissCompTestViewAttr {
        return kissCompTestViewAttr()
    }

    override fun created() {
        super.created()
        initEffect()
    }

    override fun viewDestroyed() {
        // 组件销毁时取消 watch/computed 的监听，避免内存泄漏
        kisContext.dispose()
        super.viewDestroyed()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }
            View {
                attr {
                    flex(1f)
                    allCenter()
                }
                Text {
                    attr { text("child computed(doubleCount): ${ctx.doubleCount}") }
                }
            }
        }
    }
}


internal class kissCompTestViewAttr : ComposeAttr() {

}

internal class kissCompTestViewEvent : ComposeEvent() {
    
}

internal fun ViewContainer<*, *>.KissCompTest(init: kissCompTestView.() -> Unit) {
    addChild(kissCompTestView(), init)
}