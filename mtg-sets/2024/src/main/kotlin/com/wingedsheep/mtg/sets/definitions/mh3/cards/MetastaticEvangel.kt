package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Metastatic Evangel
 * {1}{W}
 * Creature — Phyrexian Human Cleric
 * 3/1
 *
 * Whenever another nontoken creature you control enters, proliferate.
 */
val MetastaticEvangel = card("Metastatic Evangel") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Phyrexian Human Cleric"
    power = 3
    toughness = 1
    oracleText = "Whenever another nontoken creature you control enters, proliferate. " +
        "(Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl().nontoken()).enters()
        effect = Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "Volkan Baǵa"
        flavorText = "\"I am no sleeper agent. I am a liberator. It is you who are asleep.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/7/774e115a-c0dc-4901-9a1f-87754304e378.jpg?1783911299"
    }
}
