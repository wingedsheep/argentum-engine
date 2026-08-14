package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Volcanic Villain — Marvel Super Heroes #159 (common)
 * {2}{R} · Creature — Elemental Villain · 3/2
 *
 * Haste
 * Power-up — {5}{R}: Put two +1/+1 counters on this creature. (Activate each power-up ability
 * only once. Reduce the cost by its mana cost if it entered this turn.)
 *
 * Haste is what makes the power-up discount matter here beyond raw stats: `{5}{R}` − `{2}{R}` =
 * `{3}`, so on six mana this lands and immediately attacks as a 5/4, which is the whole plan.
 */
val VolcanicVillain = card("Volcanic Villain") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Villain"
    oracleText = "Haste\n" +
        "Power-up — {5}{R}: Put two +1/+1 counters on this creature. (Activate each power-up " +
        "ability only once. Reduce the cost by its mana cost if it entered this turn.)"
    power = 3
    toughness = 2

    keywords(Keyword.HASTE)

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{5}{R}")
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "159"
        artist = "Nino Is"
        flavorText = "\"Get out of my way or get burned.\"\n—Volcana, Marsha Rosenberg"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f48ba40-9934-40fd-a251-e15a68e772d2.jpg?1783902922"
    }
}
