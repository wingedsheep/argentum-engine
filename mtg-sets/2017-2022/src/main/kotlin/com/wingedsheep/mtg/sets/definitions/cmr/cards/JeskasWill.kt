package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Jeska's Will — Commander Legends #187 (canonical printing)
 * {2}{R} · Sorcery
 *
 * Choose one. If you control a commander as you cast this spell, you may choose both instead.
 * • Add {R} for each card in target opponent's hand.
 * • Exile the top three cards of your library. You may play them this turn.
 *
 * The conditional modal count is the Flame of Anor cast-time `dynamicChooseCount`: the floor stays
 * `minChooseCount = 1`, and the cap is 2 when [Conditions.YouControlACommander] holds as the spell
 * is cast (any player's commander counts), otherwise 1. The mana amount counts the targeted
 * opponent's hand on resolution; the second mode is the standard impulse draw.
 */
val JeskasWill = card("Jeska's Will") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose one. If you control a commander as you cast this spell, you may choose both instead.\n" +
        "• Add {R} for each card in target opponent's hand.\n" +
        "• Exile the top three cards of your library. You may play them this turn."

    spell {
        modal(
            chooseCount = 2,
            minChooseCount = 1,
            dynamicChooseCount = DynamicAmounts.conditional(
                condition = Conditions.YouControlACommander,
                ifTrue = 2,
                ifFalse = 1
            )
        ) {
            mode("Add {R} for each card in target opponent's hand") {
                val opponent = target(Targets.Opponent)
                effect = Effects.AddMana(Color.RED, DynamicAmounts.count(opponent.asPlayer, Zone.HAND))
            }
            mode("Exile the top three cards of your library. You may play them this turn") {
                effect = Patterns.Exile.impulse(3)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "187"
        artist = "Izzy"
        flavorText = "\"Gather your strength. Prepare yourself.\"\n—Jeska, to Radha"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e91d96d-cc69-439b-b876-a7d57039022c.jpg?1783928812"
        ruling(
            "2026-03-20",
            "Use the number of cards in the target opponent's hand as Jeska's Will resolves to determine " +
                "how much {R} to add."
        )
        ruling(
            "2026-03-20",
            "Even though the card is named after a specific character, controlling any commander will " +
                "satisfy its condition. The commander you control doesn't have to be your commander."
        )
        ruling(
            "2026-03-20",
            "Once you've chosen both modes for the spell, it doesn't matter whether you continue to " +
                "control a commander."
        )
    }
}
