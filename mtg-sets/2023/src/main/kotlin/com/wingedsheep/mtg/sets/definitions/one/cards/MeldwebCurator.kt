package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Meldweb Curator — Phyrexia: All Will Be One #59
 * {3}{U}
 * Creature — Phyrexian Wizard
 * 3/4
 *
 * When this creature enters, put up to one target instant or sorcery card from your graveyard
 * on top of your library.
 *
 * "Up to one target" is `optional = true` on the target, so the
 * trigger is legal with an empty graveyard and simply chooses nothing.
 */
val MeldwebCurator = card("Meldweb Curator") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Phyrexian Wizard"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, put up to one target instant or sorcery card from your graveyard on top of your library."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val card = target(TargetFilter.InstantOrSorceryInYourGraveyard, optional = true)
        effect = Effects.PutOnTopOfLibrary(card)
        description = "When this creature enters, put up to one target instant or sorcery card from your graveyard on top of your library."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "59"
        artist = "Pavel Kolomeyets"
        flavorText = "\"Imagine a giant room, like a library . . . except instead of books, it's filled with body parts.\"\n—Kara Vrist, resistance spymaster"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0d392b2-1dac-432d-930c-66b238f7512b.jpg?1783918062"
    }
}
