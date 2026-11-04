package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters

val SunsetPyramid = card("Sunset Pyramid") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "This artifact enters with three brick counters on it.\n" +
        "{2}, {T}, Remove a brick counter from this artifact: Draw a card.\n" +
        "{2}, {T}: Scry 1."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.BRICK,
            count = 3,
            selfOnly = true
        )
    )

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.RemoveCounterFromSelf(CounterType.BRICK))
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "166"
        artist = "Jonas De Ro"
        flavorText = "Decades to build. Hours to demolish."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/377a4e80-0f28-42a2-9972-47ab66baee63.jpg?1783936000"
    }
}
