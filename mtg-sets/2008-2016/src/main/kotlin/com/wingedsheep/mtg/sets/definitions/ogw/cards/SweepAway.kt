package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sweep Away
 * {2}{U}
 * Instant
 * Return target creature to its owner's hand. If that creature is attacking, you may put it on top
 * of its owner's library instead.
 *
 * Whether the creature is attacking is checked at resolution, while it is still on the battlefield
 * (the Prophesied End shape). If it is, the caster chooses top of library or hand (the Whirlpool Whelm
 * shape); declining returns it to its owner's hand.
 */
val SweepAway = card("Sweep Away") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target creature to its owner's hand. If that creature is attacking, you may put it " +
        "on top of its owner's library instead."

    spell {
        val creature = target(TargetFilter.Creature)
        val bounce = Effects.ReturnToHand(creature)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.attacking(), creature),
            then = Effects.May(
                effect = Effects.PutOnTopOfLibrary(creature),
                otherwise = bounce,
                prompt = "Put that creature on top of its owner's library instead of returning it to hand?"
            ),
            otherwise = bounce
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "64"
        artist = "Winona Nelson"
        flavorText = "The Roil takes many forms. It also takes many victims."
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75234f6d-a2da-42dd-a668-8db8f341e7f0.jpg?1783937916"
    }
}
