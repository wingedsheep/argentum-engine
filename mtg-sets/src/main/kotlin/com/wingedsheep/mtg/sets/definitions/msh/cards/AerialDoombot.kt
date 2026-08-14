package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aerial Doombot — Marvel Super Heroes #43 (common)
 * {U} · Artifact Creature — Robot Villain · 1/1
 *
 * Flying
 * Power-up — {5}{U}: Put three +1/+1 counters on this creature. (Activate each power-up ability
 * only once. Reduce the cost by its mana cost if it entered this turn.)
 *
 * The extreme end of the power-up curve's discount: a one-mana body whose mana cost is a single
 * `{U}`, so the reduction only cancels the ability's own `{U}` pip and leaves `{5}` — six mana
 * total on the turn it lands rather than seven. Compare Brave Brawler, where the generic half of
 * the mana cost does most of the work.
 */
val AerialDoombot = card("Aerial Doombot") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Artifact Creature — Robot Villain"
    oracleText = "Flying\n" +
        "Power-up — {5}{U}: Put three +1/+1 counters on this creature. (Activate each power-up " +
        "ability only once. Reduce the cost by its mana cost if it entered this turn.)"
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{5}{U}")
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 3, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "43"
        artist = "Michael MacRae"
        flavorText = "\"I have seen your rightful place. Beneath me.\"\n—Doctor Doom"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e727ec1c-dc3b-4f1a-8a62-18549f118b89.jpg?1783902963"
    }
}
