package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Barl's Cage
 * {4}
 * Artifact
 * {3}: Target creature doesn't untap during its controller's next untap step.
 *
 * The repeatable half of Crippling Chill, without the tap: a `DOESNT_UNTAP` grant bounded by
 * [Duration.UntilAfterAffectedControllersNextUntap], which is keyed to the *affected* creature's
 * controller rather than to the Cage's, so caging an opponent's creature skips the opponent's untap
 * step and not yours.
 */
val BarlsCage = card("Barl's Cage") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "{3}: Target creature doesn't untap during its controller's next untap step."

    activatedAbility {
        val creature = target(TargetFilter.Creature)
        cost = Costs.Mana("{3}")
        effect = Effects.GrantKeyword(
            AbilityFlag.DOESNT_UNTAP,
            creature,
            Duration.UntilAfterAffectedControllersNextUntap(),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Tom Wänerstrand"
        flavorText = "For a dozen years the Cage had held Lord Ith, but as the Pretender Mairsil's " +
            "power weakened, so did the bars."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/6768a307-da2e-435e-8efd-72d82b4d4a2b.jpg?1783947928"
    }
}
