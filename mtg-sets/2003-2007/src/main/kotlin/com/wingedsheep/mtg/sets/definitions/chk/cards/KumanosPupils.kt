package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange

/**
 * Kumano's Pupils
 * {4}{R}
 * Creature — Human Shaman
 * 3/3
 * If a creature dealt damage by this creature this turn would die, exile it instead.
 */
val KumanosPupils = card("Kumano's Pupils") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    oracleText = "If a creature dealt damage by this creature this turn would die, exile it instead."
    power = 3
    toughness = 3

    // A printed replacement read at death time, not a damage-time mark: the rulings require this
    // creature to still be on the battlefield (or leaving simultaneously) when the damaged creature
    // would die.
    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.wasDealtDamageBySourceThisTurn(),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD,
            ),
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Greg Hildebrandt"
        flavorText = "\"Long before he reluctantly joined the war, stories spread of Kumano's followers " +
            "winning victories against the kami.\"\n—*Observations of the Kami War*"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c62b233e-95fe-4cfd-ad89-86e07656bf8b.jpg?1783944298"
    }
}
