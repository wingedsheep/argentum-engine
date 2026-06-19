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
    // "Return all <filter> cards from your graveyard to your hand" (Wisdom of Ages) — a mass
    // Gather(graveyard, filter) -> MoveCollection(hand) with no selection.
    composed("ReturnEachCardFromGraveyardToHand", "Gather(graveyard, filter) -> MoveCollection(hand)", composes = listOf("MoveCollection"))
    // "Return each <filter> card from your graveyard to the battlefield" (Fix What's Broken) — the
    // battlefield analog: a mass Gather(graveyard, filter) -> MoveCollection(battlefield, under owners'
    // control). The emitter declines this (Fix What's Broken pairs it with the additional pay-X-life cost
    // that surfaces X into the graveyard filter — an extra-cost / cast-time-value shape the engine is
    // still sloppy around, per the module README), so this is capability-only -> SCAFFOLD.
    composed("PutEachGraveyardCardOntoBattlefield", "Gather(graveyard, filter) -> MoveCollection(battlefield); emitter scaffolds (additional pay-X-life cost into filter)", composes = listOf("MoveCollection"))
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
    // "Reveal cards from the top until you reveal a <type> card. Put it [in hand|onto the battlefield
    // tapped] and the rest on the bottom in a random/any order." (House Cartographer, Clifftop Lookout)
    // -> GatherUntilMatch -> Reveal -> Filter(exclude match) -> MoveCollection(match) -> MoveCollection(rest).
    composed("RevealCardsFromTheTopOfLibraryUntilACardOfTypeIsRevealed", "GatherUntilMatch -> Reveal -> Filter -> MoveCollection x2", composes = listOf("MoveCollection"))

    composed("ExilePermanent", UNIVERSAL, composes = listOf("MoveToZone"))
    // "[A player] exiles a [filter] they control" — the exile flavor of an edict, where the affected
    // player CHOOSES which of their permanents to exile (End of the Hunt: "target opponent exiles a
    // creature or planeswalker they control with the greatest mana value among …"). Expressible as a
    // Gather(that player's matching permanents) -> SelectFromCollection(chooser = TargetPlayer) ->
    // MoveCollection -> exile pipeline; a greatest-mana-value restriction adds
    // FilterCollection(GreatestManaValue). Capability-only — the emitter declines the PlayerAction +
    // highest-mana-value selection cluster -> SCAFFOLD.
    composed(
        "ExileAPermanent",
        "Gather(player's permanents) -> [FilterCollection(GreatestManaValue)] -> Select(chooser=TargetPlayer) -> MoveCollection -> exile",
        composes = listOf("MoveCollection", "MoveToZone")
    )
    // "Exile [a card]" — the generic exile action over any exilable card (e.g. a card in a graveyard:
    // Lazav, Familiar Stranger's "you may exile a card from a graveyard"). MoveToZone / Gather +
    // MoveCollection -> exile. Capability-only; the emitter declines the card-specific surrounding
    // pipeline (the may-gate + become-a-copy follow-up) -> SCAFFOLD.
    composed("Exile", "MoveToZone / Gather+MoveCollection -> exile (any exilable card)", composes = listOf("MoveToZone", "MoveCollection"))
    // "exile target <permanent> until this <permanent> leaves the battlefield" — the Banishing Light
    // O-Ring shape (Mystical Tether, Lassoed by the Law). Maps to the ExileUntilLeaves effect paired
    // with a synthesized leaves-battlefield ReturnLinkedExile trigger; only the
    // `UntilPermanentLeavesBattlefield ThisPermanent` expiration renders exactly (anything else scaffolds).
    effect("ExilePermanentUntil", "ExileUntilLeaves")
    // "Exile the top card of your library" — the impulse-draw exile half (Irascible Wolverine, Alania's
    // Pathmaker). Gather(top of library) + MoveCollection -> exile; paired with a MayPlayExiledCard grant.
    composed("ExileTopCardOfLibrary", "Gather(top of library) + MoveCollection -> exile (impulse)", composes = listOf("MoveCollection"))
    // "Mill a card" — the mill-then-play half (Tablet of Discovery). Gather(top of library) +
    // MoveCollection -> graveyard; paired with a MayPlayCardsMilledThisWay grant (which honours the
    // card sitting in the graveyard).
    composed("MillACard", "Gather(top of library) + MoveCollection -> graveyard (mill-then-play)", composes = listOf("MoveCollection"))
    // "Reveal cards from the top of your library until you reveal an instant or sorcery card. Put that
    // card into your hand and the rest on the bottom of your library in a random order." (Secrets of
    // Strixhaven — Page, Loose Leaf's Grandeur). GatherUntilMatch(top of library, stop at first match) +
    // RevealCollection + two filtered MoveCollections (matched -> hand; rest -> library bottom, random).
    // The emitter renders ONLY the instant-or-sorcery-stop / matched-to-hand / rest-to-bottom-random
    // shape; any other filter or reveal-action set declines -> SCAFFOLD.
    composed(
        "RevealCardsFromTheTopOfLibraryUntilACardOfTypeIsRevealed",
        "GatherUntilMatch(top of library) + RevealCollection + MoveCollection (matched->hand, rest->library bottom random)",
        composes = listOf("MoveCollection"),
    )
    // "Exile the top N cards of target player's library" — the parameterized impulse-exile from any
    // player's library (Laughing Jasper Flint from a target opponent; Rakdos, the Muscle from target
    // player). Gather(top N of that player's library) + MoveCollection -> that player's exile, paired
    // with a GrantMayPlayFromExile window (often `withAnyManaType`). Capability-only — the emitter
    // declines the player-targeted + may-play-with-any-mana cluster -> SCAFFOLD.
    composed("ExileTheTopNumberCardsOfPlayersLibrary", "Gather(top N of target player's library) + MoveCollection -> exile (impulse from a player's library)", composes = listOf("MoveCollection"))
    // "Each player exiles the top N cards of their library" (Memory Vessel) — the per-player impulse
    // exile, inside a ForEachPlayer body that rebinds the controller to each player. Gather(top N of
    // your library) + MoveCollection -> your exile, run once per player. Capability-only — the
    // emitter declines the surrounding per-player may-play/restriction activated-ability -> SCAFFOLD.
    composed("ExileTheTopNumberCardsOfLibrary", "ForEachPlayer { Gather(top N of your library) + MoveCollection -> exile } (per-player impulse)", composes = listOf("MoveCollection"))
    // "Put up to N land cards from your hand onto the battlefield tapped" (The Gitrog, Ravenous Ride).
    // Gather(land cards in hand) -> ChooseUpTo(N) -> MoveCollection(hand->battlefield, tapped). Same
    // shape as PutACardFromHandOnBattlefield but bounded by a dynamic count; capability-only -> SCAFFOLD.
    composed("PutUptoNumberCardsFromHandOntoBattlefield", "Gather(hand) -> ChooseUpTo(N) -> MoveCollection (hand->battlefield, optionally tapped)", composes = listOf("MoveCollection"))
    // "Return the exiled card to the battlefield" — the delayed return half of exile-then-return
    // (Conciliator's Duelist). A plain MoveToZone back to the battlefield under its owner's control.
    composed("PutExiledCardOntoBattlefield", UNIVERSAL, composes = listOf("MoveToZone"))
    // "You may cast the exiled card without paying its mana cost" (The Key to the Vault) — the
    // mid-resolution CastFromCollectionWithoutPayingCostEffect over a card just exiled by the look
    // pipeline (Sunbird's Invocation / Goliath Daydreamer shape).
    composed("CastExiledCardWithoutPaying", "CastFromCollectionWithoutPayingCost over the exiled card", composes = listOf("CastFromCollectionWithoutPayingCost"))
    // "You may cast a [filtered] spell from your hand without paying its mana cost" (Kellan, the
    // Kid). Gather hand -> FilterCollection(ManaValueAtMost(...)) -> CastFromCollectionWithoutPayingCost.
    // Coverable as a composition even though Kellan's full free-cast-or-play-land body declines to
    // SCAFFOLD in the emitter.
    composed("CastASpellFromHandWithoutPaying", "CastFromCollectionWithoutPayingCost over a hand-gathered, MV-bounded collection", composes = listOf("CastFromCollectionWithoutPayingCost"))
    // "Exile target spell" (CR 718 — Aven Interrupter). Not a counter (ignores can't-be-countered);
    // the spell leaves the stack and fails to resolve. Maps to ExileTargetSpellEffect; the paired
    // `CreateExiledCardEffect[IsPlotted]` sub-action sets `makePlotted = true` (the exiled card
    // becomes plotted for its owner).
    effect("ExileSpell", "ExileTargetSpell")
    // "It becomes plotted." — the exiled-card designation on a freshly-exiled card (CR 718).
    // Renders via the `makePlotted` flag on ExileTargetSpell (Aven) or MakePlottedEffect
    // (Make Your Own Luck); the capability is the plotted designation itself.
    supported("IsPlotted", "exiled-card effect: becomes plotted (MakePlottedEffect / ExileTargetSpell.makePlotted)")
    envelope("CreateExiledCardEffect", "envelope: apply an effect to a just-exiled card (capability is the _ExiledCardEffect)")
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
