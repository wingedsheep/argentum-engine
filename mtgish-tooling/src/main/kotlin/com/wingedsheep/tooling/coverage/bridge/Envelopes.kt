package com.wingedsheep.tooling.coverage.bridge

/** Structural mtgish wrappers that carry no capability themselves — the real capability lives in
 *  their nested nodes. ("ignore" in the legacy JSON.) */
internal fun BridgeBuilder.structuralEnvelopes() {
    envelope("SpellActions", "envelope: 'this spell does X'")
    envelope("CastEffect", "envelope: on-cast actions")
    envelope("PermanentRuleEffect", "envelope: static ability")
    envelope("TriggerA", "envelope: triggered ability (cond in _Trigger)")
    // TriggerI — a triggered ability with an intervening-if condition (CR 603.4): args are
    // [trigger, condition, actions]. The condition is rendered as a `triggerCondition = …` line; an
    // unrenderable condition declines to a scaffold (Canyon Crab's "if you haven't cast a spell from
    // your hand this turn").
    envelope("TriggerI", "envelope: triggered ability with intervening-if condition")
    envelope("TriggerOnceEachTurn", "envelope: triggered ability that triggers only once each turn")
    envelope("Activated", "envelope: activated ability")
    envelope("ActivatedWithModifiers", "envelope: activated ability")
    // Zone-scoped activated abilities: the inner Activated rule carries the real capability; the
    // wrapper only sets the ability's `activateFromZone` (Zone.HAND / Zone.GRAVEYARD).
    envelope("FromHand", "envelope: activated ability used from hand (activateFromZone = Zone.HAND)")
    envelope("FromGraveyard", "envelope: activated ability used from graveyard (activateFromZone = Zone.GRAVEYARD)")
    envelope("And", "envelope: cost/action conjunction")
    // A resolution-time intervening-if (`If[cond, [then]]` inside a spell/ability ActionList) realises as
    // a `ConditionalEffect` -> `GatedEffect(gate = WhenCondition)` (SerialName "Gated") in our trees, the
    // same compiled shape as a "you may" gate; so the conditional envelope composes the Gated capability
    // (Foolish Fate's "if you gained life this turn …", Burrog Barrage's "+1/+0 if you've cast …").
    envelope("If", "conditional envelope", composes = listOf("Gated"))

    // The "you may / unless / if you do" gate cluster (Lesson 1). A "you may [effect]" realises as a
    // `GatedEffect(gate = MayDecide)` (SerialName "Gated") in our trees, so the gate envelope composes it.
    envelope("MayAction", "gate envelope: 'you may' (Lesson 1 cluster)", composes = listOf("Gated"))
    envelope("MayCost", "gate envelope: 'you may pay' (Lesson 1 cluster)")
    envelope("Unless", "gate envelope: 'unless ...' (Lesson 1 cluster)", composes = listOf("PayOrSuffer"))

    // A controller-scoped continuous static ("spells you cast cost less", "you have shroud"); the real
    // capability is the nested _PlayerEffect (e.g. DecreaseSpellCost -> ModifySpellCost). The emitter
    // renders only the exact shapes it can express (Geyser Drake's turn-gated cost reduction) and
    // scaffolds the rest, so this is a structural envelope, never a blanket capability claim.
    envelope("PlayerEffect", "envelope: controller-scoped static (cost reduction, shroud, ...)")

    // Player-delegation envelopes.
    envelope("PlayerAction", "envelope: 'target player does X'")
    envelope("EachPlayerAction", "envelope: APNAP each-player")
    envelope("EachPlayerActions", "envelope: APNAP each-player (plural)")
    envelope("HavePlayerTakeAction", "envelope: delegated action")

    // Aura / host-permanent continuous effects. `EnchantPermanent` declares the aura's enchant
    // restriction — a universal capability (the engine supports auras). `PermanentLayerEffect` wraps
    // the host's static continuous effect, whose real capability is the nested _StaticLayerEffect.
    supported("EnchantPermanent", "aura: enchant restriction (engine supports auras)")
    // "The first time you would create one or more tokens each turn, you may instead create that many
    // copies of [attached] permanent" (Mirrormind Crown, Moonlit Meditation) — the engine's
    // ReplaceTokenCreationWithAttachedCopy replacement effect, driven by the source's AttachedToComponent.
    supported("ReplaceAPlayerWouldCreateTokens", "replace token creation with copies of attached permanent (ReplaceTokenCreationWithAttachedCopy)")
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
    // A continuous rule until end of turn. The nested `_PermanentRule` is the real capability: CantBlock /
    // CantAttack render to `CantBlockEffect` (SerialName "CantBlockTargetCreatures") / `CantAttackEffect`
    // (Duel Tactics' "it can't block this turn"), and the can't-be-blocked rules to keyword grants.
    envelope("CreatePermanentRuleEffectUntil", UNIVERSAL,
        composes = listOf("CantBlockTargetCreatures", "CantAttack"))

    // Cross-set "may" / choice / branch envelopes surfaced by calibration on later sets.
    envelopes("PlayerMayAction", "PlayerMayCost", note = UNIVERSAL, composes = listOf("May"))
    // "you may [X and Y]" — a single optional choice gating a sequence (Gustcloak cycle); like MayAction
    // it realises as `GatedEffect(gate = MayDecide)` (SerialName "Gated").
    envelope("MayActions", "gate envelope: 'you may [X and Y]'", composes = listOf("Gated"))
    envelope("ChooseACreatureType", UNIVERSAL)
    envelope("ChooseAColor", UNIVERSAL)
    envelope("IfElse", UNIVERSAL)
    // "[Effect]. [Bigger effect] instead if you controlled a [filter] as you cast this spell" — the OTJ
    // cast-time condition capture (CR 601.2i). Structural: the capability is the nested condition
    // (PlayerPassesFilter) and the two branch effects, scored on their own. The engine freezes the
    // condition at cast via `captureAtCast` and reads it with `Conditions.CapturedAtCast`; the emitter's
    // castTimeCaptureSpell renders the fixed-damage shape (Steer Clear) and scaffolds {X} arms.
    envelope("Modal_IfElse", "envelope: 'as you cast this spell' cast-time condition capture (CR 601.2i)")

    // "As ~ enters the battlefield …" — a replacement-effect wrapper. It carries no capability itself;
    // each nested `_ReplacementActionWouldEnter` (enters tapped, enters with a counter, choose a type, …)
    // is scored on its own, so an unsupported replacement still blocks the card. See the enters-replacement
    // entries in `manaCountersAndState()`.
    envelope("AsPermanentEnters", "envelope: 'as ~ enters' replacement (capability is the _ReplacementActionWouldEnter)")
}
