package com.wingedsheep.mtg.sets.definitions.all.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostZone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Force of Will — Alliances #28 (canonical printing)
 * {3}{U}{U} · Instant
 *
 * You may pay 1 life and exile a blue card from your hand rather than pay this spell's mana cost.
 * Counter target spell.
 *
 * The pitch cost is a [SelfAlternativeCost] of {0} plus two non-mana additional costs — 1 life and
 * one blue card exiled from hand — with no condition, so unlike Force of Negation it works on any
 * turn. Mana value stays 5 whichever cost is paid (ruling).
 */
val ForceOfWill = card("Force of Will") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "You may pay 1 life and exile a blue card from your hand rather than pay this " +
        "spell's mana cost.\nCounter target spell."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        additionalCosts = listOf(
            Costs.additional.PayLife(1),
            Costs.additional.ExileCards(
                count = 1,
                filter = GameObjectFilter.Any.withColor(Color.BLUE),
                fromZone = CostZone.HAND
            )
        )
    )

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "Terese Nielsen"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a879b60-4381-447d-8a5a-8e0b6a1d49ca.jpg?1783947196"
        ruling("2022-12-08", "To determine the total cost of a spell, start with the mana cost or alternative cost you're paying (such as the alternative cost of Force of Will), add any cost increases, then apply any cost reductions. Force of Will's mana value is always 5, no matter what you paid to cast it.")
    }
}
