package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Reckless Air Strike (M20 #154)
 * {R}  Sorcery
 *
 * Choose one —
 * • Reckless Air Strike deals 3 damage to target creature with flying.
 * • Destroy target artifact.
 */
val RecklessAirStrike = card("Reckless Air Strike") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Reckless Air Strike deals 3 damage to target creature with flying.\n" +
        "• Destroy target artifact."

    spell {
        effect = ModalEffect.chooseOne(
            mode("Reckless Air Strike deals 3 damage to target creature with flying") {
                val flyer = target(TargetFilter.Creature.withKeyword(Keyword.FLYING))
                effect = Effects.DealDamage(3, flyer)
            },
            mode("Destroy target artifact") {
                val artifact = target(TargetFilter.Artifact)
                effect = Effects.Destroy(artifact)
            },
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "154"
        artist = "Jakub Kasper"
        flavorText = "\"I have a *great* idea!\""
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68391b41-4a17-4450-a8ac-0ce8406fe8c1.jpg"
    }
}
