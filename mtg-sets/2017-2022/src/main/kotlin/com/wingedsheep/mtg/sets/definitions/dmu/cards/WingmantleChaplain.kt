package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

private const val BIRD_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/5/f/5f3034f6-145f-4e60-9e55-c4054fd8e70f.jpg?1783921132"

private val defender = GameObjectFilter.Creature.withKeyword(Keyword.DEFENDER)

val WingmantleChaplain = card("Wingmantle Chaplain") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 0
    toughness = 3
    oracleText = "Defender\n" +
        "When this creature enters, create a 1/1 white Bird creature token with flying for each creature with defender you control.\n" +
        "Whenever another creature you control with defender enters, create a 1/1 white Bird creature token with flying."

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = DynamicAmounts.battlefield(Player.You, defender).count(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Bird"),
            keywords = setOf(Keyword.FLYING),
            imageUri = BIRD_TOKEN_IMAGE
        )
    }

    // A noncreature artifact with defender that's a creature as it enters (March of the
    // Machines) counts: entry triggers read the entrant's projected types.
    triggeredAbility {
        trigger = Triggers.another(defender.youControl()).enters()
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Bird"),
            keywords = setOf(Keyword.FLYING),
            imageUri = BIRD_TOKEN_IMAGE
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "39"
        artist = "Miranda Meeks"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5800f3d-b2e0-4c74-94bf-f29b27f82130.jpg?1783921356"
    }
}
