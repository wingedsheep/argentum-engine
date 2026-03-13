package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Slinn Voda, the Rising Deep
 * {6}{U}{U}
 * Legendary Creature — Leviathan
 * 8/8
 * Kicker {1}{U}
 * When Slinn Voda enters, if it was kicked, return all creatures to their owners'
 * hands except for Merfolk, Krakens, Leviathans, Octopuses, and Serpents.
 */
val SlinnVodaTheRisingDeep = card("Slinn Voda, the Rising Deep") {
    manaCost = "{6}{U}{U}"
    typeLine = "Legendary Creature — Leviathan"
    power = 8
    toughness = 8
    oracleText = "Kicker {1}{U}\nWhen Slinn Voda, the Rising Deep enters, if it was kicked, return all creatures to their owners' hands except for Merfolk, Krakens, Leviathans, Octopuses, and Serpents."

    keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{1}{U}")))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ConditionalEffect(
            condition = WasKicked,
            effect = EffectPatterns.returnAllToHand(
                GroupFilter(
                    baseFilter = GameObjectFilter.Creature
                        .notSubtype(Subtype.MERFOLK)
                        .notSubtype(Subtype.KRAKEN)
                        .notSubtype(Subtype.LEVIATHAN)
                        .notSubtype(Subtype.OCTOPUS)
                        .notSubtype(Subtype.SERPENT)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "66"
        artist = "Grzegorz Rutkowski"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c92b4581-fe4a-498d-af58-c1e453235df8.jpg?1562742810"
    }
}
