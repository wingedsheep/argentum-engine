package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pyre Charger
 * {R}{R}
 * Creature — Elemental Warrior
 * 1 / 1
 *
 * Haste
 * {R}: This creature gets +1/+0 until end of turn.
 *
 * - No target: "this creature" is the source, so the pump uses [EffectTarget.Self]. Haste means the
 *   firebreathing is live the turn it enters.
 */
val PyreCharger = card("Pyre Charger") {
    manaCost = "{R}{R}"
    typeLine = "Creature — Elemental Warrior"
    power = 1
    toughness = 1
    oracleText = "Haste\n" +
        "{R}: This creature gets +1/+0 until end of turn."

    keywords(Keyword.HASTE)

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "Mark Zug"
        flavorText = "His blade was forged over coals of moaning treefolk, curved at the optimum angle for severing heads, and heated to volcanic temperatures by his touch."
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de320e0d-c1e9-4b5e-84a0-057f3f162bbf.jpg?1783942746"
    }
}
