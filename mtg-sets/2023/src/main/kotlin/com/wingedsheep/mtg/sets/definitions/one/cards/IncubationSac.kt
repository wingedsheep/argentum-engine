package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Incubation Sac
 * {G}
 * Artifact
 *
 * This artifact enters with three oil counters on it.
 * {4}, {T}, Remove an oil counter from this artifact: Create a 3/3 colorless Phyrexian Golem
 * artifact creature token. Activate only as a sorcery.
 */
val IncubationSac = card("Incubation Sac") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Artifact"
    oracleText = "This artifact enters with three oil counters on it.\n" +
        "{4}, {T}, Remove an oil counter from this artifact: Create a 3/3 colorless Phyrexian Golem " +
        "artifact creature token. Activate only as a sorcery."

    replacementEffect(EntersWithCounters(counterType = CounterType.OIL, count = 3, selfOnly = true))

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{4}"),
            Costs.Tap,
            Costs.RemoveCounterFromSelf(CounterType.OIL, 1),
        )
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            creatureTypes = setOf("Phyrexian", "Golem"),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/6/3/63ace2fa-8cfb-4641-a05c-d12830378e03.jpg?1783918166",
        )
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "171"
        artist = "Tony Foti"
        flavorText = "\"This one needs a bit longer to ripen.\"\n—Glissa Sunslayer"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6b36e23-a414-400c-a3a5-e23883e866d3.jpg?1783918015"
    }
}
