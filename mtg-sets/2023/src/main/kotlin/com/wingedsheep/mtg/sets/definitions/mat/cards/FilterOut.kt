package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Filter Out — March of the Machine: The Aftermath #7
 * {1}{U}{U} · Instant
 * Return all noncreature, nonland permanents to their owners' hands.
 */
val FilterOut = card("Filter Out") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return all noncreature, nonland permanents to their owners' hands."

    spell {
        effect = Patterns.Group.returnAllToHand(GroupFilter(GameObjectFilter.NonlandPermanent.notCreature()))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Julia Metzger"
        flavorText = "The glistening oil had been rendered inert, but planes wouldn't begin to truly heal until every last drop was purged."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a81a9eb1-240b-40fd-9282-0f5abec63449.jpg?1783916527"
    }
}
