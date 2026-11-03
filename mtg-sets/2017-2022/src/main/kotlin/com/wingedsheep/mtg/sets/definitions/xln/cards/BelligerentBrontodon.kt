package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AssignDamageEqualToToughness
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Belligerent Brontodon
 * {5}{G}{W}
 * Creature — Dinosaur
 * 4/6
 * Each creature you control assigns combat damage equal to its toughness rather than its power.
 */
val BelligerentBrontodon = card("Belligerent Brontodon") {
    manaCost = "{5}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Dinosaur"
    power = 4
    toughness = 6
    oracleText = "Each creature you control assigns combat damage equal to its toughness rather than its power."

    staticAbility {
        ability = AssignDamageEqualToToughness(
            filter = GroupFilter.AllCreaturesYouControl,
            onlyWhenToughnessGreaterThanPower = false,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Dan Murayama Scott"
        flavorText = "Predators try to separate the young, old, or injured from the rest of the herd. The alternative is crushing defeat."
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39bab75a-2c1f-4ff1-a26e-134e52f61d39.jpg?1783935714"
    }
}
