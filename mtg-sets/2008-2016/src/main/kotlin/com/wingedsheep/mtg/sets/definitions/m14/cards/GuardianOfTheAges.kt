package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Guardian of the Ages — Magic 2014 #211
 * {7} · Artifact Creature — Golem 7/7
 *
 * Defender
 * When a creature attacks you or a planeswalker you control, if this creature has defender, it
 * loses defender and gains trample.
 *
 * The attack trigger fires once per declared attacker aimed at you or one of your planeswalkers
 * (as Circle of Flame's does). The intervening "if" is checked both when it triggers and on
 * resolution, so when several creatures attack at once only the first trigger to resolve does
 * anything. Both changes are permanent — they don't wear off at end of turn.
 */
val GuardianOfTheAges = card("Guardian of the Ages") {
    manaCost = "{7}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Golem"
    power = 7
    toughness = 7
    oracleText = "Defender (This creature can't attack.)\nWhen a creature attacks you or a planeswalker " +
        "you control, if this creature has defender, it loses defender and gains trample."

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.attackingYouOrYourPlaneswalkers()).attacks()
        interveningIf = Conditions.SourceHasKeyword(Keyword.DEFENDER)
        effect = Effects.RemoveKeyword(Keyword.DEFENDER, EffectTarget.Self, Duration.Permanent) then
            Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self, Duration.Permanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "211"
        artist = "Ryan Pancoast"
        flavorText = "\"At my feet, invasions end.\"\n—Statue inscription"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c825c138-97de-44b9-8aec-70608ae035b6.jpg?1783939895"
        ruling(
            "2013-07-01",
            "The effect of Guardian of the Ages's triggered ability doesn't wear off as the turn ends. " +
                "Losing defender and gaining trample last indefinitely."
        )
        ruling(
            "2013-07-01",
            "Once Guardian of the Ages has lost defender, its triggered ability will no longer trigger when a " +
                "creature attacks you or a planeswalker you control (unless it has somehow gained defender again)."
        )
    }
}
