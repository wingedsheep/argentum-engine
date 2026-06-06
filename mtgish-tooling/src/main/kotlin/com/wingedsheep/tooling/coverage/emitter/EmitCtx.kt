package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.asInt
import com.wingedsheep.tooling.coverage.asStr
import com.wingedsheep.tooling.coverage.compact
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.findRef
import com.wingedsheep.tooling.coverage.findRefIn
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.pascalToUpperSnake
import com.wingedsheep.tooling.coverage.strField
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Result of rendering a whole card: the emitted DSL text, whether it renders WHOLE, and the
 * unrecovered-structure reasons (the SCAFFOLD worklist).
 */
data class RenderResult(val text: String, val complete: Boolean, val reasons: Set<String>)

/**
 * Per-render state for the emitter. Created once per card; threaded implicitly as the receiver of
 * every emitter extension function so the "mapping" knowledge can live in small themed files rather
 * than one monolith. [reasons] is the SCAFFOLD worklist (the structures we couldn't render exactly).
 *
 * Imports are NOT tracked here — `Shells.assemble` derives them by scanning the emitted code for SDK
 * symbols, so handlers stay pure `tag → DSL string` mappings.
 *
 * The actual rendering rules are spread across sibling files as `EmitCtx.*` extensions:
 *  - target/filter recovery → `TargetRecovery.kt`
 *  - the per-`_Action` handlers → `ActionHandlers.kt` + the themed `*Handlers.kt`
 *  - spell/trigger/activated structure → `CardStructure.kt`, whole-card shortcuts → `SpellShortcuts.kt`
 *  - static abilities → `StaticAbilities.kt`; shells + whole-card assembly → `Shells.kt`
 */
class EmitCtx(val keywords: Set<String>, val oracleText: String? = null) {
    val reasons: MutableSet<String> = mutableSetOf()
}

internal val SELF_REFS = setOf(
    "ThisPermanent", "Trigger_ThatCreature", "ThatEnteringPermanent", "Trigger_ThatPermanent",
    "ThatCreature", "ThatPermanent", "Trigger_ThatGraveyardCard", "ThatGraveyardCard",
)

// ---------------------------------------------------------------------------
// Dispatch + effect-list assembly.
// ---------------------------------------------------------------------------

/** Render one mtgish action to an Effect DSL string via the [ACTION_HANDLERS] registry. */
internal fun EmitCtx.renderAction(node: JsonObject, tvar: String?): String? {
    val handler = ACTION_HANDLERS[node.strField("_Action")] ?: return null
    return handler(node, node["args"], tvar)
}

/** Render a list of mtgish actions to one Effect (Composite if >1). Null if any can't render. */
internal fun EmitCtx.renderEffectList(actions: List<JsonObject>, tvar: String?): String? {
    echoEffect(actions)?.let { return it }
    becomeCreatureTypeEffect(actions, tvar)?.let { return it }
    chooseCreatureTypeRevealTopEffect(actions)?.let { return it }
    val rendered = mutableListOf<String>()
    for (act in actions) {
        val r = renderAction(act, tvar)
        if (r == null) { reasons.add(act.strField("_Action") ?: act.strField("_Rule") ?: "unknown-action"); return null }
        rendered.add(r)
    }
    if (rendered.isEmpty()) return null
    if (rendered.size == 1) return rendered[0]
    return composite(rendered)
}

/**
 * `Effects.Composite(...)` with one element per line, indented to sit in the `effect = ` (8-space)
 * slot. Each element's first line is placed at 12 spaces; multi-line elements keep their own inner
 * indentation. Callers pass ≥2 elements (a single effect is emitted directly, never wrapped).
 */
internal fun composite(parts: List<String>): String =
    parts.joinToString(",\n", prefix = "Effects.Composite(\n", postfix = "\n        )") { "            $it" }

// ---------------------------------------------------------------------------
// Generic amount / reference / keyword toolkit (shared by every handler).
// ---------------------------------------------------------------------------

/** A DSL amount for a plain int / X, or null (-> SCAFFOLD, never a broken emit). */
internal fun EmitCtx.amount(node: JsonElement?): String? {
    val n = findInteger(node) ?: return null
    if (n == "X") return "DynamicAmount.XValue"
    return n.toString()
}

