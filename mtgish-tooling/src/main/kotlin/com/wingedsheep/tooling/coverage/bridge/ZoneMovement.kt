package com.wingedsheep.tooling.coverage.bridge

/** Zone-movement verbs. Argentum has no leaf "destroy"/"discard"/"mill" effect — they compose from
 *  `MoveToZone` (single) / `MoveCollection` (mass), so every entry here is [composed] and names the
 *  primitives it lowers to (the fidelity scorer reads `composes`). */
internal fun BridgeBuilder.zoneMovement() {
    composed("DestroyPermanent", "MoveToZone -> graveyard", composes = listOf("MoveToZone"))
    composed("DestroyEachPermanent", "MoveCollection -> graveyard", composes = listOf("MoveCollection", "MoveToZone"))
    composed("DestroyEachPermanentNoRegen", "MoveCollection -> graveyard, no-regen flag", composes = listOf("MoveCollection", "MoveToZone"))
    composed("DestroyPermanentNoRegen", UNIVERSAL, composes = listOf("MoveToZone"))

    composed("SearchLibrary", "Gather->Select->Move SearchLibrary pattern", composes = listOf("MoveCollection", "ShuffleLibrary"))

    composed("DiscardACard", "Patterns.Hand.discardCards -> Gather/Select/MoveCollection", composes = listOf("MoveCollection"))
    composed("DiscardNumberCards", "MoveToZone hand->graveyard, N", composes = listOf("MoveCollection"))
    composed("DiscardAnyNumberOfCards", "MoveToZone hand->graveyard, any", composes = listOf("MoveCollection"))
    composed("DiscardACardAtRandom", "Patterns.Hand.discardRandom -> MoveCollection", composes = listOf("MoveCollection"))

    composed("PutGraveyardCardIntoHand", "MoveCollection graveyard->hand", composes = listOf("MoveToZone"))
    composed("PutGraveyardCardOntoBattlefield", "MoveCollection graveyard->battlefield (reanimate)", composes = listOf("MoveToZone"))
    composed("ShuffleGraveyardCardIntoLibrary", "MoveCollection + ShuffleLibrary", composes = listOf("MoveToZone", "ShuffleLibrary"))
    composed("ReturnDeadGraveyardCardToTopOfLibrary", "MoveCollection -> library top", composes = listOf("MoveToZone"))
    composed("ReturnGraveyardCardToHand", UNIVERSAL, composes = listOf("MoveToZone"))
    composed("PutEachGraveyardCardIntoHand", UNIVERSAL, composes = listOf("MoveCollection"))
    composed("ExileGraveyardCard", UNIVERSAL, composes = listOf("MoveToZone"))

    composed("ShuffleHandIntoLibrary", "MoveCollection hand->library + ShuffleLibrary", composes = listOf("MoveCollection", "ShuffleLibrary"))
    composed("ShuffleGraveyardIntoLibrary", "Patterns.Library.shuffleGraveyardIntoLibrary -> Gather + MoveCollection (shuffled)", composes = listOf("MoveCollection"))

    composed("Surveil", "Patterns.Library.surveil -> Gather/Select/MoveCollection", composes = listOf("MoveCollection"))
    composed("Scry", "Patterns.Library.scry -> Gather/Select/MoveCollection", composes = listOf("MoveCollection"))

    composed("PutACardFromHandOnBattlefield", "Patterns.Hand.putFromHand -> Gather/Select/MoveCollection", composes = listOf("MoveCollection"))
    composed("PutTopOfLibraryInHand", "look-top pipeline -> MoveCollection (library->hand)", composes = listOf("MoveCollection", "MoveToZone"))
    composed("PutTopOfLibraryInGraveyard", "look-top pipeline -> MoveCollection (library->graveyard)", composes = listOf("MoveCollection", "MoveToZone"))

    composed("PutPermanentIntoItsOwnersHand", "bounce: MoveToZone / Gather-Select-MoveCollection pipeline", composes = listOf("MoveToZone"))
    composed("PutEachPermanentIntoItsOwnersHand", "EachPlayerReturnsPermanentToHand", composes = listOf("MoveCollection", "MoveToZone"))
    composed("PutPermanentOnTopOfOwnersLibrary", "PutOnLibraryPositionOfChoice", composes = listOf("MoveToZone"))

    composed("LookAtTheTopNumberCardsOfLibrary", "Patterns.Library.lookAtTopAndKeep/Reorder -> Gather/Select/MoveCollection", composes = listOf("MoveCollection"))
    composed("LookAtTheTopNumberCardsOfPlayersLibrary", "look pipeline on opponent library -> MoveCollection", composes = listOf("MoveCollection"))

    composed("ExilePermanent", UNIVERSAL, composes = listOf("MoveToZone"))
    composed("ExileEachPermanent", UNIVERSAL, composes = listOf("MoveCollection", "MoveToZone"))
    composed("MillNumberCards", UNIVERSAL, composes = listOf("MoveCollection"))
    composed("MillCards", UNIVERSAL, composes = listOf("MoveCollection"))
}
