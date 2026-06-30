package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect

/**
 * Mentor of the Meek
 * {2}{W}
 * Creature — Human Soldier
 * 3/2
 * Whenever another creature you control with power 2 or less enters, you may pay {1}.
 * If you do, draw a card.
 *
 * Canonical printing lives here (Innistrad, 2011 — earliest real-expansion printing). FDN, M19,
 * 2X2, INR, etc. are reprints (Printing rows only).
 *
 * "Power 2 or less" is a battlefield-state characteristic, so the trigger filter resolves through
 * projected state. The optional {1} payment is modeled with [MayPayManaEffect] (the engine's
 * "you may pay {cost}; if you do, <effect>" gate) wrapping [Effects.DrawCards].
 */
val MentorOfTheMeek = card("Mentor of the Meek") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 2
    oracleText = "Whenever another creature you control with power 2 or less enters, you may pay {1}. " +
        "If you do, draw a card."

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.youControl().powerAtMost(2),
            binding = TriggerBinding.OTHER
        )
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{1}"),
            effect = Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "Jana Schirmer & Johannes Voss"
        flavorText = "\"In these halls there is no pass or fail. Your true test comes with the first full moon.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd8f179a-f6ab-4d4c-8195-ed077a7770d3.jpg?1782714825"
    }
}
