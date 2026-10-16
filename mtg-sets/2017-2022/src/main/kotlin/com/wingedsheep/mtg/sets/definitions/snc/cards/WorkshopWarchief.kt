package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Workshop Warchief
 * {3}{G}{G}
 * Creature — Rhino Warrior
 * 5/3
 * Trample
 * When this creature enters, you gain 3 life.
 * When this creature dies, create a 4/4 green Rhino Warrior creature token.
 * Blitz {4}{G}{G} (If you cast this spell for its blitz cost, it gains haste and "When this creature dies, draw a card." Sacrifice it at the beginning of the next end step.)
 */
val WorkshopWarchief = card("Workshop Warchief") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Rhino Warrior"
    oracleText = "Trample\nWhen this creature enters, you gain 3 life.\nWhen this creature dies, create a 4/4 green Rhino Warrior creature token.\nBlitz {4}{G}{G} (If you cast this spell for its blitz cost, it gains haste and \"When this creature dies, draw a card.\" Sacrifice it at the beginning of the next end step.)"
    power = 5
    toughness = 3

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(3)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Rhino", "Warrior"),
            imageUri = "https://cards.scryfall.io/normal/front/2/3/230419f9-a9f7-441d-bad7-0ed867326aa1.jpg?1783922959"
        )
    }

    keywordAbility(KeywordAbility.blitz("{4}{G}{G}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/27851834-688f-4929-967d-dfa015194f7f.jpg?1783923096"
    }
}
