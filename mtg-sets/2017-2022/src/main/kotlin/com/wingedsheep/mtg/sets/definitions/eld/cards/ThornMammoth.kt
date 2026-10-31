package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thorn Mammoth
 * {5}{G}{G}
 * Creature — Elephant
 * 6/6
 * Trample
 * Whenever this creature or another creature you control enters, this creature fights up to one
 * target creature you don't control.
 *
 * The inclusive "this creature or another creature you control enters" shape is
 * `Triggers.a(Creature.youControl()).enters()` (binding ANY, as Fallaji Vanguard); the fight is
 * Wicked Wolf's, with `optional = true` for "up to one".
 */
val ThornMammoth = card("Thorn Mammoth") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elephant"
    oracleText = "Trample\n" +
        "Whenever this creature or another creature you control enters, this creature fights up to one " +
        "target creature you don't control."
    power = 6
    toughness = 6

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        val foe = target(
            TargetFilter(
                GameObjectFilter.Creature.copy(
                    controllerPredicate = ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            ),
            optional = true
        )
        effect = Effects.Fight(EffectTarget.Self, foe)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "323"
        artist = "Svetlin Velinov"
        flavorText = "It will come to the defense of all wild things, from the shivering wren to the towering giant."
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80138656-b2e2-43bf-9fe6-7a852f53f9e9.jpg"
    }
}
