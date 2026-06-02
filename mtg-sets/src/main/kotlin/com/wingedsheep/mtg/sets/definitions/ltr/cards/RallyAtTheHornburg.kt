package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rally at the Hornburg
 * {1}{R}
 * Sorcery
 *
 * Create two 1/1 white Human Soldier creature tokens. Humans you control gain haste until end of turn.
 */
val RallyAtTheHornburg = card("Rally at the Hornburg") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Create two 1/1 white Human Soldier creature tokens. Humans you control gain haste until end of turn."

    spell {
        effect = Effects.Composite(
            listOf(
                Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Human", "Soldier"),
                    count = 2,
                    imageUri = "https://cards.scryfall.io/normal/front/a/6/a6181330-7521-4ec6-be6c-b35487c2d2d4.jpg?1699974464"
                ),
                Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.youControl().withSubtype("Human")),
                    GrantKeywordEffect(Keyword.HASTE, EffectTarget.Self, Duration.EndOfTurn)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "142"
        artist = "Ekaterina Burmak"
        flavorText = "With a cry and a great noise they charged. Down from the gates they roared, over the causeway they swept, and they drove through the hosts of Isengard as a wind among grass."
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee7292f7-1c7e-449c-9c52-7584d6a14c2c.jpg?1686969106"
    }
}
