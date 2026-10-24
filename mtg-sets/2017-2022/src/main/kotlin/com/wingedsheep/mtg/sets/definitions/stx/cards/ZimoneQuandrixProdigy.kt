package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Zimone, Quandrix Prodigy — Strixhaven: School of Mages #250 (canonical printing)
 * {G}{U} · Legendary Creature — Human Wizard · 1/2
 *
 * {1}, {T}: You may put a land card from your hand onto the battlefield tapped.
 * {4}, {T}: Draw a card. If you control eight or more lands, draw two cards instead.
 *
 * The first ability is [Patterns.Hand.putFromHand] (its up-to-one selection is the "you may"),
 * not a land play — it ignores land drops and works on any turn. The second is Tezzeret,
 * Artifice Master's `Effects.If(…, DrawCards(2), DrawCards(1))`, the land count read on resolution
 * via [Conditions.ControlLandsAtLeast].
 */
val ZimoneQuandrixProdigy = card("Zimone, Quandrix Prodigy") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Human Wizard"
    power = 1
    toughness = 2
    oracleText = "{1}, {T}: You may put a land card from your hand onto the battlefield tapped.\n" +
        "{4}, {T}: Draw a card. If you control eight or more lands, draw two cards instead."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Patterns.Hand.putFromHand(
            filter = GameObjectFilter.Land,
            entersTapped = true,
            prompt = "You may put a land card from your hand onto the battlefield tapped"
        )
        description = "{1}, {T}: You may put a land card from your hand onto the battlefield tapped."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.If(
            condition = Conditions.ControlLandsAtLeast(8),
            then = Effects.DrawCards(2),
            otherwise = Effects.DrawCards(1),
        )
        description = "{4}, {T}: Draw a card. If you control eight or more lands, draw two cards instead."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "250"
        artist = "Ryan Pancoast"
        flavorText = "\"What are they complaining about? It's not like axiomatic leyline " +
            "paratrigonometrics is hard.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0ca14c17-dc72-4f68-92f2-14a6c4019f4e.jpg?1783927283"
        ruling("2021-04-16", "Zimone's first ability isn't the same as playing a land. As it resolves, you may put a land card from your hand onto the battlefield tapped even if you have no available land plays or it's another player's turn.")
        ruling("2021-04-16", "Zimone's last ability counts the number of lands you control as it resolves, not as you activate it.")
    }
}
