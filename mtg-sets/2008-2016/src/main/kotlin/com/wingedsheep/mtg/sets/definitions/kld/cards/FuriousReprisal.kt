package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Furious Reprisal
 * {3}{R}
 * Sorcery
 * Furious Reprisal deals 2 damage to each of two targets.
 *
 * Exactly two distinct targets (ruling: the same permanent or player can't be chosen twice).
 * Canonical definition lives in KLD; KLR and J25 are reprints.
 */
val FuriousReprisal = card("Furious Reprisal") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Furious Reprisal deals 2 damage to each of two targets."

    spell {
        target = AnyTarget(count = 2)
        effect = Effects.ForEachTarget(
            Effects.DealDamage(2, EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "115"
        artist = "Svetlin Velinov"
        flavorText = "\"Now that's the Chandra I've been waiting to see.\"\n—Liliana Vess"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3eeeecd-1d62-49f3-82b0-60f1f5e57d97.jpg?1783937194"
        ruling("2016-09-20", "You can't target the same permanent or player twice to have one recipient be dealt 4 damage.")
    }
}
