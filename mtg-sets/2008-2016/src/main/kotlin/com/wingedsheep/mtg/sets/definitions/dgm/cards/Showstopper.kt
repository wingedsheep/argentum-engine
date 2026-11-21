package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Showstopper
 * {1}{B}{R}
 * Instant
 * Until end of turn, creatures you control gain "When this creature dies, it deals 2 damage to target
 * creature an opponent controls."
 *
 * The group is locked in at resolution: `ForEachInGroup` grants the trigger to each creature you control
 * then, one by one, so creatures you begin to control later in the turn get nothing. The granted trigger
 * belongs to the creature, so whoever controls it when it dies controls the trigger and picks a creature
 * one of *their* opponents controls (ruling). The dying creature is the damage source.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val Showstopper = card("Showstopper") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Instant"
    oracleText = "Until end of turn, creatures you control gain \"When this creature dies, it deals 2 damage " +
        "to target creature an opponent controls.\""

    val damageOnDeath = grantedTriggeredAbility {
        trigger = Triggers.self.dies()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.DealDamage(2, creature)
        description = "When this creature dies, it deals 2 damage to target creature an opponent controls."
    }

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.GrantTriggeredAbility(damageOnDeath, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "102"
        artist = "Steve Prescott"
        flavorText = "The audience quickly realized a few things: it wasn't a magic trick, there wasn't " +
            "candy in there, and they'd need new clothes."
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2fd1f68b-3f16-484e-95c9-5cfa8da218c9.jpg?1783940021"
        ruling("2013-04-15", "Creatures that come under your control after Showstopper resolves won't have the ability.")
        ruling(
            "2013-04-15",
            "If another player gains control of a creature you control after Showstopper resolves, and then " +
                "later that turn that creature dies, the creature's controller when it died controls the " +
                "triggered ability. That player chooses a creature that one of their opponents controls as a target."
        )
    }
}
