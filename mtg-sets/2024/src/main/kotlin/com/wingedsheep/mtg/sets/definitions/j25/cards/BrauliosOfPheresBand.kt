package com.wingedsheep.mtg.sets.definitions.j25.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Braulios of Pheres Band — Foundations Jumpstart #51 (canonical printing; its only printing)
 * {3}{G}{G} · Legendary Creature — Centaur Scout · star/star
 *
 * Braulios's power and toughness are each equal to the number of lands you control.
 * Whenever Braulios attacks, draw a card, then you may put a land card from your hand onto the
 * battlefield.
 *
 * The characteristic-defining ability is Molimo's `dynamicStats(landsYouControl)`. The attack
 * trigger is Eureka Moment's shape: a draw, then [Patterns.Hand.putFromHand] selecting *up to* one
 * land, which is the "you may". Putting the land onto the battlefield is not playing it, so it
 * ignores land drops.
 */
val BrauliosOfPheresBand = card("Braulios of Pheres Band") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Centaur Scout"
    oracleText = "Braulios's power and toughness are each equal to the number of lands you control.\n" +
        "Whenever Braulios attacks, draw a card, then you may put a land card from your hand onto the battlefield."

    dynamicStats(DynamicAmounts.landsYouControl())

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.DrawCards(1) then
            Patterns.Hand.putFromHand(filter = GameObjectFilter.Land)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Takeuchi Moto"
        flavorText = "\"Your claim to this land is as tenuous as my tolerance of your presence on it.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01789cb2-a93d-4197-a40a-612423635125.jpg?1783908856"
        ruling("2024-11-08", "The ability that defines Braulios's power and toughness works in all zones, not just the battlefield.")
    }
}
