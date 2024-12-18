package com.example.addition

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TodoModel (
    var input: String,
    var result: String,
    var isFinished: Int = -1,
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
 )
