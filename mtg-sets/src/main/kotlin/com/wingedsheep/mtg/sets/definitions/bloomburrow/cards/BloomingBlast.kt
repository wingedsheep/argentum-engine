package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blooming Blast
 * {1}{R}
 * Instant
 *
 * Gift a Treasure (You may promise an opponent a gift as you cast this spell.
 * If you do, they create a Treasure token before its other effects.
 * It's an artifact with "{T}, Sacrifice this token: Add one mana of any color.")
 *
 * Blooming Blast deals 2 damage to target creature. If the gift was promised,
 * Blooming Blast also deals 3 damage to that creature's controller.
 *
 * Gift is modeled as a modal choice. Mode 1 = no gift (2 damage to creature),
 * Mode 2 = gift (Treasure for opponent + 2 damage to creature + 3 damage to controller).
 */
val BloomingBlast = card("Blooming Blast") {
    manaCost = "{1}{R}"
    typeLine = "Instant"
    oracleText = "Gift a Treasure (You may promise an opponent a gift as you cast this spell. If you do, they create a Treasure token before its other effects. It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")\nBlooming Blast deals 2 damage to target creature. If the gift was promised, Blooming Blast also deals 3 damage to that creature's controller."

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: No gift — 2 damage to target creature
            Mode.withTarget(
                Effects.DealDamage(2, EffectTarget.ContextTarget(0)),
                Targets.Creature,
                "Don't promise a gift — deal 2 damage to target creature"
            ),
            // Mode 2: Gift a Treasure — opponent creates Treasure, 2 damage to creature, 3 damage to controller
            Mode.withTarget(
                CreatePredefinedTokenEffect("Treasure", 1, EffectTarget.PlayerRef(Player.EachOpponent))
                    .then(Effects.DealDamage(2, EffectTarget.ContextTarget(0)))
                    .then(Effects.DealDamage(3, EffectTarget.TargetController))
                    .then(Effects.GiftGiven()),
                Targets.Creature,
                "Promise a gift — opponent creates a Treasure token, deal 2 damage to target creature and 3 damage to its controller"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "Jakob Eirich"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0cd92a83-cec3-4085-a929-3f204e3e0140.jpg?1721426579"
    }
}
