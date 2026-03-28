package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.BudgetModalEffect
import com.wingedsheep.sdk.scripting.effects.BudgetMode
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount


/**
 * Season of Loss
 * {3}{B}{B}
 * Sorcery
 *
 * Choose up to five {P} worth of modes. You may choose the same mode more than once.
 * {P} — Each player sacrifices a creature of their choice.
 * {P}{P} — Draw a card for each creature that died under your control this turn.
 * {P}{P}{P} — Each opponent loses X life, where X is the number of creature cards in your graveyard.
 */
val SeasonOfLoss = card("Season of Loss") {
    manaCost = "{3}{B}{B}"
    typeLine = "Sorcery"
    oracleText = "Choose up to five {P} worth of modes. You may choose the same mode more than once.\n" +
        "{P} — Each player sacrifices a creature of their choice.\n" +
        "{P}{P} — Draw a card for each creature that died under your control this turn.\n" +
        "{P}{P}{P} — Each opponent loses X life, where X is the number of creature cards in your graveyard."

    spell {
        effect = BudgetModalEffect(
            budget = 5,
            modes = listOf(
                BudgetMode(
                    cost = 1,
                    effect = ForEachPlayerEffect(
                        players = Player.Each,
                        effects = listOf(
                            ForceSacrificeEffect(
                                filter = GameObjectFilter.Creature,
                                count = 1,
                                target = EffectTarget.Controller
                            )
                        )
                    ),
                    description = "Each player sacrifices a creature"
                ),
                BudgetMode(
                    cost = 2,
                    effect = DrawCardsEffect(
                        count = DynamicAmount.CreaturesDiedThisTurn(Player.You),
                        target = EffectTarget.Controller
                    ),
                    description = "Draw a card for each creature that died under your control this turn"
                ),
                BudgetMode(
                    cost = 3,
                    effect = LoseLifeEffect(
                        amount = DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature),
                        target = EffectTarget.PlayerRef(Player.EachOpponent)
                    ),
                    description = "Each opponent loses X life, where X is the number of creature cards in your graveyard"
                )
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "112"
        artist = "Dominik Mayer"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc540652-916b-45c5-ae5a-0a0bc557cee1.jpg?1721426509"
    }
}
