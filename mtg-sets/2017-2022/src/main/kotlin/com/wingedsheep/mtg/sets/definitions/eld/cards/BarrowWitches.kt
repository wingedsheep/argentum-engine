package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Barrow Witches
 * {4}{B}
 * Creature — Human Warlock
 * 3/4
 *
 * When this creature enters, return target Knight card from your graveyard to your hand.
 *
 * "Knight card" is a bare tribal noun, so the target is any card with the Knight subtype
 * (not only creature cards) — `CardInGraveyard.withSubtype(…).ownedByYou()`.
 */
val BarrowWitches = card("Barrow Witches") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warlock"
    oracleText = "When this creature enters, return target Knight card from your graveyard to your hand."
    power = 3
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        val knight = target(TargetFilter.CardInGraveyard.withSubtype("Knight").ownedByYou())
        effect = Effects.ReturnToHand(knight)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Alex Brock"
        flavorText = "\"Taste the sweetness, breath for breath. Keep the balance, death for death.\"\n—Barrow witch incantation"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45707c22-fce8-4dbd-9d19-73f08c68f449.jpg?1783932648"
    }
}
