package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
/**
 * Dauntless Scrapbot
 * {3}
 * Artifact Creature — Robot
 * When this creature enters, exile each opponent's graveyard. Create a Lander token. (It's an artifact with "{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.")
 * 3/1
 */
val DauntlessScrapbot = card("Dauntless Scrapbot") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Robot"
    power = 3
    toughness = 1
    oracleText = "When this creature enters, exile each opponent's graveyard. Create a Lander token. (It's an artifact with \"{2}, {T}, Sacrifice this token: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.\")"

    // ETB: exile each opponent's graveyard and create a Lander token
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(listOf(
            Effects.ExileOpponentsGraveyards(),
            Effects.CreateLander()
        ))
        description = "When this creature enters, exile each opponent's graveyard. Create a Lander token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "237"
        artist = "Alix Branwyn"
        flavorText = "The wastelands of yesterday's colonies will pave the road to tomorrow's."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0efe5342-42b7-4f49-b4b4-d77055508c4d.jpg?1752947526"
    }
}
