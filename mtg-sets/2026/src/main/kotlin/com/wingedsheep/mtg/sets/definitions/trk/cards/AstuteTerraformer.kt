package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Astute Terraformer
 * {1}{G}
 * Creature — Vulcan Scientist
 * 2/2
 *
 * Strange new worlds — {6}: This creature gets +2/+2 until end of turn. This ability costs {X}
 * less to activate, where X is the number of differently named lands you control. Activate only
 * once each turn.
 *
 * "Strange new worlds" is an ability word with no rules meaning. The self-reduction is the
 * ability's `genericCostReduction`, as on Suus Mahna Practitioner.
 */
val AstuteTerraformer = card("Astute Terraformer") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Vulcan Scientist"
    colorIdentity = "G"
    power = 2
    toughness = 2
    oracleText = "Strange new worlds — {6}: This creature gets +2/+2 until end of turn. This ability costs " +
        "{X} less to activate, where X is the number of differently named lands you control. Activate only " +
        "once each turn."

    activatedAbility {
        cost = Costs.Mana("{6}")
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        genericCostReduction = DynamicAmounts.differentlyNamedLandsYouControl()
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "184"
        artist = "Andreia Ugrai"
        flavorText = "\"Humans believe cataloging life as a predictable series of scientific reactions robs it " +
            "of its majesty. I find this attitude belittles the majesty of science.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2ef7c30c-3ee6-413d-bd24-476322f36cb8.jpg?1793361354"
    }
}
