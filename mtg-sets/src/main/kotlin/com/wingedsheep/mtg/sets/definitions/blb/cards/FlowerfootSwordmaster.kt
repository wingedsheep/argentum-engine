package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flowerfoot Swordmaster
 * {W}
 * Creature — Mouse Soldier
 * 1/2
 *
 * Offspring {2} (You may pay an additional {2} as you cast this spell. If you do,
 * when this creature enters, create a 1/1 token copy of it.)
 *
 * Valiant — Whenever this creature becomes the target of a spell or ability you
 * control for the first time each turn, Mice you control get +1/+0 until end of turn.
 */
val FlowerfootSwordmaster = card("Flowerfoot Swordmaster") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Mouse Soldier"
    power = 1
    toughness = 2
    oracleText = "Offspring {2} (You may pay an additional {2} as you cast this spell. If you do, when this creature enters, create a 1/1 token copy of it.)\nValiant — Whenever this creature becomes the target of a spell or ability you control for the first time each turn, Mice you control get +1/+0 until end of turn."

    // Offspring modeled as Kicker
    keywordAbility(KeywordAbility.OptionalAdditionalCost(ManaCost.parse("{2}")))

    // Offspring ETB: create token copy when kicked
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.CreateTokenCopyOfSelf(overridePower = 1, overrideToughness = 1)
    }

    // Valiant — Mice you control get +1/+0 until end of turn
    triggeredAbility {
        trigger = Triggers.Valiant
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Mouse")).youControl(),
            effect = ModifyStatsEffect(
                powerModifier = 1,
                toughnessModifier = 0,
                target = EffectTarget.Self,
                duration = Duration.EndOfTurn
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97ff118f-9c3c-43a2-8085-980c7fe7d227.jpg?1721425843"
    }
}
