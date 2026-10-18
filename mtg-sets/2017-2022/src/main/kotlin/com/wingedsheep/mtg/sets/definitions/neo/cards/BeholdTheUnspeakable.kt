package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Behold the Unspeakable // Vision of the Unspeakable
 * {3}{U}{U} — Enchantment — Saga // Enchantment Creature — Spirit 0/0
 *
 * Front — Behold the Unspeakable:
 *   I — Creatures you don't control get -2/-0 until your next turn.
 *   II — If you have one or fewer cards in hand, draw four cards. Otherwise, scry 2, then draw two cards.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Vision of the Unspeakable:
 *   Flying, trample
 *   This creature gets +1/+1 for each card in your hand.
 *
 * Chapter I locks in the set of affected creatures on resolution (creatures you don't control at
 * that moment); chapter II checks hand size as it resolves.
 */
private val VisionOfTheUnspeakable = card("Vision of the Unspeakable") {
    manaCost = ""
    colorIndicator = "U"
    colorIdentity = "U"
    typeLine = "Enchantment Creature — Spirit"
    oracleText = "Flying, trample\nThis creature gets +1/+1 for each card in your hand."
    power = 0
    toughness = 0

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.cardsInYourHand(),
            toughnessBonus = DynamicAmounts.cardsInYourHand(),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Ravenna Tran"
        flavorText = "Even as her students' minds succumbed to madness, Azami's obsession with the kami " +
            "of forbidden knowledge only grew."
        imageUri = "https://cards.scryfall.io/normal/back/e/4/e4d52701-e0dc-474e-b08f-88147200d210.jpg?1783923912"
    }
}

private val BeholdTheUnspeakableFront = card("Behold the Unspeakable") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I — Creatures you don't control get -2/-0 until your next turn.\n" +
        "II — If you have one or fewer cards in hand, draw four cards. Otherwise, scry 2, then draw two cards.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    sagaChapter(1) {
        effect = Patterns.Group.modifyStatsForAll(
            -2,
            0,
            GroupFilter(
                GameObjectFilter.Creature.withControllerPredicate(
                    ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
                )
            ),
            Duration.UntilYourNextTurn,
        )
    }

    sagaChapter(2) {
        effect = Effects.If(
            Conditions.CardsInHandAtMost(1),
            Effects.DrawCards(4),
            Effects.Scry(2) then Effects.DrawCards(2),
        )
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Ravenna Tran"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4d52701-e0dc-474e-b08f-88147200d210.jpg?1783923912"
    }
}

val BeholdTheUnspeakable: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = BeholdTheUnspeakableFront,
    backFace = VisionOfTheUnspeakable,
)
