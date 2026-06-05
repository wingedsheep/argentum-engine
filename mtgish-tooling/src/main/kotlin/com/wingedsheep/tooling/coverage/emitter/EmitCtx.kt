package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.asInt
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
 * than one monolith. [used] drives the import block; [reasons] is the SCAFFOLD worklist.
 *
 * The actual rendering rules are spread across sibling files as `EmitCtx.*` extensions:
 *  - target/filter recovery → `TargetRecovery.kt`
 *  - the per-`_Action` handlers → `ActionHandlers.kt` + the themed `*Handlers.kt`
 *  - spell/trigger/activated structure → `CardStructure.kt`, whole-card shortcuts → `SpellShortcuts.kt`
 *  - static abilities → `StaticAbilities.kt`; shells + whole-card assembly → `Shells.kt` / `CardRenderer.kt`
 */
class EmitCtx(val keywords: Set<String>, val oracleText: String? = null) {
    /** SerialNames/symbols the card uses → resolved to imports by [com.wingedsheep.tooling.coverage.Registry]. */
    val used: MutableSet<String> = linkedSetOf("card", "Rarity")
    val reasons: MutableSet<String> = mutableSetOf()
}

/** A per-`_Action` rendering rule. Returns the Effect DSL string, or null → SCAFFOLD. */
internal typealias ActionHandler = EmitCtx.(node: JsonObject, args: JsonElement?, tvar: String?) -> String?

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
    val rendered = mutableListOf<String>()
    for (act in actions) {
        val r = renderAction(act, tvar)
        if (r == null) { reasons.add(act.strField("_Action") ?: act.strField("_Rule") ?: "unknown-action"); return null }
        rendered.add(r)
    }
    if (rendered.isEmpty()) return null
    if (rendered.size == 1) return rendered[0]
    used.add("CompositeEffect")
    val inner = rendered.joinToString(",\n            ")
    return "CompositeEffect(\n        listOf(\n            $inner\n        )\n    )"
}

// ---------------------------------------------------------------------------
// Generic amount / reference / keyword toolkit (shared by every handler).
// ---------------------------------------------------------------------------

/** A DSL amount for a plain int / X, or null (-> SCAFFOLD, never a broken emit). */
internal fun EmitCtx.amount(node: JsonElement?): String? {
    val n = findInteger(node) ?: return null
    if (n == "X") { used.add("DynamicAmount"); return "DynamicAmount.XValue" }
    return n.toString()
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
        "Integer" -> { used.add("DynamicAmount"); return "DynamicAmount.Fixed(${node["args"].asInt()})" }
        "XValue", "X", "ValueX" -> { used.add("DynamicAmount"); return "DynamicAmount.XValue" }
        "PowerOfTheSacrificedCreature" -> { used.add("DynamicAmounts"); return "DynamicAmounts.sacrificedPower()" }
        "LifeTotalOfPlayer" -> {
            used.addAll(listOf("DynamicAmount", "Player"))
            val player = if (jsonContains(node, "_Player", "Opponent")) "Player.Opponent" else "Player.You"
            return "DynamicAmount.LifeTotal($player)"
        }
        "HalfRoundedUp", "HalfRoundedDown" -> {
            val inner = dynamicAmount(node["args"]) ?: return null
            used.add("DynamicAmount")
            val roundup = if (gn == "HalfRoundedUp") "true" else "false"
            return "DynamicAmount.Divide($inner, DynamicAmount.Fixed(2), roundUp = $roundup)"
        }
    }
    if (gn == "TheNumberOfCardsOfTypeRevealedFromHandThisWay") {
        val filter = revealedHandFilterDsl(node["args"]) ?: return null
        used.addAll(listOf("DynamicAmount", "Player", "Zone"))
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
            used.add("DynamicAmount")
            return if (mult == 1) inner else "DynamicAmount.Multiply($inner, $mult)"
        }
        return null
    }
    if ((gn != null && "NumberOf" in gn) || gn == "TheNumberOfPermanentsOnTheBattlefield") {
        used.addAll(listOf("DynamicAmount", "Player", "GameObjectFilter"))
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
        return "DynamicAmount.AggregateBattlefield($player, ${landSearchFilterDsl(node)})"
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
    if (ref in SELF_REFS) { used.add("EffectTarget"); return "EffectTarget.Self" }
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
    used.add("PayCost")
    val kind = (costNode as? JsonObject)?.strField("_Cost")
    if (kind == "SacrificeAPermanent" || kind == "SacrificeNumberPermanents") {
        val filt = landSearchFilterDsl(costNode)  // IsLandType -> Land.withSubtype, etc.
        val args = mutableListOf(filt)
        val count = if (kind == "SacrificeNumberPermanents") findInteger(costNode) else 1
        if (count is Int && count != 1) args.add("count = $count")
        return "PayCost.Sacrifice(${args.joinToString(", ")})"
    }
    if ("DiscardACardAtRandom" in blob) return "PayCost.Discard(random = true)"
    if ("Discard" in blob) {
        val oracle = oracleText?.lowercase() ?: ""
        val filter = when {
            "discard a creature card" in oracle || "\"Creature\"" in blob -> {
                used.add("GameObjectFilter"); "GameObjectFilter.Creature"
            }
            "discard a land card" in oracle || "\"Land\"" in blob -> {
                used.add("GameObjectFilter"); "GameObjectFilter.Land"
            }
            else -> null
        }
        return if (filter == null) "PayCost.Discard()" else "PayCost.Discard(filter = $filter)"
    }
    if ("Mana" in blob) return "PayCost.OwnManaCost"
    return null
}

/** [MayCost(cost), Unless(CostWasPaid, [Sacrifice...])] -> PayOrSufferEffect (echo / upkeep cost). */
internal fun EmitCtx.echoEffect(actions: List<JsonObject>): String? {
    if (actions.size != 2) return null
    val (a0, a1) = actions
    if (a0.strField("_Action") != "MayCost" || a1.strField("_Action") != "Unless") return null
    if (!jsonContains(a1, "_Condition", "CostWasPaid") || !jsonContains(a1, "_Action", "SacrificePermanent")) return null
    val cost = paycostDsl(a0["args"]) ?: return null
    used.addAll(listOf("PayOrSufferEffect", "SacrificeSelfEffect"))
    return "PayOrSufferEffect(cost = $cost, suffer = SacrificeSelfEffect)"
}
