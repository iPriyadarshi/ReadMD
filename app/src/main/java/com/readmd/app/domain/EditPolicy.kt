package com.readmd.app.domain

/** What closing the editor should do, given the autosave setting and any unsaved text. */
enum class LeaveAction {
    /** Nothing is unsaved; just close. */
    LEAVE,

    /** Save the text, then close (closing is cancelled if saving fails so nothing is lost). */
    SAVE_AND_LEAVE,

    /** Ask whether to save, discard or keep editing. */
    ASK,
}

object EditPolicy {
    /** How long typing must pause before autosave writes the file. */
    const val AUTOSAVE_IDLE_MS = 1_500L

    fun isDirty(editing: Boolean, draft: String, saved: String?): Boolean = editing && draft != saved

    fun leaveAction(dirty: Boolean, autosave: Boolean): LeaveAction = when {
        !dirty -> LeaveAction.LEAVE
        autosave -> LeaveAction.SAVE_AND_LEAVE
        else -> LeaveAction.ASK
    }
}
