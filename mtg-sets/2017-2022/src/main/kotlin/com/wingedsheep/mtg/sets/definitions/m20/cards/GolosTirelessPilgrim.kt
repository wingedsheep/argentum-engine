package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Golos, Tireless Pilgrim (M20 #226)
 * {5}  Legendary Artifact Creature — Scout  3/5
 *
 * When Golos enters, you may search your library for a land card, put that card onto the
 * battlefield tapped, then shuffle.
 * {2}{W}{U}{B}{R}{G}: Exile the top three cards of your library. You may play them this turn
 * without paying their mana costs.
 */
val GolosTirelessPilgrim = card("Golos, Tireless Pilgrim") {
    manaCost = "{5}"
    colorIdentity = "WUBRG"
    typeLine = "Legendary Artifact Creature — Scout"
    oracleText = "When Golos enters, you may search your library for a land card, put that card onto the battlefield tapped, then shuffle.\n" +
        "{2}{W}{U}{B}{R}{G}: Exile the top three cards of your library. You may play them this turn without paying their mana costs."
    power = 3
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    activatedAbility {
        cost = Costs.Mana("{2}{W}{U}{B}{R}{G}")
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(3))
            exile(exiled)
            run(Effects.GrantMayPlayFromExile(exiled))
            run(Effects.GrantPlayWithoutPayingCost(exiled))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "226"
        artist = "Joseph Meehan"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1fa48620-4c3d-4f75-be1f-c12c4aa59f51.jpg?1783932945"
        ruling("2019-07-12", "If you don't play some or all of the cards exiled with Golos's last ability, those cards remain in exile.")
        ruling("2019-07-12", "Golos's last ability doesn't change when you can play the exiled cards. For example, if you exile a sorcery card, you can cast it only during your main phase when the stack is empty. If you exile a land card, you can play it only during your main phase and only if you have an available land play remaining.")
        ruling("2019-07-12", "If a spell has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2019-07-12", "If you cast a card \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs. If the card has any mandatory additional costs (such as that of Bone Splinters), you must pay those to cast the card.")
    }
}
