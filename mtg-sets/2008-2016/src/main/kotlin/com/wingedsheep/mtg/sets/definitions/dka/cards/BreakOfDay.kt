package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Break of Day
 * {1}{W}
 * Instant
 * Creatures you control get +1/+1 until end of turn.
 * Fateful hour — If you have 5 or less life, those creatures gain indestructible until end of turn.
 *
 * Both the creature set and the fateful-hour life check are fixed on resolution (same shape as
 * Kytheon's Tactics); nothing changes control between the two passes, so "those creatures" are the
 * ones pumped.
 */
val BreakOfDay = card("Break of Day") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Creatures you control get +1/+1 until end of turn.\n" +
        "Fateful hour — If you have 5 or less life, those creatures gain indestructible until end of turn. " +
        "(Damage and effects that say \"destroy\" don't destroy them.)"

    spell {
        effect = Effects.ForEachInGroup(
            Filters.Group.creaturesYouControl,
            Effects.ModifyStats(1, 1, EffectTarget.IterationEntity)
        ) then Effects.If(
            condition = Conditions.LifeAtMost(5),
            then = Effects.ForEachInGroup(
                Filters.Group.creaturesYouControl,
                Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.IterationEntity)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e39da2a-814a-46bb-a1ca-fc5532ece842.jpg?1783940857"

        ruling("2013-07-01", "Because Break of Day affects only creatures you control when it resolves. It won't affect creatures that come under your control later in the turn.")
        ruling("2013-07-01", "Break of Day checks your life total only at the time it resolves. Any creatures that gain indestructible will continue to be indestructible that turn even if you gain life.")
    }
}
