package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Primal Whisperer
 * {4}{G}
 * Creature — Elf Soldier
 * 2/2
 * This creature gets +2/+2 for each face-down creature on the battlefield.
 * Morph {3}{G}
 */
val PrimalWhisperer = card("Primal Whisperer") {
    manaCost = "{4}{G}"
    typeLine = "Creature — Elf Soldier"
    power = 2
    toughness = 2
    oracleText = "This creature gets +2/+2 for each face-down creature on the battlefield.\nMorph {3}{G} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)"

    val faceDownCount = DynamicAmount.AggregateBattlefield(Player.Each, GameObjectFilter.Creature.faceDown())

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmount.Multiply(faceDownCount, 2),
            toughnessBonus = DynamicAmount.Multiply(faceDownCount, 2)
        )
    }

    morph = "{3}{G}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "135"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c777432f-7965-4ad8-8d53-93919ae767d4.jpg?1562935034"
    }
}
