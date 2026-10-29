package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Aven Gagglemaster
 * {3}{W}{W}
 * Creature — Bird Warrior
 * 4/3
 * Flying
 * When this creature enters, you gain 2 life for each creature you control with flying.
 *
 * The count is taken on resolution and includes the Gagglemaster itself if it's still there
 * (2020-06-23 ruling).
 */
val AvenGagglemaster = card("Aven Gagglemaster") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Warrior"
    power = 4
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, you gain 2 life for each creature you control with flying."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(
            DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature.withKeyword(Keyword.FLYING),
            ).count() * 2
        )
        description = "When this creature enters, you gain 2 life for each creature you control with flying."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "5"
        artist = "Mike Bierek"
        flavorText = "An aven's gear is enchanted to avoid weighing them down during aerial maneuvers."
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5b57247-81cc-44ec-b5a9-0702111a98a8.jpg?1783930746"
        ruling(
            "2020-06-23",
            "The amount of life you gain is determined only as Aven Gagglemaster's triggered ability " +
                "resolves. If Aven Gagglemaster is still on the battlefield at that time, it will be " +
                "counted by its own ability."
        )
    }
}
