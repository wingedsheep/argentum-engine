package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Mouth // Feed — Amonkhet #214
 *
 * Mouth {2}{G} — Sorcery: Create a 3/3 green Hippo creature token.
 * Feed {3}{G} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Draw a card for each creature you control with power 3 or greater.
 *
 * Feed counts creatures on resolution, reading projected power.
 */
val MouthFeed = card("Mouth // Feed") {
    layout = CardLayout.SPLIT
    colorIdentity = "G"

    face("Mouth") {
        manaCost = "{2}{G}"
        typeLine = "Sorcery"
        oracleText = "Create a 3/3 green Hippo creature token."

        spell {
            effect = Effects.CreateToken(
                power = 3,
                toughness = 3,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Hippo"),
                imageUri = "https://cards.scryfall.io/normal/front/1/a/1aea5e0b-dc4e-4055-9e13-1dfbc25a2f00.jpg?1783936411"
            )
        }
    }

    face("Feed") {
        manaCost = "{3}{G}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Draw a card for each creature you control with power 3 or greater."
        keywords(Keyword.AFTERMATH)

        spell {
            effect = Effects.DrawCards(
                DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.powerAtLeast(3)).count()
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a47070a0-fd05-4ed9-a175-847a864478da.jpg?1783936458"
        ruling("2017-04-18", "If another effect allows you to cast a split card with aftermath from any zone other than a graveyard, you can't cast the half with aftermath.")
        ruling("2017-04-18", "If another effect allows you to cast a split card with aftermath from a graveyard, you may cast either half. If you cast the half that has aftermath, you'll exile the card if it would leave the stack.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
    }
}
