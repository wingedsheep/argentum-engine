package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.Call
import com.wingedsheep.tooling.coverage.Composite
import com.wingedsheep.tooling.coverage.Dsl
import com.wingedsheep.tooling.coverage.Lit
import com.wingedsheep.tooling.coverage.arg
import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.amountNode
import com.wingedsheep.tooling.coverage.asInt
import com.wingedsheep.tooling.coverage.asStr
import com.wingedsheep.tooling.coverage.call
import com.wingedsheep.tooling.coverage.compact
import com.wingedsheep.tooling.coverage.field
import com.wingedsheep.tooling.coverage.dot
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.findRef
import com.wingedsheep.tooling.coverage.findRefIn
import com.wingedsheep.tooling.coverage.firstArgWordTagged
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.pascalToUpperSnake
import com.wingedsheep.tooling.coverage.render
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
 *
 * In partial-render mode ([Emitter.renderCard] `partial = true`) the card no longer bails to a bare
 * scaffold on the first un-renderable part: the parts that map are emitted and the rest become located
 * [holes]. [parts] is how many ability-bearing parts were attempted, so [renderableFraction] answers
 * "how much of this card could be implemented?" and [holes] answers "which parts still can't". In the
 * default (non-partial) path [holes] is empty and [parts] is 0 — `complete`/`reasons` are unchanged.
 */
data class RenderResult(
    val text: String,
    val complete: Boolean,
    val reasons: Set<String>,
    val holes: List<String> = emptyList(),
    val parts: Int = 0,
) {
    /** Fraction of attempted parts the emitter rendered (1.0 = whole). Defined as 1.0 when nothing
     *  ability-bearing was attempted (a vanilla creature is fully renderable). */
    val renderableFraction: Double get() = if (parts == 0) 1.0 else (parts - holes.size).toDouble() / parts
}

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

    /**
     * Named bound-target locals for a MULTI-target spell, indexed by the mtgish 1-based target ordinal:
     * `targetVars[0]` is the var for `Ref_TargetPermanent1`, `[1]` for `Ref_TargetPermanent2`, … Empty
     * for the single-target common case (which threads one `tvar` string through the handlers). Set only
     * by the multi-target spell path so [refTargetFromRef] can resolve a suffixed `Ref_TargetPermanentN`
     * to the right local; cleared otherwise.
     */
    var targetVars: List<String> = emptyList()

    /**
     * For multi-target spells whose IR uses unsuffixed refs (`Ref_TargetPlayer`, `Ref_TargetPermanent`),
     * map the ref to a target local only when that ref kind appears exactly once. Ambiguous same-kind
     * multi-targets deliberately decline rather than guessing.
     */
    var targetRefVars: Map<String, String> = emptyMap()
}

internal val SELF_REFS = setOf(
    "ThisPermanent", "Trigger_ThatCreature", "ThatEnteringPermanent", "Trigger_ThatPermanent",
    "ThatCreature", "ThatPermanent", "Trigger_ThatGraveyardCard", "ThatGraveyardCard",
)

// ---------------------------------------------------------------------------
// Dispatch + effect-list assembly.
// ---------------------------------------------------------------------------

/** Render one mtgish action to an Effect [Dsl] node via the [ACTION_HANDLERS] registry. */
internal fun EmitCtx.renderAction(node: JsonObject, tvar: String?): Dsl? =
    ACTION_HANDLERS[node.strField("_Action")]?.invoke(this, node, node["args"], tvar)

