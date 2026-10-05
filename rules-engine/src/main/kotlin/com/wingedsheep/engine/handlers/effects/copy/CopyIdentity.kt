package com.wingedsheep.engine.handlers.effects.copy

import com.wingedsheep.engine.core.CardEntityFactory
import com.wingedsheep.engine.mechanics.layers.ContinuousEffectSourceComponent
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.*

/** Install copiable identity without changing status; a retained flipped status selects its alternative. */
fun ComponentContainer.withCopyIdentity(upright: CardComponent, registry: CardRegistry): ComponentContainer {
    val flipped = has<FlippedComponent>()
    val active = (if (flipped) upright.flipSide ?: upright else upright)
        .copy(ownerId = upright.ownerId, isDoubleFaced = upright.isDoubleFaced)
    val statics = StaticAbilityHandler(registry)
    val previous = get<CardComponent>()
    val previousDefinition = previous?.let { registry.getCard(it.cardDefinitionId) }
    val oldDecorations = if (previous != null && previousDefinition != null) {
        statics.addContinuousEffectComponent(
            CardEntityFactory.applyDefinitionDecorations(ComponentContainer.of(previous), previousDefinition),
            previousDefinition,
        ).all().map { it::class.java }.filterNot { it == CardComponent::class.java }.toSet()
    } else emptySet()
    var result = ComponentContainer(components - oldDecorations).with(active)
        .without<ContinuousEffectSourceComponent>()
        .without<ReplacementEffectSourceComponent>()
        .without<ToxicComponent>()
        .without<NumericKeywordValuesComponent>()
    if (flipped) result = result.with(FlippedComponent(upright))
    val definition = registry.getCard(active.cardDefinitionId)
    if (definition != null) {
        result = CardEntityFactory.applyDefinitionDecorations(result, definition)
        result = statics.addContinuousEffectComponent(result, definition)
        result = statics.addReplacementEffectComponent(result, definition)
    } else {
        result = result.without<ProtectionComponent>().without<SelfZoneRedirectComponent>()
        result = CardEntityFactory.applyNumericKeywords(result, active.copyNumericKeywords)
    }
    return result
}

/** Remove expired layers, then select the exposed identity using the permanent's current status. */
fun ComponentContainer.expireCopyLayers(
    registry: CardRegistry,
    expires: (CopyIdentityLayer) -> Boolean,
): ComponentContainer {
    val history = get<CopyHistoryComponent>()
    if (history == null) {
        val original = get<CopyOfComponent>()?.originalCardComponent ?: return this
        return withCopyIdentity(original, registry).without<CopyOfComponent>()
    }
    val remaining = history.layers.filterNot(expires)
    if (remaining.size == history.layers.size) return this
    if (remaining.isEmpty()) {
        var result = withCopyIdentity(history.baseCard, registry).without<CopyHistoryComponent>()
        result = if (history.baseCopy == null) result.without<CopyOfComponent>() else result.with(history.baseCopy)
        return result
    }
    var result = withCopyIdentity(remaining.last().card, registry).with(history.copy(layers = remaining))
    get<CopyOfComponent>()?.let { result = result.with(it.copy(copiedCardDefinitionId = remaining.last().card.cardDefinitionId)) }
    return result
}

/** The expiry hooks already own state transitions; publish only identities actually exposed. */
fun copyExpiryEvents(before: com.wingedsheep.engine.state.GameState, after: com.wingedsheep.engine.state.GameState): List<com.wingedsheep.engine.core.GameEvent> =
    before.getBattlefield().mapNotNull { id ->
        val previous = before.getEntity(id) ?: return@mapNotNull null
        if (!previous.has<CopyHistoryComponent>() && !previous.has<RevertCopyAtEndOfTurnComponent>() &&
            !previous.has<RevertCopyAtNextEndStepComponent>() && !previous.has<RevertCopyAtYourNextTurnComponent>()) return@mapNotNull null
        if (previous.get<CardComponent>() == after.getEntity(id)?.get<CardComponent>()) null
        else com.wingedsheep.engine.core.CopiableCharacteristicsChangedEvent(id)
    }
