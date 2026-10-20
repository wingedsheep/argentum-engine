package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Undercity Upheaval — March of the Machine: The Aftermath #25 (canonical printing)
 * {1}{G}{G} · Sorcery
 *
 * Undergrowth — Distribute X +1/+1 counters among any number of target creatures you control, where
 * X is the number of creature cards in your graveyard as you cast this spell. Creatures you control
 * gain vigilance until end of turn.
 *
 * X is frozen as the spell is cast with `captureAtCast` (CR 601.2i), and the division is announced
 * at cast too (CR 601.2d) — read from the capture's amount — so a target that becomes illegal loses
 * its share rather than handing it to the others (per the rulings). Each target must get at least
 * one counter, so the target count is capped by the same graveyard count.
 */
val UndercityUpheaval = card("Undercity Upheaval") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Undergrowth — Distribute X +1/+1 counters among any number of target creatures you " +
        "control, where X is the number of creature cards in your graveyard as you cast this spell. " +
        "Creatures you control gain vigilance until end of turn."

    spell {
        captureAtCast("x", DynamicAmounts.creatureCardsInYourGraveyard())
        targets(
            TargetFilter.CreatureYouControl,
            unlimited = true,
            dynamicMaxCount = DynamicAmounts.creatureCardsInYourGraveyard(),
        )
        effect = Effects.DistributeCountersAmongTargets(DynamicAmounts.capturedAtCast("x")) then
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.youControl()),
                Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.IterationEntity)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Grady Frederick"
        flavorText = "Light shone on parts of the undercity for the first time in centuries."
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acfe6957-d971-413f-b075-01b995f1d7a3.jpg?1783916518"
        ruling("2023-05-12", "You choose how many targets Undercity Upheaval has and how the counters will be distributed as you cast the spell. Each target must receive at least one counter.")
        ruling("2023-05-12", "If some of the creatures are illegal targets as Undercity Upheaval tries to resolve, the original distribution of counters still applies and the counters that would have been put on the illegal targets are lost. They won't be put instead on a legal target.")
        ruling("2023-05-12", "If all of Undercity Upheaval's targets are illegal at the time the spell tries to resolve, it won't resolve and none of its effects will happen. Creatures you control won't gain vigilance.")
    }
}
