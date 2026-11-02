package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Crackling Drake
 * {U}{U}{R}{R}
 * Creature — Drake
 * Power/toughness: star/4
 *
 * Flying
 * Crackling Drake's power is equal to the total number of instant and sorcery cards you own in
 * exile and in your graveyard.
 * When this creature enters, draw a card.
 *
 * Enigma Drake's characteristic-defining `dynamicPower(...)` over Beacon Bolt's count: exile zones
 * are keyed by owner, so `count(Player.You, Zone.EXILE)` is "cards you own in exile". The filter is
 * `faceUp()` because face-down exiled cards have no characteristics (ruling 2018-10-05).
 */
val CracklingDrake = card("Crackling Drake") {
    manaCost = "{U}{U}{R}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Drake"
    toughness = 4
    oracleText = "Flying\n" +
        "Crackling Drake's power is equal to the total number of instant and sorcery cards you own in " +
        "exile and in your graveyard.\n" +
        "When this creature enters, draw a card."

    val spells = GameObjectFilter.InstantOrSorcery.faceUp()
    dynamicPower(
        DynamicAmounts.count(Player.You, Zone.EXILE, spells) +
            DynamicAmounts.count(Player.You, Zone.GRAVEYARD, spells)
    )

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f00fa3a7-e3e2-4b23-a126-a076e75b5dbd.jpg?1783934138"
        ruling(
            "2018-10-05",
            "If any exiled cards you own are face down, they have no characteristics. If they're normally " +
                "instants or sorceries, they won't be counted."
        )
        ruling("2018-10-05", "The ability that defines Crackling Drake's power works in all zones.")
    }
}
