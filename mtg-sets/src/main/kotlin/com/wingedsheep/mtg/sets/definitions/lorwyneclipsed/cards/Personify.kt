package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect

/**
 * Personify
 * {1}{W}
 * Instant
 *
 * Exile target creature you control, then return that card to the battlefield under
 * its owner's control. Create a 1/1 colorless Shapeshifter creature token with
 * changeling. (It's every creature type.)
 */
val Personify = card("Personify") {
    manaCost = "{1}{W}"
    typeLine = "Instant"
    oracleText = "Exile target creature you control, then return that card to the battlefield under its owner's control. Create a 1/1 colorless Shapeshifter creature token with changeling. (It's every creature type.)"

    spell {
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = MoveToZoneEffect(creature, Zone.EXILE)
            .then(MoveToZoneEffect(creature, Zone.BATTLEFIELD))
            .then(
                Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    creatureTypes = setOf("Shapeshifter"),
                    keywords = setOf(Keyword.CHANGELING),
                    imageUri = "https://cards.scryfall.io/normal/front/c/2/c2963ce1-f9d8-437a-9489-e0913a8b8d26.jpg?1767660071"
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "Slawomir Maniak"
        flavorText = "\"I found myself wishing I could run away and start anew. That's when I saw those yellow eyes.\"\n—Yenn, kithkin nomad"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/1172582d-fb2d-4022-95b1-e48b03df3a95.jpg?1767658640"
        ruling("2025-11-17", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
        ruling("2025-11-17", "Auras attached to the exiled creature will be put into their owners' graveyards. Equipment attached to the exiled creature will become unattached and remain on the battlefield. Any counters on the exiled creature will cease to exist.")
    }
}
