package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Izoni, Thousand-Eyed
 * {2}{B}{B}{G}{G}
 * Legendary Creature — Elf Shaman
 * 2/3
 * Undergrowth — When Izoni enters, create a 1/1 black and green Insect creature token for each
 * creature card in your graveyard.
 * {B}{G}, Sacrifice another creature: You gain 1 life and draw a card.
 *
 * The token count is read on resolution, so creatures sacrificed to the activated ability while
 * the enters trigger is on the stack add to it (per the 2018-10-05 ruling).
 */
val IzoniThousandEyed = card("Izoni, Thousand-Eyed") {
    manaCost = "{2}{B}{B}{G}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Elf Shaman"
    oracleText = "Undergrowth — When Izoni enters, create a 1/1 black and green Insect creature token for each " +
        "creature card in your graveyard.\n" +
        "{B}{G}, Sacrifice another creature: You gain 1 life and draw a card."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            count = DynamicAmounts.creatureCardsInYourGraveyard(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK, Color.GREEN),
            creatureTypes = setOf("Insect")
        )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{B}{G}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature)
        )
        effect = Effects.GainLife(1) then Effects.DrawCards(1)
        description = "{B}{G}, Sacrifice another creature: You gain 1 life and draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "180"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75955b0e-12d6-40ba-aab5-b7b7e2bde121.jpg?1783934132"
        ruling("2018-10-05", "You can activate Izoni's last ability while its undergrowth ability is on the stack. This will increase the number of Insect tokens you'll create.")
        ruling("2018-10-05", "Creature cards with other types, such as artifact creature cards, count for undergrowth abilities.")
        ruling("2018-10-05", "Because tokens aren't cards, they never count for undergrowth abilities.")
    }
}
