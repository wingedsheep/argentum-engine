package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Foxfire Oak
 * {5}{G}
 * Creature — Treefolk Shaman
 * 3 / 6
 *
 * {R/G}{R/G}{R/G}: This creature gets +3/+0 until end of turn.
 *
 * - The three hybrid symbols stay in the activation cost as written; each `{R/G}` is independently
 *   payable with {R} or {G}, which the mana-cost parser derives — nothing is overridden.
 * - No target: "this creature" is the source, so the pump uses [EffectTarget.Self].
 */
val FoxfireOak = card("Foxfire Oak") {
    manaCost = "{5}{G}"
    typeLine = "Creature — Treefolk Shaman"
    power = 3
    toughness = 6
    oracleText = "{R/G}{R/G}{R/G}: This creature gets +3/+0 until end of turn."

    activatedAbility {
        cost = Costs.Mana("{R/G}{R/G}{R/G}")
        effect = Effects.ModifyStats(3, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Dave Kendall"
        flavorText = "\"The brethren shall blaze with unnatural fire, and the flame shall consume and purify our rage.\"\n" +
            "—Treefolk catastrophe myth"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46e23eae-7630-40db-b265-2fa00715878e.jpg?1783942743"
    }
}
