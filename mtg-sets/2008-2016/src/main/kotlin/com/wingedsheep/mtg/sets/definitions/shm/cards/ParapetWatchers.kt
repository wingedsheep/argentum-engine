package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Parapet Watchers
 * {2}{U}
 * Creature — Kithkin Soldier
 * 2 / 2
 *
 * {W/U}: This creature gets +0/+1 until end of turn.
 *
 * - The hybrid `{W/U}` stays in the activation cost as written; it is payable with either {W} or
 *   {U} and the parser derives that from the symbol.
 * - The power modifier is an explicit `0` so the effect still matches the printed "+0/+1"; no
 *   target is declared because "this creature" is the source ([EffectTarget.Self]).
 */
val ParapetWatchers = card("Parapet Watchers") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Kithkin Soldier"
    power = 2
    toughness = 2
    oracleText = "{W/U}: This creature gets +0/+1 until end of turn."

    activatedAbility {
        cost = Costs.Mana("{W/U}")
        effect = Effects.ModifyStats(0, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Scott Altmann"
        flavorText = "A kithkin doun is not so much a town as a fortress, built to withstand the constantly besieging darkness. Only those most watchful and trustworthy are tasked with guarding its walls."
        imageUri = "https://cards.scryfall.io/normal/front/4/9/499f9987-87d8-4cd3-98c4-b6976c70739e.jpg?1783942760"
    }
}
