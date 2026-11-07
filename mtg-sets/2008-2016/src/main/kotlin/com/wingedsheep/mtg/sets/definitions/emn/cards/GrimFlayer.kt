package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Grim Flayer (Eldritch Moon #184)
 * {B}{G}
 * Creature — Human Warrior
 * 2/2
 *
 * Trample
 * Whenever this creature deals combat damage to a player, surveil 3. (Look at the top three cards
 * of your library, then put any number of them into your graveyard and the rest on top of your
 * library in any order.)
 * Delirium — This creature gets +2/+2 as long as there are four or more card types among cards
 * in your graveyard.
 *
 * Current Oracle text: the printed "look at the top three cards ... put any number into your
 * graveyard" was errata'd to surveil 3.
 */
val GrimFlayer = card("Grim Flayer") {
    manaCost = "{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 2
    oracleText = "Trample\n" +
        "Whenever this creature deals combat damage to a player, surveil 3. (Look at the top three " +
        "cards of your library, then put any number of them into your graveyard and the rest on top " +
        "of your library in any order.)\n" +
        "Delirium — This creature gets +2/+2 as long as there are four or more card types among " +
        "cards in your graveyard."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Surveil(3)
    }

    staticAbility {
        ability = ModifyStats(2, 2, Filters.Self)
        condition = Conditions.Delirium()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "184"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/025af99f-a029-4f99-9c76-676b68821520.jpg"
    }
}
