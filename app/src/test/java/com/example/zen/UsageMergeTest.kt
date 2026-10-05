package com.example.zen

import com.example.zen.data.AppUsageItem
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageMergeTest {

    @Test
    fun sumsBothTikTokPackagesIntoOneRow() {
        val merged = mergeUsageByApp(
            listOf(
                item("com.instagram.android", "Instagram", 15),
                item("com.zhiliaoapp.musically", "TikTok", 10),
                item("com.ss.android.ugc.trill", "TikTok", 25),
                item("com.google.android.youtube", "YouTube", 4)
            )
        )
        assertEquals(listOf("Instagram", "TikTok", "YouTube"), merged.map { it.appName })
        assertEquals(15L, merged[0].timeSpentMinutes)
        assertEquals(35L, merged[1].timeSpentMinutes)
        assertEquals("com.zhiliaoapp.musically", merged[1].packageName)
        assertEquals(4L, merged[2].timeSpentMinutes)
    }

    @Test
    fun oneTikTokPackageStaysThatPackage() {
        val merged = mergeUsageByApp(
            listOf(item("com.ss.android.ugc.trill", "TikTok", 8))
        )
        assertEquals(1, merged.size)
        assertEquals(8L, merged[0].timeSpentMinutes)
        assertEquals("com.ss.android.ugc.trill", merged[0].packageName)
    }

    @Test
    fun emptyStaysEmpty() {
        assertEquals(emptyList<AppUsageItem>(), mergeUsageByApp(emptyList()))
    }

    private fun item(pkg: String, name: String, minutes: Long) =
        AppUsageItem(pkg, name, minutes, "#00F2FE")
}
