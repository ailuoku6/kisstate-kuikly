package com.ailuoku6.kisstateSample

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*

import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.core.views.compose.Button
import com.ailuoku6.kisstateSample.base.BasePager
import com.ailuoku6.kuiklyKisstate.KisContext
import com.ailuoku6.kuiklyKisstate.computed
import com.ailuoku6.kuiklyKisstate.kisstate
import com.ailuoku6.kuiklyKisstate.kisstateList
import com.ailuoku6.kuiklyKisstate.watch
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.directives.vif

class Counter {
    var count by kisstate(0)
    var list by kisstateList<String>()
}

val counter = Counter()

@Page("router", supportInLocal = true)
internal class RouterPage : BasePager() {

    val kisContext = KisContext()

    val doudleCount by computed(kisContext) {
        counter.count * 2
    }

    val listLen by computed(kisContext) {
        counter.list.size
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
                    attr {
                        text("count: ${counter.count}")
                    }
                }
                vif({counter.count > 3}){
                    Text {
                        attr {
                            text("count more than 3!!")
                        }
                    }
                }
                Text {
                    attr {
                        text("double count: ${ctx.doudleCount}")
                    }
                }

                Text {
                    attr {
                        text("list length: ${ctx.listLen}")
                    }
                }

                Button {
                    attr {
                        titleAttr {
                            text("add count")
                        }
                        height(30f)
                        backgroundColor(Color.GREEN)
                    }

                    event {
                        click {
                            counter.count ++;
                        }
                    }
                }

                Button {
                    attr {
                        titleAttr {
                            text("add list")
                        }
                        height(30f)
                        backgroundColor(Color.GREEN)
                    }

                    event {
                        click {
                            counter.list.add("item: ${counter.list.size}")
                        }
                    }
                }

                Text {
                    attr { text("list:") }
                }

                vfor({ counter.list.toObservableList()}){ item ->
                    Text {
                        attr { text(item) }
                    }
                }
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
        initEffect()
        super.created()
    }

    override fun pageWillDestroy() {
        // 在 页面/组件 销毁时需要删除computed及watch的监听，防止内存泄漏
        kisContext.dispose()
        super.pageWillDestroy()
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
    }

}