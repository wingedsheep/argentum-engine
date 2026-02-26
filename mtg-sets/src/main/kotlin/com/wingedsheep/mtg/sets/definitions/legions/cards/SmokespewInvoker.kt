package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Smokespew Invoker
 * {2}{B}
 * Creature — Zombie Mutant
 * 3/1
 * {7}{B}: Target creature gets -3/-3 until end of turn.
 */
val SmokespewInvoker = card("Smokespew Invoker") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Zombie Mutant"
    power = 3
    toughness = 1
    oracleText = "{7}{B}: Target creature gets -3/-3 until end of turn."

    activatedAbility {
        cost = AbilityCost.Mana(ManaCost.parse("{7}{B}"))
        val t = target("target creature", TargetCreature())
        effect = Effects.ModifyStats(-3, -3, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "Thomas M. Baxa"
        flavorText = "The Mirari festers in its flesh."
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fea393a4-58c8-4a42-bd95-a3312504f2e2.jpg?1562946410"
    }
}
