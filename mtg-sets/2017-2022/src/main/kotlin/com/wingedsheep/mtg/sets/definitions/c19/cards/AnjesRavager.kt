package com.wingedsheep.mtg.sets.definitions.c19.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Anje's Ravager
 * {2}{R}
 * Creature — Vampire Berserker
 * 3/3
 * This creature attacks each combat if able.
 * Whenever this creature attacks, discard your hand, then draw three cards.
 * Madness {1}{R}
 */
val AnjesRavager = card("Anje's Ravager") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire Berserker"
    power = 3
    toughness = 3
    oracleText = "This creature attacks each combat if able.\n" +
        "Whenever this creature attacks, discard your hand, then draw three cards.\n" +
        "Madness {1}{R} (If you discard this card, discard it into exile. When you do, cast it for its madness cost or put it into your graveyard.)"

    staticAbility {
        ability = MustAttack()
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Hand.discardHand() then Effects.DrawCards(3)
    }

    madness("{1}{R}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22924c44-5551-4a48-a574-dfef91a5d4d7.jpg?1783932806"
    }
}
