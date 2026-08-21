package com.wingedsheep.mtg.sets.definitions.vis.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fireblast {4}{R}{R}
 * Instant (Visions, 1997)
 *
 * You may sacrifice two Mountains rather than pay this spell's mana cost.
 * Fireblast deals 4 damage to any target.
 *
 * The alternative cost is modeled via [SelfAlternativeCost] with mana cost {0} and
 * an additional sacrifice cost of two Mountains. The engine handles offering both
 * the normal ({4}{R}{R}) and alternative (sacrifice 2 Mountains) casting paths.
 */
val Fireblast = card("Fireblast") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "You may sacrifice two Mountains rather than pay this spell's mana cost.\nFireblast deals 4 damage to any target."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        additionalCosts = listOf(
            Costs.additional.SacrificePermanent(Filters.MountainCard, count = 2)
        )
    )

    spell {
        target = Targets.Any
        effect = Effects.DealDamage(4, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "79"
        artist = "Michael Danza"
        flavorText = "\"Embermages aren't well known for their diplomatic skills.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1eb5b2c-1f02-48a6-a287-88eb189d6780.jpg?1783946989"
    }
}
