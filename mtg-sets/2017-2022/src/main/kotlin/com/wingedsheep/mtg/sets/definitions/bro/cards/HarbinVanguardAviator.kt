package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Harbin, Vanguard Aviator
 * {W}{U}
 * Legendary Creature — Human Soldier
 * 3/2
 *
 * Flying
 * Whenever you attack with five or more Soldiers, creatures you control get +1/+1 and gain flying
 * until end of turn.
 *
 * "Attack with five or more Soldiers" is the once-per-declaration group trigger
 * `Triggers.you.attacks(with, minAttackers = 5)`, which counts only declared attackers matching the
 * Soldier filter (Harbin itself counts if it attacks). The payoff is one team pump over every
 * creature you control, not just the attackers.
 */
val HarbinVanguardAviator = card("Harbin, Vanguard Aviator") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Soldier"
    oracleText = "Flying\nWhenever you attack with five or more Soldiers, creatures you control get +1/+1 and gain flying until end of turn."
    power = 3
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.attacks(
            with = GameObjectFilter.Creature.withSubtype(Subtype.SOLDIER),
            minAttackers = 5
        )
        effect = Patterns.Group.pumpAndGrantToAll(
            power = 1,
            toughness = 1,
            keyword = Keyword.FLYING,
            filter = Filters.Group.creaturesYouControl
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "212"
        artist = "Kieran Yanner"
        flavorText = "\"Yotia is my birthright, father. Let me fight for it.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b88be69-3201-4345-a193-cc21bd066d15.jpg?1783920030"
    }
}
