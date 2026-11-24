package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Isperia, Supreme Judge — Return to Ravnica #171
 * {2}{W}{W}{U}{U} · Legendary Creature — Sphinx 6/4
 *
 * Flying
 * Whenever a creature attacks you or a planeswalker you control, you may draw a card.
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * The attack trigger is Guardian of the Ages' — it fires once per creature declared attacking
 * you or one of your planeswalkers — and "you may" is the builder's `optional = true`.
 */
val IsperiaSupremeJudge = card("Isperia, Supreme Judge") {
    manaCost = "{2}{W}{W}{U}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Sphinx"
    power = 6
    toughness = 4
    oracleText = "Flying\nWhenever a creature attacks you or a planeswalker you control, you may draw a card."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.attackingYouOrYourPlaneswalkers()).attacks()
        optional = true
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "171"
        artist = "Scott M. Fischer"
        flavorText = "\"I serve only justice. But through that duty, I serve all of Ravnica.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2cce2d4-3944-4ff0-98e8-80f19697f108.jpg?1783940339"
    }
}
