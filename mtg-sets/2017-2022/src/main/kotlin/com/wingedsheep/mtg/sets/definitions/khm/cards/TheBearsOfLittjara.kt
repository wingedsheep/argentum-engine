package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Bears of Littjara
 * {1}{G}{U}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Create a 2/2 blue Shapeshifter creature token with changeling.
 * II — Any number of target Shapeshifter creatures you control have base power and toughness 4/4.
 * III — Choose up to one target creature or planeswalker. Each creature with power 4 or greater
 *       you control deals damage equal to its power to that permanent.
 *
 * Chapter II's base-P/T setting lasts indefinitely (layer 7b, [Duration.Permanent]) — it does not
 * expire at end of turn. Chapter III checks "power 4 or greater" as it resolves, and each qualifying
 * creature is its own damage source (Bartz and Boko shape). With no target chosen, nothing happens.
 */
val TheBearsOfLittjara = card("The Bears of Littjara") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Create a 2/2 blue Shapeshifter creature token with changeling.\n" +
        "II — Any number of target Shapeshifter creatures you control have base power and toughness 4/4.\n" +
        "III — Choose up to one target creature or planeswalker. Each creature with power 4 or greater " +
        "you control deals damage equal to its power to that permanent."

    sagaChapter(1) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Shapeshifter"),
            keywords = setOf(Keyword.CHANGELING),
            imageUri = "https://cards.scryfall.io/normal/front/e/f/ef775ad0-b1a9-4254-ab6f-304558bb77a1.jpg?1783928082",
        )
    }

    sagaChapter(2) {
        targets(TargetFilter.CreatureYouControl.withSubtype("Shapeshifter"), unlimited = true)
        effect = Effects.ForEachTarget(
            Effects.SetBasePowerAndToughness(4, 4, EffectTarget.ContextTarget(0), Duration.Permanent)
        )
    }

    sagaChapter(3) {
        val victim = target(Targets.CreatureOrPlaneswalker, optional = true)
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl.powerAtLeast(4),
            effect = Effects.DealDamage(
                amount = DynamicAmounts.powerOf(EffectTarget.IterationEntity),
                target = victim,
                damageSource = EffectTarget.IterationEntity,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "205"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be43a643-6cb7-4bb6-a28d-8ad30b7fbb6d.jpg?1783928200"
        ruling("2021-02-05", "The effect of the chapter II ability lasts indefinitely. It doesn't expire at end of turn.")
        ruling("2021-02-05", "The chapter II ability overwrites any previous effects that set any of the Shapeshifters' base power and toughness to specific values. Any effects that set a Shapeshifter's base power and toughness that start to apply after the chapter II ability will override the effect of that ability.")
    }
}
