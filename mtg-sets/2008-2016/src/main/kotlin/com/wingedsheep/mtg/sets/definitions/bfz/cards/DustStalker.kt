package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dust Stalker
 * {2}{B}{R}
 * Creature — Eldrazi
 * 5/3
 * Devoid (This card has no color.)
 * Haste
 * At the beginning of each end step, if you control no other colorless creatures, return this
 * creature to its owner's hand.
 *
 * "Each end step" is `Triggers.anyPlayer.beginningOf(Step.END)` — every turn's end step, not only
 * yours (Archwing Dragon's shape). The intervening "if" (CR 603.4) is the negated, self-excluding
 * existence check `Conditions.YouControl(colorless creature, negate = true, excludeSelf = true)`
 * (Pugnacious Hammerskull's "you don't control another Dinosaur"): it is checked as the end step
 * begins and again on resolution, so a colorless creature that arrives in between stops the bounce.
 * The search excludes the Stalker itself, so the check never depends on its own colour (printed
 * ruling), and it reads projected colours, so a devoid creature counts as colorless and a creature
 * that has been given a colour does not.
 */
val DustStalker = card("Dust Stalker") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Eldrazi"
    power = 5
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "Haste\n" +
        "At the beginning of each end step, if you control no other colorless creatures, " +
        "return this creature to its owner's hand."

    keywords(Keyword.DEVOID, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.YouControl(
            GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless),
            negate = true,
            excludeSelf = true,
        )
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "202"
        artist = "Clint Cearley"
        flavorText = "The only traces it leaves are silent gusts and prodigious amounts of wreckage."
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2322e8c3-a983-4b3d-b6fa-f6b67f6fa8cc.jpg?1783938182"
        ruling(
            "2015-08-25",
            "Dust Stalker's last ability checks to see if you control another colorless creature as " +
                "the end step begins. If you do, the ability won't trigger. If you don't, the ability " +
                "will check again as it tries to resolve. If you somehow control another colorless " +
                "creature at that time, the ability won't do anything. You won't return Dust Stalker " +
                "to its owner's hand."
        )
        ruling(
            "2015-08-25",
            "Dust Stalker's last ability doesn't depend on Dust Stalker being colorless. If it gains " +
                "one or more colors, the ability will still look for another creature you control " +
                "that's colorless."
        )
    }
}
