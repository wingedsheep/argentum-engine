package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Revenge of Ravens — Throne of Eldraine #104
 * {3}{B} · Enchantment
 *
 * Whenever a creature attacks you or a planeswalker you control, that creature's controller loses
 * 1 life and you gain 1 life.
 *
 * An ANY-binding attack trigger fires once per declared attacker (Circle of Flame's shape); the
 * attacking creature's controller is read off the triggering entity.
 */
val RevengeOfRavens = card("Revenge of Ravens") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature attacks you or a planeswalker you control, that creature's controller " +
        "loses 1 life and you gain 1 life."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.attackingYouOrYourPlaneswalkers()).attacks()
        effect = Effects.LoseLife(1, EffectTarget.ControllerOfTriggeringEntity) then
            Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "Dmitry Burmak"
        flavorText = "Syr Tasdale declared that he never wanted to see the witch Ygretta again. Her familiars " +
            "quickly granted his request."
        imageUri = "https://cards.scryfall.io/normal/front/3/4/3474289a-193e-452d-b248-e53ee99e22c0.jpg?1783932635"
        ruling(
            "2019-10-04",
            "The triggered ability of Revenge of Ravens resolves before any abilities the attacking player " +
                "controls. If this causes the attacking player to lose the game, that player's triggered " +
                "abilities won't resolve and no combat damage will be dealt."
        )
    }
}
