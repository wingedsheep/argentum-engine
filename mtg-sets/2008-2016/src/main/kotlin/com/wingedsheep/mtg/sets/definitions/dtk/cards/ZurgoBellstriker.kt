package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Zurgo Bellstriker
 * {R}
 * Legendary Creature — Orc Warrior
 * 2/2
 * Zurgo can't block creatures with power 2 or greater.
 * Dash {1}{R}
 *
 * The block restriction is expressed as its complement (it can block only creatures with power 1
 * or less), as on Ironclaw Orcs; the comparison reads the attacker's projected power when blocks
 * are declared, so pumping the attacker afterwards doesn't undo a legal block. `dash` is the
 * builder property that adds the `KeywordAbility.Dash` the cast enumerator reads.
 */
val ZurgoBellstriker = card("Zurgo Bellstriker") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Orc Warrior"
    power = 2
    toughness = 2
    oracleText = "Zurgo can't block creatures with power 2 or greater.\n" +
        "Dash {1}{R} (You may cast this spell for its dash cost. If you do, it gains haste, and it's returned from the battlefield to its owner's hand at the beginning of the next end step.)"

    staticAbility {
        ability = CanOnlyBlockCreaturesWith(GameObjectFilter.Creature.powerAtMost(1))
    }

    dash = "{1}{R}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "169"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbac6d75-59ab-4d7d-87ae-46875abf8cc7.jpg?1783938583"
        ruling("2015-02-25", "Once Zurgo Bellstriker has legally blocked a creature, raising that creature's power to 2 or greater won't undo the block.")
        ruling("2015-02-25", "Even though both cards portray the same character, Zurgo Bellstriker and Zurgo Helmsmasher have different names. Controlling both of them won't invoke the \"legend rule.\"")
    }
}
