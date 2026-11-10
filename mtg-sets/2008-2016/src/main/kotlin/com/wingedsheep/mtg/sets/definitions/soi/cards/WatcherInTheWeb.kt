package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanBlockAdditionalForCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Watcher in the Web
 * {4}{G}
 * Creature — Spider
 * 2/5
 * Reach (This creature can block creatures with flying.)
 * This creature can block an additional seven creatures each combat.
 *
 * [CanBlockAdditionalForCreatureGroup] scoped to [GroupFilter.source] (Selesnya Sagittars' shape)
 * with `count = 7`: the projection sums `count` into `additionalBlockCount`, and
 * `BlockStaticRules` caps the blocker at `1 + additionalBlockCount` attackers — eight in all.
 */
val WatcherInTheWeb = card("Watcher in the Web") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spider"
    power = 2
    toughness = 5
    oracleText = "Reach (This creature can block creatures with flying.)\n" +
        "This creature can block an additional seven creatures each combat."

    keywords(Keyword.REACH)

    staticAbility {
        ability = CanBlockAdditionalForCreatureGroup(
            count = 7,
            filter = GroupFilter.source(),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "239"
        artist = "Vincent Proce"
        flavorText = "\"Lost in the woods\" is an optimistic way to describe a villager's disappearance. " +
            "\"Found in the woods\" is frequently more appropriate."
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be1ee2d4-5992-4081-9907-4bc26750aff0.jpg?1783937715"
    }
}
