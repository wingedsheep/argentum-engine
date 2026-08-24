package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Burning Fields
 * {4}{R}
 * Sorcery
 * Burning Fields deals 5 damage to target opponent or planeswalker.
 *
 * One sentence, one effect. The "opponent or planeswalker" half is a single target requirement
 * ([Targets.OpponentOrPlaneswalker]) rather than two, so the damage aims at whichever the caster
 * chose.
 */
val BurningFields = card("Burning Fields") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Burning Fields deals 5 damage to target opponent or planeswalker."

    spell {
        val victim = target("target", Targets.OpponentOrPlaneswalker)
        effect = Effects.DealDamage(5, victim)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "Yang Hong"
        flavorText = "\"In raiding and plundering, be like fire, in immovability like a mountain.\"\n—Sun Tzu, *Art of War* (trans. Giles)"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/dee12f01-581e-4a3c-a8b5-41bef2516781.jpg"
    }
}
