package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop

/**
 * Azusa, Lost but Seeking
 * {2}{G}
 * Legendary Creature — Human Monk
 * 1/2
 *
 * You may play two additional lands on each of your turns.
 *
 * The static [GrantAdditionalLandDrop] with `count = 2`, cumulative with other such effects.
 */
val AzusaLostButSeeking = card("Azusa, Lost but Seeking") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Monk"
    power = 1
    toughness = 2
    oracleText = "You may play two additional lands on each of your turns."

    staticAbility {
        ability = GrantAdditionalLandDrop(count = 2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "201"
        artist = "Todd Lockwood"
        flavorText = "\"I do not miss Jukai Forest. It is not my home. My home is Kamigawa, its people my family. Wherever I set my pack and rest my head, I am home.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9ed862a2-d3e2-4543-81a6-453a96399d14.jpg?1783944292"

        ruling(
            "2020-06-23",
            "Azusa's ability is cumulative with other effects that allow you to play additional lands, " +
                "such as that of Song of Creation (from the Ikoria: Lair of Behemoths set)."
        )
    }
}
