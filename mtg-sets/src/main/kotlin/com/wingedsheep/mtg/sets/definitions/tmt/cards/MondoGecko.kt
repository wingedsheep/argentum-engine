package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mondo Gecko
 * {1}{U}{U}
 * Legendary Creature — Lizard Mutant
 * 2/3
 *
 * {1}, Discard a card: Until end of turn, Mondo Gecko becomes the color of your
 * choice and gains hexproof from that color.
 * Whenever Mondo Gecko deals combat damage to a player, draw a card for each color
 * among permanents you control.
 */
val MondoGecko = card("Mondo Gecko") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Lizard Mutant"
    oracleText = "{1}, Discard a card: Until end of turn, Mondo Gecko becomes the color of your choice and gains hexproof from that color.\nWhenever Mondo Gecko deals combat damage to a player, draw a card for each color among permanents you control."
    power = 2
    toughness = 3

    // One color choice drives both halves: ChangeColorToChosen and GrantHexproofFromChosenColor
    // each read the color stamped on the context by ChooseColorThen.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Discard())
        effect = Effects.ChooseColorThen(
            Effects.Composite(
                listOf(
                    Effects.ChangeColorToChosen(EffectTarget.Self, Duration.EndOfTurn),
                    Effects.GrantHexproofFromChosenColor(EffectTarget.Self, Duration.EndOfTurn)
                )
            )
        )
        description = "{1}, Discard a card: Until end of turn, Mondo Gecko becomes the color of your choice and gains hexproof from that color."
    }

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.DrawCards(DynamicAmounts.colorsAmongPermanents())
        description = "Whenever Mondo Gecko deals combat damage to a player, draw a card for each color among permanents you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "46"
        artist = "Maël Ollivier-Henry"
        flavorText = "\"Stealth-o-rama, huh? Righteous.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/6/665e44f9-bc0d-40aa-83a4-f7fe64f2506d.jpg?1769005708"
    }
}
