package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Somberwald Stag
 * {3}{G}{G}
 * Creature — Elk
 * 4/3
 * When this creature enters, you may have it fight target creature you don't control.
 *
 * The target is chosen as the trigger goes on the stack; the "you may" is decided on resolution
 * (ruling 2016-07-13), matching Atzocan Archer / Frontier Siege.
 */
val SomberwaldStag = card("Somberwald Stag") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elk"
    power = 4
    toughness = 3
    oracleText = "When this creature enters, you may have it fight target creature you don't control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val theirs = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            )
        )
        optional = true
        effect = Effects.Fight(EffectTarget.Self, theirs)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "169"
        artist = "Lake Hurwitz"
        flavorText = "Despite the reemergence of werewolves and the proliferation of Eldrazi, elk numbers in the Somberwald have remained steady."
        imageUri = "https://cards.scryfall.io/normal/front/6/5/651428a8-27af-4137-84f0-f71274a8b4a6.jpg?1783937438"
        ruling("2016-07-13", "You choose the target of the triggered ability as it goes on the stack, but you choose whether or not to fight as that ability resolves.")
        ruling("2016-07-13", "If the targeted creature is an illegal target when Somberwald Stag’s ability tries to resolve, the ability doesn’t resolve. If Somberwald Stag is no longer on the battlefield, the targeted creature won’t deal or be dealt damage.")
    }
}
