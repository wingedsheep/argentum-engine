package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Clever Conjurer
 * {2}{U}
 * Creature — Gnome Wizard
 * 2/3
 * Mage Hand — {T}: Untap target permanent not named Clever Conjurer. Activate only as a sorcery.
 */
val CleverConjurer = card("Clever Conjurer") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Gnome Wizard"
    oracleText = "Mage Hand — {T}: Untap target permanent not named Clever Conjurer. Activate only as a sorcery."
    power = 2
    toughness = 3

    activatedAbility {
        cost = Costs.Tap
        val permanent = target(TargetFilter(GameObjectFilter.Permanent.notNamed("Clever Conjurer")))
        effect = Effects.Untap(permanent)
        timing = TimingRule.SorcerySpeed
        description = "Mage Hand — {T}: Untap target permanent not named Clever Conjurer."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Zoltan Boros"
        flavorText = "\"I'm always happy to lend a hand!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9784c97-3839-4a4b-ae8a-965844dbeb70.jpg?1783926517"
        ruling(
            "2021-07-23",
            "If Clever Conjurer somehow loses the name Clever Conjurer, perhaps because you mutated another " +
                "creature on top of it or copied it with Sakashima the Impostor, it can target itself with its " +
                "Mage Hand ability. How clever!"
        )
    }
}