/** Render a list of mtgish actions to one Effect ([Composite] if >1). Null if any can't render. */
internal fun EmitCtx.renderEffectList(actions: List<JsonObject>, tvar: String?): Dsl? {
    echoEffect(actions)?.let { return it }
    becomeCreatureTypeEffect(actions, tvar)?.let { return it }
    chooseTypeModifyStatsEffect(actions)?.let { return it }
    chooseCreatureTypeRevealTopEffect(actions)?.let { return it }
    val rendered = mutableListOf<Dsl>()
    for (act in actions) {
        val r = renderAction(act, tvar)
        if (r == null) { reasons.add(act.strField("_Action") ?: act.strField("_Rule") ?: "unknown-action"); return null }
        // Splice a sub-action's own Composite into this sequence rather than nesting it — the engine's
        // `.then()` chain (the hand-authored idiom) is flat, so an action that itself renders a Composite
        // (e.g. a layer effect granting +P/+T and a keyword) would otherwise double-wrap into
        // Composite([Composite([…]), …]) and diverge from golden (High Stride, Shore Up).
        if (r is Composite) rendered.addAll(r.parts) else rendered.add(r)
    }
    if (rendered.isEmpty()) return null
    if (rendered.size == 1) return rendered[0]
    return Composite(rendered)
}

// ---------------------------------------------------------------------------
// Generic amount / reference / keyword toolkit (shared by every handler).
// ---------------------------------------------------------------------------

/** A DSL amount for a plain int / X, or null (-> SCAFFOLD, never a broken emit). */
internal fun EmitCtx.amount(node: JsonElement?): String? = amountExpr(node)?.let(::render)

/** The [Dsl] node behind [amount] — a bare int literal or `DynamicAmount.XValue`. */
internal fun EmitCtx.amountExpr(node: JsonElement?): Dsl? {
    val n = findInteger(node) ?: return null
    if (n == "X") return Lit("DynamicAmount.XValue")
    return Lit(n.toString())
}

/** The amount DSL for a gain/lose-life effect (both take a DynamicAmount). A composite top-level
 *  `_GameNumber` (e.g. "X plus 3") routes through the typed dynamic path so it isn't flattened to a
 *  single arm; a bare int / X renders as a literal; any other recognised dynamic amount falls through. */
