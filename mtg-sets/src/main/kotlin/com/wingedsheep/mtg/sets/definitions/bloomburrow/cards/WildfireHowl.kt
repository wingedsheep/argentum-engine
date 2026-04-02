package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wildfire Howl
 * {1}{R}{R}
 * Sorcery
 *
 * Gift a card (You may promise an opponent a gift as you cast this spell.
 * If you do, they draw a card before its other effects.)
 *
 * Wildfire Howl deals 2 damage to each creature. If the gift was promised,
 * instead Wildfire Howl deals 1 damage to any target and 2 damage to each creature.
 *
 * Note: Gift is modeled as a modal choice. Mode 1 = no gift, Mode 2 = gift.
 * In 2-player games, the opponent is auto-selected for the card draw.
 */
val WildfireHowl = card("Wildfire Howl") {
    manaCost = "{1}{R}{R}"
    typeLine = "Sorcery"
    oracleText = "Gift a card (You may promise an opponent a gift as you cast this spell. If you do, they draw a card before its other effects.)\nWildfire Howl deals 2 damage to each creature. If the gift was promised, instead Wildfire Howl deals 1 damage to any target and 2 damage to each creature."

    val damageToEachCreature = ForEachInGroupEffect(
        filter = GroupFilter.AllCreatures,
        effect = DealDamageEffect(2, EffectTarget.Self)
    )

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: No gift — just 2 damage to each creature
            Mode.noTarget(
                damageToEachCreature,
                "Don't promise a gift — deal 2 damage to each creature"
            ),
            // Mode 2: Gift a card — opponent draws, then 1 damage to any target, then 2 damage to each creature
            Mode.withTarget(
                CompositeEffect(
                    listOf(
                        DrawCardsEffect(1, EffectTarget.PlayerRef(Player.EachOpponent)),
                        DealDamageEffect(1, EffectTarget.ContextTarget(0)),
                        damageToEachCreature,
                        Effects.GiftGiven()
                    )
                ),
                Targets.Any,
                "Promise a gift — an opponent draws a card, then deal 1 damage to any target and 2 damage to each creature"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "162"
        artist = "Manuel Castañón"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/7392d397-9836-4df2-944d-c930c9566811.jpg?1721426754"
        ruling("2024-07-26", "If the gift was promised and the target is illegal as Wildfire Howl tries to resolve, it won't resolve and none of its effects will happen. No creatures will be dealt damage.")
    }
}
