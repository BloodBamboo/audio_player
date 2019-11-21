package cn.com.bamboo.easy_audio_player.util

import android.app.Activity
import android.widget.NumberPicker
import cn.com.bamboo.easy_audio_player.R
import cn.com.bamboo.easy_common.util.RxJavaHelper
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import org.jetbrains.anko.alert
import java.util.concurrent.TimeUnit

class TimingUtil {
    private var timing: Disposable? = null

    companion object {
        fun alertTiming(activity: Activity?, timing: (Long) -> Unit) {
            activity?.alert {
                title = "定时,单位分钟"
                val view = activity.layoutInflater.inflate(R.layout.layout_timing, null, false)
                val numberPicker1 = view.findViewById<NumberPicker>(R.id.number_picker_1).apply {
                    maxValue = 9
                    minValue = 0
                    value = 0
                }
                val numberPicker2 = view.findViewById<NumberPicker>(R.id.number_picker_2).apply {
                    maxValue = 9
                    minValue = 0
                    value = 0
                }
                val numberPicker3 = view.findViewById<NumberPicker>(R.id.number_picker_3).apply {
                    maxValue = 9
                    minValue = 0
                    value = 0
                }
                customView = view
                positiveButton("确定") {
                    val temp1 = numberPicker1.value
                    val temp2 = numberPicker2.value
                    val temp3 = numberPicker3.value
                    val timeNum = temp1 * 100 + temp2 * 10 + temp3
                    timing(timeNum.toLong() * 60)
                }
                negativeButton("取消") {
                }
            }?.show()
        }
    }


    fun onDestroy() {
        timing?.run {
            dispose()
        }
        timing = null
    }

    fun startTiming(
        timeNum: Long, onNext: (time: Long) -> Unit,
        onError: (throeable: Throwable) -> Unit,
        onComplete: () -> Unit
    ) {
        timing?.run {
            dispose()
        }
        timing = null
        if (timeNum == 0L) {
            return
        }
        timing = Observable.intervalRange(0, timeNum, 0, 1, TimeUnit.SECONDS)
            .compose(RxJavaHelper.schedulersTransformer())
            .subscribe({
                onNext(it)
            }, {
                onError(it)
            }, {
                onComplete()
            })

    }
}