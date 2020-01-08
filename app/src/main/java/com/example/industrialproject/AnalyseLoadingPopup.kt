package com.example.industrialproject

import com.bumptech.glide.Glide
import android.view.Window.FEATURE_NO_TITLE
import android.app.Activity
import android.app.Dialog
import android.widget.ImageView
import com.bumptech.glide.load.engine.DiskCacheStrategy.NONE
import com.bumptech.glide.request.target.DrawableImageViewTarget


class AnalyseLoadingPopup {

    private var activity: Activity? = null
    private var dialog: Dialog? = null

    constructor(activity: Activity) {
        this.activity = activity
    }

    fun showDialog() {


        dialog = Dialog(activity)
        dialog!!.requestWindowFeature(FEATURE_NO_TITLE)
        dialog!!.window.setBackgroundDrawableResource(android.R.color.transparent)
        dialog!!.setCancelable(false)
        dialog!!.setContentView(R.layout.activity_analyse_loading_popup)

        val gifImageView = dialog!!.findViewById<ImageView>(R.id.loading_gif_popup)

        Glide.with(activity!!).asGif().diskCacheStrategy(NONE).load(R.raw.loading_open_source).into(gifImageView)

        dialog?.show()

    }

    //..also create a method which will hide the dialog when some work is done
    fun hideDialog() {
        dialog?.dismiss()
    }
}