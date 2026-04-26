package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Keep Out
 * {1}{W}
 * Instant
 *
 * Choose one —
 * • Keep Out deals 4 damage to target tapped creature.
 * • Destroy target enchantment.
 */
val KeepOut = card("Keep Out") {
    manaCost = "{1}{W}"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Keep Out deals 4 damage to target tapped creature.\n• Destroy target enchantment."

    spell {
        effect = ModalEffect.chooseOne(
            Mode.withTarget(
                Effects.DealDamage(4, EffectTarget.ContextTarget(0)),
                Targets.TappedCreature,
                "Keep Out deals 4 damage to target tapped creature"
            ),
            Mode.withTarget(
                Effects.Destroy(EffectTarget.ContextTarget(0)),
                Targets.Enchantment,
                "Destroy target enchantment"
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Ron Spencer"
        flavorText = "\"We don't want your kind round here.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4ab1601c-634c-4f21-8926-ba3cb92008c1.jpg?1767956928"
    }
}
