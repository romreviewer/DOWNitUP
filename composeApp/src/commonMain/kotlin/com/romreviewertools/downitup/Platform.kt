package com.romreviewertools.downitup

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform