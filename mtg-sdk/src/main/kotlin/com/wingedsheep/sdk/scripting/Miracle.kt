package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Miracle (CR 702.94) as a composable, content-agnostic primitive — the mirror of [Madness].
 *
 * CR 702.94a spells the keyword out as two abilities:
 *  - **Static, functioning in hand** — "you may reveal this card from your hand as you draw it if
 *    it's the first card you've drawn this turn." The engine asks the card's owner as the card is
 *    drawn, pausing the draw between cards. The question is private: it exists only because the
 *    card has miracle, so its owner's opponents see neither the question nor a declined answer.
 *  - **Triggered, functioning in hand** — "when you reveal this card this way, you may cast it by
 *    paying [cost] rather than its mana cost." That is [castAbility] below, the single triggered
 *    ability the engine synthesizes for a card revealed this way. It uses the stack, so it can be
 *    responded to.
 *
 * Miracle is either printed ([KeywordAbility.Miracle]) or granted to cards in hand
 * ([GrantMiracleToCardsInHand]); the engine resolves both through one lookup, so the reveal
 * question, the trigger and the cast behave identically for each.
 *
 * Because the cast happens while the trigger resolves (CR 608.2g), the spell's normal timing
 * restrictions don't apply: a miracle *sorcery* can be cast in the draw step or on an opponent's
 * turn, and the miracle cost is available at no other time.
 */
object Miracle {

    /** Pipeline collection key the cast offer uses to hand the revealed card to the cast step. */
    const val CAST_COLLECTION: String = "miracle_cast"

    /**
     * The synthesized triggered ability the engine puts on the stack for a card revealed for its
     * miracle. Functions only in hand.
     *
     * `sourceRequiredZone = HAND` is CR 702.94a's object rule in practice: if the card has left the
     * hand by the time the trigger resolves (cast normally in response, discarded), the ability
     * does nothing and nothing is asked. The cast announces miracle as its alternative cost
     * ([CastFromCollectionWithoutPayingCostEffect.castUsing]), so the cast handler prices it at
     * the miracle cost with every cost modifier applied.
     *
     * [cost] only shapes the prompt text — the charge comes from the card's effective miracle
     * ability as it is cast.
     */
    fun castAbility(cost: ManaCost): TriggeredAbility = TriggeredAbility(
        id = AbilityId("miracle_cast"),
        trigger = EventPattern.CardRevealedFromDrawEvent(),
        binding = TriggerBinding.SELF,
        activeZones = setOf(Zone.HAND),
        effect = Effects.May(
            CompositeEffect(
                listOf(
                    GatherCardsEffect(CardSource.Entity(EffectTarget.Self), storeAs = CAST_COLLECTION),
                    CastFromCollectionWithoutPayingCostEffect(
                        from = CAST_COLLECTION,
                        payManaCost = true,
                        castUsing = Keyword.MIRACLE,
                    ),
                )
            ),
            sourceRequiredZone = Zone.HAND,
            descriptionOverride = "cast it for its miracle cost $cost",
        ),
        descriptionOverride = "Miracle $cost — When you reveal this card as the first card you drew " +
            "this turn, you may cast it for its miracle cost.",
    )
}
