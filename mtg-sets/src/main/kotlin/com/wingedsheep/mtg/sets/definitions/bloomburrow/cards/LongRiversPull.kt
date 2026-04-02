package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Long River's Pull {U}{U}
 * Instant
 *
 * Gift a card (You may promise an opponent a gift as you cast this spell.
 * If you do, they draw a card before its other effects.)
 * Counter target creature spell. If the gift was promised, instead counter target spell.
 */
val LongRiversPull = card("Long River's Pull") {
    manaCost = "{U}{U}"
    typeLine = "Instant"
    oracleText = "Gift a card (You may promise an opponent a gift as you cast this spell. If you do, they draw a card before its other effects.)\nCounter target creature spell. If the gift was promised, instead counter target spell."

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 0: No gift — counter target creature spell
            Mode.withTarget(
                effect = Effects.CounterSpell(),
                target = Targets.CreatureSpell,
                description = "Counter target creature spell"
            ),
            // Mode 1: Gift promised — opponent draws a card, then counter target spell
            Mode.withTarget(
                effect = CompositeEffect(listOf(
                    DrawCardsEffect(1, EffectTarget.PlayerRef(Player.EachOpponent)),
                    Effects.CounterSpell(),
                    Effects.GiftGiven()
                )),
                target = Targets.Spell,
                description = "Gift a card — counter target spell"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "58"
        artist = "Raph Lomotan"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c81d0fa-81a1-4f9b-a5fd-5a648fd01dea.jpg?1721426147"
    }
}