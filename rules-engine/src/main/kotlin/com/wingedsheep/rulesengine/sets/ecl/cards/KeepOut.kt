package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.DealDamageEffect
import com.wingedsheep.rulesengine.ability.DestroyEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ModalEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.ManaCost

/**
 * Keep Out
 *
 * {1}{W} Instant
 * Choose one —
 * • Keep Out deals 4 damage to target tapped creature.
 * • Destroy target enchantment.
 */
object KeepOut {
    val definition = CardDefinition.instant(
        name = "Keep Out",
        manaCost = ManaCost.parse("{1}{W}"),
        oracleText = "Choose one —\n" +
                "• Keep Out deals 4 damage to target tapped creature.\n" +
                "• Destroy target enchantment.",
        metadata = ScryfallMetadata(
            collectorNumber = "19",
            rarity = Rarity.COMMON,
            artist = "Ron Spencer",
            flavorText = "\"We don't want your kind round here.\"",
            imageUri = "https://cards.scryfall.io/normal/front/4/a/4ab1601c-634c-4f21-8926-ba3cb92008c1.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Keep Out") {
        // Modal spell: Choose one
        spell(
            ModalEffect(
                modes = listOf(
                    // Mode 1: Deal 4 damage to target tapped creature
                    DealDamageEffect(
                        amount = 4,
                        target = EffectTarget.TargetTappedCreature
                    ),
                    // Mode 2: Destroy target enchantment
                    DestroyEffect(
                        target = EffectTarget.TargetEnchantment
                    )
                ),
                chooseCount = 1
            )
        )
    }
}
