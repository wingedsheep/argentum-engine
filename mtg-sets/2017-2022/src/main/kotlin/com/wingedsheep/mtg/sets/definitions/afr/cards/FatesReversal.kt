package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fates' Reversal
 * {1}{B}
 * Sorcery
 *
 * Return up to one target creature card from your graveyard to your hand. Venture into the
 * dungeon. (Enter the first room or advance to the next room.)
 *
 * Gathering of Darkness's shape with a venture in place of the amass: "up to one target" is an
 * optional target, so casting it with no creature card in the graveyard (or choosing none) still
 * ventures. If a chosen target is gone on resolution, the spell doesn't resolve and there is no
 * venture.
 */
val FatesReversal = card("Fates' Reversal") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Return up to one target creature card from your graveyard to your hand. " +
        "Venture into the dungeon. (Enter the first room or advance to the next room.)"

    spell {
        val creatureCard = target(
            TargetFilter(GameObjectFilter.Creature.ownedByYou(), zone = Zone.GRAVEYARD),
            optional = true,
        )
        effect = Effects.Move(creatureCard, Zone.HAND) then Effects.VentureIntoTheDungeon()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Alix Branwyn"
        flavorText = "A doom denied—or, perhaps, delayed."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19ff3d76-e985-4d8d-837e-89360cc23824.jpg?1783926496"
    }
}
