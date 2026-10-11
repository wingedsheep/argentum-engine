package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tear Asunder — Dominaria United #183
 * {1}{G} · Instant
 *
 * Kicker {1}{B}
 * Exile target artifact or enchantment. If this spell was kicked, exile target nonland
 * permanent instead.
 *
 * "Instead" swaps the target restriction, so the kicked cast announces a different target
 * through the `kickerTarget` / `kickerEffect` slots (as Expel the Unworthy does).
 */
val TearAsunder = card("Tear Asunder") {
    manaCost = "{1}{G}"
    colorIdentity = "BG"
    typeLine = "Instant"
    oracleText = "Kicker {1}{B} (You may pay an additional {1}{B} as you cast this spell.)\n" +
        "Exile target artifact or enchantment. If this spell was kicked, exile target nonland permanent instead."

    keywordAbility(KeywordAbility.kicker("{1}{B}"))

    spell {
        val artifactOrEnchantment = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.Exile(artifactOrEnchantment)

        val nonlandPermanent = kickerTarget(TargetFilter.NonlandPermanent)
        kickerEffect = Effects.Exile(nonlandPermanent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "183"
        artist = "Dave Kendall"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/629aa907-9533-4681-9bf2-9e56450a4cc2.jpg?1783921294"
    }
}
