package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConniveEffect

/**
 * Scorpion, Seething Striker
 * {3}{B}
 * Legendary Creature — Scorpion Human Villain
 * 3/3
 * Deathtouch
 * At the beginning of your end step, if a creature died this turn, target creature
 *   you control connives.
 */
val ScorpionSeethingStriker = card("Scorpion, Seething Striker") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Scorpion Human Villain"
    power = 3
    toughness = 3
    oracleText = "Deathtouch\nAt the beginning of your end step, if a creature died this turn, target creature you control connives. (Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on that creature.)"

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.CreatureDiedThisTurn
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = ConniveEffect(target = creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "64"
        artist = "Simon Dominic"
        flavorText = "The scorpion is the natural predator of the spider, and so Mac Gargan hunts."
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf407e08-b27f-42ba-b824-75846a80e238.jpg?1757377158"
    }
}
