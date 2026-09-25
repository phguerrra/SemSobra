package com.project.semsobra.ui.model

sealed interface UiEvent {
    data class ShowMessage(val message: UiMessage) : UiEvent
}

sealed interface UiMessage {
    val text: String

    data class Validation(override val text: String) : UiMessage
    data class Conflict(override val text: String) : UiMessage
    data class Persistence(override val text: String) : UiMessage
    data class Success(override val text: String) : UiMessage
    data class NotFound(override val text: String) : UiMessage
}
