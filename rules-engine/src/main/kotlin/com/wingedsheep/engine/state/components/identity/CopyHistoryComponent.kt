package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import kotlinx.serialization.Serializable

/** Identity layers below a temporary copy; status remains on the permanent independently. */
@Serializable
data class CopyHistoryComponent(
    val baseCard: CardComponent,
    val baseCopy: CopyOfComponent?,
    val layers: List<CopyIdentityLayer>,
) : Component

@Serializable
data class CopyIdentityLayer(
    val card: CardComponent,
    val duration: Duration,
    val controllerId: EntityId? = null,
    val attachmentId: EntityId? = null,
)

/** Start a layer only when expiry can expose an earlier identity. Permanent copies otherwise bake in. */
fun ComponentContainer.recordCopyLayer(
    copied: CardComponent,
    duration: Duration,
    controllerId: EntityId? = null,
    attachmentId: EntityId? = null,
): ComponentContainer {
    // A later permanent layer can never expose any earlier identity; discard its shadowed history.
    if (duration !in setOf(Duration.EndOfTurn, Duration.UntilNextEndStep,
            Duration.UntilYourNextTurn, Duration.WhileSourceAttachedToAffected)) {
        return without<CopyHistoryComponent>().without<RevertCopyAtEndOfTurnComponent>()
            .without<RevertCopyAtNextEndStepComponent>().without<RevertCopyAtYourNextTurnComponent>()
            .without<CopyWhileAttachedComponent>()
    }
    val existing = get<CopyHistoryComponent>()
    val current = get<FlippedComponent>()?.unflippedCard ?: get<CardComponent>() ?: return this
    val currentCopy = get<CopyOfComponent>()
    val history = existing ?: run {
        // Entry copies can already have an expiry marker before the first battlefield recopy.
        val previousDuration = when {
            has<RevertCopyAtEndOfTurnComponent>() -> Duration.EndOfTurn
            has<RevertCopyAtNextEndStepComponent>() -> Duration.UntilNextEndStep
            has<RevertCopyAtYourNextTurnComponent>() -> Duration.UntilYourNextTurn
            has<CopyWhileAttachedComponent>() -> Duration.WhileSourceAttachedToAffected
            else -> null
        }
        if (previousDuration != null && currentCopy?.originalCardComponent != null) {
            CopyHistoryComponent(currentCopy.originalCardComponent, null, listOf(CopyIdentityLayer(
                current, previousDuration, get<RevertCopyAtYourNextTurnComponent>()?.playerId,
                get<CopyWhileAttachedComponent>()?.attachmentId)))
        } else CopyHistoryComponent(current, currentCopy, emptyList())
    }
    return with(history.copy(layers = history.layers + CopyIdentityLayer(copied, duration, controllerId, attachmentId)))
}
