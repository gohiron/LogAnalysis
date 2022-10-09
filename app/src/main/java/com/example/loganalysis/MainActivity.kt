package com.example.loganalysis

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import com.example.loganalysis.databinding.ActivityMainBinding
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class MainActivity : AppCompatActivity() {

    private  val PERMISSIONS_REQUEST_CODE = 100

//    fun main(args: Array<String>)
//    {
//        println("test")
//    }
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ファイルを開くためのアクセス権限を取得する
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // すでに許可されている
                File(externalStoragePath()).listFiles().map { file ->
                    println(file.path)
                }
            } else {
                // まだ許可されていない
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSIONS_REQUEST_CODE)
            }
        }

        // ログファイルを読み込む
        val inputText = readTextFromUri(Uri.parse("file:///storage/emulated/0/Download/test_log.txt"))

        //funtion1(inputText)
        //funtion2(inputText, 2)
        //funtion3(inputText, 2, 2, 3)
        funtion4(inputText, 2)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    File(externalStoragePath()).listFiles().map { file ->
                        println(file.name)
                    }
                } else {
                    println("")
                }
                return
            }
        }
    }

    private fun externalStoragePath(): String {
        return Environment.getExternalStorageDirectory().absolutePath
    }

    // ファイル内容を1行づつ読み込みリストに格納する
    private fun readTextFromUri(uri: Uri): List<String> {
        val listString = mutableListOf<String>()

        contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                // 1行づつ読み込みリストに格納する
                var line: String? = reader.readLine()
                while (line != null) {
                    listString.add(line)
                    line = reader.readLine()
                }
            }
        }
        return listString
    }

    // 設問1
    // 監視ログファイルを読み込み、故障状態のサーバアドレスとそのサーバの故障期間を出力するプログラムを作成せよ。
    // 出力フォーマットは任意でよい。
    // なお、pingがタイムアウトした場合を故障とみなし、最初にタイムアウトしたときから、次にpingの応答が返るまでを故障期間とする。
    private fun funtion1(input: List<String>) {
        var failureList = mutableListOf<List<String>>()

        for (line in input) {
            var data = line.split(",")

            if (data[2] == "-") {
                failureList.add(data)
                println("故障:" + data[1])
            }
            else if (failureList.count() > 0) {
                for (fail in failureList) {
                    if (fail[1] == data[1]) {
                        val date1 = fail[0].toDate()
                        val date2 = data[0].toDate()
                        val minutes = ChronoUnit.SECONDS.between(date1, date2)
                        println("サーバアドレス:" + data[1] + " 故障期間:" + minutes + "秒")
                    }
                }
            }
        }
    }

    // 設問2
    // ネットワークの状態によっては、一時的にpingがタイムアウトしても、一定期間するとpingの応答が復活することがあり、
    // そのような場合はサーバの故障とみなさないようにしたい。
    // N回以上連続してタイムアウトした場合にのみ故障とみなすように、設問1のプログラムを拡張せよ。
    // Nはプログラムのパラメータとして与えられるようにすること。
    private fun funtion2(input: List<String>, n: Int) {
        var failureList = mutableListOf<RowData>()

        for (line in input) {
            var data = line.split(",")
            var rowData = RowData()
            rowData.dateTime = data[0]
            rowData.address = data[1]
            if (data[2] == "-") {
                rowData.time = 0
            } else {
                rowData.time = data[2].toInt()
            }

            if (data[2] == "-") {
                // タイムアウト

                if (failureList.isEmpty()) {
                    failureList.add(rowData)
                } else {
                    for (fail in failureList) {
                        // 故障リストのアドレスと一致するかどうか
                        if (fail.address != rowData.address) {
                            // 一致しないため新規追加
                            failureList.add(rowData)
                        } else {
                            // 一致したため回数カウント
                            fail.count += 1
                            // N回以上連続しているかどうか
                            if (fail.count > n) {
                                println("故障:" + fail.address)
                            }
                        }
                    }
                }
            }
            else if (failureList.isNotEmpty()) {
                for (fail in failureList) {
                    if (fail.address == data[1]) {
                        val date1 = fail.dateTime.toDate()
                        val date2 = data[0].toDate()
                        val minutes = ChronoUnit.SECONDS.between(date1, date2)
                        println("サーバアドレス:" + data[1] + " 故障期間:" + minutes + "秒")
                        failureList.remove(fail)
                    }
                }
            }
        }
    }

    // 設問3
    // サーバが返すpingの応答時間が長くなる場合、サーバが過負荷状態になっていると考えられる。
    // そこで、直近m回の平均応答時間がtミリ秒を超えた場合は、サーバが過負荷状態になっているとみなそう。
    // 設問2のプログラムを拡張して、各サーバの過負荷状態となっている期間を出力できるようにせよ。mとtはプログラムのパラメータとして与えられるようにすること。
    private fun funtion3(input: List<String>, n: Int, m: Int, t:Int) {
        var failureList = mutableListOf<RowData>()
        var dataList = mutableMapOf<String, MutableList<RowData>>()

        for (line in input) {
            var data = line.split(",")
            var rowData = RowData()
            rowData.dateTime = data[0]
            rowData.address = data[1]
            if (data[2] == "-") {
                rowData.time = 0
            } else {
                rowData.time = data[2].toInt()
            }

            if (dataList.get(data[1]) == null) {
                dataList.put(data[1], mutableListOf(rowData))
            } else {
                dataList[data[1]]?.add(rowData)
                var avg = dataList[data[1]]!!.sumOf { it.time } / dataList[data[1]]!!.count()
                if (avg > t) {
                    println("過負荷状態" + data[1])
                }
                if (dataList[data[1]]!!.count() >= m) {
                    dataList[data[1]]!!.removeAt(0)
                }
            }

            if (data[2] == "-") {
                // タイムアウト

                if (failureList.isEmpty()) {
                    failureList.add(rowData)
                } else {
                    for (fail in failureList) {
                        // 故障リストのアドレスと一致するかどうか
                        if (fail.address != rowData.address) {
                            // 一致しないため新規追加
                            failureList.add(rowData)
                        } else {
                            // 一致したため回数カウント
                            fail.count += 1
                            // N回以上連続しているかどうか
                            if (fail.count > n) {
                                println("故障:" + fail.address)
                            }
                        }
                    }
                }
            }
            else if (failureList.isNotEmpty()) {
                for (fail in failureList) {
                    if (fail.address == data[1]) {
                        val date1 = fail.dateTime.toDate()
                        val date2 = data[0].toDate()
                        val minutes = ChronoUnit.SECONDS.between(date1, date2)
                        println("サーバアドレス:" + data[1] + " 故障期間:" + minutes + "秒")
                        failureList.remove(fail)
                    }
                }
            }
        }
    }

    // 設問4
    // ネットワーク経路にあるスイッチに障害が発生した場合、そのスイッチの配下にあるサーバの応答がすべてタイムアウトすると想定される。
    // そこで、あるサブネット内のサーバが全て故障（ping応答がすべてN回以上連続でタイムアウト）している場合は、
    // そのサブネット（のスイッチ）の故障とみなそう。
    // 設問2または3のプログラムを拡張して、各サブネット毎にネットワークの故障期間を出力できるようにせよ。
    private fun funtion4(input: List<String>, n: Int) {
        var dataList = mutableMapOf<String, MutableList<RowData>>()

        for (line in input) {
            var data = line.split(",")
            var rowData = RowData()
            rowData.dateTime = data[0]
            rowData.address = data[1]
            if (data[2] == "-") {
                rowData.time = 0
            } else {
                rowData.time = data[2].toInt()
            }
            var subnet = data[1].substringAfterLast("/")

            if (data[2] == "-") {
                var d = dataList.get(subnet)
                if (d == null) {
                    dataList.put(subnet, mutableListOf(rowData))
                } else {
                    d.add(rowData)
                    if (d.count() >= n) {
                        val min = d.minOf { it.dateTime }
                        val max = d.maxOf { it.dateTime }
                        val date1 = min.toDate()
                        val date2 = max.toDate()
                        val minutes = ChronoUnit.SECONDS.between(date1, date2)
                        println("故障:サブネット:" + subnet + " 故障期間:" + minutes + "秒")
                    }
                }
            } else {
                var d = dataList.get(subnet)
                if (d != null) {
                    dataList.remove(subnet)
                }
            }
        }
    }

    private fun String.toDate(pattern: String = "yyyyMMddHHmmss"): LocalDateTime {
        val sdFormat = DateTimeFormatter.ofPattern(pattern)
        val date = LocalDateTime.parse(this, sdFormat)
        return date
    }

    /**
     * A native method that is implemented by the 'loganalysis' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'loganalysis' library on application startup.
        init {
            System.loadLibrary("loganalysis")
        }
    }
}

class RowData {
    var dateTime: String = ""
    var address: String = ""
    var time: Int = 0
    var count: Int = 0
}