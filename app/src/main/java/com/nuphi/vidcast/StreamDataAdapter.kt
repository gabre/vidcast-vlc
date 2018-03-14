package com.nuphi.vidcast

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.nuphi.vidcast.task.XmlParserTask.Companion.StreamData
import android.view.LayoutInflater
import com.nuphi.vidcast.vlc.CastListener
import kotlinx.android.synthetic.main.item.view.*
import pl.droidsonroids.casty.Casty


/**
 * Created by gabre on 2/2/18.
 */
// TODO is it idiomatic to pass actvities?
class StreamDataAdapter(val casty: Casty, context: Context, val mainActivity: StreamListActivity, listItems: List<StreamData>) : ArrayAdapter<StreamData>(context,
        android.R.layout.simple_list_item_1,
        listItems) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val sData = getItem(position)

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val sV = inflater.inflate(R.layout.item, null)

        sV.name.text = sData.title
        sV.description.text = sData.url.url

        sV.setOnClickListener(CastListener(casty, mainActivity, sData))

        return sV
    }
}
