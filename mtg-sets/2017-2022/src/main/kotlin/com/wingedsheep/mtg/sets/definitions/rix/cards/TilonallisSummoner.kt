package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tilonalli's Summoner
 * {1}{R}
 * Creature — Human Shaman
 * 1/1
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * Whenever this creature attacks, you may pay {X}{R}. If you do, create X 1/1 red Elemental creature
 * tokens that are tapped and attacking. At the beginning of the next end step, exile those tokens
 * unless you have the city's blessing.
 *
 * The payment is `Effects.MayPayX(additionalCost = {R})` — X and the red pip in one payment, X = 0
 * still paying {R}. The delayed end-step trigger carries the created tokens and tests the city's
 * blessing when it resolves, so getting the blessing before then keeps them. Each token picks its
 * own defender (`CONTROLLER_CHOOSES`), per the ruling that it needn't attack what the Summoner attacks.
 */
val TilonallisSummoner = card("Tilonalli's Summoner") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    oracleText = "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "Whenever this creature attacks, you may pay {X}{R}. If you do, create X 1/1 red Elemental creature " +
        "tokens that are tapped and attacking. At the beginning of the next end step, exile those tokens " +
        "unless you have the city's blessing."
    power = 1
    toughness = 1

    keywords(Keyword.ASCEND)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPayX(
            additionalCost = ManaCost.parse("{R}"),
            then = Effects.CreateToken(
                count = DynamicAmounts.xValue(),
                power = 1,
                toughness = 1,
                colors = setOf(Color.RED),
                creatureTypes = setOf("Elemental"),
                tapped = true,
                attacking = true,
                attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
            ) then Effects.CreateDelayedTrigger(
                step = Step.END,
                carryCollections = listOf(CREATED_TOKENS),
                effect = Effects.If(
                    condition = Conditions.Not(Conditions.YouHaveCitysBlessing),
                    then = Effects.ForEachInCollection(
                        CollectionSlot.CreatedTokens,
                        Effects.Move(target = EffectTarget.IterationEntity, destination = Zone.EXILE),
                    ),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "121"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b107726-3a44-4b0f-86ef-4dbbf4473e7e.jpg?1783935291"
        ruling(
            "2018-01-19",
            "As each Elemental token enters the battlefield, you choose which opponent or opposing " +
                "planeswalker it's attacking. It doesn't have to attack the same player or planeswalker " +
                "that Tilonalli's Summoner is attacking."
        )
        ruling(
            "2018-01-19",
            "Whether you have the city's blessing is checked only as the delayed triggered ability " +
                "resolves during the end step. The tokens you create can help you ascend."
        )
    }
}
