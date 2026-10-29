package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Dragonlord Atarka
 * {5}{R}{G}
 * Legendary Creature — Elder Dragon
 * 8/8
 *
 * Flying, trample
 * When Dragonlord Atarka enters, it deals 5 damage divided as you choose among any number of target
 * creatures and/or planeswalkers your opponents control.
 *
 * The targets and the division are announced as the trigger goes on the stack (CR 601.2d via
 * 603.3d); each target must get at least 1, so "any number" caps at five — the same shape as
 * Kuldotha Flamefiend, with Ureni's "creatures and/or planeswalkers your opponents control" filter.
 */
val DragonlordAtarka = card("Dragonlord Atarka") {
    manaCost = "{5}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Elder Dragon"
    power = 8
    toughness = 8
    oracleText = "Flying, trample\n" +
        "When Dragonlord Atarka enters, it deals 5 damage divided as you choose among any number of " +
        "target creatures and/or planeswalkers your opponents control."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        target = TargetObject(
            count = 5,
            minCount = 0,
            optional = true,
            filter = TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()),
            id = "target creatures and/or planeswalkers your opponents control",
        )
        effect = Effects.DividedDamage(total = 5, minTargets = 0, maxTargets = 5)
        description = "When Dragonlord Atarka enters, it deals 5 damage divided as you choose among any " +
            "number of target creatures and/or planeswalkers your opponents control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "216"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce0c3b45-cf1d-4d85-9fe3-fb26b4b15dfd.jpg?1783938573"
        ruling("2015-02-25", "You choose how many targets the ability has and how the damage is divided as you put the ability on the stack. Each target must receive at least 1 damage.")
        ruling("2015-02-25", "If some of the targets are illegal targets as the ability tries to resolve, the original division of damage still applies but no damage is dealt to the illegal targets. If all targets are illegal, the ability doesn't resolve.")
    }
}
