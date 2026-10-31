package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Seven Dwarves
 * {1}{R}
 * Creature — Dwarf
 * 2/2
 * This creature gets +1/+1 for each other creature named Seven Dwarves you control.
 * A deck can have up to seven cards named Seven Dwarves.
 *
 * The deck-construction line is enforced by the server's DeckValidator, which reads the
 * "A deck can have up to N cards named X" override straight from the oracle text.
 */
val SevenDwarves = card("Seven Dwarves") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dwarf"
    power = 2
    toughness = 2
    oracleText = "This creature gets +1/+1 for each other creature named Seven Dwarves you control.\n" +
        "A deck can have up to seven cards named Seven Dwarves."

    staticAbility {
        val otherDwarves = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Creature.named("Seven Dwarves"),
            excludeSelf = true
        ).count()
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = otherDwarves,
            toughnessBonus = otherDwarves
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "141"
        artist = "Jason Rainville"
        flavorText = "\"The gleam of rubies and my brothers and sisters by my side . . . what more could I want?\"\n—Brognold the Third"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/464adbae-70ea-48e1-b8ae-b404766f7a5a.jpg?1783932616"
        ruling("2019-10-04", "Because damage remains marked on a creature until the damage is removed as the turn ends, nonlethal damage dealt to one Seven Dwarves may become lethal if another Seven Dwarves you control leaves the battlefield during that turn.")
        ruling("2019-10-04", "The last ability of Seven Dwarves lets you ignore the \"four-of\" rule. It doesn't let you ignore format legality. For example, during a Throne of Eldraine Limited event, you can't add Seven Dwarves from your personal collection, no matter how happy they would be.")
        ruling("2019-10-04", "If you're fortunate enough to open or draft eight or more Seven Dwarves during a Limited event, you can include only seven of them in your deck. The rest remain in your sideboard.")
    }
}
