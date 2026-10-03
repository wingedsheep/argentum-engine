package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.dsl.Conditions

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wingmate Roc
 * {3}{W}{W}
 * Creature — Bird
 * 3/4
 * Flying
 * Raid — When Wingmate Roc enters the battlefield, if you attacked this turn,
 * create a 3/4 white Bird creature token with flying.
 * Whenever Wingmate Roc attacks, you gain 1 life for each attacking creature.
 */
val WingmateRoc = card("Wingmate Roc") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    power = 3
    toughness = 4
    oracleText = "Flying\nRaid — When Wingmate Roc enters, if you attacked this turn, create a 3/4 white Bird creature token with flying.\nWhenever Wingmate Roc attacks, you gain 1 life for each attacking creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouAttackedThisTurn
        effect = Effects.CreateToken(
            count = 1,
            power = 3,
            toughness = 4,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Bird"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/a/a/aa788dea-2968-45e8-9e08-c89e3e227d88.jpg?1562639973"
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.GainLife(DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Creature.attacking()).count())
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "31"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25f0f6a2-b392-45e6-97c4-4f00016144d3.jpg?1562783827"
        ruling("2014-09-20", "Count the number of attacking creatures when the last ability resolves, including Wingmate Roc itself if it's still on the battlefield, to determine how much life you gain.")
    }
}