internal fun EmitCtx.lifeAmountExpr(args: JsonElement?): Dsl? {
    val node = amountNode(args)
    if ((node as? JsonObject)?.strField("_GameNumber") == "Plus") return dynamicAmountExpr(node)
    return amountExpr(args) ?: dynamicAmountExpr(node)
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
internal fun EmitCtx.dynamicAmount(node: JsonElement?): String? = dynamicAmountExpr(node)?.let(::render)

/** The [Dsl] node behind [dynamicAmount]. Filters it embeds are still carried as [Lit] text (they are
 *  migrated in the filter layer); the structure around them is typed. */
internal fun EmitCtx.dynamicAmountExpr(node: JsonElement?): Dsl? {
    if (node !is JsonObject) return null
    val gn = node.strField("_GameNumber")
    when (gn) {
        "Integer" -> return call("DynamicAmount.Fixed", arg("${node["args"].asInt()}"))
        "XValue", "X", "ValueX" -> return Lit("DynamicAmount.XValue")
        // "that much" in a damage trigger — the amount of damage the trigger fired on (Doubtless One's
        // "gain that much life", Thrashing Mudspawn's "lose that much life").
        "Trigger_AmountOfDamageDealt" ->
            return call("DynamicAmount.ContextProperty", arg("ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT"))
        "PowerOfTheSacrificedCreature" -> return call("DynamicAmounts.sacrificedPower")
        // "the number of [filter] cards in your graveyard" (Rise of the Varmints' Varmint count). The
        // count's args are a `_CardsInGraveyard` filter — typically `And(IsCardtype Creature,
        // InAPlayersGraveyard(You))`. Render as a resolution-time `DynamicAmount.Count` over the You
        // graveyard with the recovered filter. Only the You-scoped graveyard renders; any other player
        // scope (an opponent's graveyard, "each player's") declines -> SCAFFOLD rather than miscount.
        "TheNumberOfGraveyardCards" -> {
            if (!jsonContains(node["args"], "_Player", "You")) return null
            val filter = gameObjectFilterDsl(node["args"]) ?: "GameObjectFilter.Any"
            return call(
                "DynamicAmount.Count",
                arg("Player.You"), arg("Zone.GRAVEYARD"), arg(Lit(filter)),
            )
        }
        // "X is its toughness" / "its power" on THIS permanent (Armored Armadillo's "+X/+0 where X is its
        // toughness"). Only the ThisPermanent subject maps to the source-relative facade; any other
        // permanent subject declines (-> scaffold) rather than misattribute the stat.
        "ToughnessOfPermanent" ->
            return if (jsonContains(node["args"], "_Permanent", "ThisPermanent")) call("DynamicAmounts.sourceToughness") else null
        "PowerOfPermanent" ->
            return if (jsonContains(node["args"], "_Permanent", "ThisPermanent")) call("DynamicAmounts.sourcePower") else null
        "LifeTotalOfPlayer" -> {
            val player = if (jsonContains(node, "_Player", "Opponent")) "Player.Opponent" else "Player.You"
            return call("DynamicAmount.LifeTotal", arg(player))
        }
        // "the number of [type] spells <player> has cast this turn" (Magebane Lizard). args = a spell
        // filter + a player ref. Both must map to an EXACT category/scope we can render — decline
        // (-> SCAFFOLD) on any other spell filter or player scope rather than under-counting.
        "NumSpellsCastByPlayerThisTurn" -> {
            val argv = node["args"].asArr ?: return null
            val spellsNode = argv.firstOrNull { (it as? JsonObject)?.containsKey("_Spells") == true } as? JsonObject
            val playerNode = argv.firstOrNull { (it as? JsonObject)?.containsKey("_Player") == true } as? JsonObject
            val filter = spellsCastThisTurnFilter(spellsNode) ?: return null
            val player = spellsCastThisTurnPlayer(playerNode) ?: return null
            return call("DynamicAmount.SpellsCastThisTurn", arg(player), arg(filter))
        }
        // "twice the number of …" (Pillage the Bog) — a unary doubling wrapper. Render the inner amount
        // multiplied by 2; decline if the inner amount doesn't render exactly rather than dropping the
        // doubling.
        "Twice" -> {
            val inner = dynamicAmountExpr(node["args"]) ?: return null
            return call("DynamicAmount.Multiply", arg(inner), arg("2"))
        }
        "HalfRoundedUp", "HalfRoundedDown" -> {
            val inner = dynamicAmountExpr(node["args"]) ?: return null
            val roundup = if (gn == "HalfRoundedUp") "true" else "false"
            return call(
                "DynamicAmount.Divide",
                arg(inner), arg(call("DynamicAmount.Fixed", arg("2"))), arg("roundUp", roundup),
            )
        }
    }
    if (gn == "TheNumberOfCardsOfTypeRevealedFromHandThisWay") {
        val filter = revealedHandFilterExpr(node["args"]) ?: return null
        return call("DynamicAmount.Count", arg("Player.TargetOpponent"), arg("Zone.HAND"), arg(filter))
    }
    if (gn == "Multiply" && node["args"].asArr?.size == 2) {
        val arr = node["args"].asArr!!
        val a = arr[0]; val b = arr[1]
        val intA = findInteger(a)
        val mult = if (intA is Int) intA else findInteger(b)
        val cnt = if (findInteger(a) == mult) b else a
        val inner = dynamicAmountExpr(cnt)
        if (inner != null && mult is Int) {
            return if (mult == 1) inner else call("DynamicAmount.Multiply", arg(inner), arg("$mult"))
        }
        return null
    }
    // "X plus 3" (Vitalizing Cascade): a sum of two amounts. Both arms must render exactly — a bare
    // amountExpr would flatten this to just one arm (the X), silently dropping the rest.
    if (gn == "Plus" && node["args"].asArr?.size == 2) {
        val arr = node["args"].asArr!!
        val left = dynamicAmountExpr(arr[0]) ?: return null
        val right = dynamicAmountExpr(arr[1]) ?: return null
        return call("DynamicAmount.Add", arg(left), arg(right))
    }
    // "...ThisWay" game-numbers count objects this resolution touched (e.g. Volcanic Eruption's
    // "number of Mountains put into a graveyard this way"), not a current battlefield aggregate — a
    // resolution-scoped count the AggregateBattlefield heuristic below can't express. Scaffold rather
    // than misrender it as a battlefield tally. (Recognised "...ThisWay" shapes are handled above.)
    if (gn != null && "ThisWay" in gn) return null
    if ((gn != null && "NumberOf" in gn) || gn == "TheNumberOfPermanentsOnTheBattlefield") {
        // A "shares a creature type with <it>" relational predicate (Mana Echoes) can't be expressed by
        // the flat type/subtype/controller filter below — emitting the aggregate without it would silently
        // over-count, so decline (-> SCAFFOLD) rather than misrender.
        if ("SharesACreatureTypeWithPermanent" in compact(node)) return null
        val oracle = oracleText?.lowercase() ?: ""
        // The hand/"in it" guard catches a generic "NumberOf" count that's really about hand cards. It must
        // NOT fire for an explicit battlefield count (TheNumberOfPermanentsOnTheBattlefield) — a card may
        // mention "hand" elsewhere in its text (e.g. Slate of Ancestry's "Discard your hand" cost) while the
        // count itself is unambiguously a battlefield tally.
        val battlefieldCount = gn == "TheNumberOfPermanentsOnTheBattlefield"
        if (!battlefieldCount && (" hand" in oracle || " in it" in oracle)) return null
        val player = when {
            "attacking you" in oracle -> "Player.Opponent"
            "on the battlefield" in oracle -> "Player.Each"
            "target opponent controls" in oracle || jsonContains(node, "_Player", "Ref_TargetOpponent") -> "Player.TargetOpponent"
            "target player controls" in oracle || jsonContains(node, "_Player", "Ref_TargetPlayer") -> "Player.TargetPlayer"
            // A plain "an opponent controls" controller clause is `ControlledByAPlayer{_Players: Opponent}`
            // — the key is plural `_Players`, so the singular `_Player` probe above misses it and the count
            // would wrongly default to Player.You (Pygmy Kavu's "each black creature your opponents control").
            jsonContains(node, "_Player", "Opponent") || jsonContains(node, "_Players", "Opponent") -> "Player.Opponent"
            // An explicit "you control" controller predicate (Fire Dragon's "Mountains you control").
            "ControlledByAPlayer" in compact(node) -> "Player.You"
            // A global battlefield tally with NO controller predicate ("the number of attacking
            // creatures", Divine Retribution) counts every player's permanents — Each, not the You
            // default the controller-scoped NumberOf forms want.
            battlefieldCount -> "Player.Each"
            else -> "Player.You"
        }
        // "for each Goblin/Bird/Elf on the battlefield": a creature subtype, which the land-oriented
        // search filter misses; otherwise fall back to the land/type search filter.
        val subtype = node.firstArgWordTagged("IsCreatureType")
        // "for each Shrine you control" — an enchantment subtype the land/type search filter can't express;
        // it would silently widen the count's filter to GameObjectFilter.Any and over-count every permanent.
        // Decline (-> SCAFFOLD) rather than misrender (The Spirit Oasis's "draw a card for each Shrine").
        if (subtype == null && node.firstArgWordTagged("IsEnchantmentType") != null) return null
        val filter = if (subtype != null) Lit("GameObjectFilter.Creature").dot("withSubtype", arg("\"$subtype\""))
                     else landSearchFilterExpr(node)
        return call("DynamicAmount.AggregateBattlefield", arg(player), arg(filter))
    }
    return null
}

/** The `GameObjectFilter.*` for a [NumSpellsCastByPlayerThisTurn] spell filter, or null for any filter
 *  we can't render exactly (so the count scaffolds rather than under-/over-count). Mirrors the strict
 *  category mapping used for the WhenAPlayerCastsASpell trigger filter. */
private fun spellsCastThisTurnFilter(spells: JsonObject?): String? = when (spells?.strField("_Spells")) {
    null, "AnySpell" -> "GameObjectFilter.Any"
    "IsCardtype" -> if (spells.field("args").asStr() == "Creature") "GameObjectFilter.Creature" else null
    "IsNonCardtype" -> if (spells.field("args").asStr() == "Creature") "GameObjectFilter.Noncreature" else null
    else -> null
}

/** The `Player.*` scope for a [NumSpellsCastByPlayerThisTurn] player ref, or null for a scope we don't
 *  render. "that player" in a cast trigger is the triggering caster; an explicit You/Opponent map too. */
private fun spellsCastThisTurnPlayer(player: JsonObject?): String? = when (player?.strField("_Player")) {
    "Trigger_ThatPlayer" -> "Player.TriggeringPlayer"
    "You" -> "Player.You"
    "Opponent" -> "Player.Opponent"
    else -> null
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
    // A suffixed multi-target ref (Ref_TargetPermanent1 / Ref_TargetPermanent2 / …) indexes the named
    // per-target locals set up by the multi-target spell path. The ordinal is 1-based in the IR.
    if (ref != null && targetVars.isNotEmpty()) {
        Regex("^Ref_TargetPermanent(\\d+)$").matchEntire(ref)?.let { m ->
            val idx = m.groupValues[1].toInt() - 1
            return targetVars.getOrNull(idx)
        }
    }
    if (ref in setOf("Ref_TargetPermanent", "Ref_TargetPlayer", "Ref_TargetGraveyardCard")) {
        return if (targetVars.isNotEmpty()) targetRefVars[ref] else tvar
    }
    if (ref in SELF_REFS) return "EffectTarget.Self"
    // "that player" in a trigger ("the player ~ dealt combat damage to") -> the triggering player.
    if (ref == "Trigger_ThatPlayer") return "EffectTarget.PlayerRef(Player.TriggeringPlayer)"
    // A plain player reference (no target) — the controller / "you" or an opponent. The pain-land idiom
    // "{T}: Add {C}. This land deals N damage to you" carries a `_DamageRecipient: Player{You}` recipient
    // that is the controller, not a chosen target (Adarkar Wastes, Caldera Lake, Ancient Tomb).
    if (ref == "You") return "EffectTarget.PlayerRef(Player.You)"
    if (ref == "Opponent") return "EffectTarget.PlayerRef(Player.Opponent)"
    return tvar
}

internal fun EmitCtx.keywordOf(node: JsonElement?): String? {
    for (m in Regex("\"(\\w+)\"").findAll(compact(node))) {
        val kw = pascalToUpperSnake(m.groupValues[1])
        if (kw in keywords) return kw
    }
    return null
}

/**
 * A resolution-time condition node (inside an `IfElse` / `If` action) -> a `Conditions.*` DSL string,
 * or null (-> SCAFFOLD) for any shape we can't express exactly. Only the shapes our calibrated cards
 * need render; declining beats widening.
 *
 *  - `PlayerPassesFilter(You, ControlsA(<filter>))` ("if you control a <filter>") ->
 *    `Conditions.YouControl(<filter>)` (Take the Fall: "if you control an outlaw" -> the outlaw group).
 */
internal fun EmitCtx.actionConditionDsl(cond: JsonObject?): String? {
    if (cond == null) return null
    if (cond.strField("_Condition") != "PlayerPassesFilter") return null
    val args = cond["args"].asArr ?: return null
    if ((args.getOrNull(0) as? JsonObject)?.strField("_Player") != "You") return null
    val controls = args.getOrNull(1) as? JsonObject ?: return null
    if (controls.strField("_Players") != "ControlsA") return null
    val filter = gameObjectFilterDsl(controls["args"]) ?: return null
    return render(call("Conditions.YouControl", arg(Lit(filter))))
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
internal fun EmitCtx.becomeCreatureTypeEffect(actions: List<JsonObject>, tvar: String?): Dsl? {
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
    val parts = mutableListOf(arg("target", Lit(target)))
    if (excluded != null) parts.add(arg("excludedTypes", "listOf(\"${ktStr(excluded)}\")"))
    return Call("BecomeCreatureTypeEffect", parts)
}

/**
 * [ChooseACreatureType, CreateEachPermanentLayerEffectUntil(creatures of the chosen type get ±X/±X)] ->
 * a single `Effects.ChooseCreatureTypeModifyStats(...)` (Tribal Unity: "Creatures of the creature type
 * of your choice get +X/+X until end of turn"). Only the bare ±X/±X over the chosen-type group at
 * end-of-turn collapses; a riding keyword grant or any other shape declines (null -> SCAFFOLD).
 */
internal fun EmitCtx.chooseTypeModifyStatsEffect(actions: List<JsonObject>): Dsl? {
    if (actions.size != 2) return null
    if (actions.none { it.strField("_Action") == "ChooseACreatureType" }) return null
    val layer = actions.firstOrNull { it.strField("_Action") == "CreateEachPermanentLayerEffectUntil" } ?: return null
    val blob = compact(layer)
    if ("IsCreatureTypeVariable" !in blob || "TheChosenCreatureType" !in blob) return null
    if (!jsonContains(layer, "_Expiration", "UntilEndOfTurn")) return null
    val layerEffects = (layer["args"].asArr)?.getOrNull(1) as? JsonArray ?: return null
    if (layerEffects.size != 1) return null  // a lone ±X/±X; a riding grant would need grantKeyword
    val le = layerEffects[0] as? JsonObject ?: return null
    if (le.strField("_LayerEffect") != "AdjustPTX") return null
    val a = le["args"].asArr ?: return null
    if (a.size != 3) return null
    val amt = dynamicAmountExpr(a[2]) ?: return null
    val power = adjustModX(a.getOrNull(0), amt) ?: return null
    val toughness = adjustModX(a.getOrNull(1), amt) ?: return null
    return Call("Effects.ChooseCreatureTypeModifyStats", listOf(arg(power), arg(toughness)))
}

/**
 * [ChooseACreatureType, RevealTopCardOfLibrary, IfElse(revealed creature is of the chosen type ->
 * hand, else -> graveyard)] -> the single `Patterns.CreatureType.chooseCreatureTypeRevealTop()` pattern
 * (Bloodline Shaman). The whole three-action chain collapses to the named SDK pattern, so it renders
 * as one effect rather than three; any deviation from this exact shape declines (null -> SCAFFOLD).
 */
internal fun EmitCtx.chooseCreatureTypeRevealTopEffect(actions: List<JsonObject>): Dsl? {
    if (actions.size != 3) return null
    if (actions[0].strField("_Action") != "ChooseACreatureType") return null
    if (actions[1].strField("_Action") != "RevealTopCardOfLibrary") return null
    if (actions[2].strField("_Action") != "IfElse") return null
    val blob = compact(actions[2])
    if ("ACardWasRevealedThisWay" !in blob || "IsCreatureTypeVariable" !in blob ||
        "TheChosenCreatureType" !in blob) return null
    if ("PutTopOfLibraryInHand" !in blob || "PutTopOfLibraryInGraveyard" !in blob) return null
    return call("Patterns.CreatureType.chooseCreatureTypeRevealTop")
}

/** [MayCost(cost), Unless(CostWasPaid, [Sacrifice...])] -> PayOrSufferEffect (echo / upkeep cost). */
internal fun EmitCtx.echoEffect(actions: List<JsonObject>): Dsl? {
    if (actions.size != 2) return null
    val (a0, a1) = actions
    if (a0.strField("_Action") != "MayCost" || a1.strField("_Action") != "Unless") return null
    if (!jsonContains(a1, "_Condition", "CostWasPaid") || !jsonContains(a1, "_Action", "SacrificePermanent")) return null
    val cost = paycostDsl(a0["args"]) ?: return null
    return call("PayOrSufferEffect", arg("cost", Lit(cost)), arg("suffer", "SacrificeSelfEffect"))
}
