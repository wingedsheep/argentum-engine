package com.wingedsheep.tooling.coverage.bridge

/** Structural mtgish wrappers that carry no capability themselves — the real capability lives in
 *  their nested nodes. ("ignore" in the legacy JSON.) */
internal fun BridgeBuilder.structuralEnvelopes() {
    envelope("SpellActions", "envelope: 'this spell does X'")
    // Spree (CR 702.166, Outlaws of Thunder Junction): "choose one or more additional costs". The rule
    // is a structural wrapper over its SpreeAction modes (each a PayMana cost + nested actions); the
    // real capability lives in those nested actions. The emitter renders it as a `ModalEffect` with
    // per-mode `additionalManaCost`, `minChooseCount = 1` (Explosive Derailment, Caught in the Crossfire).
    envelope("SpellActions_Spree", "envelope: Spree — choose one or more additional-cost modes")
    envelope("CastEffect", "envelope: on-cast actions")
    // "[that spell] does X" / "as it resolves, …" — a one-shot effect attached to a spell on the
    // stack. The real capability is the nested _SpellEffect / _ResolveAction (e.g. Lilah, Undefeated
    // Slickshot's AsResolves -> ExileResolvingSpellAndPlotIt). Both are structural wrappers.
    envelope("CreateSpellEffect", "envelope: a one-shot effect on a spell (capability is the nested _SpellEffect)")
    envelope("AsResolves", "envelope: an as-it-resolves effect on a spell (capability is the nested _ResolveAction)")
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

    // A global, every-player continuous static ("Each player can't cast more than one spell each
    // turn", High Noon). The real capability is the nested _PlayerEffect; the emitter's
    // eachPlayerEffectBlock renders the shapes it can express exactly (the spell-count cap ->
    // `RestrictSpellsCastPerTurn(eachPlayer = true)`) and scaffolds the rest.
    envelope("EachPlayerEffect", "envelope: global every-player static (capability is the _PlayerEffect)")

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

    // Loop / repeat control-flow. The capability is the nested action body + a repeat count; the engine
    // expresses bounded repeats via `RepeatDynamicTimesEffect` and "repeat until a condition" via a
    // `RepeatWhileEffect`. These are STRUCTURAL — the real capability is the nested actions, scored on
    // their own. The emitter declines (a loop's exact bound — "X more times" vs "once" vs "while" — is
    // card-specific around the engine's still-rough cast-time X / extra-cost handling), so cards using
    // them stay SCAFFOLD even though the capability is present.
    //   - "… Then repeat this process X more times." (Another Round) — a count-bounded repeat.
    envelope("RepeatableActionsNumTimes", "envelope: repeat a sequence N times (RepeatDynamicTimesEffect)", composes = listOf("RepeatDynamicTimes"))
    //   - "… Then if [cond], repeat this process [once]." (Claim Jumper) — a do-once-then-repeat loop.
    envelope("RepeatableActions", "envelope: repeatable action sequence (RepeatDynamicTimes / RepeatWhile)", composes = listOf("RepeatDynamicTimes", "RepeatWhile"))
    //   - The "repeat this process" loop-back marker inside a RepeatableActions body.
    supported("RepeatThisProcess", "loop-back marker inside a repeatable action sequence")
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
