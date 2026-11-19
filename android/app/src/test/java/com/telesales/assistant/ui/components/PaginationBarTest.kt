package com.telesales.assistant.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PaginationBarTest {

    @Test
    fun buildPageSlots_smallTotalPages_showsAllPages() {
        assertEquals(listOf(1, 2, 3), buildPageSlots(currentPage = 1, lastPage = 3))
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), buildPageSlots(currentPage = 4, lastPage = 7))
    }

    @Test
    fun buildPageSlots_nearBeginning_showsEllipsisBeforeEnd() {
        // e.g. page 1 of 50 -> 1, 2, ..., 50
        val slots = buildPageSlots(currentPage = 1, lastPage = 50)
        assertEquals(listOf(1, 2, -1, 50), slots)

        // page 2 of 50 -> 1, 2, 3, ..., 50
        val slots2 = buildPageSlots(currentPage = 2, lastPage = 50)
        assertEquals(listOf(1, 2, 3, -1, 50), slots2)
    }

    @Test
    fun buildPageSlots_inMiddle_showsEllipsisBothSides() {
        // page 12 of 50 -> 1, ..., 11, 12, 13, ..., 50
        val slots = buildPageSlots(currentPage = 12, lastPage = 50)
        assertEquals(listOf(1, -1, 11, 12, 13, -1, 50), slots)
    }

    @Test
    fun buildPageSlots_nearEnd_showsEllipsisAfterBeginning() {
        // page 50 of 50 -> 1, ..., 49, 50
        val slots = buildPageSlots(currentPage = 50, lastPage = 50)
        assertEquals(listOf(1, -1, 49, 50), slots)

        // page 49 of 50 -> 1, ..., 48, 49, 50
        val slots49 = buildPageSlots(currentPage = 49, lastPage = 50)
        assertEquals(listOf(1, -1, 48, 49, 50), slots49)
    }
}

