package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Escape Tunnel
 * Land
 * {T}, Sacrifice this land: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.
 * {T}, Sacrifice this land: Target creature with power 2 or less can't be blocked this turn.
 */
val EscapeTunnel = card("Escape Tunnel") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "{T}, Sacrifice this land: Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.\n{T}, Sacrifice this land: Target creature with power 2 or less can't be blocked this turn."

    // First ability: Search for basic land
    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.SacrificeSelf
        )
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        )
        manaAbility = false
        description = "Search your library for a basic land card, put it onto the battlefield tapped, then shuffle."
    }

    // Second ability: Make creature unblockable
    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.SacrificeSelf
        )
        val target = target(TargetFilter.Creature.powerAtMost(2))
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, target)
        manaAbility = false
        description = "Target creature with power 2 or less can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "261"
        artist = "Carlos Palma Cruchaga"
        flavorText = "No self-respecting criminal has a basement with only one exit."
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93ddde4f-d35e-4128-8f43-d0eadbd715de.jpg?1706242339"
    }
}
