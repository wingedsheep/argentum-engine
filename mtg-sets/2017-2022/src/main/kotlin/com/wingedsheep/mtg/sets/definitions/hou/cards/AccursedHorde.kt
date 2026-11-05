package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Accursed Horde
 * {3}{B}
 * Creature — Zombie
 * 3/3
 *
 * {1}{B}: Target attacking Zombie gains indestructible until end of turn.
 */
val AccursedHorde = card("Accursed Horde") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    oracleText = "{1}{B}: Target attacking Zombie gains indestructible until end of turn. " +
        "(Damage and effects that say \"destroy\" don't destroy it. If its toughness is 0 or less, it still dies.)"
    power = 3
    toughness = 3

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        val zombie = target(
            TargetFilter(GameObjectFilter.Creature.withSubtype(Subtype.ZOMBIE).attacking()),
        )
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, zombie)
        description = "{1}{B}: Target attacking Zombie gains indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/691b79ef-87da-4b52-8311-2f6f46c6368d.jpg?1783936043"
        ruling("2017-07-14", "Accursed Horde's ability can target itself while it's attacking.")
        ruling(
            "2017-07-14",
            "The target Zombie gains indestructible for the rest of the turn, even after it stops being an " +
                "attacking creature.",
        )
    }
}
