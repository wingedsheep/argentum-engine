package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.Arg
import com.wingedsheep.tooling.coverage.Call
import com.wingedsheep.tooling.coverage.Composite
import com.wingedsheep.tooling.coverage.Dsl
import com.wingedsheep.tooling.coverage.Lit
import com.wingedsheep.tooling.coverage.arg
import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.asInt
import com.wingedsheep.tooling.coverage.asStr
import com.wingedsheep.tooling.coverage.call
import com.wingedsheep.tooling.coverage.compact
import com.wingedsheep.tooling.coverage.dot
import com.wingedsheep.tooling.coverage.field
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.firstArgWordTagged
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.pascalToUpperSnake
import com.wingedsheep.tooling.coverage.strField
import com.wingedsheep.tooling.coverage.subtypes
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** Tap/untap, continuous P/T & keyword grants (CreatePermanentLayerEffectUntil), and turn-state
 *  effects (skip untap / skip combat / extra lands). */
internal val tapLayerStateHandlers: Map<String, ActionHandler> = actionHandlers {

    on("TapPermanent", "UntapPermanent") { node, args, tvar ->
        val tgt = refTarget(args, tvar) ?: return@on null
        call("Effects.${if (node.strField("_Action") == "TapPermanent") "Tap" else "Untap"}", arg(Lit(tgt)))
    }
    on("GoadCreature") { _, args, tvar ->  // CR 701.15: goad target creature
        val tgt = refTarget(args, tvar) ?: return@on null
        call("Effects.Goad", arg(Lit(tgt)))
    }
    on("RemoveCreatureFromCombat") { _, args, tvar ->  // "remove it from combat" (Gustcloak cycle)
        val tgt = refTarget(args, tvar) ?: return@on null
        call("Effects.RemoveFromCombat", arg(Lit(tgt)))
    }
    on("RemoveCreatureFromCombatAndUnblockBlockers") { _, args, tvar ->
        // Ydwen Efreet: "remove this creature from combat … creatures it was blocking that had become
        // blocked by only this creature become unblocked" — the pre-modern override of CR 509.1h,
        // carried by RemoveFromCombat's unblockSoleBlockedAttackers flag.
        val tgt = refTarget(args, tvar) ?: return@on null
        call("Effects.RemoveFromCombat", arg(Lit(tgt)), arg("unblockSoleBlockedAttackers", "true"))
    }
    on("If") { node, args, tvar ->
        // Resolution-time intervening-if inside a spell/ability ActionList:
        //   If[<condition>, [<then actions>]]  ->  ConditionalEffect(condition = …, effect = …)
        // (Foolish Fate's "if you gained life this turn, …", Burrog Barrage's "+1/+0 if you've cast
        // another instant or sorcery this turn"). An `If` with an else-branch (args[2]) declines —
        // a resolution ConditionalEffect can carry an else, but no calibrated card needs it yet and a
        // wrong else is worse than a scaffold. The condition must render exactly via actionConditionDsl
        // and the then-branch via the normal effect-list path; either declining scaffolds the card.
        val arr = args.asArr ?: return@on null
        val cond = arr.getOrNull(0) as? JsonObject ?: return@on null
        if (arr.getOrNull(2) != null) return@on null
        val thenActions = (arr.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>()
            ?: return@on null
        if (thenActions.isEmpty()) return@on null
        val condDsl = actionConditionDsl(cond) ?: return@on null
        val thenEffect = renderEffectList(thenActions, tvar) ?: return@on null
        call(
            "ConditionalEffect",
            arg("condition", Lit(condDsl)),
            arg("effect", thenEffect),
        )
    }
    on("RegeneratePermanent") { _, args, _ ->
        // Self-regeneration ("{cost}: Regenerate this") renders faithfully. A chosen target's
        // requirement isn't always recovered exactly (e.g. "Regenerate target Zombie" flattens the
        // subtype to "permanent"), so scaffold the targeted case rather than emit a too-broad target.
        if (!jsonContains(args, "_Permanent", "ThisPermanent")) return@on null
        call("RegenerateEffect", arg("EffectTarget.Self"))
    }
    on("TapEachPermanent", "UntapEachPermanent") { node, args, _ ->
        val verb = if (node.strField("_Action") == "TapEachPermanent") "Tap" else "Untap"
        if (jsonContains(node, "_Permanents", "Ref_TargetPermanents")) {  // Tidal Surge: each chosen target
            return@on call("Effects.${verb}EachTarget")
        }
        val filter = groupFilterExpr(args) ?: return@on null  // mass: tap/untap a group
        call("Effects.ForEachInGroup", arg(filter), arg(call("Effects.$verb", arg("EffectTarget.Self"))))
    }

    on("PutACounterOfTypeOnPermanent") { _, args, tvar ->
        // "Put a +1/+1 (or -1/-1) counter on <permanent>." or a named keyword counter ("a flying
        // counter"). Only the bare ±1/±1 PTCounter and the keyword counters we name render; any other
        // counter kind scaffolds rather than guess. The subject ref is self or the bound target.
        val arr = args.asArr ?: return@on null
        val counter = counterTypeDsl(arr.getOrNull(0)) ?: return@on null
        val tgt = refTarget(arr.getOrNull(1), tvar) ?: return@on null
        call("AddCountersEffect", arg("counterType", counter), arg("count", "1"), arg("target", tgt))
    }

    on("PutACounterOfTypeOnEachPermanent") { _, args, _ ->
        // "Put a +1/+1 counter on each <filter>." — the mass form of PutACounterOfTypeOnPermanent
        // (Bounding Felidar's "each other creature you control"). IR args are [<CounterType>, <filter>].
        // Render as ForEachInGroup(AddCountersEffect(..., Self)) over the recovered group filter; the
        // same named-counter restriction applies, and an unrenderable filter declines (-> SCAFFOLD).
        val arr = args.asArr ?: return@on null
        val counter = counterTypeDsl(arr.getOrNull(0)) ?: return@on null
        val filter = groupFilterExpr(arr.getOrNull(1)) ?: return@on null
        call(
            "Effects.ForEachInGroup", arg(filter),
            arg(call("AddCountersEffect", arg(Lit(counter)), arg("1"), arg("EffectTarget.Self"))),
        )
    }

    on("DoubleCountersOfTypeOnPermanent") { _, args, tvar ->
        // "Double the number of +1/+1 counters on <permanent>." (Ornery Tumblewagg's saddled attack.)
        // IR args are [<CounterType>, <permanent ref>]. Reads the current count and adds that many more
        // via Effects.DoubleCounters. Only the named ±1/±1 / keyword counters render (same restriction as
        // the put-a-counter handlers); the subject is self or the bound target.
        val arr = args.asArr ?: return@on null
        val counter = counterTypeDsl(arr.getOrNull(0)) ?: return@on null
        val tgt = refTarget(arr.getOrNull(1), tvar) ?: return@on null
        call("Effects.DoubleCounters", arg(Lit(counter)), arg(Lit(tgt)))
    }

    on("PutNumberCountersOfTypeOnPermanent") { _, args, tvar ->
        // "Put N +1/+1 (or -1/-1) counters on <permanent>." — the plural form of
        // PutACounterOfTypeOnPermanent. IR args are [<N>, <counterType>, <permanent ref>]. Only the
        // bare ±1/±1 PTCounter and the keyword counters we name render with a fixed integer N; any
        // other counter kind or a derived count scaffolds rather than guess.
        val arr = args.asArr ?: return@on null
        val count = findInteger(arr.getOrNull(0)) as? Int ?: return@on null
        val counter = counterTypeDsl(arr.getOrNull(1)) ?: return@on null
        val tgt = refTarget(arr.getOrNull(2), tvar) ?: return@on null
        call("AddCountersEffect", arg("counterType", counter), arg("count", "$count"), arg("target", tgt))
    }

    on("PutNumberCountersOfTypeOnEachPermanent") { _, args, tvar ->
        // "Put N <counter> counters on <permanents>." — the mass / dynamic-count form. IR args are
        // [<amount>, <counterType>, <permanents>]. The only recipient we render here is the just-created
        // token (`TheTokensCreatedThisWay` -> the CREATED_TOKENS pipeline slot, Outlaw Stitcher: "put two
        // +1/+1 counters on that token for each spell …"); a real "each <group>" filter would need a
        // ForEach over the group, which this handler deliberately leaves to scaffold. The amount may be
        // dynamic, so render through AddDynamicCounters with the recovered DynamicAmount. Only the named
        // ±1/±1 / keyword counters render; an unrenderable amount or non-token recipient declines.
        val arr = args.asArr ?: return@on null
        val counter = counterTypeDsl(arr.getOrNull(1)) ?: return@on null
        val recipient = arr.getOrNull(2) as? JsonObject ?: return@on null
        if (recipient.strField("_Permanents") != "TheTokensCreatedThisWay") return@on null
        val amount = dynamicAmount(arr.getOrNull(0)) ?: return@on null
        call(
            "Effects.AddDynamicCounters",
            arg("counterType", counter),
            arg("amount", Lit(amount)),
            arg("target", "EffectTarget.PipelineTarget(CREATED_TOKENS, 0)"),
        )
    }

    on("CreateReplaceWouldPutCountersUntil") { node, _, _ ->
        // "Until end of turn, if you would put one or more +1/+1 counters on a creature you control,
        // put that many plus N +1/+1 counters on it instead." (Prairie Dog's {4}{W}.) This installs the
        // duration-/controller-scoped analogue of Hardened Scales' static ModifyCounterPlacement, which
        // is exactly Effects.GrantCounterPlacementModifier(). The facade's defaults (+1/+1 counters, a
        // creature you control, until end of turn) match the common case, so only the modifier amount is
        // threaded. Render ONLY the fully-defaulted shape; any deviation (other counter type, a recipient
        // other than "a creature you control", a non-You placer, a non-EOT expiration, or a replacement
        // amount that isn't "the number of counters plus a fixed N") declines -> SCAFFOLD rather than
        // emit a lossy approximation.
        val modifier = grantCounterPlacementModifierAmount(node) ?: return@on null
        if (modifier == 1) call("Effects.GrantCounterPlacementModifier")
        else call("Effects.GrantCounterPlacementModifier", arg("modifier", "$modifier"))
    }

    on("Earthbend") { _, args, tvar ->
        // Earthbend N (TLA keyword action): "target land becomes a 0/0 creature with haste that's still
        // a land. Put N +1/+1 counters on it. When it dies or is exiled, return it to the battlefield
        // tapped." The whole keyword action lowers to Effects.Earthbend(N, target). The IR args are
        // [<target land ref>, <N>]. Only a fixed integer N renders — an "Earthbend X" carries a
        // cast-time/dynamic X the Int-typed facade can't take, so it scaffolds. Guard the dynamic shape
        // explicitly: "Earthbend X, where X is the number of …" carries a `TheNumberOf…` game number, and
        // findInteger would otherwise latch onto an unrelated integer in the X definition (e.g. the
        // "power 4 or greater" bound) and emit a wrong fixed N (The Boulder, Ready to Rumble).
        if ("TheNumberOf" in compact(args)) return@on null
        val n = findInteger(args) as? Int ?: return@on null
        val tgt = refTarget(args, tvar) ?: return@on null
        call("Effects.Earthbend", arg("$n"), arg(Lit(tgt)))
    }

    on("CreatePermanentLayerEffectUntil", "CreateEachPermanentLayerEffectUntil") { node, _, tvar ->
        // "Enchanted creature and other creatures that share a creature type with it get …" (Onslaught
        // Crowns) — a group keyed to the host permanent, which the generic ForEachInGroup can't express.
        enchantedTypeGroupGrant(node)?.let { return@on it }
        renderLayerEffect(node, node.strField("_Action")!!, tvar)
    }

    on("CreatePermanentLayerEffect") { node, _, tvar ->
        // The durationless (permanent) layer effect — the "becomes a N/N [type] creature with [keywords]
        // in addition to its other types" animation that has no end (CR ruling: "remains a creature
        // indefinitely", Emergent Haunting). The args are `[target, [layerEffects]]` (no expiration). Only
        // the become-creature shape renders, at Duration.Permanent; anything else declines -> SCAFFOLD.
        val target = refTarget(node["args"], tvar) ?: return@on null
        val layerEffects = (node["args"].asArr)?.getOrNull(1) as? JsonArray ?: return@on null
        if (layerEffects.isEmpty()) return@on null
        becomeCreatureLayerEffect(layerEffects, target, duration = "Duration.Permanent", allowExtendedGrants = true)
    }

    on("CreatePermanentRuleEffectUntil") { node, _, tvar ->
        // A permanent rule-effect until end of turn. Several single-rule shapes render to a matching
        // Effects facade; anything else (multi-rule, non-EOT duration, an unmodeled rule) scaffolds:
        //  - CantBlock / CantAttack ("it can't block this turn", Duel Tactics / Ydwen Efreet).
        //  - CantBeBlocked ("can't be blocked this turn", Crafty Pathmage) -> CANT_BE_BLOCKED flag.
        //  - CantBeBlockedExceptByDefenders ("except by creatures with <X>", Resilient Roadrunner).
        val args = node["args"].asArr ?: return@on null
        val tgt = refTarget(args, tvar) ?: return@on null
        if (!jsonContains(node, "_Expiration", "UntilEndOfTurn")) return@on null
        val rules = (args.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return@on null
        if (rules.size != 1) return@on null
        when (rules[0].strField("_PermanentRule")) {
            "CantBlock" -> call("Effects.CantBlock", arg(Lit(tgt)))
            "CantAttack" -> call("Effects.CantAttack", arg(Lit(tgt)))
            "CantBeBlocked" -> call("GrantKeywordEffect", arg("AbilityFlag.CANT_BE_BLOCKED.name"), arg(Lit(tgt)))
            "CantBeBlockedExceptByDefenders" -> {
                val bf = cantBeBlockedExceptByFilter(rules[0]) ?: return@on null
                call("Effects.GrantCantBeBlockedExceptBy", arg(Lit(tgt)), arg(Lit(bf)))
            }
            else -> null
        }
    }

    on("CreatePlayerEffectUntil") { node, _, _ ->  // Summer Bloom: may play N additional lands
        val n = findInteger(node)
        if (jsonContains(node, "_PlayerEffect", "MayPlayAdditionalLands") && n is Int) {
            return@on call("PlayAdditionalLandsEffect", arg("$n"))
        }
        // "Until end of turn / end of your next turn, you may play that card." — the second half of the
        // impulse-draw idiom (Irascible Wolverine, Alania's Pathmaker). The grant reads the card the paired
        // `ExileTopCardOfLibrary` action stashed under "impulseExiled" (matching `Patterns.Exile.impulse`'s
        // default storeAs key). Only the controller-scoped grant on the card-exiled-this-way renders, and
        // only for the two expirations the MayPlayExpiry facade names; any other player scope or expiration
        // declines (-> SCAFFOLD) rather than emit a wrong window.
        if (jsonContains(node, "_PlayerEffect", "MayPlayExiledCard") &&
            jsonContains(node, "_CardInExile", "TheCardExiledThisWay") &&
            jsonContains(node, "_Player", "You")
        ) {
            val expiry = when (firstExpiration(node)) {
                "UntilEndOfTurn" -> "MayPlayExpiry.EndOfTurn"
                "UntilEndOfNextTurn" -> "MayPlayExpiry.UntilEndOfNextTurn"
                else -> return@on null
            }
            return@on call("GrantMayPlayFromExileEffect", arg("\"impulseExiled\""), arg(Lit(expiry)))
        }
        null
    }

    on("EachPermanentDoesntUntapDuringControllersNextUntap") { _, _, tvar ->
        if (tvar != null) call("SkipUntapEffect", arg(Lit(tvar))) else call("SkipUntapEffect")
    }
    on("SkipAllCombatPhasesTheirNextTurn") { _, _, tvar ->
        if (tvar != null) call("SkipCombatPhasesEffect", arg(Lit(tvar))) else call("SkipCombatPhasesEffect")
    }
}

/** A mtgish `_CounterType` node -> the `Counters.*` constant the AddCountersEffect facade takes, or null
 *  for a counter kind we can't name (-> the caller scaffolds rather than guess). Shared by every "put a
 *  counter" handler (single / N / each). Only the bare ±1/±1 PTCounter, modeled keyword counters, and
 *  engine-wired utility counters render. */
internal fun counterTypeDsl(counterNode: JsonElement?): String? {
    val node = counterNode as? JsonObject ?: return null
    return when (node.strField("_CounterType")) {
        "PTCounter" -> {
            val pt = node["args"].asArr ?: return null
            when (Pair(pt.getOrNull(0).asInt(), pt.getOrNull(1).asInt())) {
                Pair(1, 1) -> "Counters.PLUS_ONE_PLUS_ONE"
                Pair(-1, -1) -> "Counters.MINUS_ONE_MINUS_ONE"
                else -> null
            }
        }
        // Keyword counters (CR 122.1c) that grant their keyword via the engine's keyword-counter
        // projection. Only the ones we can name render; anything else scaffolds.
        "FlyingCounter" -> "Counters.FLYING"
        // Deathtouch counter (CR 122.1c): grants the DEATHTOUCH keyword via the StateProjector's
        // keyword-counter projection. Adding one is a plain AddCounters(Counters.DEATHTOUCH, …)
        // (Vraska Joins Up: "put a deathtouch counter on each creature you control").
        "DeathtouchCounter" -> "Counters.DEATHTOUCH"
        // Stun counter (CR 122.1d): a built-in replacement ("if a permanent with a stun counter would
        // become untapped, instead remove a stun counter from it"), engine-wired via `untapOrConsumeStun`.
        // Adding one is a plain AddCounters(Counters.STUN, …) (Rapier Wit, Fractal Mascot).
        "StunCounter" -> "Counters.STUN"
        "FinalityCounter" -> "Counters.FINALITY"
        // Loot counter (OTJ — Bandit's Haul): a passive storage counter with no inherent rule; the
        // card's own abilities accumulate it and spend it. Adding one is a plain AddCounters.
        "LootCounter" -> "Counters.LOOT"
        else -> null
    }
}

/**
 * The fixed extra-counter amount [N] for a `CreateReplaceWouldPutCountersUntil` action that exactly
 * matches `Effects.GrantCounterPlacementModifier()`'s defaults — "until end of turn, if YOU would put
 * one or more +1/+1 counters on a creature YOU control, put that many plus N +1/+1 counters on it
 * instead" — or null for any shape that doesn't (so the card scaffolds rather than render a lossy
 * approximation). The IR args are `[<replacable-event>, [<replacement-action>], <expiration>]`:
 *  - replacable event: `APlayerWouldPutAnyNumberOfCountersOfTypeOnAPermanent(SinglePlayer(You),
 *    PTCounter(1,1), And(IsCardtype Creature, ControlledByAPlayer(SinglePlayer(You))))`.
 *  - replacement: `PutNewAmount(Plus(WouldPutCounters_NumberOfCounters, Integer N))` with `N >= 1`.
 *  - expiration: `UntilEndOfTurn`.
 * Anything else (other counter type, other player scope, a recipient that isn't "a creature you
 * control", a non-EOT expiration, or a replacement that isn't "the placed count plus a fixed N")
 * declines. This mirrors the engine's `GrantCounterPlacementModifierEffect`, whose defaults are the
 * common case; only the `modifier` is parameterised here.
 */
private fun grantCounterPlacementModifierAmount(node: JsonObject): Int? {
    val args = node["args"].asArr ?: return null
    val event = args.getOrNull(0) as? JsonObject ?: return null
    val replacements = (args.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return null
    val expiration = args.getOrNull(2) as? JsonObject ?: return null

    if (event.strField("_ReplacableEventWouldPutCounters") !=
        "APlayerWouldPutAnyNumberOfCountersOfTypeOnAPermanent"
    ) return null
    val eventArgs = event["args"].asArr ?: return null
    // Placer must be exactly "you" (the controller-scoped 'you' the facade assumes).
    val placer = eventArgs.getOrNull(0) as? JsonObject
    if (placer?.strField("_Players") != "SinglePlayer" ||
        placer.field("args").strField("_Player") != "You"
    ) return null
    // Counter must be exactly a +1/+1 counter.
    val counter = eventArgs.getOrNull(1) as? JsonObject
    if (counter?.strField("_CounterType") != "PTCounter") return null
    val pt = counter["args"].asArr
    if (pt?.getOrNull(0).asInt() != 1 || pt?.getOrNull(1).asInt() != 1) return null
    // Recipient must be exactly "a creature you control": And(IsCardtype Creature,
    // ControlledByAPlayer(SinglePlayer(You))).
    val recipient = eventArgs.getOrNull(2) as? JsonObject ?: return null
    if (recipient.strField("_Permanents") != "And") return null
    val recipientArms = recipient["args"].asArr?.filterIsInstance<JsonObject>() ?: return null
    if (recipientArms.size != 2) return null
    val hasCreature = recipientArms.any {
        it.strField("_Permanents") == "IsCardtype" && it.field("args").asStr() == "Creature"
    }
    val controlledByYou = recipientArms.any { arm ->
        arm.strField("_Permanents") == "ControlledByAPlayer" &&
            arm.field("args").strField("_Players") == "SinglePlayer" &&
            arm.field("args").field("args").strField("_Player") == "You"
    }
    if (!hasCreature || !controlledByYou) return null

    // Expiration must be exactly end of turn.
    if (expiration.strField("_Expiration") != "UntilEndOfTurn") return null

    // Replacement must be exactly "PutNewAmount(Plus(<the placed count>, Integer N))" with N >= 1.
    val replacement = replacements.singleOrNull() ?: return null
    if (replacement.strField("_ReplacementActionWouldPutCounters") != "PutNewAmount") return null
    val plus = replacement["args"] as? JsonObject ?: return null
    if (plus.strField("_GameNumber") != "Plus") return null
    val plusArgs = plus["args"].asArr ?: return null
    if (plusArgs.size != 2) return null
    val base = plusArgs.getOrNull(0) as? JsonObject
    if (base?.strField("_GameNumber") != "WouldPutCounters_NumberOfCounters") return null
    val addend = plusArgs.getOrNull(1) as? JsonObject
    if (addend?.strField("_GameNumber") != "Integer") return null
    val n = addend.field("args").asInt() ?: return null
    return if (n >= 1) n else null
}

/** CreatePermanentLayerEffectUntil / its each-permanent form -> ModifyStats / SetBasePowerToughness /
 *  GrantKeyword, optionally over a group (ForEachInGroup). The `args` are always `[target/filter,
 *  [layerEffects], expiration]`; EVERY entry in the layer-effects list must render, or the whole card
 *  scaffolds — a layer effect we silently drop (e.g. an AddAbility granting a triggered ability
 *  alongside a P/T buff, or an AddCardtype riding a SetPT "becomes a 0/0 artifact") would emit a
 *  confidently-wrong card. The `_Expiration` is honoured exactly: end-of-turn uses the default-duration
 *  facade; "for as long as it remains tapped" carries an explicit Duration.WhileSourceTapped(); any
 *  other expiration scaffolds rather than emit a wrong duration. */
internal fun EmitCtx.renderLayerEffect(node: JsonObject, action: String, tvar: String?): Dsl? {
    val mass = action == "CreateEachPermanentLayerEffectUntil"
    val target = if (mass) "EffectTarget.Self" else refTarget(node["args"], tvar)
    if (target == null) return null
    val duration = expirationDsl(node) ?: return null  // unknown expiration -> SCAFFOLD

    // The layer-effects list (mtgish always shapes the action's args as [target/filter, list, expiration]).
    val layerEffects = (node["args"].asArr)?.getOrNull(1) as? JsonArray ?: return null
    if (layerEffects.isEmpty()) return null

    // "becomes a [color] [creature-type] with base power and toughness P/T" (Metamorphic Blast's Spree
    // mode) — a SetPT alongside an optional SetColor and/or SetCreatureType, and nothing else. This is the
    // engine's atomic `Effects.BecomeCreature` shape (one effect spanning the COLOR/TYPE/P_T layers), so a
    // per-layer Composite would diverge from the hand-authored tree. Render BecomeCreature only for the
    // single-target case (BecomeCreature has no group form). Any other layer effect alongside declines.
    if (!mass) becomeCreatureLayerEffect(layerEffects, target)?.let { return it }
    val inner = mutableListOf<Dsl>()
    for (le in layerEffects) {
        val leObj = le as? JsonObject ?: return null
        when (leObj.strField("_LayerEffect")) {
            "AdjustPT" -> {
                val pt = leObj["args"].asArr
                if (pt == null || pt.size != 2) return null
                // ModifyStats' facade carries no duration param, so a non-default duration uses the raw effect.
                inner.add(
                    if (duration.isEmpty()) call("Effects.ModifyStats", arg("${pt[0].asInt()}"), arg("${pt[1].asInt()}"), arg(Lit(target)))
                    else call("ModifyStatsEffect", arg("${pt[0].asInt()}"), arg("${pt[1].asInt()}"), arg(Lit(target)), arg(Lit(duration))),
                )
            }
            "SetPT" -> {
                // "becomes a P/T" — set base power and toughness (CR 613.4b, layer 7b). The IR nests the
                // values one level deeper than AdjustPT: args is `{_PT: PT, args: [p, t]}`. Unlike ModifyStats,
                // SetBasePowerToughnessEffect defaults to Duration.Permanent, so the end-of-turn case MUST emit
                // an explicit Duration.EndOfTurn rather than relying on the default (which would set it forever).
                val pt = (leObj["args"] as? JsonObject)?.get("args").asArr
                if (pt == null || pt.size != 2) return null
                val p = pt[0].asInt() ?: return null
                val t = pt[1].asInt() ?: return null
                val dur = if (duration.isEmpty()) "Duration.EndOfTurn" else duration
                inner.add(call("SetBasePowerToughnessEffect", arg(Lit(target)), arg("$p"), arg("$t"), arg(Lit(dur))))
            }
            "AdjustPTX" -> {
                // "+X/+X" / "-X/-X" where X is a dynamic game number (Wirewood Pride: +Elf-count, Feeding
                // Frenzy: -Zombie-count). The DynamicAmount ModifyStats facade carries no duration, so a
                // non-default duration scaffolds rather than emit a wrong one.
                if (duration.isNotEmpty()) return null
                val a = leObj["args"].asArr
                if (a == null || a.size != 3) return null
                val amt = dynamicAmountExpr(a[2]) ?: return null
                val power = adjustModX(a.getOrNull(0), amt) ?: return null
                val toughness = adjustModX(a.getOrNull(1), amt) ?: return null
                inner.add(call("Effects.ModifyStats", arg(power), arg(toughness), arg(Lit(target))))
            }
            "AdjustPTForEach" -> {
                // "+P/+T for each <count>" (Goblin Piledriver +2/+0 per other attacking Goblin). The base
                // P/T are fixed ints scaled by the dynamic count; only the "other attacking <subtype>" count
                // renders exactly (see adjustForEachCount), anything else scaffolds.
                if (duration.isNotEmpty()) return null
                val a = leObj["args"].asArr
                if (a == null || a.size != 3) return null
                val pBase = a.getOrNull(0).asInt() ?: return null
                val tBase = a.getOrNull(1).asInt() ?: return null
                val count = adjustForEachCount(a.getOrNull(2)) ?: return null
                inner.add(call("Effects.ModifyStats", arg(scaleByBase(count, pBase)), arg(scaleByBase(count, tBase)), arg(Lit(target))))
            }
            "AddAbility" -> {
                // A bare keyword grant renders as Effects.GrantKeyword; a renderable static permanent rule
                // (e.g. "can't be blocked by more than one creature") renders as Effects.GrantStaticAbility.
                // A granted triggered/activated ability or a parameterized keyword carries structure this
                // can't reproduce, so scaffold instead of dropping it.
                val kw = grantedKeyword(leObj)
                if (kw != null) {
                    val gkArgs = mutableListOf(arg("Keyword.$kw"), arg(Lit(target)))
                    if (duration.isNotEmpty()) gkArgs.add(arg(Lit(duration)))
                    inner.add(Call("Effects.GrantKeyword", gkArgs))
                } else {
                    val staticAbility = grantedStaticAbility(leObj) ?: return null
                    val gsArgs = mutableListOf(arg(staticAbility), arg(Lit(target)))
                    if (duration.isNotEmpty()) gsArgs.add(arg(Lit(duration)))
                    inner.add(Call("Effects.GrantStaticAbility", gsArgs))
                }
            }
            else -> return null  // any other layer effect (e.g. add card type, lose abilities) -> SCAFFOLD
        }
    }
    if (inner.isEmpty()) return null
    val effect = if (inner.size == 1) inner[0] else Composite(inner)
    if (mass) {
        val gfArg = (node["args"].asArr)?.getOrNull(0) ?: JsonObject(emptyMap())
        // "they each get +X/+X" — the group is the spell's CHOSEN TARGETS (Ref_TargetPermanents), not a
        // battlefield filter. A static GroupFilter can't express "the creatures this spell targeted"; the
        // generic recovery would widen it to GameObjectFilter.Permanent (every permanent on the
        // battlefield). Decline so it scaffolds rather than buff the whole board (Fancy Footwork).
        if (jsonContains(gfArg, "_Permanents", "Ref_TargetPermanents")) return null
        val filter = groupFilterExpr(gfArg) ?: return null
        return call("Effects.ForEachInGroup", arg(filter), arg(effect))
    }
    return effect
}

/** Colour names mtgish's `SimpleColorList` uses -> the `Color` enum constant. */
private val LAYER_EFFECT_COLOR = mapOf(
    "White" to "WHITE", "Blue" to "BLUE", "Black" to "BLACK", "Red" to "RED", "Green" to "GREEN",
)

/**
 * The "becomes a [color] [creature-type] with base power and toughness P/T [and keywords]" shape — a
 * layer-effect list whose entries are exactly one `SetPT`/`SetCreatureType`/`AddCreatureType`/`SetColor`/
 * `AddCardtype Creature`/`AddAbility <bare keyword>` and nothing else. Renders the engine's atomic
 * `Effects.BecomeCreature(target, power, toughness, creatureTypes, keywords, colors, duration)` (CR 613,
 * layers 4/5/6/7b), matching the hand-authored idiom (Metamorphic Blast's EOT mode; Emergent Haunting's
 * permanent end-step animation). The [duration] DSL defaults to `Duration.EndOfTurn`; the durationless
 * `CreatePermanentLayerEffect` path passes `Duration.Permanent`.
 *
 * Returns null — so the caller falls through to the per-layer renderer or scaffolds — whenever:
 *  - there's no `SetPT` (no "becomes a P/T" base — not a become-creature shape),
 *  - any layer effect other than the recognised set above is present,
 *  - a colour isn't one of the five renderable mono-colours, or a SetColor lists more/zero than one,
 *  - an `AddAbility` grant isn't a single bare keyword, or an `AddCardtype` adds anything but Creature,
 *  - the P/T values aren't plain integers.
 *
 * `AddCardtype Creature` is the "in addition to its other types" marker — `BecomeCreature` already adds
 * the CREATURE type (it never removes the existing Enchantment/Land type since `removeTypes` stays empty),
 * so the marker is consumed and contributes nothing extra.
 */
internal fun EmitCtx.becomeCreatureLayerEffect(
    layerEffects: JsonArray,
    target: String,
    duration: String = "Duration.EndOfTurn",
    allowExtendedGrants: Boolean = false,
): Dsl? {
    // The until-EOT callers (CreatePermanentLayerEffectUntil) keep the original strict shape — exactly
    // SetPT plus an optional SetColor/SetCreatureType — so their golden output is unchanged. The
    // durationless animate path opts into the extended grants (AddCreatureType / AddCardtype Creature /
    // AddAbility <keyword>) used by "becomes a N/N [type] creature with [keyword] in addition to its
    // other types" (Emergent Haunting).
    val baseKinds = setOf("SetPT", "SetColor", "SetCreatureType")
    val extendedKinds = setOf("AddCreatureType", "AddCardtype", "AddAbility")
    val recognised = if (allowExtendedGrants) baseKinds + extendedKinds else baseKinds
    val typeOrColourKinds = if (allowExtendedGrants) setOf("SetColor", "SetCreatureType", "AddCreatureType", "AddCardtype", "AddAbility")
                            else setOf("SetColor", "SetCreatureType")
    val effects = layerEffects.filterIsInstance<JsonObject>()
    if (effects.size != layerEffects.size) return null
    if (effects.none { it.strField("_LayerEffect") == "SetPT" }) return null
    if (effects.any { it.strField("_LayerEffect") !in recognised }) return null
    // A BARE SetPT ("becomes a 5/1 until end of turn", no type/colour/keyword change) is a base-P/T set,
    // not a "becomes a creature" — keep it as SetBasePowerToughnessEffect (the per-layer renderer). Only
    // the genuine "becomes a [colour] [type] creature" shape (a type/colour/keyword grant present) maps
    // to BecomeCreature.
    if (effects.none { it.strField("_LayerEffect") in typeOrColourKinds }) return null

    var power: Int? = null
    var toughness: Int? = null
    val creatureTypes = mutableListOf<String>()
    val colors = mutableListOf<String>()
    val grantedKeywords = mutableListOf<String>()
    for (le in effects) {
        when (le.strField("_LayerEffect")) {
            "SetPT" -> {
                val pt = (le["args"] as? JsonObject)?.get("args").asArr ?: return null
                if (pt.size != 2) return null
                power = pt[0].asInt() ?: return null
                toughness = pt[1].asInt() ?: return null
            }
            "SetCreatureType", "AddCreatureType" -> {
                val sub = le["args"].asStr() ?: return null
                creatureTypes.add(sub)
            }
            "SetColor" -> {
                val names = (le["args"] as? JsonObject)?.takeIf { it.strField("_SettableColor") == "SimpleColorList" }
                    ?.get("args").asArr?.mapNotNull { it.asStr() } ?: return null
                if (names.size != 1) return null  // multicolour / colourless set isn't this shape
                colors.add(LAYER_EFFECT_COLOR[names[0]] ?: return null)
            }
            // "in addition to its other types" — BecomeCreature adds CREATURE itself, so only the Creature
            // card type is the no-op marker; any other added card type isn't a BecomeCreature shape.
            "AddCardtype" -> if (le["args"].asStr() != "Creature") return null
            // "with <keyword>" — only a single bare keyword grant (Flying) renders; anything richer declines.
            "AddAbility" -> grantedKeyword(le)?.let { grantedKeywords.add(it) } ?: return null
        }
    }
    if (power == null || toughness == null) return null

    val parts = mutableListOf(arg("target", Lit(target)), arg("power", "$power"), arg("toughness", "$toughness"))
    if (grantedKeywords.isNotEmpty()) {
        parts.add(arg("keywords", call("setOf", *grantedKeywords.map { arg(Lit("Keyword.$it")) }.toTypedArray())))
    }
    if (creatureTypes.isNotEmpty()) {
        parts.add(arg("creatureTypes", call("setOf", *creatureTypes.map { arg(Lit("\"${ktStr(it)}\"")) }.toTypedArray())))
    }
    if (colors.isNotEmpty()) {
        parts.add(arg("colors", call("setOf", *colors.map { arg(Lit("Color.$it.name")) }.toTypedArray())))
    }
    parts.add(arg("duration", Lit(duration)))
    return Call("Effects.BecomeCreature", parts)
}

/** A ±X modifier (`PlusX` / `MinusX` / `Zero`) applied to a dynamic amount: PlusX keeps it; MinusX
 *  negates it via `Multiply(amt, -1)` (the golden's negated-count idiom — Feeding Frenzy's "-X/-X");
 *  Zero is the constant `Fixed(0)` arm of an asymmetric "+X/+0" (Armored Armadillo). */
internal fun EmitCtx.adjustModX(modNode: JsonElement?, amt: Dsl): Dsl? =
    when ((modNode as? JsonObject)?.strField("_ModX")) {
        "PlusX" -> amt
        "MinusX" -> call("DynamicAmount.Multiply", arg(amt), arg("-1"))
        "Zero" -> call("DynamicAmount.Fixed", arg("0"))
        else -> null
    }

/** A fixed per-each base scaled by a dynamic [count]: 0 -> `Fixed(0)`, 1 -> the bare count, else
 *  `Multiply(count, base)` (Goblin Piledriver's +2/+0 -> power = Multiply(count, 2), toughness = Fixed(0)). */
private fun EmitCtx.scaleByBase(count: Dsl, base: Int): Dsl = when (base) {
    0 -> call("DynamicAmount.Fixed", arg("0"))
    1 -> count
    else -> call("DynamicAmount.Multiply", arg(count), arg("$base"))
}

/** The dynamic count of an `AdjustPTForEach` game number — only the "other attacking <subtype>" shape
 *  renders exactly: `AggregateBattlefield(You, Creature.withSubtype(X).attacking())`, minus one when the
 *  filter carries an `Other(self)` clause ("each OTHER attacking …"). Anything else returns null (-> SCAFFOLD). */
private fun EmitCtx.adjustForEachCount(gn: JsonElement?): Dsl? {
    val gnObj = gn as? JsonObject ?: return null
    if (gnObj.strField("_GameNumber") != "TheNumberOfPermanentsOnTheBattlefield") return null
    val subtype = gnObj.firstArgWordTagged("IsCreatureType") ?: return null
    val blob = compact(gnObj)
    if ("IsAttacking" !in blob) return null
    val filter = Lit("GameObjectFilter.Creature").dot("withSubtype", arg("\"$subtype\"")).dot("attacking")
    var count: Dsl = call("DynamicAmount.AggregateBattlefield", arg("Player.You"), arg(filter))
    if ("\"Other\"" in blob) count = call("DynamicAmount.Subtract", arg(count), arg(call("DynamicAmount.Fixed", arg("1"))))
    return count
}

/**
 * The Onslaught Crowns' activated effect: "Enchanted creature and other creatures that share a creature
 * type with it get +P/+T / gain <keyword> / gain protection from <colors> until end of turn." mtgish
 * shapes this as a `CreateEachPermanentLayerEffectUntil` over the group `Or[HostPermanent,
 * And[Creature, SharesACreatureTypeWithPermanent(HostPermanent)]]` — a host-keyed group the generic
 * ForEachInGroup can't express. Recognise exactly that shape (+ end-of-turn) and render the dedicated
 * `GrantToEnchantedCreatureTypeGroupEffect`; anything else returns null so the generic path / scaffold runs.
 */
internal fun EmitCtx.enchantedTypeGroupGrant(node: JsonObject): Dsl? {
    val args = node["args"].asArr ?: return null
    val blob = compact(args.getOrNull(0))
    if ("SharesACreatureTypeWithPermanent" !in blob || "HostPermanent" !in blob) return null
    if (firstExpiration(node) !in setOf(null, "UntilEndOfTurn")) return null  // non-EOT -> decline
    val layerEffects = (args.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return null
    if (layerEffects.isEmpty()) return null
    val params = mutableListOf<Arg>()
    for (le in layerEffects) {
        when (le.strField("_LayerEffect")) {
            "AdjustPT" -> {
                val pt = le["args"].asArr ?: return null
                if (pt.size != 2) return null
                params.add(arg("powerModifier", "${pt[0].asInt()}"))
                params.add(arg("toughnessModifier", "${pt[1].asInt()}"))
            }
            "AddAbility" -> {
                val granted = (le["args"].asArr)?.getOrNull(0) as? JsonObject
                if (granted != null && jsonContains(granted, "_Protectable", "FromColor")) {
                    val colors = protectionGrantColors(granted) ?: return null
                    params.add(arg("protectionColors", "setOf(${colors.joinToString(", ") { "Color.$it" }})"))
                } else {
                    val kw = grantedKeyword(le) ?: return null
                    params.add(arg("keyword", "Keyword.$kw"))
                }
            }
            else -> return null
        }
    }
    if (params.isEmpty()) return null
    return Call("GrantToEnchantedCreatureTypeGroupEffect", params)
}

/** The granted [Keyword] for an `AddAbility` layer effect, or null (-> SCAFFOLD) when the grant is not
 *  a single bare keyword — a granted triggered/activated ability or a parameterized keyword carries
 *  structure this faithful-keyword grant can't reproduce. */
private fun EmitCtx.grantedKeyword(addAbility: JsonObject): String? {
    val rule = (addAbility["args"].asArr)?.getOrNull(0) as? JsonObject ?: return null
    val rname = rule.strField("_Rule") ?: return null
    if (rname == "Landwalk") {  // AddAbility{Landwalk{Forest}} -> FORESTWALK
        val sub = subtypes(rule).firstOrNull()?.let { it.uppercase() + "WALK" }
        return if (sub != null && sub in keywords) sub else null
    }
    // A bare keyword rule carries no further structure. Anything with args (TriggerA, a parameterized
    // keyword, ...) is not a faithful bare-keyword grant.
    if (rule["args"] != null) return null
    val kw = pascalToUpperSnake(rname)
    return if (kw in keywords) kw else null
}

/**
 * The granted [com.wingedsheep.sdk.scripting.StaticAbility] DSL for an `AddAbility` layer effect that
 * wraps a `PermanentRuleEffect` (e.g. "gains 'This creature can't be blocked by more than one
 * creature'"), or null (-> SCAFFOLD) when the rule isn't a single self-scoped permanent rule the
 * static-ability renderer can reproduce. Renders the same `StaticAbility` constant
 * [staticAbilityExpr] already emits for printed statics, wrapped in `Effects.GrantStaticAbility(...)`
 * by the caller. Only a lone self-rule renders; a rule with its own filter/args, or more than one
 * rule in the `PermanentRuleEffect`, declines so the card scaffolds rather than dropping structure.
 */
private fun EmitCtx.grantedStaticAbility(addAbility: JsonObject): Dsl? {
    val grant = (addAbility["args"].asArr)?.getOrNull(0) as? JsonObject ?: return null
    if (grant.strField("_Rule") != "PermanentRuleEffect") return null
    // PermanentRuleEffect args = [subject-permanent, [rule, ...]]. Render only the self ("ThisPermanent")
    // single-rule case; anything filtered or compound carries structure GrantStaticAbility can't scope.
    val ruleArgs = grant["args"].asArr ?: return null
    if (!jsonContains(ruleArgs.getOrNull(0), "_Permanent", "ThisPermanent")) return null
    val rules = ruleArgs.getOrNull(1).asArr ?: return null
    val ruleNode = rules.singleOrNull() as? JsonObject ?: return null
    val ruleName = ruleNode.strField("_PermanentRule") ?: return null
    return staticAbilityExpr(ruleName, ruleNode)
}

/** The layer effect's `_Expiration` -> "" for the default (end-of-turn) facade, an explicit
 *  `Duration.*` DSL for a recognised non-default duration, or null (-> SCAFFOLD) for one we can't
 *  render exactly (so the emitter never silently substitutes the wrong duration). */
private fun expirationDsl(node: JsonObject): String? =
    when (firstExpiration(node)) {
        null, "UntilEndOfTurn" -> ""
        "ForAsLongAsPermanentRemainsTapped" -> "Duration.WhileSourceTapped()"
        else -> null
    }

/** The first `_Expiration` discriminator value anywhere in the subtree. */
private fun firstExpiration(node: JsonElement?): String? {
    when (node) {
        is JsonObject -> {
            node.strField("_Expiration")?.let { return it }
            node.values.forEach { firstExpiration(it)?.let { v -> return v } }
        }
        is JsonArray -> node.forEach { firstExpiration(it)?.let { v -> return v } }
        else -> {}
    }
    return null
}
