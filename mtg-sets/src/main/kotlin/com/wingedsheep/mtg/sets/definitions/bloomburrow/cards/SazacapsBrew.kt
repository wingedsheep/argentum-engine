package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Sazacap's Brew
 * {1}{R}
 * Instant
 *
 * Gift a tapped Fish (You may promise an opponent a gift as you cast this spell.
 * If you do, they create a tapped 1/1 blue Fish creature token before its other effects.)
 *
 * As an additional cost to cast this spell, discard a card.
 *
 * Target player draws two cards. If the gift was promised, target creature you
 * control gets +2/+0 until end of turn.
 *
 * Note: Gift is modeled as a modal choice. Mode 1 = no gift, Mode 2 = gift.
 */
val SazacapsBrew = card("Sazacap's Brew") {
    manaCost = "{1}{R}"
    typeLine = "Instant"
    oracleText = "Gift a tapped Fish (You may promise an opponent a gift as you cast this spell. If you do, they create a tapped 1/1 blue Fish creature token before its other effects.)\nAs an additional cost to cast this spell, discard a card.\nTarget player draws two cards. If the gift was promised, target creature you control gets +2/+0 until end of turn."

    additionalCost(AdditionalCost.DiscardCards())

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: No gift — target player draws 2
            Mode.withTarget(
                DrawCardsEffect(2, EffectTarget.ContextTarget(0)),
                Targets.Player,
                "Don't promise a gift — target player draws two cards"
            ),
            // Mode 2: Gift a tapped Fish — opponent gets Fish token, target player draws 2,
            // target creature you control gets +2/+0 until end of turn
            Mode(
                effect = CompositeEffect(
                    listOf(
                        CreateTokenEffect(
                            count = DynamicAmount.Fixed(1),
                            power = 1,
                            toughness = 1,
                            colors = setOf(Color.BLUE),
                            creatureTypes = setOf("Fish"),
                            tapped = true,
                            controller = EffectTarget.PlayerRef(Player.EachOpponent),
                            imageUri = "https://cards.scryfall.io/normal/front/d/e/de0d6700-49f0-4233-97ba-cef7821c30ed.jpg?1721431109"
                        ),
                        DrawCardsEffect(2, EffectTarget.ContextTarget(0)),
                        ModifyStatsEffect(
                            powerModifier = 2,
                            toughnessModifier = 0,
                            target = EffectTarget.ContextTarget(1),
                            duration = Duration.EndOfTurn
                        ),
                        Effects.GiftGiven()
                    )
                ),
                targetRequirements = listOf(
                    Targets.Player,
                    Targets.CreatureYouControl
                ),
                description = "Promise a gift — opponent creates a tapped 1/1 blue Fish token, target player draws two cards, target creature you control gets +2/+0 until end of turn"
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "151"
        artist = "Sam Guay"
        imageUri = "https://cards.scryfall.io/normal/front/6/d/6d963080-b3ec-467d-82f7-39db6ecd6bbc.jpg?1721426699"
    }
}
