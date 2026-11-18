package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sanctified Charge
 * {4}{W}
 * Instant
 * Creatures you control get +2/+1 until end of turn. White creatures you control also gain first
 * strike until end of turn.
 *
 * Two group sweeps run back to back as the spell resolves: +2/+1 to every creature you control, then
 * first strike to the white ones (color read from projected state, so a creature painted white
 * counts).
 */
val SanctifiedCharge = card("Sanctified Charge") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Creatures you control get +2/+1 until end of turn. White creatures you control also gain first strike until end of turn. (They deal combat damage before creatures without first strike.)"

    spell {
        effect = Patterns.Group.modifyStatsForAll(2, 1, GroupFilter.AllCreaturesYouControl) then
            Patterns.Group.grantKeywordToAll(
                Keyword.FIRST_STRIKE,
                GroupFilter(GameObjectFilter.Creature.withColor(Color.WHITE).youControl())
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "30"
        artist = "Dave Kendall"
        flavorText = "You need only raise your spear to receive this blessing."
        imageUri = "https://cards.scryfall.io/normal/front/6/4/6481bf34-192c-4102-8336-4d2d007d9e3c.jpg?1783939197"
    }
}
