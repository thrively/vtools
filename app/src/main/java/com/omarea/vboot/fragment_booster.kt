package com.omarea.vboot

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.Settings
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.CheckBox
import android.widget.ListView
import android.widget.TabHost
import android.widget.TextView
import com.omarea.shared.ConfigInfo
import com.omarea.shared.ServiceHelper
import com.omarea.shared.cmd_shellTools
import com.omarea.ui.list_adapter
import kotlinx.android.synthetic.main.layout_booster.*
import java.util.*
import kotlin.collections.ArrayList


class fragment_booster : Fragment() {

    lateinit internal var frameView: View

    internal var cmdshellTools: cmd_shellTools? = null
    internal var thisview: main? = null

    override fun onResume() {
        val serviceState = ServiceHelper.serviceIsRunning(context)
        btn_booster_service_not_active.visibility = if (serviceState) GONE else VISIBLE
        btn_booster_dynamicservice_not_active.visibility = if (serviceState && !ConfigInfo.getConfigInfo().AutoBooster) VISIBLE else GONE

        super.onResume()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.layout_booster, container, false)
    }


    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        this.frameView = view!!

        cacheclear.isChecked = ConfigInfo.getConfigInfo().AutoClearCache
        dozemod.isChecked = ConfigInfo.getConfigInfo().UsingDozeMod

        cacheclear.setOnCheckedChangeListener { buttonView, isChecked -> ConfigInfo.getConfigInfo().AutoClearCache = isChecked }
        dozemod.setOnCheckedChangeListener { buttonView, isChecked -> ConfigInfo.getConfigInfo().UsingDozeMod = isChecked }

        btn_booster_service_not_active.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        btn_booster_dynamicservice_not_active.setOnClickListener {
            val intent = Intent(thisview, accessibility_settings::class.java)
            startActivity(intent)
        }

        val tabHost = view.findViewById(R.id.blacklist_tabhost) as TabHost
        tabHost.setup()

        tabHost.addTab(tabHost.newTabSpec("tab_1")
                .setContent(R.id.blacklist_tab1).setIndicator(context.getString(R.string.autobooster_tab_blacklist)))
        tabHost.addTab(tabHost.newTabSpec("tab_2")
                .setContent(R.id.blacklist_tab2).setIndicator(context.getString(R.string.autobooster_tab_details)))
        tabHost.currentTab = 0

        SetList(view)

        booster_blacklist = view.findViewById(R.id.booster_blacklist) as ListView

        val config_powersavelistClick = OnItemClickListener { parent, view, position, id ->
            ToogleBlackItem((view.findViewById(R.id.ItemText) as TextView).text.toString())
            val checkBox = view.findViewById(R.id.select_state) as CheckBox
            checkBox.isChecked = !checkBox.isChecked
            print(view)
        }
        booster_blacklist.onItemClickListener = config_powersavelistClick
    }

    lateinit internal var booster_blacklist: ListView

    internal val myHandler: Handler = object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
        }
    }

    private fun SetList(view: View) {
        thisview!!.progressBar.visibility = View.VISIBLE

        Thread(Runnable {
            if (installedList == null) {
                LoadList()
            }
            SetListData(installedList!!, booster_blacklist)
        }).start()
    }

    internal fun SetListData(dl: ArrayList<HashMap<String, Any>>, lv: ListView) {
        myHandler.post {
            thisview!!.progressBar.visibility = View.GONE
            lv.adapter = list_adapter(context, dl)
        }
    }

    lateinit internal var packageManager: PackageManager
    internal var installedList: ArrayList<HashMap<String, Any>>? = null

    internal fun LoadList() {
        packageManager = thisview!!.packageManager
        var packageInfos = packageManager.getInstalledApplications(0)

        installedList = ArrayList<HashMap<String, Any>>()/*在数组中存放数据*/
        for (i in packageInfos.indices) {
            var packageInfo = packageInfos[i];
            if (packageInfo.sourceDir.indexOf("/system") == 0)
                continue
            val item = HashMap<String, Any>()
            val d = packageInfo.loadIcon(packageManager)
            item.put("icon", d)
            val pkgName = packageInfo.packageName
            item.put("select_state", false)
            for (blitem in ConfigInfo.getConfigInfo().blacklist) {
                if (blitem == pkgName) {
                    item.put("select_state", true)
                    break
                }
            }
            item.put("name", packageInfo.loadLabel(packageManager))
            item.put("packageName", pkgName)
            installedList!!.add(item)
        }
    }

    internal fun ToogleBlackItem(pkgName: String) {
        try {
            if (!ConfigInfo.getConfigInfo().blacklist.contains(pkgName)) {
                ConfigInfo.getConfigInfo().blacklist.add(pkgName)
            } else {
                ConfigInfo.getConfigInfo().blacklist.remove(pkgName)
            }
        } catch (ex: Exception) {

        }

    }

    override fun onDestroy() {
        if (installedList != null) {
            installedList!!.clear()
            SetListData(installedList!!, booster_blacklist)
        }
        cmdshellTools = null

        super.onDestroy()
    }

    companion object {

        fun Create(thisView: main, cmdshellTools: cmd_shellTools): Fragment {
            val fragment = fragment_booster()
            fragment.cmdshellTools = cmdshellTools
            fragment.thisview = thisView
            return fragment
        }
    }
}
