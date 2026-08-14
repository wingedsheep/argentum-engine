package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Abomination, Terrifying Titan — Marvel Super Heroes #198 (uncommon)
 * {3}{R/G} · Legendary Creature — Gamma Villain · 4/4
 *
 * Trample
 * Power-up — {5}{R/G}{R/G}: Put a +1/+1 counter on Abomination. He fights up to one target
 * creature an opponent controls. (Activate each power-up ability only once. Reduce the cost by
 * his mana cost if he entered this turn.)
 *
 * The only hybrid power-up in the cycle, and the one that proves the reduction is genuinely
 * symbol-wise rather than color-wise: `{5}{R/G}{R/G}` − `{3}{R/G}` = `{2}{R/G}`, the reduction's
 * hybrid pip cancelling one of the ability's identical hybrid pips (CR 702.193b / 118.7).
 *
 * Printed order is load-bearing here: the counter goes on *before* the fight, so Abomination
 * fights as a 5/5. "Up to one target" is `optional = true`, so the ability can be activated purely
 * for the counter when the opponent has no creatures — and if the only legal target is removed in
 * response, the ability still resolves and still grows him.
 */
val AbominationTerrifyingTitan = card("Abomination, Terrifying Titan") {
    manaCost = "{3}{R/G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Gamma Villain"
    oracleText = "Trample\n" +
        "Power-up — {5}{R/G}{R/G}: Put a +1/+1 counter on Abomination. He fights up to one target " +
        "creature an opponent controls. (Activate each power-up ability only once. Reduce the " +
        "cost by his mana cost if he entered this turn.)"
    power = 4
    toughness = 4

    keywords(Keyword.TRAMPLE)

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{5}{R/G}{R/G}")
        val foe = target(
            "up to one target creature an opponent controls",
            TargetCreature(optional = true, filter = TargetFilter.CreatureOpponentControls)
        )
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Effects.Fight(EffectTarget.Self, foe)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "Piotr Dura"
        flavorText = "\"Stop me? Puny fools. Stop me how?\""
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c6ab9bb-dba2-4b5b-a4d9-54735f65ac21.jpg?1783902908"
    }
}
