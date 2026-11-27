package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Devastation Tide
 * {3}{U}{U}
 * Sorcery
 *
 * Return all nonland permanents to their owners' hands.
 * Miracle {1}{U} (You may cast this card for its miracle cost when you draw it if it's the first card you drew this turn.)
 */
val DevastationTide = card("Devastation Tide") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return all nonland permanents to their owners' hands.\n" +
        "Miracle {1}{U} (You may cast this card for its miracle cost when you draw it if it's the " +
        "first card you drew this turn.)"

    spell {
        effect = Patterns.Group.returnAllToHand(GroupFilter(GameObjectFilter.NonlandPermanent))
    }

    keywordAbility(KeywordAbility.miracle("{1}{U}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "48"
        artist = "Raymond Swanland"
        flavorText = "It's hard to argue with a force made of divine will."
        imageUri = "https://cards.scryfall.io/normal/front/2/3/23b62be6-1a80-4f16-a94a-374203052662.jpg?1783940722"
    }
}
