package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Betor, Kin to All — Tarkir: Dragonstorm #172
 * {2}{W}{B}{G} · Legendary Creature — Spirit Dragon · 5/7
 *
 * Flying
 * At the beginning of your end step, if creatures you control have total toughness 10 or
 * greater, draw a card. Then if creatures you control have total toughness 20 or greater,
 * untap each creature you control. Then if creatures you control have total toughness 40
 * or greater, each opponent loses half their life, rounded up.
 */
val BetorKinToAll = card("Betor, Kin to All") {
    manaCost = "{2}{W}{B}{G}"
    colorIdentity = "WBG"
    typeLine = "Legendary Creature — Spirit Dragon"
    power = 5
    toughness = 7
    oracleText = "Flying\n" +
        "At the beginning of your end step, if creatures you control have total toughness 10 or " +
        "greater, draw a card. Then if creatures you control have total toughness 20 or greater, " +
        "untap each creature you control. Then if creatures you control have total toughness 40 " +
        "or greater, each opponent loses half their life, rounded up."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.YourEndStep
        // Intervening "if" (CR 603.4): the 10-toughness gate is checked both as the trigger
        // would go on the stack and again on resolution.
        triggerCondition = totalToughnessAtLeast(10)
        effect = Effects.DrawCards(1)
            // "Then if ... 20 or greater, untap each creature you control."
            .then(
                ConditionalEffect(
                    condition = totalToughnessAtLeast(20),
                    effect = ForEachInGroupEffect(
                        filter = GroupFilter.AllCreaturesYouControl,
                        effect = TapUntapEffect(EffectTarget.Self, tap = false)
                    )
                )
            )
            // "Then if ... 40 or greater, each opponent loses half their life, rounded up."
            // The loss amount is computed once from Player.Opponent's life, then applied to every
            // EachOpponent target — exact in 1v1. In multiplayer each opponent would lose half of a
            // single opponent's life rather than their own (a LoseLifeExecutor architecture limit,
            // not this card's); revisit if/when multiplayer is supported.
            .then(
                ConditionalEffect(
                    condition = totalToughnessAtLeast(40),
                    effect = Effects.LoseHalfLife(
                        roundUp = true,
                        target = EffectTarget.PlayerRef(Player.EachOpponent),
                        lifePlayer = Player.Opponent
                    )
                )
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "172"
        artist = "Alexander Ostrowski"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b475b071-5545-483e-a397-89451f258602.jpg?1743204665"
    }
}

/** "Creatures you control have total toughness [threshold] or greater." */
private fun totalToughnessAtLeast(threshold: Int): Condition = Compare(
    DynamicAmount.AggregateBattlefield(
        player = Player.You,
        filter = GameObjectFilter.Creature,
        aggregation = Aggregation.SUM,
        property = CardNumericProperty.TOUGHNESS
    ),
    ComparisonOperator.GTE,
    DynamicAmount.Fixed(threshold)
)
