package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Destroy the Evidence
 * {4}{B}
 * Sorcery
 * Destroy target land. Its controller reveals cards from the top of their library until they
 * reveal a land card, then puts those cards into their graveyard.
 *
 * Polymorph's destroy-then-reveal-until shape (the reveal follows even if the land survives),
 * with Undercity Informer's GatherUntilMatch + reveal + graveyard tail aimed at the land's
 * controller.
 */
val DestroyTheEvidence = card("Destroy the Evidence") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target land. Its controller reveals cards from the top of their library " +
        "until they reveal a land card, then puts those cards into their graveyard."

    spell {
        val t = target(TargetFilter.Land)
        val controllerOfTarget = Player.ControllerOf("target land")
        effect = Effects.Pipeline {
            run(Effects.Destroy(t))
            val (_, allRevealed) = gatherUntilMatch(GameObjectFilter.Land, player = controllerOfTarget)
            reveal(allRevealed)
            toGraveyard(allRevealed, controllerOfTarget)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "64"
        artist = "Clint Cearley"
        flavorText = "Everyone knew the Dimir had done the damage. Everyone suspected the Izzet had hired them."
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bca53097-108d-457e-831c-e3d6cb499a41.jpg?1783940363"
    }
}
