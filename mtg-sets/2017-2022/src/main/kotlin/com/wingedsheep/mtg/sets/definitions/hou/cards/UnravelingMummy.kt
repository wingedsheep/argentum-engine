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
 * Unraveling Mummy
 * {1}{W}{B}
 * Creature — Zombie
 * 2/3
 *
 * {1}{W}: Target attacking Zombie gains lifelink until end of turn.
 * {1}{B}: Target attacking Zombie gains deathtouch until end of turn.
 */
val UnravelingMummy = card("Unraveling Mummy") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Zombie"
    oracleText = "{1}{W}: Target attacking Zombie gains lifelink until end of turn.\n" +
        "{1}{B}: Target attacking Zombie gains deathtouch until end of turn."
    power = 2
    toughness = 3

    activatedAbility {
        cost = Costs.Mana("{1}{W}")
        val zombie = target(
            TargetFilter(GameObjectFilter.Creature.withSubtype(Subtype.ZOMBIE).attacking()),
        )
        effect = Effects.GrantKeyword(Keyword.LIFELINK, zombie)
        description = "{1}{W}: Target attacking Zombie gains lifelink until end of turn."
    }

    activatedAbility {
        cost = Costs.Mana("{1}{B}")
        val zombie = target(
            TargetFilter(GameObjectFilter.Creature.withSubtype(Subtype.ZOMBIE).attacking()),
        )
        effect = Effects.GrantKeyword(Keyword.DEATHTOUCH, zombie)
        description = "{1}{B}: Target attacking Zombie gains deathtouch until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Tomasz Jedruszek"
        flavorText = "\"We are no longer in control.\"\n—Elekh, vizier of embalming"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe3802f3-feb3-4dcd-a5ba-f493baf3d447.jpg?1783936008"
        ruling("2017-07-14", "Unraveling Mummy's abilities can target itself while it's attacking.")
        ruling(
            "2017-07-14",
            "The target Zombie will still have lifelink or deathtouch even after combat when it stops being an " +
                "attacking creature.",
        )
        ruling("2017-07-14", "Multiple instances of lifelink and deathtouch are redundant.")
    }
}
