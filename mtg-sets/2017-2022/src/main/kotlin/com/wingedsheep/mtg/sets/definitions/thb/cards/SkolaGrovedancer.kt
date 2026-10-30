package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Skola Grovedancer
 * {1}{G}
 * Enchantment Creature — Satyr Druid
 * 2/2
 * Whenever a land card is put into your graveyard from anywhere, you gain 1 life.
 * {2}{G}: Mill a card.
 *
 * Slogurk's per-card "land card into your graveyard from anywhere" zone-change trigger (nontoken,
 * owned by you) feeding a 1-life gain, plus a mill-one activated ability.
 */
val SkolaGrovedancer = card("Skola Grovedancer") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Satyr Druid"
    power = 2
    toughness = 2
    oracleText = "Whenever a land card is put into your graveyard from anywhere, you gain 1 life.\n" +
        "{2}{G}: Mill a card. (Put the top card of your library into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.ownedByYou().nontoken()).changesZone(to = Zone.GRAVEYARD)
        effect = Effects.GainLife(1)
        description = "Whenever a land card is put into your graveyard from anywhere, you gain 1 life."
    }

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        effect = Patterns.Library.mill(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "202"
        artist = "Josh Hass"
        flavorText = "\"From the ashes of last night's revel, I bring the dawn of new growth.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/7/076ec900-345d-4720-96b6-441bce01618a.jpg?1783931528"
        ruling(
            "2020-01-24",
            "Because Skola Grovedancer's ability triggers when land cards are put into your graveyard " +
                "\"from anywhere,\" Skola Grovedancer must be on the battlefield immediately after a land card " +
                "is put there in order to trigger. This means that if Skola Grovedancer dies at the same time " +
                "as a land you control, its ability won't trigger."
        )
    }
}
