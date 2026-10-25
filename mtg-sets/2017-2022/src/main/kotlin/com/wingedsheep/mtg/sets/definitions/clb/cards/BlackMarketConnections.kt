package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Black Market Connections
 * {2}{B}
 * Enchantment
 * At the beginning of your first main phase, choose one or more —
 * • Sell Contraband — Create a Treasure token. You lose 1 life.
 * • Buy Information — Draw a card. You lose 2 life.
 * • Hire a Mercenary — Create a 3/2 colorless Shapeshifter creature token with changeling. You lose 3 life.
 *
 * A first-main-phase trigger whose effect is a "choose one or more" [Effects.Modal]
 * (`chooseCount = 3, minChooseCount = 1`, Sentinel of Lost Lore's shape). Each mode is its reward
 * followed by its life loss — the loss is part of the mode's effect, not a cost, so it happens
 * regardless of the reward.
 */
val BlackMarketConnections = card("Black Market Connections") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your first main phase, choose one or more —\n" +
        "• Sell Contraband — Create a Treasure token. You lose 1 life.\n" +
        "• Buy Information — Draw a card. You lose 2 life.\n" +
        "• Hire a Mercenary — Create a 3/2 colorless Shapeshifter creature token with changeling. You lose 3 life. " +
        "(It is every creature type.)"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.PRECOMBAT_MAIN)
        effect = Effects.Modal(
            modes = listOf(
                mode("Sell Contraband — Create a Treasure token. You lose 1 life.") {
                    effect = Effects.CreateTreasure(
                        imageUri = "https://cards.scryfall.io/normal/front/7/7/770b988d-4f31-4746-8c23-4ed8d9a6b393.jpg?1783922318"
                    ) then Effects.LoseLife(1, EffectTarget.Controller)
                },
                mode("Buy Information — Draw a card. You lose 2 life.") {
                    effect = Effects.DrawCards(1) then Effects.LoseLife(2, EffectTarget.Controller)
                },
                mode("Hire a Mercenary — Create a 3/2 colorless Shapeshifter creature token with changeling. You lose 3 life.") {
                    effect = Effects.CreateToken(
                        power = 3,
                        toughness = 2,
                        creatureTypes = setOf("Shapeshifter"),
                        keywords = setOf(Keyword.CHANGELING),
                        imageUri = "https://cards.scryfall.io/normal/front/1/1/1186b536-e6c8-433d-a906-b29f334d619c.jpg?1783922314"
                    ) then Effects.LoseLife(3, EffectTarget.Controller)
                },
            ),
            chooseCount = 3,
            minChooseCount = 1,
        )
        description = "At the beginning of your first main phase, choose one or more — " +
            "Sell Contraband (Treasure, lose 1 life); Buy Information (draw, lose 2 life); " +
            "Hire a Mercenary (3/2 changeling, lose 3 life)."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "620"
        artist = "Evyn Fong"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b28572c-d2ba-4834-8630-3d82202ebb6f.jpg?1783922535"
    }
}
