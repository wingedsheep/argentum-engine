package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nocturnal Hunger
 * {2}{B}
 * Instant
 *
 * Gift a Food (You may promise an opponent a gift as you cast this spell.
 * If you do, they create a Food token before its other effects.)
 *
 * Destroy target creature. If the gift wasn't promised, you lose 2 life.
 *
 * Gift is modeled as a modal choice. Mode 1 = no gift (destroy + lose life),
 * Mode 2 = gift (Food token for opponent + destroy).
 */
val NocturnalHunger = card("Nocturnal Hunger") {
    manaCost = "{2}{B}"
    typeLine = "Instant"
    oracleText = "Gift a Food (You may promise an opponent a gift as you cast this spell. If you do, they create a Food token before its other effects. It's an artifact with \"{2}, {T}, Sacrifice this artifact: You gain 3 life.\")\nDestroy target creature. If the gift wasn't promised, you lose 2 life."

    val destroyEffect = Effects.Destroy(EffectTarget.ContextTarget(0))

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: No gift — destroy target creature, you lose 2 life
            Mode.withTarget(
                destroyEffect.then(Effects.LoseLife(2, EffectTarget.Controller)),
                Targets.Creature,
                "Don't promise a gift — destroy target creature, you lose 2 life"
            ),
            // Mode 2: Gift a Food — opponent creates Food token, then destroy target creature
            Mode.withTarget(
                Effects.CreateFood(1, EffectTarget.PlayerRef(Player.EachOpponent))
                    .then(destroyEffect)
                    .then(Effects.GiftGiven()),
                Targets.Creature,
                "Promise a gift — opponent creates a Food token, then destroy target creature"
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Sam Guay"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/742c0409-9abd-4559-b52e-932cc90c531a.jpg?1721426459"

        ruling("2024-07-26", "For instants and sorceries with gift, the gift is given to the appropriate opponent as part of the resolution of the spell. This happens before any of the spell's other effects would take place.")
        ruling("2024-07-26", "If a spell for which the gift was promised is countered, doesn't resolve, or is otherwise removed from the stack, the gift won't be given. None of its other effects will happen either.")
    }
}
