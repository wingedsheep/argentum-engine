package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shreds of Sanity — Eldritch Moon #141 (canonical printing)
 * {2}{R} · Sorcery
 *
 * Return up to one target instant card and up to one target sorcery card from your graveyard to
 * your hand, then discard a card. Exile Shreds of Sanity.
 *
 * Retrieve's shape: two independent optional graveyard slots, each returned to hand, then a
 * discard that resolves after the returns (so a returned card may be the one discarded), and
 * `selfExile()` for "Exile Shreds of Sanity". With no targets chosen it still resolves and
 * discards; if every chosen target becomes illegal it doesn't resolve at all.
 */
val ShredsOfSanity = card("Shreds of Sanity") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Return up to one target instant card and up to one target sorcery card from your " +
        "graveyard to your hand, then discard a card. Exile Shreds of Sanity."

    spell {
        selfExile()
        val instant = target(
            TargetFilter(GameObjectFilter.Instant.ownedByYou(), zone = Zone.GRAVEYARD),
            optional = true,
        )
        val sorcery = target(
            TargetFilter(GameObjectFilter.Sorcery.ownedByYou(), zone = Zone.GRAVEYARD),
            optional = true,
        )
        effect = Effects.ReturnToHand(instant) then
            Effects.ReturnToHand(sorcery) then
            Effects.Discard(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Yeong-Hao Han"
        flavorText = "A few gnawing thoughts can eat at the mind until nothing else remains."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/782e5600-ac63-4f90-acbe-49c04d2840ee.jpg?1783937454"
        ruling(
            "2016-07-13",
            "You may discard one of the cards you return to your hand. If you have no other cards " +
                "in hand, you must discard one of the cards you return to your hand.",
        )
        ruling(
            "2016-07-13",
            "You may cast Shreds of Sanity with just one target. You may also cast it without any " +
                "targets if you just want to discard a card.",
        )
        ruling(
            "2016-07-13",
            "If you target two cards and one of the targeted cards becomes illegal, you'll still " +
                "return the other one, discard a card, and exile Shreds of Sanity. If each targeted " +
                "card becomes illegal, Shreds of Sanity doesn't resolve and you won't discard a card " +
                "or exile Shreds of Sanity.",
        )
    }
}
