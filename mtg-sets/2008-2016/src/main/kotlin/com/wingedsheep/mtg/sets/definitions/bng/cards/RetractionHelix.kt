package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Retraction Helix — Born of the Gods #49
 * {U} · Instant
 *
 * Until end of turn, target creature gains "{T}: Return target nonland permanent to its owner's hand."
 *
 * The targeted creature gains a `{T}` bounce ability for the turn. The granted ability's own target is
 * chosen when it's activated (any nonland permanent, including the creature itself); summoning sickness
 * still gates the `{T}` cost.
 */
val RetractionHelix = card("Retraction Helix") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature gains \"{T}: Return target nonland permanent to its owner's hand.\""

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                val permanent = target(TargetFilter.NonlandPermanent)
                effect = Effects.ReturnToHand(permanent)
                description = "Return target nonland permanent to its owner's hand."
            },
            target = creature,
            duration = Duration.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Phill Simmer"
        flavorText = "Where the geometries of the world collapse, a master of the Æther can harvest power beyond comprehending."
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4fe8c0b9-fdf4-4fc0-aa7c-774546cdd792.jpg?1783939566"
    }
}
