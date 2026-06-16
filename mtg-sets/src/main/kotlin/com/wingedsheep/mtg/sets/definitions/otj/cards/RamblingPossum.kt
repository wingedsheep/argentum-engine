package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rambling Possum
 * {2}{G}
 * Creature — Possum Mount
 * 3/3
 *
 * Whenever this creature attacks while saddled, it gets +1/+2 until end of turn. Then you may
 * return any number of creatures that saddled it this turn to their owner's hand.
 * Saddle 1 (Tap any number of other creatures you control with total power 1 or more: This Mount
 * becomes saddled until end of turn. Saddle only as a sorcery.)
 *
 * "While saddled" is an intervening-if (CR 603.4) on the attack trigger — it checks the source's
 * SaddledComponent both when the trigger would fire and again as it resolves. The "creatures that
 * saddled it this turn" set is read off the engine's source-relative
 * `crewedOrSaddledSourceThisTurn` filter (backed by CrewSaddleContributorsComponent), gathered as
 * an inline pipeline where the controller may select any number (0..all) to bounce. Moving a
 * battlefield permanent to HAND routes to its owner (MoveCollectionExecutor), so the return is
 * faithful even if control of a saddler changed after it saddled.
 */
val RamblingPossum = card("Rambling Possum") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Possum Mount"
    power = 3
    toughness = 3
    oracleText = "Whenever this creature attacks while saddled, it gets +1/+2 until end of turn. " +
        "Then you may return any number of creatures that saddled it this turn to their owner's " +
        "hand.\n" +
        "Saddle 1 (Tap any number of other creatures you control with total power 1 or more: This " +
        "Mount becomes saddled until end of turn. Saddle only as a sorcery.)"

    keywordAbility(KeywordAbility.saddle(1))

    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Conditions.SourceIsSaddled
        effect = Effects.Composite(
            Effects.ModifyStats(1, 2, EffectTarget.Self),
            Effects.Pipeline {
                val saddlers = gather(
                    CardSource.BattlefieldMatching(
                        filter = GameObjectFilter.Creature.crewedOrSaddledSourceThisTurn()
                    )
                )
                val chosen = chooseAnyNumber(from = saddlers, useTargetingUI = true)
                toHand(chosen)
            }
        )
        description = "Whenever this creature attacks while saddled, it gets +1/+2 until end of " +
            "turn. Then you may return any number of creatures that saddled it this turn to " +
            "their owner's hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "176"
        artist = "Adrián Rodríguez Pérez"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19d1e75f-0fee-4e07-9420-df771b696e85.jpg?1712401596"
    }
}
