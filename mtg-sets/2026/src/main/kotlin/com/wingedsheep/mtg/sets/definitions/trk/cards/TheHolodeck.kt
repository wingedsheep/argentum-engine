package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayPlayCardsFromExile

/**
 * The Holodeck
 * {7}
 * Legendary Artifact
 *
 * Once during each of your turns, you may play a permanent card you own from outside the game or
 * from your graveyard.
 *
 * One once-per-turn allowance covers both zones: the static grant reaches the player's own
 * graveyard and sideboard (the engine's "outside the game", CR 400.11a), and a play from either
 * spends it. "Play" covers lands as well as permanent spells.
 */
val TheHolodeck = card("The Holodeck") {
    manaCost = "{7}"
    typeLine = "Legendary Artifact"
    oracleText = "Once during each of your turns, you may play a permanent card you own from outside " +
        "the game or from your graveyard."

    staticAbility {
        ability = MayPlayCardsFromExile(
            filter = GameObjectFilter.Permanent.ownedByYou(),
            condition = Conditions.IsYourTurn,
            oncePerTurn = true,
            zones = setOf(Zone.GRAVEYARD, Zone.SIDEBOARD),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "265"
        artist = "Jessica Rossier"
        flavorText = "Early holodecks used holograms and force fields to simulate simple environments, " +
            "but the addition of replicator technology in the 2360s allowed users to immerse all five " +
            "senses in fictional worlds."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57e830f5-564d-4899-87c0-74192c561fc8.jpg?1792508432"
    }
}
