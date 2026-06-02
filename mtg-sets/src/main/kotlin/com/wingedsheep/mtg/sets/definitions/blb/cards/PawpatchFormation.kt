package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pawpatch Formation
 * {1}{G}
 * Instant
 *
 * Choose one —
 * - Destroy target creature with flying.
 * - Destroy target enchantment.
 * - Draw a card. Create a Food token.
 */
val PawpatchFormation = card("Pawpatch Formation") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Destroy target creature with flying.\n" +
        "• Destroy target enchantment.\n" +
        "• Draw a card. Create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: Destroy target creature with flying
            Mode.withTarget(
                Effects.Destroy(EffectTarget.ContextTarget(0)),
                Targets.CreatureWithKeyword(Keyword.FLYING),
                "Destroy target creature with flying"
            ),
            // Mode 2: Destroy target enchantment
            Mode.withTarget(
                Effects.Destroy(EffectTarget.ContextTarget(0)),
                Targets.Enchantment,
                "Destroy target enchantment"
            ),
            // Mode 3: Draw a card, create a Food token
            Mode.noTarget(
                Effects.Composite(
                    listOf(
                        Effects.DrawCards(1),
                        Effects.CreateFood()
                    )
                ),
                "Draw a card. Create a Food token."
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "186"
        artist = "Julia Griffin"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b82c20ad-0f69-4822-ae76-770832cccdf7.jpg?1721426886"
    }
}
