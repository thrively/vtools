package com.omarea.vboot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.omarea.shared.ConfigInfo
import java.io.DataOutputStream
import java.io.IOException

class receiver_boot_openswap : BroadcastReceiver() {
    private var p: Process? = null
    internal var out: DataOutputStream? = null

    internal fun tryExit() {
        try {
            if (out != null)
                out!!.close()
        } catch (ex: Exception) {
        }

        try {
            p!!.destroy()
        } catch (ex: Exception) {
        }

    }

    @JvmOverloads internal fun DoCmd(cmd: String, isRedo: Boolean = false) {
        Thread(Runnable {
            try {
                tryExit()
                if (p == null || isRedo || out == null) {
                    tryExit()
                    p = Runtime.getRuntime().exec("su")
                    out = DataOutputStream(p!!.outputStream)
                }
                out!!.writeBytes(cmd)
                out!!.writeBytes("\n")
                out!!.flush()
                //out!!.close()
            } catch (e: IOException) {
                //重试一次
                if (!isRedo)
                    DoCmd(cmd, true)
                else
                {

                }
            }
        }).start()
    }

    override fun onReceive(context: Context, intent: Intent) {
        var sb = StringBuilder("setenforce 0\n")
        if (ConfigInfo.getConfigInfo().AutoStartSwap) {

            if(ConfigInfo.getConfigInfo().AutoStartZRAM) {
                var zramSize = ConfigInfo.getConfigInfo().AutoStartZRAMSize
                sb.append("if [ `cat /sys/block/zram0/disksize` != '" + zramSize + "000000' ] ; then ")
                sb.append("swapoff /dev/block/zram0 &> /dev/null;")
                sb.append("echo 1 > /sys/block/zram0/reset;")
                sb.append("echo " + zramSize + "000000 > /sys/block/zram0/disksize;")
                sb.append("mkswap /dev/block/zram0 &> /dev/null;")
                sb.append("swapon /dev/block/zram0 &> /dev/null;")
                sb.append("fi;\n")
            }

            if (ConfigInfo.getConfigInfo().AutoStartSwapDisZram) {
                sb.append("swapon /data/swapfile -p 32767\n")
                //sb.append("swapoff /dev/block/zram0\n")
            } else {
                sb.append("swapon /data/swapfile\n")
            }

            sb.append("echo 65 > /proc/sys/vm/swappiness\n")
            sb.append("echo " + ConfigInfo.getConfigInfo().AutoStartSwappiness + " > /proc/sys/vm/swappiness\n")
        }
        sb.append("sh /data/data/me.piebridge.brevent/brevent.sh;");
        DoCmd(sb.toString())
    }
}
