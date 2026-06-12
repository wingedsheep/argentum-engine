package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Battle-Scarred Goblin
 * {1}{R}
 * Creature — Goblin Warrior
 * 2/2
 *
 * Whenever this creature becomes blocked, it deals 1 damage to each creature blocking it.
 *
 * Gap 15 (deal damage to each blocker): composes from the existing `Patterns.Group.dealDamageToAll`
 * over a new source-relative `GameObjectFilter.Creature.blockingSource()` filter (backed by the new
 * `StatePredicate.IsBlockingSource`), which matches only the blockers of the ability's source.
 */
val BattleScarredGoblin = card("Battle-Scarred Goblin") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature becomes blocked, it deals 1 damage to each creature blocking it."

    triggeredAbility {
        trigger = Triggers.BecomesBlocked
        effect = Patterns.Group.dealDamageToAll(1, GroupFilter(GameObjectFilter.Creature.blockingSource()))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Hristo D. Chukov"
        flavorText = "\"An Orc shot Balin from behind a stone. We slew the Orc, but many more came.\"\n—*Book of Mazarbul*"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b91859de-1983-4c9c-a9e6-289c7dba1eb4.jpg?1686968799"
    }
}
