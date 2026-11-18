package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Profane Memento
 * {1}
 * Artifact
 * Whenever a creature card is put into an opponent's graveyard from anywhere, you gain 1 life.
 *
 * A per-card zone change to the graveyard with no origin, so a creature dying, a discarded or
 * milled creature card and a countered creature spell all count; tokens aren't cards. "From
 * anywhere" is never a leaves-the-battlefield ability (CR 603.6c), so Profane Memento destroyed
 * at the same time as an opponent's creature doesn't look back and doesn't trigger.
 */
val ProfaneMemento = card("Profane Memento") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever a creature card is put into an opponent's graveyard from anywhere, you gain 1 life."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.nontoken().ownedByOpponent())
            .changesZone(to = Zone.GRAVEYARD)
        effect = Effects.GainLife(1)
        description = "Whenever a creature card is put into an opponent's graveyard from anywhere, you gain 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "226"
        artist = "Franz Vohwinkel"
        flavorText = "\"An angel's skull is left too plain by death. I made a few aesthetic modifications.\"\n—Dommique, blood artist"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2c97632-3cf1-4b79-9d18-d8991654dcca.jpg?1783939156"
    }
}
