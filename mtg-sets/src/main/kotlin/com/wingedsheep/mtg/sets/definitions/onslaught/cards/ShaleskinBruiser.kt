package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Shaleskin Bruiser
 * {6}{R}
 * Creature — Beast
 * 4/4
 * Trample
 * Whenever Shaleskin Bruiser attacks, it gets +3/+0 until end of turn for each other attacking Beast.
 */
val ShaleskinBruiser = card("Shaleskin Bruiser") {
    manaCost = "{6}{R}"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "Trample\nWhenever Shaleskin Bruiser attacks, it gets +3/+0 until end of turn for each other attacking Beast."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.ModifyStats(
            power = DynamicAmount.Multiply(
                DynamicAmount.Subtract(
                    DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Creature.withSubtype("Beast").attacking()),
                    DynamicAmount.Fixed(1)
                ),
                3
            ),
            toughness = DynamicAmount.Fixed(0),
            target = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "226"
        artist = "Mark Zug"
        flavorText = "Its only predators are the elements."
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc2de8a4-0d84-4f7c-bbe4-3a31172186ab.jpg?1562954767"
    }
}
