package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalETBOrLTBTriggers
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Yarok, the Desecrated (M20 #220)
 * {2}{B}{G}{U}  Legendary Creature — Elemental Horror  3/5
 *
 * Deathtouch, lifelink
 * If a permanent entering causes a triggered ability of a permanent you control to trigger,
 * that ability triggers an additional time.
 *
 * Starfield Vocalist's shape: any permanent entering counts (`mustBeYouControl = false` — per the
 * rulings you need only control the permanent with the triggered ability), entering direction only.
 */
val YarokTheDesecrated = card("Yarok, the Desecrated") {
    manaCost = "{2}{B}{G}{U}"
    colorIdentity = "BGU"
    typeLine = "Legendary Creature — Elemental Horror"
    power = 3
    toughness = 5
    oracleText = "Deathtouch, lifelink\n" +
        "If a permanent entering causes a triggered ability of a permanent you control to trigger, " +
        "that ability triggers an additional time."

    keywords(Keyword.DEATHTOUCH, Keyword.LIFELINK)

    staticAbility {
        ability = AdditionalETBOrLTBTriggers(
            filter = GameObjectFilter.Any,
            mustBeYouControl = false,
            description = "If a permanent entering causes a triggered ability of a permanent you control " +
                "to trigger, that ability triggers an additional time"
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "220"
        artist = "Daarken"
        flavorText = "\"Yarok grieves within the waste\nOf Bala Ged's corrupted land.\"\n—Lament for Bala Ged, stanza 1"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a1001d43-e11b-4e5e-acd4-4a50ef89977f.jpg?1783932948"
        ruling(
            "2019-07-12",
            "Yarok affects a permanent's own enters-the-battlefield triggered abilities as well as other " +
                "triggered abilities that trigger when that permanent enters the battlefield. Such triggered " +
                "abilities start with \"when\" or \"whenever.\""
        )
        ruling(
            "2019-07-12",
            "Yarok's effect doesn't copy the triggered ability; it just causes the ability to trigger twice. " +
                "Any choices made as you put the ability onto the stack, such as modes and targets, are made " +
                "separately for each instance of the ability. Any choices made on resolution, such as whether " +
                "to put counters on a permanent, are also made individually."
        )
        ruling(
            "2019-07-12",
            "If a permanent entering the battlefield at the same time as Yarok (including Yarok itself) causes " +
                "a triggered ability of a permanent you control to trigger, that ability triggers an additional time."
        )
        ruling(
            "2019-07-12",
            "Abilities that apply \"as [this creature] enters the battlefield,\" such as choosing a color with " +
                "Diamond Knight, are also unaffected."
        )
        ruling(
            "2019-07-12",
            "If you somehow control two Yaroks, a permanent entering the battlefield causes abilities to trigger " +
                "three times, not four. A third Yarok causes abilities to trigger four times, a fourth causes " +
                "abilities to trigger five times, and so on."
        )
        ruling(
            "2019-07-12",
            "Replacement effects are unaffected by Yarok's ability. For example, a creature that enters the " +
                "battlefield with one +1/+1 counter on it won't receive an additional +1/+1 counter."
        )
        ruling(
            "2019-07-12",
            "You don't need to control the permanent entering the battlefield, only the permanent that has the " +
                "triggered ability."
        )
    }
}
