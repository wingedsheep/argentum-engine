package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Madness (CR 702.35) as a composable, content-agnostic primitive.
 *
 * Madness is a **hand-and-exile** mechanic. CR 702.35a spells the keyword out as two abilities:
 *  - **Static, functioning in hand** — "if a player would discard this card, that player discards
 *    it, but exiles it instead of putting it into their graveyard." The engine applies this as a
 *    card-intrinsic zone-change replacement in the discard path, so it holds for *every* discard:
 *    an opponent's Mind Rot, a cycling cost, the cleanup-step hand-size discard. The card still
 *    counts as discarded, so "whenever you discard" payoffs still see it.
 *  - **Triggered, functioning on that exile** — "when this card is exiled this way, its owner may
 *    cast it by paying [cost] rather than paying its mana cost. If that player doesn't, they put
 *    this card into their graveyard." That is [castAbility] below, the single triggered ability the
 *    engine synthesizes for a card exiled by the madness replacement.
 *
 * Like Suspend and Paradigm, the mechanic lives off a **marker component** stamped as the card
 * lands in exile rather than off printed script, so the same machinery would serve a future effect
 * that *grants* madness. The marker also carries the madness cost, which the engine surfaces as a
 * fixed alternative mana cost on the exiled card — that is what makes the ordinary
 * [CastFromCollectionWithoutPayingCostEffect] (with `payManaCost = true`) charge the madness cost
 * instead of the printed one, following the alternative-cost rules of CR 702.35b / 601.2b.
 *
 * Because the cast happens synchronously while the trigger resolves — exactly like Cascade — the
 * spell's normal timing restrictions don't apply: a discarded madness *sorcery* can be cast during
 * an opponent's turn.
 */
object Madness {

    /** Pipeline collection key the cast offer uses to hand the exiled card to the cast step. */
    const val CAST_COLLECTION: String = "madness_cast"

    /**
     * The synthesized triggered ability the engine puts on the stack for a card exiled by the
     * madness discard replacement. Functions only in exile.
     *
     * The trailing [MoveToZoneEffect] is CR 702.35a's "if that player doesn't, they put this card
     * into their graveyard": its `fromZone = EXILE` gate makes it a no-op whenever the card is no
     * longer in exile — which is exactly the case when the cast succeeded and the card is on the
     * stack, and also the safe answer if some other effect moved it in the meantime. So the same
     * one step covers "declined", "couldn't pay", and "cast it" without a separate outcome gate.
     *
     * [cost] only shapes the prompt text — the actual charge comes from the fixed alternative mana
     * cost stamped on the exiled card — but spelling it out matters: the yes/no is the player's one
     * chance to take the card, and "cast it for {R}" is a decision they can make without going to
     * look the card up.
     */
    fun castAbility(cost: ManaCost): TriggeredAbility = TriggeredAbility(
        id = AbilityId("madness_cast"),
        trigger = EventPattern.ZoneChangeEvent(
            filter = GameObjectFilter.Any,
            from = Zone.HAND,
            to = Zone.EXILE
        ),
        binding = TriggerBinding.SELF,
        activeZones = setOf(Zone.EXILE),
        effect = CompositeEffect(
            listOf(
                MayEffect(
                    CompositeEffect(
                        listOf(
                            GatherCardsEffect(CardSource.Self, storeAs = CAST_COLLECTION),
                            CastFromCollectionWithoutPayingCostEffect(
                                from = CAST_COLLECTION,
                                payManaCost = true,
                            ),
                        )
                    ),
                    descriptionOverride = "cast it for its madness cost $cost",
                ),
                MoveToZoneEffect(EffectTarget.Self, Zone.GRAVEYARD, fromZone = Zone.EXILE),
            )
        ),
        descriptionOverride = "When this card is discarded into exile, its owner may cast it for " +
            "its madness cost $cost. If they don't, they put it into their graveyard.",
    )
}