/** A "draw/discard N cards" count read from the amount's TOP-LEVEL `_GameNumber` only: a fixed Integer
 *  (-> the int), or X (-> [forX], when the caller's effect accepts a DynamicAmount). Any derived game
 *  number (Power for "2ˣ", "number of colours of mana spent", …) returns null (-> SCAFFOLD). Unlike
 *  [amount]/findInteger, it never digs a misleading literal out of a nested expression (e.g. the base
 *  `2` of "2ˣ"), which would emit a confidently-wrong fixed count. */
internal fun strictCardCount(amountNode: JsonElement?, forX: String? = null): String? =
    when ((amountNode as? JsonObject)?.strField("_GameNumber")) {
        "Integer" -> (amountNode as JsonObject)["args"].asInt()?.toString()
        "XValue", "X", "ValueX" -> forX
        else -> null
    }

/** GainLifeForEach args = [perAmount, countExpr] -> a synthetic Multiply(count, per) node. */
internal fun gainForEachAmount(args: JsonElement?): JsonElement? {
    val arr = args.asArr
    if (arr != null && arr.size == 2) {
        return buildJsonObject {
            put("_GameNumber", JsonPrimitive("Multiply"))
            put("args", buildJsonArray { add(arr[0]); add(arr[1]) })
        }
    }
    return args
}

