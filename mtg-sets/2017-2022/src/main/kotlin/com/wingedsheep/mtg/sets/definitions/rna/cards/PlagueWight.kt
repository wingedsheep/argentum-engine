package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Plague Wight
 * {1}{B}
 * Creature — Zombie
 * 2/1
 * Whenever this creature becomes blocked, each creature blocking it gets -1/-1 until end of turn.
 *
 * Same block trigger as Baneblade Scoundrel: `becomesBlocked()` over the source-relative
 * `blockingSource()` group.
 */
val PlagueWight = card("Plague Wight") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 1
    oracleText = "Whenever this creature becomes blocked, each creature blocking it gets -1/-1 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked()
        effect = Patterns.Group.modifyStatsForAll(-1, -1, GroupFilter(GameObjectFilter.Creature.blockingSource()))
        description = "Whenever this creature becomes blocked, each creature blocking it gets -1/-1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Tomasz Jedruszek"
        flavorText = "For some goods, the best couriers are the dead."
        imageUri = "https://cards.scryfall.io/normal/front/9/6/962d354f-f2ad-4b47-8666-0ed64543b676.jpg?1783933692"
    }
}
