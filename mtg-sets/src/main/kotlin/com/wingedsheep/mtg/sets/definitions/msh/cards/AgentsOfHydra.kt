package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Agents of HYDRA
 * {1}{B}
 * Creature — Human Spy Villain
 * 1/1
 * When this creature dies, create a 2/1 black Villain creature token with menace.
 */
val AgentsOfHydra = card("Agents of HYDRA") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Spy Villain"
    oracleText = "When this creature dies, create a 2/1 black Villain creature token with menace. (It can't be blocked except by two or more creatures.)"
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf(Subtype.VILLAIN.value),
            keywords = setOf(Keyword.MENACE),
            imageUri = "https://cards.scryfall.io/normal/front/4/a/4a51b6a0-9a54-4f01-b959-0a28c15d103f.jpg?1783902804"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Wero Gallo"
        flavorText = "\"Hail HYDRA! Immortal HYDRA! We shall never be destroyed! Cut off one head and two more shall take its place! We serve none but the Master—as the world shall soon serve us! Hail HYDRA!\""
        imageUri = "https://cards.scryfall.io/normal/front/8/5/857fef2e-df1f-4ec6-a262-f6fa52389cf9.jpg?1783902948"
    }
}
