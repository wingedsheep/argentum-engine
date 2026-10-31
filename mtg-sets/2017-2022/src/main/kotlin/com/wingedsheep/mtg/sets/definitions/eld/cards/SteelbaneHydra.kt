package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Steelbane Hydra
 * {X}{G}{G}
 * Creature — Turtle Hydra
 * 0/0
 *
 * This creature enters with X +1/+1 counters on it.
 * {2}{G}, Remove a +1/+1 counter from this creature: Destroy target artifact or enchantment.
 */
val SteelbaneHydra = card("Steelbane Hydra") {
    manaCost = "{X}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Turtle Hydra"
    oracleText = "This creature enters with X +1/+1 counters on it.\n" +
        "{2}{G}, Remove a +1/+1 counter from this creature: Destroy target artifact or enchantment."
    power = 0
    toughness = 0

    replacementEffect(EntersWithDynamicCounters(count = DynamicAmounts.xValue()))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{G}"),
            Costs.RemoveCounterFromSelf(CounterType.PLUS_ONE_PLUS_ONE, 1),
        )
        val t = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "322"
        artist = "Nicholas Gregory"
        flavorText = "Many an ancient treasure has been reduced to jagged caltrops by its snapping jaws."
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91d0e54d-6e18-44a5-adc2-d875d5b6784f.jpg"
    }
}
