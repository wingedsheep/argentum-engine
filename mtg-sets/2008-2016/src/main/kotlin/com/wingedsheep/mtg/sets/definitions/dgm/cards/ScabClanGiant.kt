package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetChooser

/**
 * Scab-Clan Giant
 * {4}{R}{G}
 * Creature — Giant Warrior
 * 4/5
 * When this creature enters, it fights target creature an opponent controls chosen at random.
 *
 * The target is drawn at random as the trigger goes on the stack (it is still targeted: hexproof
 * creatures are out of the draw, and an illegal target at resolution means no fight).
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val ScabClanGiant = card("Scab-Clan Giant") {
    manaCost = "{4}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Giant Warrior"
    power = 4
    toughness = 5
    oracleText = "When this creature enters, it fights target creature an opponent controls chosen at random."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val theirs = target(TargetFilter.CreatureOpponentControls, chooser = TargetChooser.Random)
        effect = Effects.Fight(EffectTarget.Self, theirs)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "Zoltan Boros"
        flavorText = "For the Gruul, bone-crushing rage is the first priority. Accuracy comes in a distant ninetieth."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8e360ae-4c78-47a9-81d4-1849cfa518b7.jpg"
    }
}
