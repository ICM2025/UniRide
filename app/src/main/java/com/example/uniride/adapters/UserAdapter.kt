package com.example.uniride.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.BaseAdapter
import com.example.uniride.R
import com.example.uniride.model.User


class UserAdapter(private val context: Context, private val users: List<User>) : BaseAdapter() {

    override fun getCount(): Int = users.size

    override fun getItem(position: Int): Any = users[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.comment_item, parent, false)
        val user = users[position]

        val userImage = view.findViewById<ImageView>(R.id.imageUser)
        val userName = view.findViewById<TextView>(R.id.UserName)
        val userComment = view.findViewById<TextView>(R.id.Commentary)

        userImage.setImageResource(user.imageResId)
        userName.text = user.name
        userComment.text = user.Comment

        return view
    }
}