package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.BudgetModalEffect
import com.wingedsheep.sdk.scripting.effects.BudgetMode
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfChosenPermanentEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount


/**
 * Season of Weaving
 * {4}{U}{U}
 * Sorcery
 *
 * Choose up to five {P} worth of modes. You may choose the same mode more than once.
 * {P} — Draw a card.
 * {P}{P} — Choose an artifact or creature you control. Create a token that's a copy of it.
 * {P}{P}{P} — Return each nonland, nontoken permanent to its owner's hand.
 */
val SeasonOfWeaving = card("Season of Weaving") {
    manaCost = "{4}{U}{U}"
    typeLine = "Sorcery"
    oracleText = "Choose up to five {P} worth of modes. You may choose the same mode more than once.\n" +
        "{P} — Draw a card.\n" +
        "{P}{P} — Choose an artifact or creature you control. Create a token that's a copy of it.\n" +
        "{P}{P}{P} — Return each nonland, nontoken permanent to its owner's hand."

    spell {
        effect = BudgetModalEffect(
            budget = 5,
            modes = listOf(
                BudgetMode(
                    cost = 1,
                    effect = DrawCardsEffect(
                        count = DynamicAmount.Fixed(1),
                        target = EffectTarget.Controller
                    ),
                    description = "Draw a card"
                ),
                BudgetMode(
                    cost = 2,
                    effect = CreateTokenCopyOfChosenPermanentEffect(
                        filter = GameObjectFilter.Artifact or GameObjectFilter.Creature
                    ),
                    description = "Choose an artifact or creature you control. Create a token that's a copy of it"
                ),
                BudgetMode(
                    cost = 3,
                    effect = EffectPatterns.returnAllToHand(
                        GroupFilter(
                            baseFilter = GameObjectFilter.NonlandPermanent.nontoken()
                        )
                    ),
                    description = "Return each nonland, nontoken permanent to its owner's hand"
                )
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "68"
        artist = "Wylie Beckert"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5713bb4-bdd9-4253-b6b9-e590532ed773.jpg?1721426229"
    }
}
