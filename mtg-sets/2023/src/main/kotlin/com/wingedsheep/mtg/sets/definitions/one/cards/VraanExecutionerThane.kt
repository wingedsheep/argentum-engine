package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vraan, Executioner Thane
 * {1}{B}
 * Legendary Creature — Phyrexian Vampire
 * 2/2
 *
 * Whenever one or more other creatures you control die, each opponent loses 2 life and you gain
 * 2 life. This ability triggers only once each turn.
 */
val VraanExecutionerThane = card("Vraan, Executioner Thane") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Phyrexian Vampire"
    power = 2
    toughness = 2
    oracleText = "Whenever one or more other creatures you control die, each opponent loses 2 life and " +
        "you gain 2 life. This ability triggers only once each turn."

    triggeredAbility {
        trigger = Triggers.oneOrMoreOther(GameObjectFilter.Creature).die()
        oncePerTurn = true
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "114"
        artist = "Helge C. Balzer"
        flavorText = "\"Soon enough each praetor will have hired me to kill the others, and I will stand " +
            "alone as the true Father of Machines.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c4a3119-c70a-46ff-8ede-6356f2b7bc13.jpg?1783918037"
    }
}
