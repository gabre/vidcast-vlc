package com.nuphi.vidcast

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_stream_list.*
import kotlinx.android.synthetic.main.content_stream_list.*
import android.view.View
import com.nuphi.vidcast.task.XmlParserTask.Companion.StreamData
import com.nuphi.vidcast.common.Types
import com.nuphi.vidcast.task.DownloadTask
import com.nuphi.vidcast.task.XmlParserTask
import pl.droidsonroids.casty.Casty

class StreamListActivity : AppCompatActivity() {
    private val listXml = "https://onlinestream.hu/list.xml"
    private val listItems = ArrayList<StreamData>()

    private lateinit var casty: Casty
    private val adapter by lazy {
        StreamDataAdapter(casty,this,this, listItems)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream_list)
        setSupportActionBar(toolbar)

        casty = Casty.create(this).withMiniController()
        itemListView.adapter = adapter

        DownloadTask({ XmlParserTask(this::addItem, {}, this::reportError).execute(it) }, this::reportError).execute(listXml)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        casty.addMediaRouteMenuItem(menu)
        getMenuInflater().inflate(R.menu.menu_stream_list, menu);
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun reportError(err: Types.VidCastError) {
        val rootView = this.getWindow().getDecorView().findViewById<View>(android.R.id.content)
        Snackbar.make(rootView, err.description, Snackbar.LENGTH_LONG).show()
    }

    private fun addItem(item: StreamData) {
        listItems.add(item);
        adapter.notifyDataSetChanged();
    }
}
