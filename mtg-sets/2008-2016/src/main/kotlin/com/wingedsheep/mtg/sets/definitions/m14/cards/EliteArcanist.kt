package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elite Arcanist
 * {3}{U}
 * Creature — Human Wizard
 * 1/1
 *
 * When this creature enters, you may exile an instant card from your hand.
 * {X}, {T}: Copy the exiled card. You may cast the copy without paying its mana cost. X is the
 * mana value of the exiled card.
 *
 * Isochron Scepter's imprint and copy-and-cast, priced like Soul Foundry: the ETB is a linked exile
 * (`linkToSource = true`) from your own hand with no reveal, and `xDefinedAs` fixes the `{X}` to
 * the linked exiled card's mana value, so there's no X prompt (CR 107.3c — the ability's text
 * defines X). The pile is re-read live, so if the exiled card leaves exile nothing is copied.
 *
 * "Copy the exiled card" is mandatory and only the cast is optional, but a copy that's never cast
 * just sits in exile until the next state-based-action check removes it, and nothing triggers on
 * that. So declining the copy and declining the cast look the same, and one prompt covers both,
 * as on Isochron Scepter.
 */
val EliteArcanist = card("Elite Arcanist") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, you may exile an instant card from your hand.\n" +
        "{X}, {T}: Copy the exiled card. You may cast the copy without paying its mana cost. " +
        "X is the mana value of the exiled card."

    // When this creature enters, you may exile an instant card from your hand.
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Patterns.Hand.revealHandAndExileChosen(
                target = EffectTarget.Controller,
                filter = GameObjectFilter.Instant,
                prompt = "Choose an instant card to exile",
                revealHand = false,
                linkToSource = true
            ),
            descriptionOverride = "You may exile an instant card from your hand."
        )
    }

    // {X}, {T}: Copy the exiled card. You may cast the copy without paying its mana cost.
    // X is the mana value of the exiled card.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap)
        xDefinedAs = DynamicAmounts.manaValueOf(EffectTarget.LinkedExiledCard())
        // No ability-level description: the generated label carries the resolved cost.
        effect = Effects.May(
            Effects.Pipeline {
                val arcanistImprinted = gather(CardSource.FromLinkedExile())
                val arcanistCopy = copyCards(arcanistImprinted)
                run(Effects.CastFromCollectionWithoutPayingCost(arcanistCopy))
            },
            descriptionOverride = "Copy the exiled card. You may cast the copy without paying its mana cost."
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "James Zapata"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99b225fe-c07d-4d8a-bf2b-c1777bd29061.jpg?1783939934"

        ruling(
            "2013-07-01",
            "You don't choose the value of {X} in the activation cost of Elite Arcanist's ability. " +
                "It's defined by the mana value of the exiled instant card."
        )
        ruling(
            "2013-07-01",
            "The copy is created in and cast from exile. You must still follow any timing " +
                "restrictions, such as \"Cast [this spell] only during combat.\""
        )
        ruling(
            "2013-07-01",
            "You can't pay any alternative costs such as overload costs. You can pay additional " +
                "costs such as kicker costs. If the spell has any mandatory additional costs, you " +
                "must pay those."
        )
        ruling(
            "2013-07-01",
            "If the copied card has {X} in its mana cost, you must choose 0 as its value when " +
                "casting the copy. Don't confuse that {X} with the {X} in the activation cost of " +
                "Elite Arcanist's ability."
        )
        ruling(
            "2013-07-01",
            "If the exiled card is a split card, then X is determined by the combined mana cost of " +
                "both halves. However, when you cast the copy, you choose only one half to cast. You " +
                "may cast a different half each time you create and cast a copy."
        )
        ruling(
            "2013-07-01",
            "If Elite Arcanist leaves the battlefield, you will no longer be able to copy the " +
                "exiled card. If it returns to the battlefield, you'll be able to exile another " +
                "instant card from your hand, but Elite Arcanist will be a different permanent with " +
                "no connection to the first exiled card."
        )
    }
}
