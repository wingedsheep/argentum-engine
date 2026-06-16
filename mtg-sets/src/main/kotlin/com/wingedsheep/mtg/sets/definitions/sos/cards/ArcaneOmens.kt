package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Arcane Omens
 * {4}{B}
 * Sorcery
 *
 * Converge — Target player discards X cards, where X is the number of colors of mana spent to
 * cast this spell.
 *
 * X is resolved while the spell is still on the stack, so it reads the live per-colour payment
 * buckets via [DynamicAmounts.colorsOfManaSpent] (`DynamicAmount.DistinctColorsManaSpent`).
 */
val ArcaneOmens = card("Arcane Omens") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Converge — Target player discards X cards, where X is the number of colors of " +
        "mana spent to cast this spell."

    spell {
        val player = target("target player", TargetPlayer())
        effect = Effects.Discard(DynamicAmounts.colorsOfManaSpent(), player)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d357d997-9d4e-4ade-81f2-37629853f13a.jpg?1775937419"
    }
}
