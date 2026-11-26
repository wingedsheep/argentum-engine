package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blood Reckoning — Magic 2013 #81
 * {3}{B} · Enchantment
 *
 * Whenever a creature attacks you or a planeswalker you control, that creature's controller loses
 * 1 life.
 *
 * An ANY-binding attack trigger fires once per declared attacker (Revenge of Ravens' shape); the
 * attacking creature's controller is read off the triggering entity.
 */
val BloodReckoning = card("Blood Reckoning") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature attacks you or a planeswalker you control, that creature's controller " +
        "loses 1 life."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.attackingYouOrYourPlaneswalkers()).attacks()
        effect = Effects.LoseLife(1, EffectTarget.ControllerOfTriggeringEntity)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Wayne Reynolds"
        flavorText = "\"There is no greater folly than standing against me.\"\n—Nicol Bolas"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24577bb2-61b0-4675-84e6-5d675b28fc0e.jpg?1783940498"
        ruling(
            "2012-07-01",
            "Blood Reckoning will trigger once for each creature that attacks you or a planeswalker you control."
        )
        ruling(
            "2012-07-01",
            "Creatures that are put onto the battlefield attacking were never declared as attackers and won't " +
                "cause Blood Reckoning to trigger."
        )
    }
}
