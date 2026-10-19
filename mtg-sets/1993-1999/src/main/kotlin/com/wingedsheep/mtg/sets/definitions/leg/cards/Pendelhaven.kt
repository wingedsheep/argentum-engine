package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pendelhaven
 * Legendary Land
 *
 * {T}: Add {G}.
 * {T}: Target 1/1 creature gets +1/+2 until end of turn.
 *
 * "1/1 creature" reads the creature's current (projected) power and toughness, as The Seedcore does.
 */
val Pendelhaven = card("Pendelhaven") {
    typeLine = "Legendary Land"
    colorIdentity = "G"
    oracleText = "{T}: Add {G}.\n{T}: Target 1/1 creature gets +1/+2 until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.power(1).toughnessAtLeast(1).toughnessAtMost(1))
        )
        cost = Costs.Tap
        effect = Effects.ModifyStats(1, 2, creature)
        description = "{T}: Target 1/1 creature gets +1/+2 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "305"
        artist = "Bryon Wackwitz"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79427109-c1f3-476d-a029-0049217237b5.jpg?1783948022"
    }
}
