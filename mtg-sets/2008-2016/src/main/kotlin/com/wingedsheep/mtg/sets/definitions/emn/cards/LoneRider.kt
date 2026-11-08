package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lone Rider // It That Rides as One (Eldritch Moon #33)
 *
 * Front: {1}{W} Creature — Human Knight 1/1
 * First strike, lifelink
 * At the beginning of the end step, if you gained 3 or more life this turn, transform this creature.
 *
 * Back: Creature — Eldrazi Horror 4/4 (colorless)
 * First strike, trample, lifelink
 *
 * - "The end step" is every end step, not just yours: [Triggers.anyPlayer] (Angelic Accord's shape).
 * - The transform is an intervening "if" ([Conditions.YouGainedLifeThisTurnAtLeast]) — it counts the
 *   total life gained this turn, not net change, checked as the end step begins and again on
 *   resolution, so life gained during the end step can't make it trigger (rulings).
 */
private val LoneRiderFront = card("Lone Rider") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 1
    toughness = 1
    oracleText = "First strike, lifelink\n" +
        "At the beginning of the end step, if you gained 3 or more life this turn, transform this creature."

    keywords(Keyword.FIRST_STRIKE, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.YouGainedLifeThisTurnAtLeast(3)
        effect = Effects.Transform(EffectTarget.Self)
        description = "At the beginning of the end step, if you gained 3 or more life this turn, " +
            "transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "Daarken"
        flavorText = "\"Horse and rider are often of one mind.\"\n—Ulda, Gavony Rider"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a55b60a-5d90-4f73-984e-53fdcc0366e4.jpg?1783937519"
        ruling(
            "2016-07-13",
            "Lone Rider's last ability triggers even if it wasn't on the battlefield when you gained the " +
                "life this turn."
        )
        ruling(
            "2016-07-13",
            "Lone Rider's ability checks how much life you've gained during the turn, not what your life " +
                "total is compared to what it was when the turn began. For example, if you gained 4 life and " +
                "then lost 7 life this turn, it still triggers."
        )
        ruling(
            "2016-07-13",
            "You must have gained life before your end step begins. Lone Rider's last ability won't trigger " +
                "if you haven't, even if an ability triggering at the same time will cause you to gain life."
        )
        ruling(
            "2016-07-13",
            "In a Two-Headed Giant game, events that cause a player to gain life affect each player " +
                "separately, even though the result affects the team's life total. For example, if your " +
                "teammate's creatures with lifelink dealt 5 damage, your teammate gained life, not you, and " +
                "Lone Rider's last ability won't trigger."
        )
    }
}

private val ItThatRidesAsOne = card("It That Rides as One") {
    manaCost = ""
    colorIdentity = "W"
    typeLine = "Creature — Eldrazi Horror"
    power = 4
    toughness = 4
    oracleText = "First strike, trample, lifelink"

    keywords(Keyword.FIRST_STRIKE, Keyword.TRAMPLE, Keyword.LIFELINK)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "Daarken"
        flavorText = "\". . . but until this morning, I'd never seen them be of one body.\"\n—Ulda, Gavony Rider"
        imageUri = "https://cards.scryfall.io/normal/back/9/a/9a55b60a-5d90-4f73-984e-53fdcc0366e4.jpg?1783937519"
    }
}

val LoneRider: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = LoneRiderFront,
    backFace = ItThatRidesAsOne,
)
