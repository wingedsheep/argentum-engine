package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mortal's Resolve
 * {1}{G}
 * Instant
 *
 * Target creature gets +1/+1 and gains indestructible until end of turn. (Damage and effects that
 * say "destroy" don't destroy it.)
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val MortalsResolve = card("Mortal's Resolve") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +1/+1 and gains indestructible until end of turn. " +
        "(Damage and effects that say \"destroy\" don't destroy it.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 1, creature) then Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "127"
        artist = "Svetlin Velinov"
        flavorText = "The Fates tried to cut her string but found the thread had turned to iron."
        imageUri = "https://cards.scryfall.io/normal/front/1/3/131a96b4-393d-4e8e-9b6a-f15f3804febb.jpg?1783939533"
    }
}