/** A DynamicAmount DSL for a dynamic mtgish _GameNumber, or null if unrecognised. */
internal fun EmitCtx.dynamicAmount(node: JsonElement?): String? {
    if (node !is JsonObject) return null
    val gn = node.strField("_GameNumber")
    when (gn) {
        "Integer" -> return "DynamicAmount.Fixed(${node["args"].asInt()})"
        "XValue", "X", "ValueX" -> return "DynamicAmount.XValue"
        // "that much" in a damage trigger — the amount of damage the trigger fired on (Doubtless One's
        // "gain that much life", Thrashing Mudspawn's "lose that much life").
        "Trigger_AmountOfDamageDealt" -> return "DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT)"
        "PowerOfTheSacrificedCreature" -> return "DynamicAmounts.sacrificedPower()"
        "LifeTotalOfPlayer" -> {
            val player = if (jsonContains(node, "_Player", "Opponent")) "Player.Opponent" else "Player.You"
            return "DynamicAmount.LifeTotal($player)"
        }
        "HalfRoundedUp", "HalfRoundedDown" -> {
            val inner = dynamicAmount(node["args"]) ?: return null
            val roundup = if (gn == "HalfRoundedUp") "true" else "false"
            return "DynamicAmount.Divide($inner, DynamicAmount.Fixed(2), roundUp = $roundup)"
        }
    }
    if (gn == "TheNumberOfCardsOfTypeRevealedFromHandThisWay") {
        val filter = revealedHandFilterDsl(node["args"]) ?: return null
        return "DynamicAmount.Count(Player.TargetOpponent, Zone.HAND, $filter)"
    }
    if (gn == "Multiply" && node["args"].asArr?.size == 2) {
        val arr = node["args"].asArr!!
        val a = arr[0]; val b = arr[1]
        val intA = findInteger(a)
        val mult = if (intA is Int) intA else findInteger(b)
        val cnt = if (findInteger(a) == mult) b else a
        val inner = dynamicAmount(cnt)
        if (inner != null && mult is Int) {
            return if (mult == 1) inner else "DynamicAmount.Multiply($inner, $mult)"
        }
        return null
    }
    // "...ThisWay" game-numbers count objects this resolution touched (e.g. Volcanic Eruption's
    // "number of Mountains put into a graveyard this way"), not a current battlefield aggregate — a
    // resolution-scoped count the AggregateBattlefield heuristic below can't express. Scaffold rather
    // than misrender it as a battlefield tally. (Recognised "...ThisWay" shapes are handled above.)
    if (gn != null && "ThisWay" in gn) return null
    if ((gn != null && "NumberOf" in gn) || gn == "TheNumberOfPermanentsOnTheBattlefield") {
        val oracle = oracleText?.lowercase() ?: ""
        if (" hand" in oracle || " in it" in oracle) return null
        val player = when {
            "attacking you" in oracle -> "Player.Opponent"
            "on the battlefield" in oracle -> "Player.Each"
            "target opponent controls" in oracle || jsonContains(node, "_Player", "Ref_TargetOpponent") -> "Player.TargetOpponent"
            "target player controls" in oracle || jsonContains(node, "_Player", "Ref_TargetPlayer") -> "Player.TargetPlayer"
            jsonContains(node, "_Player", "Opponent") -> "Player.Opponent"
            else -> "Player.You"
        }
        // "for each Goblin/Bird/Elf on the battlefield": a creature subtype, which the land-oriented
        // search filter misses; otherwise fall back to the land/type search filter.
        val subtype = Regex(""""IsCreatureType",\s*"args":\s*"(\w+)"""").find(compact(node))?.groupValues?.get(1)
        val filter = if (subtype != null) "GameObjectFilter.Creature.withSubtype(\"$subtype\")" else landSearchFilterDsl(node)
        return "DynamicAmount.AggregateBattlefield($player, $filter)"
    }
    return null
}

/** Resolve an action's subject ref to an EffectTarget DSL (the bound `tvar`, or EffectTarget.Self). */
internal fun EmitCtx.refTarget(args: JsonElement?, tvar: String?): String? {
    val ref = findRef(args)
    return refTargetFromRef(ref, tvar)
}

/** Resolve the ref under a marked subtree, such as `_DamageRecipient`, to an EffectTarget DSL. */
internal fun EmitCtx.refTargetIn(args: JsonElement?, markerKey: String, tvar: String?): String? {
    return refTargetFromRef(findRefIn(args, markerKey), tvar)
}

private fun EmitCtx.refTargetFromRef(ref: String?, tvar: String?): String? {
    if (ref in setOf("Ref_TargetPermanent", "Ref_TargetPlayer", "Ref_TargetGraveyardCard")) return tvar
    if (ref in SELF_REFS) return "EffectTarget.Self"
    // "that player" in a trigger ("the player ~ dealt combat damage to") -> the triggering player.
    if (ref == "Trigger_ThatPlayer") return "EffectTarget.PlayerRef(Player.TriggeringPlayer)"
    return tvar
}

internal fun EmitCtx.keywordOf(node: JsonElement?): String? {
    for (m in Regex("\"(\\w+)\"").findAll(compact(node))) {
        val kw = pascalToUpperSnake(m.groupValues[1])
        if (kw in keywords) return kw
    }
    return null
}

/** The nested _Action node inside an envelope action (PlayerAction / MayAction). */
internal fun innerAction(node: JsonObject): JsonObject? {
    val args = node["args"]
    if (args is JsonArray) {
        for (it in args) if (it is JsonObject && it.containsKey("_Action")) return it
    }
    if (args is JsonObject && args.containsKey("_Action")) return args
    return null
}

// ---------------------------------------------------------------------------
// Cost recovery (PayCost) — shared by the echo/unless gate.
// ---------------------------------------------------------------------------
internal fun EmitCtx.paycostDsl(costNode: JsonElement?): String? {
    val blob = compact(costNode)
    val kind = (costNode as? JsonObject)?.strField("_Cost")
    if (kind == "SacrificeAPermanent" || kind == "SacrificeNumberPermanents") {
        val filt = landSearchFilterDsl(costNode)  // IsLandType -> Land.withSubtype, etc.
        val args = mutableListOf(filt)
        val count = if (kind == "SacrificeNumberPermanents") findInteger(costNode) else 1
        if (count is Int && count != 1) args.add("count = $count")
        return "Costs.pay.Sacrifice(${args.joinToString(", ")})"
    }
    if ("DiscardACardAtRandom" in blob) return "Costs.pay.Discard(random = true)"
    if ("Discard" in blob) {
        val oracle = oracleText?.lowercase() ?: ""
        val filter = when {
            "discard a creature card" in oracle || "\"Creature\"" in blob -> "GameObjectFilter.Creature"
            "discard a land card" in oracle || "\"Land\"" in blob -> "GameObjectFilter.Land"
            else -> null
        }
        return if (filter == null) "Costs.pay.Discard()" else "Costs.pay.Discard(filter = $filter)"
    }
    if ("Mana" in blob) return "Costs.pay.OwnManaCost"
    return null
}

/**
 * [ChooseACreatureType, CreatePermanentLayerEffectUntil{AddCreatureTypeVariable}] -> a single
 * `BecomeCreatureTypeEffect` ("becomes the creature type of your choice until end of turn"). Only the
 * bare type-change (no riding +P/T or keyword grant) and only end-of-turn collapse to this effect.
 * `ChooseACreatureTypeOtherThan X` carries an excluded type (Imagecrafter / Mistform Mutant's "other
 * than Wall") -> `excludedTypes = listOf("X")`.
 */
internal fun EmitCtx.becomeCreatureTypeEffect(actions: List<JsonObject>, tvar: String?): String? {
    val chooser = actions.firstOrNull {
        it.strField("_Action") in setOf("ChooseACreatureType", "ChooseACreatureTypeOtherThan")
    } ?: return null
    val layer = actions.firstOrNull {
        it.strField("_Action") in setOf("CreatePermanentLayerEffectUntil", "CreateEachPermanentLayerEffectUntil")
    } ?: return null
    if (!jsonContains(layer, "_LayerEffect", "AddCreatureTypeVariable")) return null
    if (jsonContains(layer, "_LayerEffect", "AdjustPT") || jsonContains(layer, "_LayerEffect", "AddAbility")) return null
    if (!jsonContains(layer, "_Expiration", "UntilEndOfTurn")) return null  // non-EOT -> SCAFFOLD
    val target = refTarget(layer["args"], tvar) ?: return null
    val excluded = if (chooser.strField("_Action") == "ChooseACreatureTypeOtherThan") chooser["args"].asStr() else null
    return if (excluded != null) "BecomeCreatureTypeEffect(target = $target, excludedTypes = listOf(\"${ktStr(excluded)}\"))"
    else "BecomeCreatureTypeEffect(target = $target)"
}

/**
 * [ChooseACreatureType, RevealTopCardOfLibrary, IfElse(revealed creature is of the chosen type ->
 * hand, else -> graveyard)] -> the single `Patterns.CreatureType.chooseCreatureTypeRevealTop()` pattern
 * (Bloodline Shaman). The whole three-action chain collapses to the named SDK pattern, so it renders
 * as one effect rather than three; any deviation from this exact shape declines (null -> SCAFFOLD).
 */
internal fun EmitCtx.chooseCreatureTypeRevealTopEffect(actions: List<JsonObject>): String? {
    if (actions.size != 3) return null
    if (actions[0].strField("_Action") != "ChooseACreatureType") return null
    if (actions[1].strField("_Action") != "RevealTopCardOfLibrary") return null
    if (actions[2].strField("_Action") != "IfElse") return null
    val blob = compact(actions[2])
    if ("ACardWasRevealedThisWay" !in blob || "IsCreatureTypeVariable" !in blob ||
        "TheChosenCreatureType" !in blob) return null
    if ("PutTopOfLibraryInHand" !in blob || "PutTopOfLibraryInGraveyard" !in blob) return null
    return "Patterns.CreatureType.chooseCreatureTypeRevealTop()"
}

/** [MayCost(cost), Unless(CostWasPaid, [Sacrifice...])] -> PayOrSufferEffect (echo / upkeep cost). */
internal fun EmitCtx.echoEffect(actions: List<JsonObject>): String? {
    if (actions.size != 2) return null
    val (a0, a1) = actions
    if (a0.strField("_Action") != "MayCost" || a1.strField("_Action") != "Unless") return null
    if (!jsonContains(a1, "_Condition", "CostWasPaid") || !jsonContains(a1, "_Action", "SacrificePermanent")) return null
    val cost = paycostDsl(a0["args"]) ?: return null
    return "PayOrSufferEffect(cost = $cost, suffer = SacrificeSelfEffect)"
}
