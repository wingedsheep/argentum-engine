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
    composed("PutGraveyardCardOnBottomOfLibrary", "MoveCollection -> library bottom (Tomb Trawler)", composes = listOf("MoveToZone"))
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
    // "Return a <filter> you control to its owner's hand" — the controller CHOOSES the permanent (not a
    // target); expressible as a self-bounce target constrained to the chooser's own permanents (the Karoo
    // bounce-land idiom, Arid Archway). Capability only — the emitter scaffolds the "another Desert was
    // returned this way" follow-up (see ZoneHandlers' `If` decline), so this is SCAFFOLD-tier in practice.
    composed("PutAPermanentIntoItsOwnersHand", "chosen bounce: self-bounce target over the chooser's permanents -> MoveToZone", composes = listOf("MoveToZone"))
    composed("PutEachPermanentIntoItsOwnersHand", "EachPlayerReturnsPermanentToHand", composes = listOf("MoveCollection", "MoveToZone"))
    // "Return any number of [filter] to their owner's hand" (Rambling Possum) — Gather -> ChooseAnyNumber
    // -> MoveCollection (battlefield->hand routes to owner).
    composed("ReturnAnyNumberOfPermanentsToTheirOwnersHands", "Gather->ChooseAnyNumber->MoveCollection (battlefield->owner hand)", composes = listOf("MoveCollection", "MoveToZone"))
    composed("PutPermanentOnTopOfOwnersLibrary", "PutOnLibraryPositionOfChoice", composes = listOf("MoveToZone"))

    composed("LookAtTheTopNumberCardsOfLibrary", "Patterns.Library.lookAtTopAndKeep/Reorder -> Gather/Select/MoveCollection", composes = listOf("MoveCollection"))
    composed("LookAtTheTopNumberCardsOfPlayersLibrary", "look pipeline on opponent library -> MoveCollection", composes = listOf("MoveCollection"))

    composed("ExilePermanent", UNIVERSAL, composes = listOf("MoveToZone"))
    // "exile target <permanent> until this <permanent> leaves the battlefield" — the Banishing Light
    // O-Ring shape (Mystical Tether, Lassoed by the Law). Maps to the ExileUntilLeaves effect paired
    // with a synthesized leaves-battlefield ReturnLinkedExile trigger; only the
    // `UntilPermanentLeavesBattlefield ThisPermanent` expiration renders exactly (anything else scaffolds).
    effect("ExilePermanentUntil", "ExileUntilLeaves")
    // "Exile the top card of your library" — the impulse-draw exile half (Irascible Wolverine, Alania's
    // Pathmaker). Gather(top of library) + MoveCollection -> exile; paired with a MayPlayExiledCard grant.
    composed("ExileTopCardOfLibrary", "Gather(top of library) + MoveCollection -> exile (impulse)", composes = listOf("MoveCollection"))
    // "Return the exiled card to the battlefield" — the delayed return half of exile-then-return
    // (Conciliator's Duelist). A plain MoveToZone back to the battlefield under its owner's control.
    composed("PutExiledCardOntoBattlefield", UNIVERSAL, composes = listOf("MoveToZone"))
    // "You may cast the exiled card without paying its mana cost" (The Key to the Vault) — the
    // mid-resolution CastFromCollectionWithoutPayingCostEffect over a card just exiled by the look
    // pipeline (Sunbird's Invocation / Goliath Daydreamer shape).
    composed("CastExiledCardWithoutPaying", "CastFromCollectionWithoutPayingCost over the exiled card", composes = listOf("CastFromCollectionWithoutPayingCost"))
    composed("ExileEachPermanent", UNIVERSAL, composes = listOf("MoveCollection", "MoveToZone"))
    composed("MillNumberCards", UNIVERSAL, composes = listOf("MoveCollection"))
    composed("MillCards", UNIVERSAL, composes = listOf("MoveCollection"))

    // "Exile any number of [permanents] you control, then return them …" — the player-chosen blink
    // (Another Round). A Gather -> ChooseAnyNumber -> MoveCollection(-> exile, linked) pipeline; the
    // return half is `PutEachExiledCardOntoTheBattlefield`. The emitter declines (the loop/return shape
    // is card-specific), so this is capability-only -> SCAFFOLD.
    composed("ExileAnyNumberOfPermanents", "Gather->ChooseAnyNumber->MoveCollection (battlefield->exile, linked)", composes = listOf("MoveCollection", "MoveToZone"))
    // "Return them to the battlefield under their owner's control" — the return half of a blink:
    // Gather(from linked exile) + MoveCollection(-> battlefield, underOwnersControl).
    composed("PutEachExiledCardOntoTheBattlefield", "Gather(linked exile) + MoveCollection (exile->battlefield, owners' control)", composes = listOf("MoveCollection", "MoveToZone"))
    // "If you searched your library this way, shuffle" — a deferred shuffle gated on whether a search
    // happened (Claim Jumper). The search itself shuffles per `searchLibrary`, so the engine expresses
    // this; capability-only here (the loop body is card-specific) -> SCAFFOLD.
    composed("ShuffleLibraryIfSearched", "ShuffleLibrary, gated on whether a search occurred", composes = listOf("ShuffleLibrary"))
}
