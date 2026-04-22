package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Surly Farrier {1}{G}
 * Creature — Kithkin Citizen
 * 2/2
 *
 * {T}: Target creature you control gets +1/+1 and gains vigilance until end of turn.
 * Activate only as a sorcery.
 */
val SurlyFarrier = card("Surly Farrier") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Kithkin Citizen"
    power = 2
    toughness = 2
    oracleText = "{T}: Target creature you control gets +1/+1 and gains vigilance until end of turn. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Tap
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.ModifyStats(1, 1, creature)
            .then(Effects.GrantKeyword(Keyword.VIGILANCE, creature))
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "196"
        artist = "Jake Murray"
        flavorText = "\"I don't care what the others say. Luck left us during the Invasion. The only thing we can trust now is our own grit.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/5/056f7f51-18a9-4d80-8928-decaf4d12c0d.jpg?1767732856"
    }
}
