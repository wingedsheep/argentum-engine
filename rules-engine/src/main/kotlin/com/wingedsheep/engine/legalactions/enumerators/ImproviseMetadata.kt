package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.TapForGenericPermanentData
import com.wingedsheep.engine.mechanics.mana.TapForGeneric
import com.wingedsheep.engine.mechanics.mana.spellPaymentContextFor
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId

/**
 * Post-process: surface **improvise** (CR 702.126) on the cast actions already enumerated.
 *
 * Improvise is neither an additional nor an alternative cost (CR 702.126b), so — unlike the
 * waterbend pass — this adds no second action and changes no cost: it only attaches the
 * tap-to-help metadata (eligible untapped artifacts, the "improvise" label, no cap beyond the
 * generic in the cost) so the client can offer the payment. Doing it here rather than at each
 * `LegalAction(...)` emission site means every cast shape — plain, modal, kicked, or-pay,
 * split — gets it for free. Shared by the hand casts ([CastSpellEnumerator]) and the casts from
 * other zones ([CastFromZoneEnumerator]).
 *
 * The keyword is resolved through the granted-keyword resolver, so a spell that only has
 * improvise because of Ironheart, Clever Champion is covered identically to a printed one.
 * Actions that already carry a tap-for-generic payment (a waterbend cost) are left alone —
 * one tap payment per action, and no card has both.
 *
 * Also stamps [LegalAction.tapForGenericRequired] — whether the taps are *needed* or merely
 * offered. That costs one extra `canPay` per improvise-eligible cast, which is why it is
 * computed behind the two gates above (no untapped artifacts, or no improvise → no call).
 */
internal fun applyImproviseMetadata(
    context: EnumerationContext,
    actions: List<LegalAction>
): List<LegalAction> {
    val state = context.state
    // Both lookups scan the battlefield, so memoize: the artifacts per caster, and the keyword
    // answer per (caster, card) — a hand of modal/kicked variants otherwise re-asks the same
    // question for every emitted action. Keyed by the card, not its definition: a zone-scoped
    // grant ("spells you cast from exile …") can answer differently for two copies.
    val artifactsByPlayer = mutableMapOf<EntityId, List<TapForGenericPermanentData>>()
    val hasImproviseByCard = mutableMapOf<Pair<EntityId, EntityId>, Boolean>()
    return actions.map { la ->
        val cs = la.action as? CastSpell
        if (cs == null || la.hasTapForGeneric) return@map la
        // Cheapest gate first: with no untapped artifacts there is nothing to offer either way.
        val artifacts = artifactsByPlayer.getOrPut(cs.playerId) {
            context.costUtils.findTapForGenericPermanents(state, cs.playerId, TapForGeneric.IMPROVISE)
        }
        if (artifacts.isEmpty()) return@map la
        val cardComponent = state.getEntity(cs.cardId)?.get<CardComponent>() ?: return@map la
        val cardDef = context.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return@map la
        val hasImprovise = hasImproviseByCard.getOrPut(cs.playerId to cs.cardId) {
            context.grantedKeywordResolver.hasKeyword(state, cs.playerId, cardDef, Keyword.IMPROVISE, cs.cardId)
        }
        if (!hasImprovise) return@map la
        // Are the taps needed, or just offered? Improvise is optional (CR 702.126a "you may"),
        // and an automatic payer that always fills it can tap a mana rock for {1} that was
        // worth more as mana and make its own cast unpayable — see [LegalAction.tapForGenericRequired].
        val payableWithManaAlone = la.manaCostString?.let { costString ->
            context.manaSolver.canPay(
                state, cs.playerId, ManaCost.parse(costString),
                spellContext = spellPaymentContextFor(cardComponent),
                precomputedSources = context.availableManaSources
            )
        } ?: false
        la.copy(
            hasTapForGeneric = true,
            tapForGenericPermanents = artifacts,
            // No cap: CR 702.126a bounds the taps at the generic mana in the total cost, which
            // the client derives from the cost itself.
            tapForGenericAmount = null,
            tapForGenericLabel = TapForGeneric.IMPROVISE.label,
            tapForGenericRequired = !payableWithManaAlone
        )
    }
}
