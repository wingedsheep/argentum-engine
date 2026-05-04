package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Remnant Elemental
 * {1}{R}
 * Creature — Elemental
 * Reach
 * Landfall — Whenever a land you control enters, this creature gets +2/+0 until end of turn.
 * 0/4
 */
val RemnantElemental = card("Remnant Elemental") {
    manaCost = "{1}{R}"
    typeLine = "Creature — Elemental"
    oracleText = "Reach\nLandfall — Whenever a land you control enters, this creature gets +2/+0 until end of turn."
    power = 0
    toughness = 4

    keywords(Keyword.REACH)

    // Landfall triggered ability: +2/+0 until end of turn when land enters
    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "Nereida"
        flavorText = "Even until Exodus Day, those in power on Kavaron refused to acknowledge that the planet was trying to tell them something."
        imageUri = "https://cards.scryfall.io/normal/front/8/3/830d5532-3b24-470e-912f-f0f5df1cb530.jpg?1752947179"
    }
}
