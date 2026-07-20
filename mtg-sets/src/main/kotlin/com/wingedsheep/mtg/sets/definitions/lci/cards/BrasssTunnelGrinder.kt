package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Brass's Tunnel-Grinder // Tecutlan, the Searing Rift (The Lost Caverns of Ixalan)
 * {2}{R}
 * Legendary Artifact // Legendary Land — Cave
 *
 * Front — Brass's Tunnel-Grinder ({2}{R} Legendary Artifact)
 *   When Brass's Tunnel-Grinder enters, discard any number of cards, then draw that many cards
 *   plus one.
 *   At the beginning of your end step, if you descended this turn, put a bore counter on Brass's
 *   Tunnel-Grinder. Then if there are three or more bore counters on it, remove those counters and
 *   transform it.
 *
 * Back — Tecutlan, the Searing Rift (Legendary Land — Cave)
 *   {T}: Add {R}.
 *   Whenever you cast a permanent spell using mana produced by Tecutlan, discover X, where X is
 *   that spell's mana value.
 *
 * Implementation:
 *  - ETB loots via [Patterns.Hand.discardAnyNumber] (stores the discard count under `discarded`),
 *    then draws `discarded_count + 1` ([DynamicAmount.Add] of the stored count and one).
 *  - End-step [Conditions.YouDescendedThisTurn] intervening-if adds a [Counters.BORE] passive
 *    counter; a resolution-time [ConditionalEffect] on [Conditions.SourceCounterCountAtLeast]`(bore,
 *    3)` removes three and flips it (Grasping Shadows' dread idiom).
 *  - Tecutlan's cast trigger uses [SpellCastPredicate.PaidWithManaFromSource] — the mana-source
 *    provenance the engine records for the mana Tecutlan produced — and discovers for the triggering
 *    spell's mana value ([ContextPropertyKey.TRIGGERING_SPELL_MANA_VALUE]).
 */

private val BrasssTunnelGrinderFront = card("Brass's Tunnel-Grinder") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Artifact"
    oracleText = "When Brass's Tunnel-Grinder enters, discard any number of cards, then draw that " +
        "many cards plus one.\n" +
        "At the beginning of your end step, if you descended this turn, put a bore counter on " +
        "Brass's Tunnel-Grinder. Then if there are three or more bore counters on it, remove those " +
        "counters and transform it. (You descended if a permanent card was put into your graveyard " +
        "from anywhere.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            Patterns.Hand.discardAnyNumber(storeAs = "discarded"),
            Effects.DrawCards(
                DynamicAmount.Add(
                    DynamicAmount.VariableReference("discarded_count"),
                    DynamicAmount.Fixed(1),
                )
            ),
        )
        description = "When Brass's Tunnel-Grinder enters, discard any number of cards, then draw " +
            "that many cards plus one."
    }

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.YouDescendedThisTurn()
        effect = Effects.Composite(
            Effects.AddCounters(Counters.BORE, 1, EffectTarget.Self),
            ConditionalEffect(
                condition = Conditions.SourceCounterCountAtLeast(Counters.BORE, 3),
                effect = Effects.Composite(
                    Effects.RemoveCounters(Counters.BORE, 3, EffectTarget.Self),
                    TransformEffect(EffectTarget.Self),
                ),
            ),
        )
        description = "At the beginning of your end step, if you descended this turn, put a bore " +
            "counter on Brass's Tunnel-Grinder. Then if there are three or more bore counters on " +
            "it, remove those counters and transform it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "135"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d61d8895-7f2e-4c77-951f-4f1a49e96f57.jpg?1782694501"
    }
}

private val Tecutlan = card("Tecutlan, the Searing Rift") {
    manaCost = ""
    colorIdentity = "R"
    typeLine = "Legendary Land — Cave"
    oracleText = "{T}: Add {R}.\n" +
        "Whenever you cast a permanent spell using mana produced by Tecutlan, discover X, where X " +
        "is that spell's mana value."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED, 1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    triggeredAbility {
        trigger = Triggers.youCastSpell(
            spellFilter = GameObjectFilter.Permanent,
            requires = setOf(SpellCastPredicate.PaidWithManaFromSource),
        )
        effect = Effects.Discover(DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGERING_SPELL_MANA_VALUE))
        description = "Whenever you cast a permanent spell using mana produced by Tecutlan, " +
            "discover X, where X is that spell's mana value."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "135"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/back/d/6/d61d8895-7f2e-4c77-951f-4f1a49e96f57.jpg?1782694501"
    }
}

val BrasssTunnelGrinder: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = BrasssTunnelGrinderFront,
    backFace = Tecutlan,
)
