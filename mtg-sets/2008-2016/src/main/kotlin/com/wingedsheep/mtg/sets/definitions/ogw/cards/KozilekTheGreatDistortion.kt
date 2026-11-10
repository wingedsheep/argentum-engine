package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.minus

/**
 * Kozilek, the Great Distortion
 * {8}{C}{C}
 * Legendary Creature — Eldrazi
 * 12/12
 * When you cast this spell, if you have fewer than seven cards in hand, draw cards equal to the
 * difference.
 * Menace
 * Discard a card with mana value X: Counter target spell with mana value X.
 *
 * The counter's cost is a measured discard ([Costs.DiscardCardWithManaValueX]): the discarded
 * card's mana value is X, so the engine asks for the discard first — offering only cards whose
 * mana value matches a spell on the stack — and then for a spell of exactly that mana value
 * (the rulings' "you can't activate the ability unless you can match the mana value of a spell on
 * the stack to that of a card in your hand"). A spell's `{X}` counts at the value it was cast for;
 * a discarded card's `{X}` is 0.
 */
val KozilekTheGreatDistortion = card("Kozilek, the Great Distortion") {
    manaCost = "{8}{C}{C}"
    colorIdentity = ""
    typeLine = "Legendary Creature — Eldrazi"
    power = 12
    toughness = 12
    oracleText = "When you cast this spell, if you have fewer than seven cards in hand, draw cards " +
        "equal to the difference.\n" +
        "Menace\n" +
        "Discard a card with mana value X: Counter target spell with mana value X."

    triggeredAbility {
        trigger = Triggers.self.isCast()
        interveningIf = Conditions.CardsInHandAtMost(6)
        effect = Effects.DrawCards(7 - DynamicAmounts.cardsInYourHand())
        description = "When you cast this spell, if you have fewer than seven cards in hand, draw " +
            "cards equal to the difference."
    }

    keywords(Keyword.MENACE)

    activatedAbility {
        cost = Costs.DiscardCardWithManaValueX()
        target(TargetFilter.SpellOnStack.manaValueEqualsX())
        effect = Effects.CounterSpell()
        description = "Discard a card with mana value X: Counter target spell with mana value X."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "4"
        artist = "Aleksi Briclot"
        flavorText = "A void as cryptic as reality itself."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f06fc6e0-b22c-40d3-bb53-d5ec400d921c.jpg?1783937930"
        ruling("2016-01-22", "Kozilek's triggered ability checks to see if you have fewer than seven cards in hand when you cast it. Kozilek is on the stack at this time and not in your hand. If you don't, the ability won't trigger at all. If the ability does trigger, it will check again as it tries to resolve. If you have seven or more cards in hand at that time, the ability won't do anything.")
        ruling("2016-01-22", "To activate the last ability, there must be a legal target: a spell on the stack. That target will determine the value of X and the mana value of the card you discard. You can't activate the ability unless you can match the mana value of a spell on the stack to that of a card in your hand.")
        ruling("2016-01-22", "The mana value of a spell doesn't change, even if it's been cast using an alternative cost (such as an awaken cost). For example, the mana value of a Sheer Drop (a spell with mana cost {2}{W}) that's been cast for its awaken cost of {5}{W} is 3.")
        ruling("2016-01-22", "If there's an {X} in the mana cost of the card you discard, that X is 0. Any {X} in the mana cost of the target spell will have whatever value was determined for it as the spell was cast.")
    }
}
