package com.wingedsheep.mtg.sets.definitions.ody.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardsInZonesCantBeTargeted

/**
 * Ground Seal — Odyssey #242
 * {1}{G} · Enchantment
 *
 * When this enchantment enters, draw a card.
 * Cards in graveyards can't be the targets of spells or abilities.
 *
 * The graveyard lock is [CardsInZonesCantBeTargeted] (default: every card in every graveyard, for
 * everyone's spells and abilities), as on Silent Gravestone and Dennick, Pious Apprentice.
 */
val GroundSeal = card("Ground Seal") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, draw a card.\n" +
        "Cards in graveyards can't be the targets of spells or abilities."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = CardsInZonesCantBeTargeted()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "242"
        artist = "Edward P. Beard, Jr."
        flavorText = "\"Let all we have buried be buried forever.\"\n—Seton, centaur druid"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/977383a8-9a31-4aad-ace2-6ed4d0dd1cbe.jpg?1783945217"
        ruling(
            "2012-07-01",
            "Only spells and abilities that target cards in graveyards will be affected. Abilities that " +
                "don't target cards in graveyards (like the one Tormod's Crypt has) can still affect those cards."
        )
    }
}
