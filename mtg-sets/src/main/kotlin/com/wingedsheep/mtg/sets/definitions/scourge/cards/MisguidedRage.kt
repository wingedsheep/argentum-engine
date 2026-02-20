package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Misguided Rage
 * {2}{R}
 * Sorcery
 * Target player sacrifices a permanent.
 */
val MisguidedRage = card("Misguided Rage") {
    manaCost = "{2}{R}"
    typeLine = "Sorcery"
    oracleText = "Target player sacrifices a permanent."

    spell {
        target = Targets.Player
        effect = ForceSacrificeEffect(GameObjectFilter.Permanent, 1, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "99"
        artist = "Pete Venters"
        flavorText = "\"If you don't have a target, you can always hit yourself.\"\n—Goblin military manual"
        imageUri = "https://cards.scryfall.io/large/front/7/4/74b5e00a-fef0-4711-9112-2fd067321890.jpg?1562530657"
    }
}
