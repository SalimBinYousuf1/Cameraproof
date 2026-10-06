package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntitlementLogicTest {

    data class EntitlementConfig(
        val isPro: Boolean,
        val projectCount: Int
    ) {
        val canCreateMoreProjects: Boolean = isPro || projectCount < 3
        val showPdfWatermark: Boolean = !isPro
        val allowCustomPdfHeader: Boolean = isPro
    }

    @Test
    fun testFreeTierEntitlement() {
        val freeUser = EntitlementConfig(isPro = false, projectCount = 3)
        assertFalse("Free user at 3 projects cannot create more", freeUser.canCreateMoreProjects)
        assertTrue("Free user PDF must show watermark", freeUser.showPdfWatermark)
        assertFalse("Free user cannot customize header", freeUser.allowCustomPdfHeader)
    }

    @Test
    fun testProTierEntitlement() {
        val proUser = EntitlementConfig(isPro = true, projectCount = 50)
        assertTrue("Pro user can always create more projects", proUser.canCreateMoreProjects)
        assertFalse("Pro user PDF must not show watermark", proUser.showPdfWatermark)
        assertTrue("Pro user can customize header", proUser.allowCustomPdfHeader)
    }
}
