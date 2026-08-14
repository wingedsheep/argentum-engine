package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Seize the Storm
 * {4}{R}
 * Sorcery
 *
 * Creates an Elemental whose characteristic-defining ability continuously counts instant and
 * sorcery cards in its controller's graveyard plus cards with flashback they own in exile.
 * Flashback {6}{R}.
 */
val SeizeTheStorm = card("Seize the Storm") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Create a red Elemental creature token with trample and \"This token's power and " +
        "toughness are each equal to the number of instant and sorcery cards in your graveyard plus " +
        "the number of cards with flashback you own in exile.\"\n" +
        "Flashback {6}{R} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    val stormCount = DynamicAmount.Add(
        DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.InstantOrSorcery),
        DynamicAmount.Count(
            Player.You,
            Zone.EXILE,
            GameObjectFilter.Any.withKeyword(Keyword.FLASHBACK).ownedByYou()
        )
    )

    spell {
        effect = Effects.CreateToken(
            power = 0,
            toughness = 0,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Elemental"),
            keywords = setOf(Keyword.TRAMPLE),
            imageUri = "https://cards.scryfall.io/normal/front/c/4/c4052aed-981b-41d0-85f0-20c2599811ba.jpg?1783925224",
            staticAbilities = listOf(SetBasePowerToughnessDynamicStatic(stormCount, stormCount))
        )
    }

    keywordAbility(KeywordAbility.flashback("{6}{R}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "158"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/532e079f-55b7-4b92-b489-aed4d3becae7.jpg?1783925591"

        ruling("2021-09-24", "The power and toughness of the Elemental token will change as the number of instant and sorcery cards in your graveyard and the number of cards with flashback you own in exile change.")
        ruling("2021-09-24", "If you cast Seize the Storm with no instant or sorcery cards in your graveyard and no cards with flashback in exile, the Elemental will survive because Seize the Storm will be in your graveyard or in exile before state-based actions are checked.")
    }
}
