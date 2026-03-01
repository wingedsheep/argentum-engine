package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.TurnFaceDownEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mischievous Quanar
 * {4}{U}
 * Creature — Beast
 * 3/3
 * {3}{U}{U}: Turn Mischievous Quanar face down.
 * Morph {1}{U}{U}
 * When Mischievous Quanar is turned face up, copy target instant or sorcery spell.
 * You may choose new targets for the copy.
 */
val MischievousQuanar = card("Mischievous Quanar") {
    manaCost = "{4}{U}"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 3
    oracleText = "{3}{U}{U}: Turn Mischievous Quanar face down.\nMorph {1}{U}{U} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen Mischievous Quanar is turned face up, copy target instant or sorcery spell. You may choose new targets for the copy."

    // {3}{U}{U}: Turn this creature face down.
    activatedAbility {
        cost = Costs.Mana("{3}{U}{U}")
        effect = TurnFaceDownEffect(target = EffectTarget.Self)
    }

    morph = "{1}{U}{U}"

    // When turned face up, copy target instant or sorcery spell
    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        target = Targets.InstantOrSorcerySpell
        effect = Effects.CopyTargetSpell()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc48c2db-f5b4-4c24-a5fa-00750b7ff56f.jpg?1562535674"
        ruling("2013-04-15", "If a spell with split second is on the stack, you can still respond by turning this creature face up and targeting that spell with the trigger.")
    }
}
