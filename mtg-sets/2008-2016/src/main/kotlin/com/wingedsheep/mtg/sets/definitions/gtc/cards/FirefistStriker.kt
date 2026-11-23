package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Firefist Striker
 * {1}{R}
 * Creature — Human Soldier
 * 2/1
 * Battalion — Whenever this creature and at least two other creatures attack, target creature
 * can't block this turn.
 */
val FirefistStriker = card("Firefist Striker") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 1
    oracleText = "Battalion — Whenever this creature and at least two other creatures attack, target creature " +
        "can't block this turn."

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackerCountAtLeast(3)))
        val t = target(TargetFilter.Creature)
        effect = Effects.CantBlock(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Tyler Jacobson"
        flavorText = "The flaming fist of Boros holds the promise of both protection and destruction in its grasp."
        imageUri = "https://cards.scryfall.io/normal/front/c/c/ccbc2f22-4500-4c74-a1a2-51d8238c1d16.jpg?1783940125"
    }
}
