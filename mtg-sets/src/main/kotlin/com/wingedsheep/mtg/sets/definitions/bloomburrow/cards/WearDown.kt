package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wear Down
 * {1}{G}
 * Sorcery
 *
 * Gift a card (You may promise an opponent a gift as you cast this spell.
 * If you do, they draw a card before its other effects.)
 *
 * Destroy target artifact or enchantment. If the gift was promised, instead
 * destroy two target artifacts and/or enchantments.
 *
 * Gift is modeled as a modal choice. Mode 1 = no gift (1 target),
 * Mode 2 = gift (2 targets, both chosen at cast time).
 */
val WearDown = card("Wear Down") {
    manaCost = "{1}{G}"
    typeLine = "Sorcery"
    oracleText = "Gift a card (You may promise an opponent a gift as you cast this spell. If you do, they draw a card before its other effects.)\nDestroy target artifact or enchantment. If the gift was promised, instead destroy two target artifacts and/or enchantments."

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: No gift — destroy one artifact or enchantment
            Mode.withTarget(
                Effects.Destroy(EffectTarget.ContextTarget(0)),
                Targets.ArtifactOrEnchantment,
                "Don't promise a gift — destroy target artifact or enchantment"
            ),
            // Mode 2: Gift a card — opponent draws, then destroy two artifacts and/or enchantments
            Mode(
                effect = CompositeEffect(
                    listOf(
                        DrawCardsEffect(1, EffectTarget.PlayerRef(Player.EachOpponent)),
                        Effects.Destroy(EffectTarget.ContextTarget(0)),
                        Effects.Destroy(EffectTarget.ContextTarget(1)),
                        Effects.GiftGiven()
                    )
                ),
                targetRequirements = listOf(Targets.ArtifactOrEnchantment, Targets.ArtifactOrEnchantment),
                description = "Promise a gift — an opponent draws a card, then destroy two target artifacts and/or enchantments"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "203"
        artist = "Iris Compiet"
        flavorText = "The birdfolk guards flitted angrily. \"Seriously, Timbles? Again?!\""
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fded2b83-3b7d-4c8c-83c4-0624a1069628.jpg?1721426996"

        ruling("2024-07-26", "As an additional cost to cast a spell with gift, you can promise the listed gift to an opponent. That opponent is chosen as part of that additional cost.")
        ruling("2024-07-26", "For instants and sorceries with gift, the gift is given to the appropriate opponent as part of the resolution of the spell. This happens before any of the spell's other effects would take place.")
        ruling("2024-07-26", "If the gift was promised and all targets are illegal as Wear Down tries to resolve, it won't resolve and none of its effects will happen.")
    }
}
