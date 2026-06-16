package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Badlands Revival
 * {3}{B}{G}
 * Sorcery
 *
 * Return up to one target creature card from your graveyard to the battlefield. Return up to one
 * target permanent card from your graveyard to your hand.
 *
 * Two independent "up to one target" slots (optional targets, minCount 0): the first restricted to
 * creature cards you own in your graveyard (reanimated to the battlefield), the second to permanent
 * cards you own in your graveyard (returned to hand). Either slot may be left empty; an empty slot
 * simply does nothing on resolution.
 */
val BadlandsRevival = card("Badlands Revival") {
    manaCost = "{3}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Sorcery"
    oracleText = "Return up to one target creature card from your graveyard to the battlefield. " +
        "Return up to one target permanent card from your graveyard to your hand."

    spell {
        val creature = target(
            "creature",
            TargetObject(optional = true, filter = TargetFilter.CreatureInYourGraveyard),
        )
        val permanent = target(
            "permanent",
            TargetObject(
                optional = true,
                filter = TargetFilter(
                    GameObjectFilter.Permanent.ownedByYou(),
                    zone = Zone.GRAVEYARD,
                ),
            ),
        )
        effect = Effects.Composite(
            listOf(
                Effects.PutOntoBattlefield(creature),
                Effects.ReturnToHand(permanent),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Carlos Palma Cruchaga"
        flavorText = "The cactusfolk were a young culture, but they already knew the pains of death " +
            "and joys of birth."
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d3ef971-cdd4-410c-97c3-df98e4f02ab2.jpg?1712356053"
    }
}
