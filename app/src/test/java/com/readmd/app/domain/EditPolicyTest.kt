package com.readmd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditPolicyTest {

    @Test
    fun `text is dirty only while editing and different from the saved text`() {
        assertTrue(EditPolicy.isDirty(editing = true, draft = "new", saved = "old"))
        assertFalse(EditPolicy.isDirty(editing = true, draft = "same", saved = "same"))
        assertFalse(EditPolicy.isDirty(editing = false, draft = "new", saved = "old"))
    }

    @Test
    fun `an empty draft over a non empty file counts as a change`() {
        assertTrue(EditPolicy.isDirty(editing = true, draft = "", saved = "text"))
        assertTrue(EditPolicy.isDirty(editing = true, draft = "text", saved = null))
    }

    @Test
    fun `closing with nothing unsaved just leaves`() {
        assertEquals(LeaveAction.LEAVE, EditPolicy.leaveAction(dirty = false, autosave = true))
        assertEquals(LeaveAction.LEAVE, EditPolicy.leaveAction(dirty = false, autosave = false))
    }

    @Test
    fun `closing with unsaved text saves first when autosave is on`() {
        assertEquals(LeaveAction.SAVE_AND_LEAVE, EditPolicy.leaveAction(dirty = true, autosave = true))
    }

    @Test
    fun `closing with unsaved text asks when autosave is off`() {
        assertEquals(LeaveAction.ASK, EditPolicy.leaveAction(dirty = true, autosave = false))
    }

    @Test
    fun `autosave waits for a pause in typing`() {
        assertTrue(EditPolicy.AUTOSAVE_IDLE_MS in 500L..5_000L)
    }
}
