package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Search for Azcanta // Azcanta, the Sunken Ruin (Ixalan — the card's earliest printing)
 * {1}{U}
 * Legendary Enchantment // Legendary Land
 *
 * Oracle errata: the printed "look at the top card of your library. You may put that card into
 * your graveyard." is now "surveil 1" (current Oracle wording, followed here).
 *
 * Front — Search for Azcanta (Legendary Enchantment, {1}{U})
 *   At the beginning of your upkeep, surveil 1. Then if you have seven or more cards in your
 *   graveyard, you may transform Search for Azcanta.
 *
 * Back — Azcanta, the Sunken Ruin (Legendary Land)
 *   {T}: Add {U}.
 *   {2}{U}, {T}: Look at the top four cards of your library. You may reveal a noncreature,
 *   nonland card from among them and put it into your hand. Put the rest on the bottom of your
 *   library in any order.
 *
 * Implementation:
 *  - Upkeep trigger: [Patterns.Library.surveil]`(1)` then an [Effects.If] on
 *    [Conditions.CardsInGraveyardAtLeast]`(7)` wrapping an optional [Effects.Transform]. The
 *    graveyard count is checked on resolution, after the surveil — not an intervening-if —
 *    so the surveilled card counts toward seven and a card that reached the graveyard
 *    elsewhere waits for the next upkeep (rulings).
 *  - Back's dig is [Patterns.Library.lookAtTopRevealMatchingToHand] (Wandering Mind's filter),
 *    with the rest to the bottom in the controller's chosen order.
 */

private val SearchForAzcantaFront = card("Search for Azcanta") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Enchantment"
    oracleText = "At the beginning of your upkeep, surveil 1. Then if you have seven or more " +
        "cards in your graveyard, you may transform Search for Azcanta. (Look at the top card " +
        "of your library. You may put that card into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Patterns.Library.surveil(1) then
            Effects.If(
                Conditions.CardsInGraveyardAtLeast(7),
                then = Effects.May(Effects.Transform(EffectTarget.Self)),
            )
        description = "At the beginning of your upkeep, surveil 1. Then if you have seven or " +
            "more cards in your graveyard, you may transform Search for Azcanta."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Magali Villeneuve"
        flavorText = "The Sunken Ruin houses all the learning of a long-forgotten age."
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a7e242e-bb48-4134-a1c2-6033713d658f.jpg?1783935781"
        ruling(
            "2017-09-29",
            "If a seventh card is put into your graveyard by something other than resolving " +
                "Search for Azcanta's triggered ability, you won't transform it yet. You'll have " +
                "to wait until your next upkeep.",
        )
        ruling(
            "2017-09-29",
            "If you have seven or more cards in your graveyard, you may transform Search for " +
                "Azcanta while resolving its triggered ability even if you choose not to put the " +
                "top card of your library into your graveyard.",
        )
    }
}

private val AzcantaTheSunkenRuin = card("Azcanta, the Sunken Ruin") {
    manaCost = ""
    colorIdentity = "U"
    typeLine = "Legendary Land"
    oracleText = "(Transforms from Search for Azcanta.)\n" +
        "{T}: Add {U}.\n" +
        "{2}{U}, {T}: Look at the top four cards of your library. You may reveal a noncreature, " +
        "nonland card from among them and put it into your hand. Put the rest on the bottom of " +
        "your library in any order."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE, 1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}"), Costs.Tap)
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 4,
            filter = GameObjectFilter.Noncreature and GameObjectFilter.Nonland,
            prompt = "You may reveal a noncreature, nonland card and put it into your hand",
            restOrder = CardOrder.ControllerChooses,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/back/1/a/1a7e242e-bb48-4134-a1c2-6033713d658f.jpg?1783935781"
    }
}

val SearchForAzcanta: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = SearchForAzcantaFront,
    backFace = AzcantaTheSunkenRuin,
)
