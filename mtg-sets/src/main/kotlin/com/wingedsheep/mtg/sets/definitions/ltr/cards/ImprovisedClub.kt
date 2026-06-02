package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.Costs

/**
 * Improvised Club
 * {1}{R}
 * Instant
 *
 * As an additional cost to cast this spell, sacrifice an artifact or creature.
 * Improvised Club deals 4 damage to any target.
 */
val ImprovisedClub = card("Improvised Club") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, sacrifice an artifact or creature.\nImprovised Club deals 4 damage to any target."

    additionalCost(
        Costs.additional.SacrificePermanent(
            filter = GameObjectFilter.Artifact or GameObjectFilter.Creature
        )
    )

    spell {
        val target = target("any target", Targets.Any)
        effect = Effects.DealDamage(4, target)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "137"
        artist = "Pablo Mendoza"
        flavorText = "\"'For a couple o' pins,' says Troll, and grins, 'I'll eat thee too and gnaw thy shins.'\"\n—Sam"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8397d13-eeaf-4b4e-b3cd-9a9ac231873a.jpg?1687695211"
    }
}
