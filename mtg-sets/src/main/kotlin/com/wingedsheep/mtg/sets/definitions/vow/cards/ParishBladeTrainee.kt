package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.training
import com.wingedsheep.sdk.model.Rarity

/**
 * Parish-Blade Trainee
 * {1}{W}
 * Creature — Human Soldier
 * 1/2
 * Training (Whenever this creature attacks with another creature with greater power, put a
 * +1/+1 counter on this creature.)
 * When this creature dies, put its counters on target creature you control.
 *
 * Two independent pieces:
 *  - [training] gives the keyword + the attack trigger, which grows the Trainee with +1/+1
 *    counters over the course of combats.
 *  - A dies trigger ([Triggers.Dies]) that relocates *its counters* onto a creature you control.
 *    [Effects.MoveAllLastKnownCounters] reads the Trainee's counters from last-known information
 *    (the permanent is already in the graveyard when the trigger resolves) and moves every kind,
 *    faithful to "put its counters" — so the +1/+1 counters Training accrued aren't wasted.
 */
val ParishBladeTrainee = card("Parish-Blade Trainee") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 2
    oracleText = "Training (Whenever this creature attacks with another creature with greater " +
        "power, put a +1/+1 counter on this creature.)\n" +
        "When this creature dies, put its counters on target creature you control."

    training()

    triggeredAbility {
        trigger = Triggers.Dies
        val recipient = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.MoveAllLastKnownCounters(recipient)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "29"
        artist = "Zara Alfonso"
        flavorText = "The recruits trained in eternal night, fueled by the hope that they might " +
            "one day see the sun again."
        imageUri = "https://cards.scryfall.io/normal/front/9/8/9845cdf2-e5ba-44a0-8136-72a1eb03a6a1.jpg?1783924914"
    }
}
