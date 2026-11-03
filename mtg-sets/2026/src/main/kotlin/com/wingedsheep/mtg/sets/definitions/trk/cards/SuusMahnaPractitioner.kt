package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Suus Mahna Practitioner
 * {2}{U}
 * Creature — Vulcan Officer
 * 3/2
 *
 * Strange new worlds — {4}{U}, {T}: Tap target creature. This ability costs {X} less to activate,
 * where X is the number of differently named lands you control.
 *
 * "Strange new worlds" is an ability word with no rules meaning. The self-reduction is the
 * ability's `genericCostReduction` (Survey Mechan), so it only trims the {4} — never the {U}.
 */
val SuusMahnaPractitioner = card("Suus Mahna Practitioner") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Vulcan Officer"
    colorIdentity = "U"
    power = 3
    toughness = 2
    oracleText = "Strange new worlds — {4}{U}, {T}: Tap target creature. This ability costs {X} less to " +
        "activate, where X is the number of differently named lands you control."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{U}"), Costs.Tap)
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature)
        genericCostReduction = DynamicAmounts.differentlyNamedLandsYouControl()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Lucas Durham"
        flavorText = "Vulcans practice martial arts as a meditative exercise, though their physical aspects " +
            "are sometimes useful."
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb204e41-06a9-4ad2-81f2-a4d62f771cfa.jpg?1793361270"
    }
}
