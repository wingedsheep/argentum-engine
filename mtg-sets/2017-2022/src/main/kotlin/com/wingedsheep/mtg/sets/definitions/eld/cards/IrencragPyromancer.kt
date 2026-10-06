package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Irencrag Pyromancer
 * {2}{R}
 * Creature — Human Wizard
 * 0/4
 * Whenever you draw your second card each turn, this creature deals 3 damage to any target.
 */
val IrencragPyromancer = card("Irencrag Pyromancer") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Wizard"
    power = 0
    toughness = 4
    oracleText = "Whenever you draw your second card each turn, this creature deals 3 damage to any target."

    triggeredAbility {
        trigger = Triggers.you.drawsNth(2)
        val t = target(Targets.Any)
        effect = Effects.DealDamage(3, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "Jason Rainville"
        flavorText = "\"Fear of fire is a sensible instinct. If I were you, I'd be terrified.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a7b0ead-5629-429d-bede-8154f3fae96d.jpg?1783932622"
        ruling(
            "2019-10-04",
            "The triggered ability can trigger only once each turn. It doesn't matter whether the permanent " +
                "with that ability was on the battlefield when the first card was drawn. If it's not on the " +
                "battlefield when the second card is drawn, the ability can't trigger at all that turn. It " +
                "won't trigger when the third or fourth card is drawn."
        )
        ruling(
            "2019-10-04",
            "If an effect instructs you to draw multiple cards, the ability triggers after you draw whichever " +
                "is the second one for the turn. You choose a target (if any) for the ability after you've drawn " +
                "and looked at all of the cards and finished resolving the spell or ability that caused you to draw them."
        )
        ruling(
            "2019-10-04",
            "If a spell or ability causes you to put cards into your hand without specifically using the word " +
                "\"draw,\" it's not a card drawn."
        )
    }
}
