package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Niambi, Esteemed Speaker
 * {W}{U}
 * Legendary Creature — Human Cleric
 * 2/1
 * Flash
 * When Niambi enters, you may return another target creature you control to its owner's hand. If you
 * do, you gain life equal to that creature's mana value.
 * {1}{W}{U}, {T}, Discard a legendary card: Draw two cards.
 *
 * The lone "you may" is answered at targeting (choosing no target declines); an illegal target
 * on resolution fizzles the whole ability, per ruling. Serum Snare's shape freezes the mana
 * value with `storeNumber` *before* the bounce — a returned token ceases to exist — and the life
 * gain reads the stored number, so it happens only on the branch where the creature was returned.
 */
val NiambiEsteemedSpeaker = card("Niambi, Esteemed Speaker") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Human Cleric"
    power = 2
    toughness = 1
    oracleText = "Flash\n" +
        "When Niambi enters, you may return another target creature you control to its owner's hand. If you " +
        "do, you gain life equal to that creature's mana value.\n" +
        "{1}{W}{U}, {T}, Discard a legendary card: Draw two cards."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.May(
            Effects.Pipeline {
                val manaValue = storeNumber(DynamicAmounts.manaValueOf(creature))
                run(Effects.ReturnToHand(creature))
                run(Effects.GainLife(manaValue.amount))
            }
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{W}{U}"),
            Costs.Tap,
            Costs.Discard(GameObjectFilter.Any.legendary())
        )
        effect = Effects.DrawCards(2)
        description = "Draw two cards."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e21827eb-fa49-4784-a86b-aef164a5018e.jpg?1783930660"
        ruling("2020-06-23", "If a creature on the battlefield has {X} in its mana cost, X is considered to be 0.")
        ruling(
            "2020-06-23",
            "If the target creature is an illegal target by the time Niambi's triggered ability tries to resolve, " +
                "the ability won't resolve. You won't return the card from the zone it moved to or gain any life.",
        )
    }
}
