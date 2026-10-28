package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.handlers.effects.copy.withCopyIdentity
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.ComponentContainer
import kotlinx.serialization.Serializable

/**
 * This permanent's text box was exchanged with another's (CR 612.5, 701.12h): its rules text is
 * [textOf]'s, everything else — name, mana cost, colors, type line, power/toughness — is still
 * [ownCard]'s.
 *
 * Abilities are read off the definition a [CardComponent] names, so the exchange is installed like
 * a copy identity ([withCopyIdentity]): the permanent's [CardComponent] becomes [ownCard] wearing
 * [textOf]'s rules-text fields ([withRulesTextOf]), and its static and replacement sources are
 * re-derived from that text. This component keeps the two halves apart:
 *
 *  - [ownCard] is the permanent's copiable values (CR 707.2 — a text-changing effect isn't one),
 *    so [copiableCardComponent] returns it, and a copy effect applied later replaces it while the
 *    exchanged text stays on top (layer 1 before layer 3, CR 613.1).
 *  - [ownCard] is also what the card is again once it leaves the battlefield (CR 400.7):
 *    [revertTextBoxExchange] runs before any copy revert.
 *
 * @property ownCard the permanent's own identity, without the exchanged text.
 * @property textOf the card whose rules text it now has, captured as that permanent's rules text was
 *   at the moment of the exchange ("the previous rules text of the other").
 */
@Serializable
data class TextBoxExchangeComponent(
    val ownCard: CardComponent,
    val textOf: CardComponent,
) : Component

/**
 * This card with [donor]'s rules text (CR 612.5): its abilities — the definition they are read
 * from, its keywords and ability flags, its copy-added abilities and the precomputed "has an
 * ability of kind X" flags — and its oracle text. Name, mana cost, colors, type line,
 * power/toughness, owner and art stay this card's.
 */
fun CardComponent.withRulesTextOf(donor: CardComponent): CardComponent = copy(
    cardDefinitionId = donor.cardDefinitionId,
    oracleText = donor.oracleText,
    baseKeywords = donor.baseKeywords,
    baseFlags = donor.baseFlags,
    spellEffect = donor.spellEffect,
    hasNonManaActivatedAbility = donor.hasNonManaActivatedAbility,
    hasActivatedAbility = donor.hasActivatedAbility,
    hasCycling = donor.hasCycling,
    hasKicker = donor.hasKicker,
    copyNumericKeywords = donor.copyNumericKeywords,
    copyStaticAbilities = donor.copyStaticAbilities,
    copyTriggeredAbilities = donor.copyTriggeredAbilities,
    copyActivatedAbilities = donor.copyActivatedAbilities,
    copyWardCosts = donor.copyWardCosts,
    manaSpendingGrants = donor.manaSpendingGrants,
)

/** The permanent's own identity under an exchanged text box, or null when its text is its own. */
fun ComponentContainer.ownCardUnderTextExchange(): CardComponent? = get<TextBoxExchangeComponent>()?.ownCard

/**
 * Give this permanent [donor]'s rules text, keeping everything else its own. A permanent whose text
 * was already exchanged keeps its own identity and takes the new text.
 */
fun ComponentContainer.exchangeRulesTextFor(donor: CardComponent, registry: CardRegistry): ComponentContainer {
    val own = ownCardUnderTextExchange() ?: get<FlippedComponent>()?.unflippedCard ?: get<CardComponent>() ?: return this
    return without<TextBoxExchangeComponent>()
        .with(TextBoxExchangeComponent(ownCard = own, textOf = donor))
        .withCopyIdentity(own, registry)
}

/** End the exchange: the permanent has its own rules text again. No-op when nothing was exchanged. */
fun ComponentContainer.revertTextBoxExchange(registry: CardRegistry): ComponentContainer {
    val exchange = get<TextBoxExchangeComponent>() ?: return this
    return without<TextBoxExchangeComponent>().withCopyIdentity(exchange.ownCard, registry)
}

/**
 * Run [change] — a flip or a transform, which rebuild the permanent's identity from its own card —
 * on [entityId] with its own text, then lay the exchanged text back over the result. The text-changing
 * effect keeps applying to the same object whichever face is up (CR 613.1c sits above the face
 * chosen in layer 1). [change] returning null (nothing to do) leaves the state untouched.
 */
fun <R> GameState.withTextBoxExchangeSuspended(
    entityId: EntityId,
    registry: CardRegistry,
    change: (GameState) -> Pair<GameState, R>?,
): Pair<GameState, R>? {
    val exchange = getEntity(entityId)?.get<TextBoxExchangeComponent>() ?: return change(this)
    val (changed, result) = change(updateEntity(entityId) { it.revertTextBoxExchange(registry) }) ?: return null
    return changed.updateEntity(entityId) { it.exchangeRulesTextFor(exchange.textOf, registry) } to result
}
