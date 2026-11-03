package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sela, Tal Shiar Paradox — Star Trek #128
 * {3}{B} · Legendary Creature — Romulan Human Rogue · 3/3
 *
 * Whenever Sela attacks, exile up to one target card from a graveyard.
 * Whenever one or more cards leave your graveyard, create a tapped 2/2 black Romulan creature token.
 *
 * - The attack trigger is the Sungold Sentinel shape: an optional graveyard-card target moved to
 *   exile. Targeting a card in your own graveyard feeds the second ability.
 * - "One or more cards leave" is CR 603.2c batch wording, so `Triggers.oneOrMore(...).leaveYourGraveyard()`
 *   (the Stonebound Mentor shape) fires once per event no matter how many cards moved or where
 *   they went — a flashback cast, a reanimation, or an exile all count.
 */
val SelaTalShiarParadox = card("Sela, Tal Shiar Paradox") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Romulan Human Rogue"
    oracleText = "Whenever Sela attacks, exile up to one target card from a graveyard.\n" +
        "Whenever one or more cards leave your graveyard, create a tapped 2/2 black Romulan creature token."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val exiled = target(TargetFilter.CardInGraveyard, optional = true)
        effect = Effects.Move(exiled, Zone.EXILE)
        description = "Whenever Sela attacks, exile up to one target card from a graveyard."
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Any).leaveYourGraveyard()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Romulan"),
            tapped = true,
        )
        description = "Whenever one or more cards leave your graveyard, create a tapped 2/2 black " +
            "Romulan creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "Tony Foti"
        flavorText = "Her mother shouldn't have existed, and young Sela ensured she did not."
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb680a44-3395-4c96-a73d-af1e0b7b8a9f.jpg?1793101376"
    }
}
