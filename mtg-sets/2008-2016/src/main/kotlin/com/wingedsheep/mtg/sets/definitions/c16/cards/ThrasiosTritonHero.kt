package com.wingedsheep.mtg.sets.definitions.c16.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Thrasios, Triton Hero
 * {G}{U}
 * Legendary Creature — Merfolk Wizard
 * 1/3
 * {4}: Scry 1, then reveal the top card of your library. If it's a land card, put it onto the
 * battlefield tapped. Otherwise, draw a card.
 * Partner (You can have two commanders if both have partner.)
 *
 * The nonland branch is a real draw (it triggers "whenever you draw" abilities), not a move to hand;
 * per the ruling, the revealed nonland card is the card drawn. Partner is deck construction only.
 */
val ThrasiosTritonHero = card("Thrasios, Triton Hero") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Merfolk Wizard"
    power = 1
    toughness = 3
    oracleText = "{4}: Scry 1, then reveal the top card of your library. If it's a land card, put it onto the battlefield tapped. Otherwise, draw a card.\n" +
        "Partner (You can have two commanders if both have partner.)"

    keywordAbility(KeywordAbility.partner())

    activatedAbility {
        cost = Costs.Mana("{4}")
        effect = Effects.Pipeline {
            run(Effects.Scry(1))
            val top = gather(CardSource.TopOfLibrary(1))
            reveal(top, fromZone = Zone.LIBRARY)
            ifNotEmpty(top, filter = GameObjectFilter.Land) {
                move(top, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
            } orElse {
                run(Effects.DrawCards(1))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Josu Hernaiz"
        flavorText = "Beneath crashing waves lies an ocean of secrets waiting to be explored."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21e27b91-c7f1-4709-aa0d-8b5d81b22a0a.jpg?1783937081"
        ruling("2020-11-10", "The nonland card you reveal will be the card you draw.")
        ruling("2016-11-08", "No player may take another action while you're resolving the activated ability.")
    }
}
