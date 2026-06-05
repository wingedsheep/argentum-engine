package com.wingedsheep.tooling.coverage.bridge

/** Structural mtgish wrappers that carry no capability themselves — the real capability lives in
 *  their nested nodes. ("ignore" in the legacy JSON.) */
internal fun BridgeBuilder.structuralEnvelopes() {
    envelope("SpellActions", "envelope: 'this spell does X'")
    envelope("CastEffect", "envelope: on-cast actions")
    envelope("PermanentRuleEffect", "envelope: static ability")
    envelope("TriggerA", "envelope: triggered ability (cond in _Trigger)")
    envelope("Activated", "envelope: activated ability")
    envelope("ActivatedWithModifiers", "envelope: activated ability")
    envelope("And", "envelope: cost/action conjunction")
    envelope("If", "conditional envelope")

    // The "you may / unless / if you do" gate cluster (Lesson 1).
    envelope("MayAction", "gate envelope: 'you may' (Lesson 1 cluster)")
    envelope("MayCost", "gate envelope: 'you may pay' (Lesson 1 cluster)")
    envelope("Unless", "gate envelope: 'unless ...' (Lesson 1 cluster)", composes = listOf("PayOrSuffer"))

    // Player-delegation envelopes.
    envelope("PlayerAction", "envelope: 'target player does X'")
    envelope("EachPlayerAction", "envelope: APNAP each-player")
    envelope("EachPlayerActions", "envelope: APNAP each-player (plural)")
    envelope("HavePlayerTakeAction", "envelope: delegated action")

    // Continuous-effect envelopes (the capability is the nested _LayerEffect / _Rule).
    envelope("CreatePermanentLayerEffectUntil", "envelope: continuous effect (capability is the _LayerEffect)")
    envelope("CreateEachPermanentLayerEffectUntil", "envelope: continuous effect, each")
    envelope("CreateEachPermanentRuleEffectUntil", "envelope: continuous rule, each")
    envelope("CreatePlayerEffectUntil", "envelope: player-scoped continuous effect")
    envelope("CreatePermanentLayerEffect", UNIVERSAL)
    envelope("CreatePermanentRuleEffectUntil", UNIVERSAL)

    // Cross-set "may" / choice / branch envelopes surfaced by calibration on later sets.
    envelopes("PlayerMayAction", "PlayerMayCost", "MayActions", note = UNIVERSAL, composes = listOf("May"))
    envelope("ChooseACreatureType", UNIVERSAL)
    envelope("ChooseAColor", UNIVERSAL)
    envelope("IfElse", UNIVERSAL)
}
