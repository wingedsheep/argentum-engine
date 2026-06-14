package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.Call
import com.wingedsheep.tooling.coverage.Composite
import com.wingedsheep.tooling.coverage.Dsl
import com.wingedsheep.tooling.coverage.Lit
import com.wingedsheep.tooling.coverage.Raw
import com.wingedsheep.tooling.coverage.Registry
import com.wingedsheep.tooling.coverage.amountNode
import com.wingedsheep.tooling.coverage.arg
import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.asInt
import com.wingedsheep.tooling.coverage.asStr
import com.wingedsheep.tooling.coverage.call
import com.wingedsheep.tooling.coverage.compact
import com.wingedsheep.tooling.coverage.field
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.findRef
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.nodesTagged
import com.wingedsheep.tooling.coverage.strField
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Zone movement: destroy / bounce / reanimate / search / look / mill. Argentum has no leaf
 *  destroy/discard verb — they compose from MoveToZone (single) / MoveCollection (mass). */
internal val zoneHandlers: Map<String, ActionHandler> = actionHandlers {

    on("PutEachPermanentIntoItsOwnersHand") { node, _, _ ->  // bounce each chosen target
        if (jsonContains(node, "_Permanents", "Ref_TargetPermanents")) {
            call("ForEachTargetEffect", arg(call("listOf", arg(call("Effects.Move", arg("EffectTarget.ContextTarget(0)"), arg("Zone.HAND"))))))
        } else null
    }

    on("AttachPermanentToPermanent") { _, args, tvar ->
        // "attach it to target …" — an Equipment/Aura attaching ITSELF to a chosen permanent. The
        // engine idiom is `Effects.AttachEquipment(target)`, which always attaches the source. So this
        // renders ONLY the self-attach shape: args[0] (the permanent being attached) must be a self-ref
        // (`ThatEnteringPermanent` for an ETB "attach it to target creature you control", Thunder Lasso).
        // Anything else (attaching a different permanent) declines -> SCAFFOLD.
        val arr = args.asArr ?: return@on null
        val subjectRef = findRef(arr.getOrNull(0)) ?: return@on null
        if (subjectRef !in SELF_REFS) return@on null
        val tgt = refTargetFromRef(findRef(arr.getOrNull(1)), tvar) ?: return@on null
        call("Effects.AttachEquipment", arg(Lit(tgt)))
    }

    on("DestroyPermanent") { _, args, tvar ->
        val tgt = refTarget(args, tvar) ?: return@on null
        call("Effects.Move", arg(Lit(tgt)), arg("Zone.GRAVEYARD"), arg("byDestruction", "true"))
    }
    on("DestroyPermanentNoRegen") { _, args, tvar ->  // "Destroy …. It can't be regenerated." (Terror, Tunnel)
        val tgt = refTarget(args, tvar) ?: return@on null
        call("Effects.Destroy", arg(Lit(tgt)), arg("noRegenerate", "true"))
    }
    on("DestroyEachPermanent", "DestroyEachPermanentNoRegen") { node, args, _ ->
        if (jsonContains(args, "_Permanents", "Ref_TargetPermanents")) {
            val moveArgs = mutableListOf(arg("EffectTarget.ContextTarget(0)"), arg("Zone.GRAVEYARD"), arg("byDestruction", "true"))
            if (node.strField("_Action") == "DestroyEachPermanentNoRegen") moveArgs.add(arg("noRegenerate", "true"))
            return@on call("ForEachTargetEffect", arg(call("listOf", arg(Call("Effects.Move", moveArgs)))))
        }
        if (oracleText?.contains("target", ignoreCase = true) == true) return@on null
        val noregen = if (node.strField("_Action") == "DestroyEachPermanentNoRegen") "true" else "false"
        val filter = groupFilterExpr(args) ?: return@on null
        call(
            "Effects.ForEachInGroup", arg(filter),
            arg(call("Effects.Move", arg("EffectTarget.Self"), arg("Zone.GRAVEYARD"), arg("byDestruction", "true"))),
            arg("noRegenerate", noregen),
        )
    }

    on("ExilePermanent") { _, args, tvar ->  // "Exile target permanent" -> the atomic Exile facade
        val tgt = refTarget(args, tvar) ?: return@on null
        call("Effects.Exile", arg(Lit(tgt)))
    }

    // "exile target <permanent> until this <permanent> leaves the battlefield" — the Banishing Light /
    // O-Ring shape (Mystical Tether, Lassoed by the Law). The action renders `Effects.ExileUntilLeaves`;
    // the matching leaves-battlefield ReturnLinkedExile trigger is synthesized once at card assembly
    // (see [linkedExileReturnTrigger] in Emitter). Only the `UntilPermanentLeavesBattlefield ThisPermanent`
    // expiration — the only one the linked-exile return models — renders; any other expiration declines
    // (-> SCAFFOLD) rather than emit an exile with the wrong / no return.
    on("ExilePermanentUntil") { _, args, tvar ->
        val a = args.asArr ?: return@on null
        val tgt = refTarget(a.getOrNull(0), tvar) ?: refTarget(args, tvar) ?: return@on null
        val expiration = a.getOrNull(1) as? JsonObject ?: return@on null
        if (expiration.strField("_Expiration") != "UntilPermanentLeavesBattlefield") return@on null
        if (!jsonContains(expiration, "_Permanent", "ThisPermanent")) return@on null
        call("Effects.ExileUntilLeaves", arg(Lit(tgt)))
    }

    // "Exile the top card of your library." (the first half of the impulse-draw idiom — Irascible
    // Wolverine, Alania's Pathmaker). The exiled card is stored under the shared "exiledCard" key that
    // the paired `CreatePlayerEffectUntil{MayPlayExiledCard(TheCardExiledThisWay)}` action reads to grant
    // the may-play window (see TapLayerStateHandlers' CreatePlayerEffectUntil branch). The exile itself is
    // the gather + move-to-exile atomic pair.
    on("ExileTopCardOfLibrary") { _, _, _ ->
        Composite(listOf(
            Lit("GatherCardsEffect(CardSource.TopOfLibrary(DynamicAmount.Fixed(1)), storeAs = \"exiledCard\")"),
            Lit("MoveCollectionEffect(from = \"exiledCard\", destination = CardDestination.ToZone(Zone.EXILE))"),
        ))
    }

    // "Return the exiled card to the battlefield under its owner's control" (the second half of the
    // exile-then-return idiom — Conciliator's Duelist, Parting Gust). `TheCardExiledThisWay` refers to
    // the same bound target that was exiled earlier in the ability, so it resolves to the ability's
    // bound `tvar`; a plain Move to the battlefield returns it under its owner's control (the default).
    // Only the under-owner's-control form renders; an under-your-control / no-bound-target form declines.
    on("PutExiledCardOntoBattlefield") { node, _, tvar ->
        if (tvar == null) return@on null
        if (!jsonContains(node, "_CardInExile", "TheCardExiledThisWay")) return@on null
        val flagBlob = compact(node)
        if ("EntersUnderPlayersControl" in flagBlob || "EntersUnderYourControl" in flagBlob) return@on null
        // Under-owner's-control return. We render the bare return and the one extra enter flag we can
        // express faithfully: a single fixed ±1/±1 counter on the returned card (Daydream's "with a
        // +1/+1 counter on it"). A returned card isn't a permanent until it's back on the battlefield,
        // so the counter is a chained AddCountersEffect after the Move (matching the hand-authored card),
        // not an enters-with replacement. Any other extra flag — a dynamic/derived counter count,
        // entering tapped, a non-±1/±1 counter kind — would be silently dropped, so decline -> SCAFFOLD.
        if ("EntersWithNumberCounters" in flagBlob || "EntersTapped" in flagBlob) return@on null
        val move = call("Effects.Move", arg(Lit(tvar)), arg("Zone.BATTLEFIELD"))
        if ("EntersWithACounter" !in flagBlob) return@on move
        // Pull the EntersWithACounter flag's counter node and render it (counterTypeDsl declines any
        // non-±1/±1 PTCounter / unnamed kind, downgrading the card to SCAFFOLD rather than guessing).
        val counterNode = node.nodesTagged("EntersWithACounter")
            .firstOrNull()?.get("args") ?: return@on null
        val counter = counterTypeDsl(counterNode) ?: return@on null
        Composite(listOf(
            move,
            call("AddCountersEffect", arg("counterType", counter), arg("count", "1"), arg("target", Lit(tvar))),
        ))
    }

    // "...at the beginning of the next end step" delayed trigger (the return half of exile-then-return).
    // Renders only the next-end-step timing as a `CreateDelayedTriggerEffect(step = Step.END, …)`; the
    // body is the normal action-list renderer sharing the ability's bound `tvar` so the exiled target
    // returns. Any other future-trigger timing, or a body the renderer can't express, declines -> SCAFFOLD.
    on("CreateFutureTrigger") { _, args, tvar ->
        val a = args.asArr ?: return@on null
        val timing = a.getOrNull(0) as? JsonObject ?: return@on null
        if (timing.strField("_FutureTrigger") != "AtTheBeginningOfTheNextEndStep") return@on null
        val actionsNode = a.getOrNull(1) as? JsonObject ?: return@on null
        val inner = actionsNode["args"].asArr?.filterIsInstance<JsonObject>() ?: return@on null
        val body = renderEffectList(inner, tvar) ?: return@on null
        call("CreateDelayedTriggerEffect", arg("step", "Step.END"), arg("effect", body))
    }

    on("PutPermanentIntoItsOwnersHand") { _, args, tvar ->  // bounce
        val tgt = refTarget(args, tvar) ?: return@on null
        call("Effects.Move", arg(Lit(tgt)), arg("Zone.HAND"))
    }

    on("GainControlOfPermanentUntil") { _, args, tvar ->
        // "gain control of <permanent> until …" — renders the two durations we can pin exactly to an
        // Argentum `Duration` variant: end-of-turn (Threaten) and "for as long as you control this
        // creature" (Aladdin — IR `UntilPermanentChangesControl` always refers to the source). Any
        // other expiration scaffolds rather than risk a wrong duration.
        val tgt = refTarget(args, tvar) ?: return@on null
        when {
            jsonContains(args, "_Expiration", "UntilEndOfTurn") ->
                call("Effects.GainControl", arg(Lit(tgt)), arg("Duration.EndOfTurn"))
            jsonContains(args, "_Expiration", "UntilPermanentChangesControl") ->
                call("Effects.GainControl", arg(Lit(tgt)), arg("Duration.WhileYouControlSource()"))
            else -> null
        }
    }

    on("SacrificePermanent") { _, args, _ ->  // "sacrifice ~" (Blistering Firecat's end-step sacrifice)
        if (jsonContains(args, "_Permanent", "ThisPermanent")) Lit("SacrificeSelfEffect") else null
    }
    on("SacrificeAPermanent") { _, args, _ ->
        // "sacrifice a <filter>" by the resolving player (Accursed Centaur's ETB "sacrifice a creature").
        // A player-directed sacrifice ("that player sacrifices …") arrives wrapped in a PlayerAction and is
        // handled there; the bare effect sacrifices via the controller, so render SacrificeEffect(filter).
        val filter = gameObjectFilterExpr(args) ?: return@on null
        // "sacrifice a creature OTHER THAN this creature" (Demonic Taskmaster): the filter carries an
        // `Other(ThisPermanent)` self-exclusion that gameObjectFilterExpr drops. SacrificeEffect models it
        // with `excludeSource = true`; render it so the source isn't a legal sacrifice (else we'd widen).
        val excludeSource = jsonContains(args, "_Permanents", "Other") &&
            jsonContains(args, "_Permanent", "ThisPermanent")
        if (excludeSource) call("SacrificeEffect", arg(filter), arg("excludeSource", "true"))
        else call("SacrificeEffect", arg(filter))
    }

    on("ShuffleGraveyardCardIntoLibrary") { _, args, tvar ->  // e.g. Alabaster Dragon
        val tgt = refTarget(args, tvar) ?: "EffectTarget.Self"
        call("Effects.Move", arg(Lit(tgt)), arg("Zone.LIBRARY"), arg("ZonePlacement.Shuffled"))
    }

    on("Surveil") { _, args, _ ->  // "Surveil N" -> the look-top / keep-or-bin pipeline
        (findInteger(args) as? Int)?.let { call("Patterns.Library.surveil", arg("$it")) }
    }

    // Inline `If{cond}[effects]` action (inside an ActionList) -> a `ConditionalEffect`. Renders only the
    // condition shapes we can express faithfully:
    //  - "if [the targeted permanent] had mana value N or less"
    //    (`PermanentPassesFilter(Ref_TargetPermanent, ManaValueIs(LessThanOrEqualTo Integer N))`) ->
    //    `Conditions.TargetSpellManaValueAtMost(DynamicAmount.Fixed(N))` — Consuming Ashes' "Exile target
    //    creature. If it had mana value 3 or less, surveil 2."
    //  - "if you control a <filter>" (`PlayerPassesFilter(You, ControlsA(<filter>))`) ->
    //    `Conditions.YouControl(<filter>)` via [actionConditionDsl] — Failed Fording's "Return target
    //    nonland permanent to its owner's hand. If you control a Desert, surveil 1."
    // Any other condition / target index / comparator declines (-> SCAFFOLD) rather than widening.
    on("If") { node, args, tvar ->
        val a = args.asArr ?: return@on null
        val cond = a.getOrNull(0) as? JsonObject ?: return@on null
        val inner = (a.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return@on null
        // "If a permanent was returned this way, [do X]" (Nurturing Pixie) — the `up to one target …
        // return to hand` idiom gates a follow-up on whether the optional target was actually returned.
        // The engine expresses "the optional target still exists / was acted on" as
        // `Conditions.TargetMatchesFilter(<zone-agnostic type filter>)` (the moved card still matches its
        // type line after going to hand). Render only the bare `AnyPermanent` filter -> Permanent, and
        // only when the ability has a bound target (the "this way" subject); a narrower/other filter, or
        // no bound target, declines -> SCAFFOLD rather than emit a wrong gate.
        if (cond.strField("_Condition") == "APermanentWasPutIntoHandThisWay") {
            if (tvar == null) return@on null
            if (cond["args"].strField("_Permanents") != "AnyPermanent") return@on null
            val edsl = renderEffectList(inner, tvar) ?: return@on null
            return@on call(
                "ConditionalEffect",
                arg("condition", call("Conditions.TargetMatchesFilter", arg("GameObjectFilter.Permanent"))),
                arg("effect", edsl),
            )
        }
        // "if you control a <filter>" — a resolution-time state test over your own board.
        if (cond.strField("_Condition") == "PlayerPassesFilter") {
            val condDsl = actionConditionDsl(cond) ?: return@on null
            val edsl = renderEffectList(inner, tvar) ?: return@on null
            return@on call(
                "ConditionalEffect",
                arg("condition", Lit(condDsl)),
                arg("effect", edsl),
            )
        }
        // "If [N] or more mana was spent to cast that spell, [do X]" (Expressive Firedancer) — a
        // `SpellPassesFilter(Trigger_ThatSpell, AnAmountOfManaWasSpentToCastIt{>= N})` gate on the
        // triggering spell's mana. Renders a `Compare(ContextProperty(MANA_SPENT_ON_TRIGGERING_SPELL),
        // >=, Fixed(N))`. Only the triggering spell + a `GreaterThanOrEqualTo` Integer compare render;
        // any other spell subject / comparator declines -> SCAFFOLD rather than misread the threshold.
        if (cond.strField("_Condition") == "SpellPassesFilter") {
            val cargs = cond["args"].asArr ?: return@on null
            if ((cargs.getOrNull(0) as? JsonObject)?.strField("_Spell") != "Trigger_ThatSpell") return@on null
            val spellFilter = cargs.getOrNull(1) as? JsonObject ?: return@on null
            if (spellFilter.strField("_Spells") != "AnAmountOfManaWasSpentToCastIt") return@on null
            val cmp = spellFilter["args"] as? JsonObject ?: return@on null
            if (cmp.strField("_Comparison") != "GreaterThanOrEqualTo") return@on null
            val n = (cmp["args"].asInt()) ?: ((cmp["args"] as? JsonObject)?.get("args").asInt()) ?: return@on null
            val edsl = renderEffectList(inner, tvar) ?: return@on null
            return@on call(
                "ConditionalEffect",
                arg(
                    "condition",
                    call(
                        "Compare",
                        arg(call("DynamicAmount.ContextProperty", arg("ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL"))),
                        arg("ComparisonOperator.GTE"),
                        arg(call("DynamicAmount.Fixed", arg("$n"))),
                    ),
                ),
                arg("effect", edsl),
            )
        }
        if (cond.strField("_Condition") != "PermanentPassesFilter") return@on null
        val condArgs = cond["args"].asArr ?: return@on null
        // Subject must be the first targeted permanent (Ref_TargetPermanent / Ref_TargetPermanent1).
        val subject = (condArgs.getOrNull(0) as? JsonObject)?.strField("_Permanent")
        if (subject != "Ref_TargetPermanent" && subject != "Ref_TargetPermanent1") return@on null
        val filter = condArgs.getOrNull(1) as? JsonObject ?: return@on null
        if (filter.strField("_Permanents") != "ManaValueIs") return@on null
        val cmp = filter["args"] as? JsonObject ?: return@on null
        if (cmp.strField("_Comparison") != "LessThanOrEqualTo") return@on null
        val n = (cmp["args"].asInt()) ?: ((cmp["args"] as? JsonObject)?.get("args").asInt()) ?: return@on null
        val edsl = renderEffectList(inner, tvar) ?: return@on null
        call(
            "ConditionalEffect",
            arg("condition", call("Conditions.TargetSpellManaValueAtMost", arg(call("DynamicAmount.Fixed", arg("$n"))))),
            arg("effect", edsl),
        )
    }

    // Inline `IfElse{cond}[thenEffects][elseEffects]` action -> a `ConditionalEffect(cond, then, else)`.
    // The condition resolves via [actionConditionDsl] (only the exact shapes we can express render);
    // both branches reuse the normal action-list renderer (sharing the spell's bound `tvar`). Take the
    // Fall: "Target creature gets -1/-0 …. It gets -4/-0 … instead if you control an outlaw." — the
    // "instead" makes this an either/or branch, exactly a ConditionalEffect (never both arms). Anything
    // the condition or a branch can't render declines -> SCAFFOLD rather than dropping a clause.
    on("IfElse") { _, args, tvar ->
        val a = args.asArr ?: return@on null
        val cond = a.getOrNull(0) as? JsonObject ?: return@on null
        val condDsl = actionConditionDsl(cond) ?: return@on null
        val thenActions = (a.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return@on null
        val elseActions = (a.getOrNull(2) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return@on null
        val thenEffect = renderEffectList(thenActions, tvar) ?: return@on null
        val elseEffect = renderEffectList(elseActions, tvar) ?: return@on null
        call(
            "ConditionalEffect",
            arg("condition", Lit(condDsl)),
            arg("effect", thenEffect),
            arg("elseEffect", elseEffect),
        )
    }

    on("Scry") { _, args, _ ->  // "Scry N" -> look-top, reorder/bottom pipeline
        (findInteger(args) as? Int)?.let { call("Patterns.Library.scry", arg("$it")) }
    }

    on("MillNumberCards") { _, args, tvar ->
        // "target player mills N cards" (Altar of Dementia). The count is a fixed Integer or a recognised
        // dynamic amount (e.g. the sacrificed creature's power); the milled player is the bound target — an
        // untargeted/self form falls back to the controller. An unrenderable count scaffolds.
        val amt = amountExpr(args) ?: dynamicAmountExpr(amountNode(args)) ?: return@on null
        val tgt = refTarget(args, tvar) ?: tvar
        if (tgt != null) call("Patterns.Library.mill", arg(amt), arg(Lit(tgt)))
        else call("Patterns.Library.mill", arg(amt))
    }

    // "You may return any number of [filter] to their owner's hand" (Rambling Possum: "any number of
    // creatures that saddled it this turn"). Modeled as the Gather → ChooseAnyNumber → toHand pipeline:
    // gather the matching battlefield permanents into a collection, let the controller select any subset
    // on the battlefield (useTargetingUI), and move them — a battlefield→HAND move routes each permanent
    // to its owner's hand. Emitted as the raw pipeline-step trio with the deterministic `gathered0` /
    // `selected1` slot keys the inline `Effects.Pipeline { }` builder produces, so the serialized tree
    // matches the hand-authored card. The filter must render exactly (gameObjectFilterExpr declines an
    // unrenderable one -> SCAFFOLD) rather than widen the group to every permanent.
    on("ReturnAnyNumberOfPermanentsToTheirOwnersHands") { _, args, _ ->
        val filter = gameObjectFilterDsl(args) ?: return@on null
        // Wrap the three steps in an explicit `Effects.Composite(...)` call (not a `Composite` Dsl node,
        // which `renderEffectList` would splice flat into the surrounding effect list). The inline
        // `Effects.Pipeline { }` builder the hand-authored card uses produces exactly this nested
        // Composite of the same three steps with the `gathered0` / `selected1` slot keys.
        call(
            "Effects.Composite",
            arg(Lit("GatherCardsEffect(CardSource.BattlefieldMatching($filter), storeAs = \"gathered0\")")),
            arg(Lit(
                "SelectFromCollectionEffect(from = \"gathered0\", selection = SelectionMode.ChooseAnyNumber, " +
                    "storeSelected = \"selected1\", useTargetingUI = true)"
            )),
            arg(Lit("MoveCollectionEffect(from = \"selected1\", destination = CardDestination.ToZone(Zone.HAND))")),
        )
    }

    on("ReturnGraveyardCardToHand") { _, args, _ ->
        // "Return this card from your graveyard to your hand" (Gangrenous Goliath's graveyard ability). Only
        // the self (this graveyard card) form renders; a chosen graveyard-card target scaffolds.
        if (jsonContains(args, "_GraveyardCard", "ThisGraveyardCard"))
            call("Effects.Move", arg("EffectTarget.Self"), arg("Zone.HAND")) else null
    }

    on("ExileGraveyardCard") { _, args, tvar ->
        // "Exile target card from a graveyard" (Cremate, Withered Wretch). The target graveyard card was
        // already recovered into the bound `tvar`; exile is a plain Move to the exile zone. Self ("exile
        // this card from your graveyard") falls back to EffectTarget.Self.
        val tgt = refTarget(args, tvar) ?: "EffectTarget.Self"
        call("Effects.Move", arg(Lit(tgt)), arg("Zone.EXILE"))
    }

    on("PutACardFromHandOnBattlefield") { _, args, _ ->  // "you may put a [basic land] card from your hand …"
        val arr = args.asArr ?: return@on null
        val blob = compact(arr.getOrNull(0))
        val filter = when {
            "IsSupertype" in blob && "\"Basic\"" in blob && "\"Land\"" in blob -> "GameObjectFilter.BasicLand"
            "\"Land\"" in blob && "IsSupertype" !in blob && "IsLandType" !in blob -> "GameObjectFilter.Land"
            else -> return@on null
        }
        val entersTapped = "EntersTapped" in compact(arr.getOrNull(1))
        val parts = mutableListOf(arg("filter", filter))
        if (entersTapped) parts.add(arg("entersTapped", "true"))
        Call("Patterns.Hand.putFromHand", parts)
    }

    on("SearchLibrary") { _, args, _ -> renderSearch(args) }
    on("LookAtTheTopNumberCardsOfLibrary", "LookAtTheTopNumberCardsOfPlayersLibrary") { node, args, tvar -> renderLook(node, args, tvar) }

    on("PutGraveyardCardOnBottomOfLibrary") { _, args, tvar ->
        // "Put target card from your graveyard on the bottom of your library" (Tomb Trawler). The
        // target graveyard card is already recovered into the bound `tvar`; a plain Move to the bottom
        // of the library expresses it. Self ("this card from your graveyard") falls back to Self.
        val tgt = refTarget(args, tvar) ?: "EffectTarget.Self"
        call("Effects.Move", arg(Lit(tgt)), arg("Zone.LIBRARY"), arg("ZonePlacement.Bottom"))
    }

    on("PutGraveyardCardOntoBattlefield", "PutGraveyardCardIntoHand",
        "ReturnDeadGraveyardCardToTopOfLibrary", "PutPermanentOnTopOfOwnersLibrary") { node, args, tvar ->
        val a = node.strField("_Action")
        // ReturnDead… ("return this card from the graveyard") often has no ref -> Self
        var tgt = refTarget(args, tvar)
        if (tgt == null) {
            if (a == "ReturnDeadGraveyardCardToTopOfLibrary") tgt = "EffectTarget.Self" else return@on null
        }
        if (a == "PutGraveyardCardOntoBattlefield") {
            // "onto the battlefield under your control" (Ashen Powder) carries an EntersUnderPlayersControl
            // flag — a plain Move would (wrongly) return it under its owner's control. Render the
            // control-grabbing put; under another player's control isn't expressible, so scaffold.
            // An EntersTapped flag ("…to the battlefield tapped", Teacher's Pest) needs the
            // PutOntoBattlefield facade (Move can't carry `tapped`); combining tapped with a control
            // grab isn't expressible by either facade, so scaffold that pairing.
            val flagBlob = compact(args)
            val entersTapped = "EntersTapped" in flagBlob
            return@on when {
                "EntersUnderPlayersControl" !in flagBlob ->
                    if (entersTapped) call("Effects.PutOntoBattlefield", arg(Lit(tgt)), arg("tapped", "true"))
                    else call("Effects.Move", arg(Lit(tgt)), arg("Zone.BATTLEFIELD"))
                entersTapped -> null
                "\"You\"" in flagBlob -> call("Effects.PutOntoBattlefieldUnderYourControl", arg(Lit(tgt)))
                else -> null
            }
        }
        val zone = mapOf(
            "PutGraveyardCardIntoHand" to "HAND",
            "ReturnDeadGraveyardCardToTopOfLibrary" to "LIBRARY", "PutPermanentOnTopOfOwnersLibrary" to "LIBRARY",
        )[a]
        if (a == "PutPermanentOnTopOfOwnersLibrary" || a == "ReturnDeadGraveyardCardToTopOfLibrary") {
            call("Effects.Move", arg(Lit(tgt)), arg("Zone.$zone"), arg("ZonePlacement.Top"))
        } else {
            call("Effects.Move", arg(Lit(tgt)), arg("Zone.$zone"))
        }
    }
}

internal fun EmitCtx.renderSearch(args: JsonElement?): Dsl? {
    val blob = compact(args)
    // A destination CHOICE ("put it into your hand or graveyard") or a destination we don't model
    // (graveyard) can't be rendered as a single fixed SearchDestination — scaffold rather than silently
    // pick one arm (Dina's Guidance).
    if ("ChooseAnAction" in blob || "PutFoundCardsIntoGraveyard" in blob) return null
    // A CONDITIONAL destination ("If a creature died this turn, you may put that card onto the
    // battlefield instead" — Caravan Vigil's morbid rider): an `If`/`MayPutFoundCardsOntoBattlefield`
    // search action. The fixed-destination render would silently pick one arm (and the May-substring
    // even satisfies the BATTLEFIELD check below, upgrading the card to strictly-stronger-than-printed).
    // Decline -> SCAFFOLD rather than drop the condition and the choice.
    if (jsonContains(args, "_SearchLibraryAction", "If") || "MayPutFoundCardsOntoBattlefield" in blob) return null
    // "put it onto the battlefield ATTACHED TO target player" (Bitterheart Witch's Curse tutor): an
    // EntersAttachedTo* enter-flag searchLibrary has no attach parameter for — the Aura would enter
    // unattached and the declared target would be dead weight. Decline -> SCAFFOLD.
    if ("EntersAttachedTo" in blob) return null
    // "with different names" (Three Dreams) is a group constraint searchLibrary can't enforce — it would
    // silently let the search grab duplicates. Decline rather than drop the restriction.
    if ("DifferentNames" in blob) return null
    // "search for UP TO X cards, where X is the number of …" — a dynamic count this fixed-count search
    // can't express, and the X definition's own subtype filter (e.g. "number of Shrines you control")
    // leaks into the search-filter regexes below. Decline rather than emit a wrong filter/count
    // (Kyoshi Island Plaza).
    if ("TheNumberOf" in blob) return null
    // "exile them, then create that many tokens" / "any number of cards" (Myr Incubator) aren't modeled by
    // the fixed SearchDestination + single-filter render: there is no EXILE destination, the any-number
    // count isn't a fixed amount, and the CreateTokens payoff would be dropped. Decline -> SCAFFOLD rather
    // than emit a wrong hand-search that silently loses the token clause.
    if ("ExileFoundCards" in blob || "CreateTokens" in blob || "FindAnyNumberOfCardsOfType" in blob) return null
    // "search for an Equipment card" (Steelshaper's Gift): an artifact-subtype (IsArtifactType) the
    // land/type search filter can't express — it falls through to GameObjectFilter.Any, silently dropping
    // the subtype. "...artifact card with mana value 1 or less" (Trinket Mage): a ManaValueIs cap the
    // search filter drops, widening the tutor to any artifact. Decline (-> SCAFFOLD) for either rather
    // than emit a too-broad search.
    if ("IsArtifactType" in blob || "ManaValueIs" in blob) return null
    val dest = when {
        "PutFoundCardsOntoBattlefield" in blob -> "BATTLEFIELD"
        "PutFoundCardsIntoHand" in blob -> "HAND"
        "PutSetAsideCardsOnTopOfLibrary" in blob || "OnTopOfLibrary" in blob -> "TOP_OF_LIBRARY"
        else -> "HAND"
    }
    // "search for a card named X" (Avarax, Daru Cavalier, …) -> a name filter, which the land/type
    // search filter can't express.
    val named = Regex(""""NamedCard",\s*"args":\s*"([^"]+)"""").find(blob)?.groupValues?.get(1)
    val searchSubtype = Regex(""""IsCreatureType",\s*"args":\s*"(\w+)"""").find(blob)?.groupValues?.get(1)
    val enchSubtype = Regex(""""IsEnchantmentType",\s*"args":\s*"(\w+)"""").find(blob)?.groupValues?.get(1)
    // "an instant or sorcery card" / "an artifact or enchantment card" (the tutors): an Or of card-type
    // predicates. Map the whole union to the matching SDK constant. A union we have no constant for
    // declines to SCAFFOLD rather than silently dropping a type — which picking a single arm would do.
    val unionTypes = orCardTypes(blob)
    val filt = when {
        named != null -> "GameObjectFilter.Any.named(\"$named\")"
        searchSubtype != null -> "GameObjectFilter.Any.withSubtype(\"$searchSubtype\")"  // "an Elf card"
        enchSubtype != null -> "GameObjectFilter.Enchantment.withSubtype(\"$enchSubtype\")"  // "an Aura card"
        unionTypes != null -> cardTypeUnionFilter(unionTypes) ?: return null
        else -> landSearchFilterDsl(args)
    }
    val count = findInteger(args)
    val parts = mutableListOf(arg("filter", filt))
    if (count is Int && count != 1) parts.add(arg("count", "$count"))
    parts.add(arg("destination", "SearchDestination.$dest"))
    if ("EntersTapped" in blob) parts.add(arg("entersTapped", "true"))  // "...onto the battlefield tapped"
    if ("RevealFoundCards" in blob) parts.add(arg("reveal", "true"))
    return Call("Patterns.Library.searchLibrary", parts)
}

/**
 * The card types under an `Or` of `IsCardtype` predicates ("instant or sorcery", "artifact or
 * enchantment"), or null when the search filter is not a card-type union. The `Or` token gates this
 * so an `And` ("artifact creature card") doesn't read as a union.
 */
private fun orCardTypes(blob: String): List<String>? {
    if ("\"Or\"" !in blob) return null
    val types = Regex(""""IsCardtype",\s*"args":\s*"(\w+)"""").findAll(blob).map { it.groupValues[1] }.toList()
    return if (types.size >= 2) types else null
}

/**
 * Map a card-type union to the matching predefined SDK [GameObjectFilter] constant, or null when no
 * constant exists for it — the caller then declines to SCAFFOLD rather than emit a wrong filter.
 */
private fun cardTypeUnionFilter(types: List<String>): String? {
    val set = types.map { it.lowercase() }.toSortedSet()
    val constant = mapOf(
        sortedSetOf("instant", "sorcery") to "InstantOrSorcery",
        sortedSetOf("artifact", "enchantment") to "ArtifactOrEnchantment",
        sortedSetOf("creature", "land") to "CreatureOrLand",
        sortedSetOf("creature", "sorcery") to "CreatureOrSorcery",
        sortedSetOf("creature", "enchantment") to "CreatureOrEnchantment",
        sortedSetOf("creature", "artifact") to "CreatureOrArtifact",
        sortedSetOf("creature", "planeswalker") to "CreatureOrPlaneswalker",
    )[set] ?: return null
    return "GameObjectFilter.$constant"
}

internal fun EmitCtx.renderLook(node: JsonObject, args: JsonElement?, tvar: String?): Dsl? {
    val blob = compact(node)
    // A conditional look ("...if there is an instant and a sorcery card in your graveyard, instead put
    // two of them...") branches the kept count; the flat lookAtTopAndKeep can't express it (and the
    // keep-count regex below would wrongly read the conditional branch's number), so scaffold.
    if ("IfElse" in blob || "_Condition" in blob) return null
    // "look at the top N of TARGET player's library, put one in their graveyard, the rest back" — the
    // distribute pipeline. Gate it on the player-targeted ACTION, not on the oracle mentioning "target"
    // (a card may target a spell/creature elsewhere — Discombobulate's "counter target spell").
    if (node.strField("_Action") == "LookAtTheTopNumberCardsOfPlayersLibrary") {
        val look = findInteger(node) ?: return null
        if (tvar == null) return null
        if ("PutAGenericCardIntoGraveyard" !in blob || "PutTheRemainingCardsOnTopOfLibraryInAnyOrder" !in blob) return null
        // The look-and-distribute pipeline keeps its hand-indented multi-line element strings as Raw —
        // a shape no leaf node models yet — inside the multi-line Composite node.
        return Composite(listOf(
            Lit("GatherCardsEffect(CardSource.TopOfLibrary(DynamicAmount.Fixed($look), Player.TargetOpponent), storeAs = \"looked\")"),
            Raw(
                "SelectFromCollectionEffect(\n" +
                    "                from = \"looked\",\n" +
                    "                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),\n" +
                    "                storeSelected = \"toGraveyard\",\n" +
                    "                storeRemainder = \"toTop\",\n" +
                    "                selectedLabel = \"Put in graveyard\",\n" +
                    "                remainderLabel = \"Put on top\"\n" +
                    "            )",
            ),
            Lit("MoveCollectionEffect(from = \"toGraveyard\", destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.TargetOpponent))"),
            Raw(
                "MoveCollectionEffect(\n" +
                    "                from = \"toTop\",\n" +
                    "                destination = CardDestination.ToZone(Zone.LIBRARY, Player.TargetOpponent, ZonePlacement.Top),\n" +
                    "                order = CardOrder.ControllerChooses\n" +
                    "            )",
            ),
        ))
    }
    // "Look at the top N. You may reveal a <type> card and put it into your hand. Put the rest on the
    // BOTTOM of your library in a random order." (Frontier Seeker). This is the filtered-may-keep / bottom
    // shape: a ChooseUpTo(1) over a card-type filter, kept revealed to hand, remainder bottomed at random.
    // The flat lookAtTopAndKeep pattern can't carry a selection filter, so render the atomic pipeline.
    // Only render when the reveal filter translates faithfully; an untranslatable predicate scaffolds.
    if ("MayRevealACardOfTypeAndPutIntoHand" in blob &&
        "PutTheRemainingCardsOnTheBottomOfLibraryInARandomOrder" in blob
    ) {
        val n = findInteger(node) ?: return null
        val filterNode = mayRevealFilterNode(node) ?: return null
        val pred = cardsPredicateDsl(filterNode) ?: return null
        return Composite(listOf(
            Lit("GatherCardsEffect(CardSource.TopOfLibrary(DynamicAmount.Fixed($n)), storeAs = \"looked\")"),
            Raw(
                "SelectFromCollectionEffect(\n" +
                    "                from = \"looked\",\n" +
                    "                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),\n" +
                    "                filter = GameObjectFilter(cardPredicates = listOf($pred)),\n" +
                    "                storeSelected = \"kept\",\n" +
                    "                storeRemainder = \"rest\",\n" +
                    "                selectedLabel = \"Put in hand\",\n" +
                    "                remainderLabel = \"Put on bottom\"\n" +
                    "            )",
            ),
            Lit("MoveCollectionEffect(from = \"kept\", destination = CardDestination.ToZone(Zone.HAND), revealed = true)"),
            Raw(
                "MoveCollectionEffect(\n" +
                    "                from = \"rest\",\n" +
                    "                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),\n" +
                    "                order = CardOrder.Random\n" +
                    "            )",
            ),
        ))
    }
    // "Look at the top N. You may put a <type> card from among them onto the battlefield tapped. Put the
    // rest on the BOTTOM of your library in a random order." (Freestrider Lookout). Same filtered-may-keep /
    // bottom shape as the reveal-to-hand variant above, but the kept card enters the battlefield tapped
    // instead of going to hand. Only EntersTapped is renderable; any other enter flag scaffolds. Render only
    // when the type filter translates faithfully.
    if ("MayPutACardOfTypeOntoTheBattlefield" in blob &&
        "PutTheRemainingCardsOnTheBottomOfLibraryInARandomOrder" in blob
    ) {
        val n = findInteger(node) ?: return null
        val putAction = node.field("args").asArr?.getOrNull(1).asArr?.firstOrNull {
            it.strField("_LookAtTopOfLibraryAction") == "MayPutACardOfTypeOntoTheBattlefield"
        } as? JsonObject ?: return null
        val putArgs = putAction.field("args").asArr ?: return null
        val flagNames = (putArgs.getOrNull(1) as? JsonArray)
            ?.mapNotNull { (it as? JsonObject)?.strField("_EnterFlag") } ?: emptyList()
        // Only the tapped enter state renders; any other flag (or none we recognise) scaffolds.
        if (flagNames != listOf("EntersTapped")) return null
        val pred = cardsPredicateDsl(putArgs.getOrNull(0)) ?: return null
        return Composite(listOf(
            Lit("GatherCardsEffect(CardSource.TopOfLibrary(DynamicAmount.Fixed($n)), storeAs = \"looked\")"),
            Raw(
                "SelectFromCollectionEffect(\n" +
                    "                from = \"looked\",\n" +
                    "                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),\n" +
                    "                filter = GameObjectFilter(cardPredicates = listOf($pred)),\n" +
                    "                storeSelected = \"kept\",\n" +
                    "                storeRemainder = \"rest\",\n" +
                    "                showAllCards = true,\n" +
                    "                selectedLabel = \"Put onto the battlefield\",\n" +
                    "                remainderLabel = \"Put on bottom\"\n" +
                    "            )",
            ),
            Lit("MoveCollectionEffect(from = \"kept\", destination = CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))"),
            Raw(
                "MoveCollectionEffect(\n" +
                    "                from = \"rest\",\n" +
                    "                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),\n" +
                    "                order = CardOrder.Random\n" +
                    "            )",
            ),
        ))
    }
    // "Look at the top X cards ... Put one of them into your hand and the rest on the bottom of your
    // library in a random order." (Pillage the Bog). One generic card kept to hand, the remainder
    // bottomed at random — and the look count may be dynamic ("twice the number of lands you control").
    // The flat keepCount=1 lookAtTopAndKeep with hand/bottom/random destinations renders this exactly.
    if ("PutAGenericCardIntoHand" in blob &&
        "PutTheRemainingCardsOnTheBottomOfLibraryInARandomOrder" in blob
    ) {
        val count = findInteger(node)?.toString()?.let { "DynamicAmount.Fixed($it)" }
            ?: dynamicAmount(amountNode(node)) ?: return null
        return call(
            "Patterns.Library.lookAtTopAndKeep",
            arg("count", count),
            arg("keepCount", "DynamicAmount.Fixed(1)"),
            arg("keepDestination", "CardDestination.ToZone(Zone.HAND)"),
            arg("restDestination", "CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom)"),
            arg("restOrder", "CardOrder.Random"),
        )
    }
    // "Look at the top N. You may exile a nonland card from among them. If you do, it becomes plotted.
    // Put the rest into your hand." (Make Your Own Luck). The IR is a `MayExileACardOfType` (filter) +
    // `CreateExiledCardEffect[IsPlotted]` + `PutRemainingCardsInHand` sub-action triple. Render the
    // atomic gather -> choose-up-to-split(filter) -> exile -> MakePlotted -> rest-to-hand pipeline.
    // Only the nonland filter and the IsPlotted exiled-card effect are renderable here; any other
    // filter or exiled-card effect declines (-> SCAFFOLD) rather than drop a constraint. Plotting
    // a card with no plot cost has no cast-time-X / additional-cost hazard, so a complete render is safe.
    if ("MayExileACardOfType" in blob && "IsPlotted" in blob && "PutRemainingCardsInHand" in blob) {
        val n = findInteger(node) ?: return null
        val subActions = node.field("args").asArr?.getOrNull(1).asArr ?: return null
        // Decline if the exiled-card effect is anything other than the lone IsPlotted designation.
        val exiledEffects = subActions
            .firstOrNull { it.strField("_LookAtTopOfLibraryAction") == "CreateExiledCardEffect" }
            ?.field("args").asArr?.getOrNull(1).asArr
            ?.mapNotNull { (it as? JsonObject)?.strField("_ExiledCardEffect") } ?: return null
        if (exiledEffects != listOf("IsPlotted")) return null
        // Render the exile filter. Only "nonland" (IsNonCardtype Land) is supported here.
        val exileFilterNode = subActions
            .firstOrNull { it.strField("_LookAtTopOfLibraryAction") == "MayExileACardOfType" }
            ?.field("args") as? JsonObject ?: return null
        val filterExpr = when {
            exileFilterNode.strField("_Cards") == "IsNonCardtype" &&
                exileFilterNode.field("args").asStr() == "Land" -> "GameObjectFilter.Nonland"
            else -> return null
        }
        return Composite(listOf(
            Lit("GatherCardsEffect(CardSource.TopOfLibrary(DynamicAmount.Fixed($n)), storeAs = \"looked\")"),
            Raw(
                "SelectFromCollectionEffect(\n" +
                    "                from = \"looked\",\n" +
                    "                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),\n" +
                    "                filter = $filterExpr,\n" +
                    "                storeSelected = \"toPlot\",\n" +
                    "                storeRemainder = \"toHand\",\n" +
                    "                selectedLabel = \"Exile and plot\",\n" +
                    "                remainderLabel = \"Put into hand\"\n" +
                    "            )",
            ),
            Lit("MoveCollectionEffect(from = \"toPlot\", destination = CardDestination.ToZone(Zone.EXILE))"),
            Lit("MakePlottedEffect(from = \"toPlot\")"),
            Lit("MoveCollectionEffect(from = \"toHand\", destination = CardDestination.ToZone(Zone.HAND))"),
        ))
    }
    // "Look at the top N, put some into your hand" — only a fixed look/keep count renders this way.
    val look = findInteger(node)
    var keep: Int? = null
    for (m in Regex(""""PutNumber\w*IntoHand".*?"args":\s*(\d+)""").findAll(blob)) keep = m.groupValues[1].toInt()
    if (keep != null && look != null) return call("Patterns.Library.lookAtTopAndKeep", arg("count", "$look"), arg("keepCount", "$keep"))
    // "Look at the top N, then put them back in any order." N may be fixed (Discombobulate: 4) or a
    // dynamic count (Information Dealer: number of Wizards you control).
    if ("PutTheRemainingCardsOnTopOfLibraryInAnyOrder" in blob) {
        val count = look?.toString() ?: dynamicAmount(amountNode(node)) ?: return null
        return call("Patterns.Library.lookAtTopAndReorder", arg("count", count))
    }
    return null
}

/**
 * The `_Cards` filter node of the `MayRevealACardOfTypeAndPutIntoHand` sub-action inside a
 * `LookAtTheTopNumberCardsOfLibrary` action, or null. The look action's `args` is
 * `[Integer, [subActions…]]`; we find the reveal sub-action and return its `args` (the filter tree).
 */
private fun mayRevealFilterNode(node: JsonObject): JsonElement? {
    val subActions = node.field("args").asArr?.getOrNull(1).asArr ?: return null
    val reveal = subActions.firstOrNull {
        it.strField("_LookAtTopOfLibraryAction") == "MayRevealACardOfTypeAndPutIntoHand"
    } ?: return null
    return reveal.field("args")
}

/**
 * Translate a mtgish `_Cards` filter tree into a `CardPredicate.*` DSL string, or null when any leaf
 * is a predicate we can't render faithfully (power/mana-value/ability/variable/colour/etc.). Declining
 * the whole filter sends the card to SCAFFOLD rather than silently dropping a constraint.
 *
 * Faithfully handled: card-type (`IsCardtype`), creature subtype (`IsCreatureType`), land/artifact/
 * enchantment subtype, supertype, and `And`/`Or` compositions of those — the shapes that map exactly
 * onto `CardPredicate` constants.
 */
private fun cardsPredicateDsl(node: JsonElement?): String? {
    val obj = node as? JsonObject ?: return null
    return when (obj.strField("_Cards")) {
        "And" -> obj.field("args").asArr?.let { parts ->
            val mapped = parts.map { cardsPredicateDsl(it) ?: return null }
            "CardPredicate.And(listOf(${mapped.joinToString(", ")}))"
        }
        "Or" -> obj.field("args").asArr?.let { parts ->
            val mapped = parts.map { cardsPredicateDsl(it) ?: return null }
            "CardPredicate.Or(listOf(${mapped.joinToString(", ")}))"
        }
        "IsCardtype" -> when (obj.field("args").asStr()) {
            "Creature" -> "CardPredicate.IsCreature"
            "Land" -> "CardPredicate.IsLand"
            "Artifact" -> "CardPredicate.IsArtifact"
            "Enchantment" -> "CardPredicate.IsEnchantment"
            "Instant" -> "CardPredicate.IsInstant"
            "Sorcery" -> "CardPredicate.IsSorcery"
            "Planeswalker" -> "CardPredicate.IsPlaneswalker"
            else -> null
        }
        // Subtype filters all route through HasSubtype(Subtype("X")); use the named SDK constant when
        // one exists ("Plains" -> Subtype.PLAINS) so the output matches hand-authored convention.
        "IsCreatureType", "IsLandType", "IsArtifactType", "IsEnchantmentType" ->
            obj.field("args").asStr()?.let { "CardPredicate.HasSubtype(${subtypeRef(it)})" }
        else -> null
    }
}

/** `Subtype.PLAINS` when a named companion constant exists for the value, else `Subtype("Mount")`. */
private fun subtypeRef(value: String): String =
    Registry.subtypeConstant(value)?.let { "Subtype.$it" } ?: "Subtype(\"$value\")"
