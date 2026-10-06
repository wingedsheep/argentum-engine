package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Challenger Troll
 * {4}{G}
 * Creature — Troll
 * 6/5
 *
 * Each creature you control with power 4 or greater can't be blocked by more than one creature.
 *
 * A group-scoped [CantBeBlockedByMoreThan]: the blocker validation scans battlefield hosts and
 * matches each attacker against the filter on projected power, so the Troll covers itself and any
 * creature pumped to power 4 or greater, and stops covering one shrunk below it.
 */
val ChallengerTroll = card("Challenger Troll") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Troll"
    power = 6
    toughness = 5
    oracleText = "Each creature you control with power 4 or greater can't be blocked by more than one creature."

    staticAbility {
        ability = CantBeBlockedByMoreThan(
            maxBlockers = 1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl().powerAtLeast(4)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "157"
        artist = "Svetlin Velinov"
        flavorText = "For most, war is a calamity. For some, an opportunity. And for the very few, a pleasure."
        imageUri = "https://cards.scryfall.io/normal/front/3/2/328d778a-ef16-437f-9a9d-204cf061919b.jpg?1783933416"
    }
}
