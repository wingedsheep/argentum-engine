package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompanionCondition
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/** Jegantha's mana pays colored, {C} and {S} symbols only — never a generic one. */
private val noGeneric = ManaRestriction.CannotPayGenericCosts

/**
 * Jegantha, the Wellspring — Ikoria: Lair of Behemoths #222
 * {4}{R/G} · Legendary Creature — Elemental Elk · 5/5
 *
 * Companion — every card in the starting deck has no mana symbol twice in its printed mana cost
 * (exact symbols: `{X}{X}` and `{R/G}{R/G}` repeat, `{R}{R/G}` doesn't). The mana ability adds one
 * of each color, all of it barred from generic costs.
 */
val JeganthaTheWellspring = card("Jegantha, the Wellspring") {
    manaCost = "{4}{R/G}"
    typeLine = "Legendary Creature — Elemental Elk"
    power = 5
    toughness = 5
    oracleText = "Companion — No card in your starting deck has more than one of the same mana symbol in its " +
        "mana cost. (If this card is your chosen companion, you may put it into your hand from outside the " +
        "game for {3} as a sorcery.)\n" +
        "{T}: Add {W}{U}{B}{R}{G}. This mana can't be spent to pay generic mana costs."

    keywordAbility(
        KeywordAbility.companion(
            CompanionCondition.EachCard(
                GameObjectFilter.Any.withCardPredicate(CardPredicate.Not(CardPredicate.HasRepeatedManaSymbol))
            )
        )
    )

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE, restriction = noGeneric) then
            Effects.AddMana(Color.BLUE, restriction = noGeneric) then
            Effects.AddMana(Color.BLACK, restriction = noGeneric) then
            Effects.AddMana(Color.RED, restriction = noGeneric) then
            Effects.AddMana(Color.GREEN, restriction = noGeneric)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d52e527-3835-4350-8c01-0f2d5d623b9c.jpg?1783931011"
        ruling("2020-04-17", "You can spend mana from Jegantha's mana ability to pay for a hybrid symbol such as {2/W}, but only if you choose to pay the colored mana component, not the generic mana component.")
        ruling("2020-04-17", "A generic mana cost is usually represented by numeric mana symbols ({1}, {2}, and so on) and also {X}. It is any cost requiring mana where that cost isn't {C}, {W}, {U}, {B}, {R}, or {G}.")
        ruling("2020-04-17", "Jegantha's companion ability compares the exact symbols in the mana costs of cards in your deck. If any one card has the same symbol twice, such as {X}{X}{R} or {(r/g)}{(r/g)}, the companion condition isn't satisfied.")
    }
}
