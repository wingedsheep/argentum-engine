package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Thornplate Intimidator {3}{B}
 * Creature — Rat Rogue
 * 4/3
 *
 * Offspring {3}
 * When this creature enters, target opponent loses 3 life unless they sacrifice
 * a nonland permanent of their choice or discard a card.
 *
 * Modeled as a ChooseActionEffect: the opponent picks "Sacrifice", "Discard", or "Lose 3 life".
 * Infeasible options (no nonland permanents / no cards in hand) are filtered automatically.
 */
val ThornplateIntimidator = card("Thornplate Intimidator") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat Rogue"
    power = 4
    toughness = 3
    oracleText = "Offspring {3} (You may pay an additional {3} as you cast this spell. If you do, when this creature enters, create a 1/1 token copy of it.)\n" +
        "When this creature enters, target opponent loses 3 life unless they sacrifice a nonland permanent of their choice or discard a card."

    // Offspring modeled as Kicker
    keywordAbility(KeywordAbility.OptionalAdditionalCost(ManaCost.parse("{3}")))

    // Offspring ETB: create token copy when kicked
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.CreateTokenCopyOfSelf(overridePower = 1, overrideToughness = 1)
    }

    // ETB: target opponent chooses sacrifice, discard, or lose 3 life
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val opponent = target("target opponent", Targets.Opponent)
        effect = ChooseActionEffect(
            choices = listOf(
                EffectChoice(
                    label = "Sacrifice a nonland permanent",
                    effect = ForceSacrificeEffect(
                        filter = GameObjectFilter.NonlandPermanent,
                        count = 1,
                        target = opponent
                    ),
                    feasibilityCheck = FeasibilityCheck.ControlsPermanentMatching(GameObjectFilter.NonlandPermanent)
                ),
                EffectChoice(
                    label = "Discard a card",
                    effect = HandPatterns.discardCards(1, opponent),
                    feasibilityCheck = FeasibilityCheck.HasCardsInZone(Zone.HAND)
                ),
                EffectChoice(
                    label = "Lose 3 life",
                    effect = LoseLifeEffect(3, opponent)
                )
            ),
            player = opponent
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Daren Bader"
        imageUri = "https://cards.scryfall.io/normal/front/4/2/42f66c4a-feaa-4ba6-aa56-955b43329a9e.jpg?1721426537"
    }
}
