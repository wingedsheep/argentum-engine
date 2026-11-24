package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Armory Guard
 * {3}{W}
 * Creature — Giant Soldier
 * 2/5
 *
 * This creature has vigilance as long as you control a Gate.
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * A conditional self keyword grant (Tribal Golem's shape) gated on controlling a Gate land.
 */
val ArmoryGuard = card("Armory Guard") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Giant Soldier"
    oracleText = "This creature has vigilance as long as you control a Gate."
    power = 2
    toughness = 5

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.source())
        condition = Conditions.YouControl(GameObjectFilter.Land.withSubtype("Gate"))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "Karl Kopinski"
        flavorText = "The Dimir agents watched from the shadows. \"Eight hours, and I've yet to see him blink,\" Nefara hissed. " +
            "\"I suggest we find another way in.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c03498c1-b7f7-41fb-8e2a-1c087d4e9990.jpg?1783940378"
    }
}
