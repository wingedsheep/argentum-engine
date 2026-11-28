package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Moonveil Dragon — Dark Ascension #99 (canonical printing)
 * {3}{R}{R}{R} · Creature — Dragon · 5/5
 * Flying
 * {R}: Each creature you control gets +1/+0 until end of turn.
 *
 * Team firebreathing: the group pump (Inner-Flame Igniter's first clause) on a single-red
 * activation. The group is bound on resolution, so the Dragon pumps itself too, and creatures
 * that arrive later in the turn don't get the bonus.
 */
val MoonveilDragon = card("Moonveil Dragon") {
    manaCost = "{3}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 5
    toughness = 5
    oracleText = "Flying\n{R}: Each creature you control gets +1/+0 until end of turn."
    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Patterns.Group.modifyStatsForAll(1, 0, GroupFilter(GameObjectFilter.Creature.youControl()))
        description = "Each creature you control gets +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "99"
        artist = "Todd Lockwood"
        flavorText = "\"Their hearts beat, their lungs draw breath, and they have one true form. I count them as allies of the living.\"\n" +
            "—Thalia, Knight-Cathar"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92503118-b37b-4c52-b40a-487f6ad695ef.jpg?1783940815"
    }
}
