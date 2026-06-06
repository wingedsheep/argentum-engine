package com.wingedsheep.tooling.coverage.bridge

/** Structural mtgish wrappers that carry no capability themselves — the real capability lives in
 *  their nested nodes. ("ignore" in the legacy JSON.) */
internal fun BridgeBuilder.structuralEnvelopes() {
    envelope("SpellActions", "envelope: 'this spell does X'")
    envelope("CastEffect", "envelope: on-cast actions")
    envelope("PermanentRuleEffect", "envelope: static ability")
    envelope("TriggerA", "envelope: triggered ability (cond in _Trigger)")
    envelope("TriggerOnceEachTurn", "envelope: triggered ability that triggers only once each turn")
    envelope("Activated", "envelope: activated ability")
    envelope("ActivatedWithModifiers", "envelope: activated ability")
    envelope("And", "envelope: cost/action conjunction")
    envelope("If", "conditional envelope")

    // The "you may / unless / if you do" gate cluster (Lesson 1). A "you may [effect]" realises as a
    // `GatedEffect(gate = MayDecide)` (SerialName "Gated") in our trees, so the gate envelope composes it.
    envelope("MayAction", "gate envelope: 'you may' (Lesson 1 cluster)", composes = listOf("Gated"))
    envelope("MayCost", "gate envelope: 'you may pay' (Lesson 1 cluster)")
    envelope("Unless", "gate envelope: 'unless ...' (Lesson 1 cluster)", composes = listOf("PayOrSuffer"))

    // Player-delegation envelopes.
    envelope("PlayerAction", "envelope: 'target player does X'")
    envelope("EachPlayerAction", "envelope: APNAP each-player")
    envelope("EachPlayerActions", "envelope: APNAP each-player (plural)")
    envelope("HavePlayerTakeAction", "envelope: delegated action")

    // Aura / host-permanent continuous effects. `EnchantPermanent` declares the aura's enchant
    // restriction — a universal capability (the engine supports auras). `PermanentLayerEffect` wraps
    // the host's static continuous effect, whose real capability is the nested _StaticLayerEffect.
    supported("EnchantPermanent", "aura: enchant restriction (engine supports auras)")
    envelope("PermanentLayerEffect", "envelope: host continuous effect (capability is the _StaticLayerEffect)")
    // A static "each/other matching permanent gets …" lord (Crusade, Goblin King) — the capability is
    // the nested _StaticLayerEffect; the emitter's staticLordBlock renders it.
    envelope("EachPermanentLayerEffect", "envelope: lord continuous effect (capability is the _StaticLayerEffect)")
    // "Enchanted creature has protection from <color>" (the Alpha Ward cycle) — granted to the host as
    // a `GrantProtection(color)` static ability; the trailing "doesn't remove this Aura" clause is moot.
    composed("ProtectionAndDoesntRemovePermanents", "grant protection from color (aura)", composes = listOf("GrantProtection"))
    // "You control enchanted permanent" (Control Magic, Steal Artifact) -> ControlEnchantedPermanent.
    composed("SetController", "gain control of enchanted permanent (aura)", composes = listOf("ControlEnchantedPermanent"))

    // Continuous-effect envelopes (the capability is the nested _LayerEffect / _Rule).
    envelope("CreatePermanentLayerEffectUntil", "envelope: continuous effect (capability is the _LayerEffect)")
    // "Each matching permanent gets … until end of turn." The capability is normally the nested
    // _LayerEffect (ModifyStats / GrantKeyword), but when the group is keyed to an Aura's host permanent
    // ("enchanted creature and creatures that share a type with it", the Onslaught Crowns) it lowers to
    // the dedicated GrantToEnchantedCreatureTypeGroupEffect — which the emitter renders for that shape.
    envelope("CreateEachPermanentLayerEffectUntil", "envelope: continuous effect, each",
        composes = listOf("GrantToEnchantedCreatureTypeGroup"))
    envelope("CreateEachPermanentRuleEffectUntil", "envelope: continuous rule, each")
    envelope("CreatePlayerEffectUntil", "envelope: player-scoped continuous effect")
    envelope("CreatePermanentLayerEffect", UNIVERSAL)
    envelope("CreatePermanentRuleEffectUntil", UNIVERSAL)

    // Cross-set "may" / choice / branch envelopes surfaced by calibration on later sets.
    envelopes("PlayerMayAction", "PlayerMayCost", note = UNIVERSAL, composes = listOf("May"))
    // "you may [X and Y]" — a single optional choice gating a sequence (Gustcloak cycle); like MayAction
    // it realises as `GatedEffect(gate = MayDecide)` (SerialName "Gated").
    envelope("MayActions", "gate envelope: 'you may [X and Y]'", composes = listOf("Gated"))
    envelope("ChooseACreatureType", UNIVERSAL)
    envelope("ChooseAColor", UNIVERSAL)
    envelope("IfElse", UNIVERSAL)
}
