package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.Assign
import com.wingedsheep.tooling.coverage.Block
import com.wingedsheep.tooling.coverage.Dsl
import com.wingedsheep.tooling.coverage.Eval
import com.wingedsheep.tooling.coverage.Lit
import com.wingedsheep.tooling.coverage.Local
import com.wingedsheep.tooling.coverage.RawLine
import com.wingedsheep.tooling.coverage.Stmt
import com.wingedsheep.tooling.coverage.Sub
import com.wingedsheep.tooling.coverage.arg
import com.wingedsheep.tooling.coverage.argWordsTagged
import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.asInt
import com.wingedsheep.tooling.coverage.asStr
import com.wingedsheep.tooling.coverage.call
import com.wingedsheep.tooling.coverage.compact
import com.wingedsheep.tooling.coverage.field
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.strField
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** (targets|null, actions|null) from the first Targeted / ActionList envelope in a subtree.
 *  Shared by spells (SpellActions) and triggered abilities (TriggerA). */
internal fun extractEnvelope(node: JsonElement?): Pair<List<JsonObject>?, List<JsonObject>?> {
    var foundTargets: List<JsonObject>? = null
    var foundActions: List<JsonObject>? = null
    fun walk(n: JsonElement?) {
        when (n) {
            is JsonObject -> {
                val actionsKind = n.strField("_Actions")
                val args = n["args"].asArr
                if (actionsKind == "Targeted" && args != null && args.size >= 2) {
                    if (foundActions == null) {
                        foundTargets = (args[0].asArr)?.filterIsInstance<JsonObject>() ?: emptyList()
                        foundActions = (args[1] as? JsonObject)?.get("args").asArr?.filterIsInstance<JsonObject>() ?: emptyList()
                    }
                } else if (actionsKind == "ActionList" && args != null && foundActions == null) {
                    foundActions = args.filterIsInstance<JsonObject>()
                }
                n.values.forEach { walk(it) }
            }
            is JsonArray -> n.forEach { walk(it) }
            else -> {}
        }
    }
    walk(node)
    return foundTargets to foundActions
}

/** (targetNode, tvar) for an envelope's targets; null if unrenderable (bail); (null, null) if none. */
internal fun EmitCtx.spellTargetExpr(targets: List<JsonObject>?, actions: List<JsonObject>? = null): Pair<Dsl?, String?>? {
    if (targets.isNullOrEmpty()) return null to null
    if (targets.size > 1) { reasons.add("multi-target"); return null }
    val t = targetExpr(targets[0], actions) ?: run { reasons.add("target:${targets[0].strField("_Target")}"); return null }
    return t to "t"
}

/** `val t = target("target", <node>)` — the bound-target local statement. */
private fun targetLocal(node: Dsl): Stmt = Local("t", call("target", arg("\"target\""), arg(node)))

private fun EmitCtx.conditionDsl(ifNode: JsonElement?): String? {
    val blob = compact(ifNode)
    if ("ControlsMorePermanentThanPlayer" in blob && "\"Land\"" in blob) return "Conditions.OpponentControlsMoreLands"
    if ("ControlsMorePermanentThanPlayer" in blob && "\"Creature\"" in blob) return "Conditions.OpponentControlsMoreCreatures"
    return null
}

internal fun EmitCtx.castEffectHandled(rule: JsonObject): Boolean {
    val node = rule["args"] as? JsonObject ?: return false
    return when (node.strField("_CastEffect")) {
        "CantBeCastUnless" -> castRestrictionLines(listOf(rule)) != null
        "AdditionalCastingCost" -> additionalCostLine(rule) != null
        else -> false
    }
}

internal fun EmitCtx.cardLevelCastEffectLines(card: JsonObject): List<String>? {
    val lines = mutableListOf<String>()
    for (rule in (card["Rules"].asArr ?: JsonArray(emptyList())).filterIsInstance<JsonObject>()) {
        if (rule.strField("_Rule") != "CastEffect") continue
        val line = additionalCostLine(rule)
        if (line != null) lines.add(line)
        else if (!castEffectHandled(rule)) return null
    }
    return lines
}

private fun EmitCtx.additionalCostLine(rule: JsonObject): String? {
    val node = rule["args"] as? JsonObject ?: return null
    if (node.strField("_CastEffect") != "AdditionalCastingCost") return null
    val cost = node["args"] as? JsonObject ?: return null
    if (cost.strField("_Cost") != "SacrificeAPermanent") return null
    val filter = gameObjectFilterDsl(cost["args"]) ?: return null
    return "    additionalCost(Costs.additional.SacrificePermanent($filter))"
}

private fun EmitCtx.castRestrictionLines(rules: List<JsonObject>): List<String>? {
    val lines = mutableListOf<String>()
    for (rule in rules) {
        val node = rule["args"] as? JsonObject ?: return null
        if (node.strField("_CastEffect") != "CantBeCastUnless") continue
        val blob = compact(node)
        if ("IsDuringDeclareAttackersStep" in blob && "IsAttacked" in blob) {
            lines.add("        castOnlyDuring(Step.DECLARE_ATTACKERS)")
            lines.add("        castOnlyIf(YouWereAttackedThisStep)")
        } else {
            return null
        }
    }
    return lines
}

/** Top-level `If{cond}[effect]` -> spell `condition =` gate + the inner effect (Gift of Estates). */
private fun EmitCtx.conditionalSpell(card: JsonObject): List<Stmt>? {
    val (_, actions) = extractEnvelope(card["Rules"])
    if (actions == null || actions.size != 1 || actions[0].strField("_Action") != "If") return null
    val ifNode = actions[0]
    val cond = conditionDsl(ifNode) ?: return null
    val body = ifNode["args"].asArr
    val inner = if (body != null && body.size > 1 && body[1] is JsonArray) (body[1] as JsonArray).filterIsInstance<JsonObject>() else null
    if (inner == null) return null
    val edsl = renderEffectList(inner, null) ?: return null
    return listOf(Sub(Block("spell", listOf(Assign("condition", Lit(cond)), Assign("effect", edsl)))))
}

internal fun EmitCtx.spellBlock(card: JsonObject): List<Stmt>? {
    // Modal spells ("Choose one —", "Choose up to four", …) carry a `Modal_*` envelope whose children
    // are the individual modes. The generic envelope path below would grab only the FIRST mode and
    // silently drop the rest, so scaffold the whole card rather than emit one arm of a modal spell.
    if ("\"Modal_" in compact(card["Rules"])) { reasons.add("modal-spell"); return null }
    // One-line `effect =` shortcuts, then whole-block shortcuts, then the generic envelope path.
    eachplayerMaydraw(card)?.let { return spellOf(it) }
    fluxEffect(card)?.let { return spellOf(it) }
    windsEffect(card)?.let { return spellOf(it) }
    extraTurnEffect(card)?.let { return spellOf(it) }
    distributedSpell(card)?.let { return it }
    balanceEffect(card)?.let { return it }
    conditionalSpell(card)?.let { return it }

    val (targets, actions) = extractEnvelope(card["Rules"])
    if (actions == null) return null
    val (tnode, tvar) = spellTargetExpr(targets, actions) ?: return null
    val edsl = renderEffectList(actions, tvar) ?: return null
    val restrictions = castRestrictionLines((card["Rules"].asArr ?: JsonArray(emptyList())).filterIsInstance<JsonObject>()) ?: return null
    val stmts = mutableListOf<Stmt>()
    restrictions.forEach { stmts.add(RawLine(it)) }
    if (tvar != null) stmts.add(targetLocal(tnode!!))
    stmts.add(Assign("effect", edsl))
    return listOf(Sub(Block("spell", stmts)))
}

private fun spellOf(effect: Dsl): List<Stmt> = listOf(Sub(Block("spell", listOf(Assign("effect", effect)))))

/** mtgish actions whose Argentum rendering already embeds the "you may" choice (so a wrapping
 *  MayAction must NOT also set the ability's `optional = true`). */
private val SELF_OPTIONAL_ACTIONS = setOf("PutACardFromHandOnBattlefield")

private val TRIGGER_SPEC = mapOf(
    "WhenAPermanentEntersTheBattlefield" to "Triggers.EntersBattlefield",
    "WhenACreatureOrPlaneswalkerDies" to "Triggers.Dies",
    "WhenACreatureAttacks" to "Triggers.Attacks",
    "WhenACreatureBlocks" to "Triggers.Blocks",
    "WhenACreatureDealsCombatDamageToAPlayer" to "Triggers.DealsCombatDamageToPlayer",
    "WhenACreatureBecomesBlocked" to "Triggers.BecomesBlocked",
)

/** A TriggerA rule (self-triggered) -> triggeredAbility { trigger; [target]; effect }.
 *  [oncePerTurn] is set by the `TriggerOnceEachTurn` rule envelope, whose body is otherwise shaped
 *  identically to a TriggerA. */
internal fun EmitCtx.triggerBlock(rule: JsonObject, oncePerTurn: Boolean = false): List<Stmt>? {
    // A modal triggered ability ("When this enters, choose one — …") carries a `Modal_*` envelope; the
    // generic action path would render only the first mode and silently drop the rest, so decline the
    // whole ability rather than emit one arm (mirrors the spell-side modal guard).
    if ("\"Modal_" in compact(rule)) { reasons.add("modal-trigger"); return null }
    val spec = triggerSpecFor(rule) ?: run { reasons.add("trigger-shape"); return null }
    val (targets, actions) = extractEnvelope(rule)
    if (actions == null) { reasons.add("trigger-actions"); return null }
    val (tnode, tvar) = spellTargetExpr(targets, actions) ?: return null

    // "you may [do X]" on a triggered ability is an OPTIONAL ability (declined at announcement /
    // by choosing no targets), not a resolution-time MayEffect. Unwrap a lone MayAction so the
    // ability carries `optional = true` and a plain effect — the engine's idiom for "may [target]".
    val mayWrapped = actions.singleOrNull()?.strField("_Action") == "MayAction"
    val mayInner = if (mayWrapped) innerAction(actions.single()) ?: return null else null
    val effectActions = if (mayWrapped) listOf(mayInner!!) else actions
    // Some effects already carry their own "you may" choice (putFromHand prompts whether to put the
    // card), so the MayAction wrapper is absorbed by the effect, not re-expressed as `optional = true`
    // (which would double-wrap vs the golden — Elvish Pioneer).
    val selfOptional = mayInner?.strField("_Action") in SELF_OPTIONAL_ACTIONS
    val edsl = renderEffectList(effectActions, tvar) ?: return null

    val stmts = mutableListOf<Stmt>(Assign("trigger", Lit(spec)))
    if (oncePerTurn) stmts.add(Assign("oncePerTurn", Lit("true")))
    if (mayWrapped && !selfOptional) stmts.add(Assign("optional", Lit("true")))
    if (tvar != null) stmts.add(targetLocal(tnode!!))
    stmts.add(Assign("effect", edsl))
    return listOf(Sub(Block("triggeredAbility", stmts)))
}

/** The triggered-ability trigger spec for a TriggerA / TriggerOnceEachTurn rule, or null (-> SCAFFOLD)
 *  for a trigger shape we can't render exactly. The first arg of a TriggerA rule is the `_Trigger`
 *  node; scoping checks to it (not the whole rule) keeps an action's `You`/`ThisPermanent` markers from
 *  being mistaken for trigger scopes. */
private fun EmitCtx.triggerSpecFor(rule: JsonObject): String? {
    val trig = rule["args"].asArr?.firstOrNull() as? JsonObject ?: return null

    // A compound `Or` of two triggers ("Whenever this creature OR another creature you control with
    // flying enters") can't be expressed as one filter+binding — a nested-trigger scan would match the
    // inner enter-trigger and emit a single OTHER-binding trigger that drops the self arm. Decline.
    if (trig.strField("_Trigger") == "Or") return null

    // SELF self-triggers (this permanent enters / dies / attacks / deals combat damage to a player).
    // `isSelf` distinguishes "the subject IS this permanent" from an `Other(ThisPermanent)` clause
    // ("another …"), which contains ThisPermanent only as the exclusion reference.
    for ((mtTrigger, dsl) in TRIGGER_SPEC) {
        if (jsonContains(trig, "_Trigger", mtTrigger) && isSelf(trig)) return dsl
    }

    // "Whenever ~ deals damage" / "Whenever ~ is dealt damage" (SELF) — paired with a "that much"
    // gain/lose-life or token effect.
    if (jsonContains(trig, "_Trigger", "WhenAPermanentDealsDamage") && isSelf(trig))
        return "Triggers.DealsDamage"
    if (jsonContains(trig, "_Trigger", "WhenAPermanentIsDealtDamage") && isSelf(trig))
        return "Triggers.TakesDamage"

    // Phase/step triggers. "your upkeep" is scoped to You; "each upkeep" to any player; "each
    // opponent's upkeep" to Opponent. The host-relative scopes (HostController / HostPlayer, an Aura
    // granting an upkeep trigger to the enchanted permanent's controller) decline -> SCAFFOLD, as do
    // the niche dynamic scopes (TheChosenPlayer, ControllerOfSpell, ...).
    if (jsonContains(trig, "_Trigger", "AtTheBeginningOfAPlayersUpkeep")) {
        if (jsonContains(trig, "_Player", "You")) return "Triggers.YourUpkeep"
        if (jsonContains(trig, "_Players", "AnyPlayer")) return "Triggers.EachUpkeep"
        if (jsonContains(trig, "_Players", "Opponent")) return "Triggers.EachOpponentUpkeep"
    }
    // "your end step" is scoped to You (a SinglePlayer(You) subject); "each end step" to any player.
    // The opponent / host-relative end-step scopes have no matching Triggers.* constant yet, so they
    // decline -> SCAFFOLD, mirroring the upkeep block above (which has an EachOpponentUpkeep but no
    // end-step counterpart exists).
    if (jsonContains(trig, "_Trigger", "AtTheBeginningOfAPlayersEndStep")) {
        if (jsonContains(trig, "_Player", "You")) return "Triggers.YourEndStep"
        if (jsonContains(trig, "_Players", "AnyPlayer")) return "Triggers.EachEndStep"
    }

    // "Whenever a player cycles a card" (any player) — Fleeting Aven, Invigorating Boon.
    if (jsonContains(trig, "_Trigger", "WhenAPlayerCyclesACard") && jsonContains(trig, "_Players", "AnyPlayer"))
        return "Triggers.AnyPlayerCycles"

    // "Whenever a creature attacks" — only the unrestricted any-creature shape (no subtype / controller /
    // count clause), which maps to a filterless ANY-binding attacks trigger (Righteous Cause).
    if (jsonContains(trig, "_Trigger", "WhenACreatureAttacks") && isPlainCreatureFilter(trig))
        return "Triggers.attacks(binding = TriggerBinding.ANY)"

    // "Whenever you attack with N or more creatures" — WhenAPlayerAttacksWithANumberOfCreatures scoped to
    // You + a `>= N` comparison + a plain creature filter (Overwhelming Instinct). Only the
    // greater-than-or-equal (N-or-more) shape maps to YouAttackEvent(minAttackers); any other comparison
    // or a typed/controlled attacker filter declines.
    if (jsonContains(trig, "_Trigger", "WhenAPlayerAttacksWithANumberOfCreatures") &&
        jsonContains(trig, "_Player", "You") && jsonContains(trig, "_Comparison", "GreaterThanOrEqualTo")
    ) {
        val blob = compact(trig)
        val plainCreature = "Creature" in trig.argWordsTagged("IsCardtype") &&
            "IsCreatureType" !in blob && "ControlledByAPlayer" !in blob && "\"Other\"" !in blob && "_Color" !in blob
        val n = findInteger(trig) as? Int
        if (plainCreature && n != null) return "TriggerSpec(EventPattern.YouAttackEvent(minAttackers = $n), TriggerBinding.ANY)"
    }

    // "Whenever a [filtered] permanent enters the battlefield" (the SELF case returned above): an
    // `Other(ThisPermanent)` clause means "another …" -> OTHER binding (Elvish Vanguard's "another
    // Elf", Wretched Anurid's "another creature"); otherwise "a …" -> ANY (Wirewood Savage's "a Beast").
    if (jsonContains(trig, "_Trigger", "WhenAPermanentEntersTheBattlefield")) {
        val binding = if (jsonContains(trig, "_Permanents", "Other")) "TriggerBinding.OTHER" else "TriggerBinding.ANY"
        val filter = gameObjectFilterDsl(trig) ?: return null
        return "Triggers.entersBattlefield(filter = $filter, binding = $binding)"
    }

    // "Whenever a [creature type] deals combat damage to a player, …" — non-self
    // WhenACreatureDealsCombatDamageToAPlayer whose source filter is purely a creature subtype, to any
    // player (Cabal Slaver's "a Goblin"). Anything beyond a bare subtype (controller / colour / count /
    // "another") declines so we never widen the source filter.
    if (jsonContains(trig, "_Trigger", "WhenACreatureDealsCombatDamageToAPlayer") && !isSelf(trig) &&
        jsonContains(trig, "_Players", "AnyPlayer")
    ) {
        val subtype = creatureTypeIn(trig)
        val blob = compact(trig)
        val bareSubtype = subtype != null && "ControlledByAPlayer" !in blob &&
            "_Color" !in blob && "_Comparison" !in blob && "\"Other\"" !in blob
        if (bareSubtype) return "TriggerSpec(EventPattern.DealsDamageEvent(damageType = DamageType.Combat, " +
            "recipient = RecipientFilter.AnyPlayer, sourceFilter = GameObjectFilter.Creature.withSubtype(\"$subtype\")), " +
            "TriggerBinding.ANY)"
    }

    // "Whenever {you / a player / an opponent} casts a [type] spell" — WhenAPlayerCastsASpell. The
    // first arg is the caster scope (You / AnyPlayer / Opponent — anything else, e.g. HostPlayer,
    // declines); the second is the spell filter, classified to an EXACT category. A filter carrying
    // any extra constraint (mana value, colour, targets, kicked, name, a mixed And/Or) yields a null
    // category and declines -> SCAFFOLD, rather than silently dropping the clause.
    if (jsonContains(trig, "_Trigger", "WhenAPlayerCastsASpell")) {
        val argv = trig["args"].asArr
        val scope = castScope(argv?.getOrNull(0) as? JsonObject) ?: return null
        val category = spellCastCategory(argv?.getOrNull(1) as? JsonObject) ?: return null
        return castTriggerDsl(scope, category)
    }
    return null
}

/** The caster scope of a WhenAPlayerCastsASpell trigger, or null for a scope we don't render
 *  (HostPlayer, EnchantedPlayer, …). Read from the trigger's own first arg, NOT a whole-trigger
 *  search — a `_Player: You` buried in the spell filter (e.g. WasCastFromAPlayersGraveyard(You))
 *  must not be mistaken for the caster. */
private enum class CastScope { YOU, ANY, OPPONENT }

private fun castScope(players: JsonObject?): CastScope? = when (players?.strField("_Players")) {
    "AnyPlayer" -> CastScope.ANY
    "Opponent" -> CastScope.OPPONENT
    "SinglePlayer" -> if (players.field("args").strField("_Player") == "You") CastScope.YOU else null
    else -> null
}

/** Canonical category for a WhenAPlayerCastsASpell spell-filter node, or null when the filter carries
 *  any constraint we can't render exactly. Strict by design: decline -> SCAFFOLD beats dropping a clause. */
private fun spellCastCategory(spells: JsonObject?): String? = when (spells?.strField("_Spells")) {
    "AnySpell" -> "any"
    "IsHistoric" -> "historic"
    "IsCardtype" -> when (spells.field("args").asStr()) {
        "Creature" -> "creature"
        "Enchantment" -> "enchantment"
        else -> null
    }
    "IsNonCardtype" -> if (spells.field("args").asStr() == "Creature") "noncreature" else null
    // "an instant or sorcery spell" — an Or of exactly the two cardtype clauses, nothing else.
    "Or" -> {
        val parts = spells["args"].asArr.orEmpty().map { it as? JsonObject }
        val types = parts.map { if (it?.strField("_Spells") == "IsCardtype") it.field("args").asStr() else null }
        if (types.none { it == null } && types.filterNotNull().toSet() == setOf("Instant", "Sorcery")) "instantOrSorcery" else null
    }
    else -> null
}

/** (scope, category) -> the exact `Triggers.*` constant/factory. You has named constants; the
 *  any-player / opponent scopes use the `anyPlayerCasts` / `opponentCasts` factories with a
 *  [GameObjectFilter]. */
private fun castTriggerDsl(scope: CastScope, category: String): String? {
    val filter = when (category) {
        "any" -> null
        "creature" -> "GameObjectFilter.Creature"
        "noncreature" -> "GameObjectFilter.Noncreature"
        "enchantment" -> "GameObjectFilter.Enchantment"
        "instantOrSorcery" -> "GameObjectFilter.InstantOrSorcery"
        "historic" -> "GameObjectFilter.Historic"
        else -> return null
    }
    return when (scope) {
        CastScope.YOU -> when (category) {
            "any" -> "Triggers.YouCastSpell"
            "creature" -> "Triggers.YouCastCreature"
            "noncreature" -> "Triggers.YouCastNoncreature"
            "enchantment" -> "Triggers.YouCastEnchantment"
            "instantOrSorcery" -> "Triggers.YouCastInstantOrSorcery"
            "historic" -> "Triggers.YouCastHistoric"
            else -> null
        }
        CastScope.ANY -> if (filter == null) "Triggers.AnyPlayerCastsSpell" else "Triggers.anyPlayerCasts($filter)"
        CastScope.OPPONENT -> if (filter == null) "Triggers.OpponentCastsSpell" else "Triggers.opponentCasts($filter)"
    }
}

/** True when a trigger's subject IS this permanent — ThisPermanent present, but NOT merely as the
 *  reference inside an `Other(ThisPermanent)` "another permanent" exclusion clause. */
private fun isSelf(trig: JsonObject): Boolean {
    // A self-trigger's subject is a *direct* SinglePermanent(ThisPermanent) reference. A `_Permanents`
    // filter (And / HasAbility / …) that merely mentions ThisPermanent inside a sub-predicate — e.g.
    // Trophy Hunter's WasDealtDamageByPermanentThisTurn(ThisPermanent), "a creature with flying dealt
    // damage by THIS dies" — is NOT a self-trigger, so scope the check to the trigger's own subject
    // rather than a deep search that any nested ThisPermanent reference would satisfy.
    val subject = trig["args"]
    return subject.strField("_Permanents") == "SinglePermanent" &&
        subject.field("args").strField("_Permanent") == "ThisPermanent"
}

/** True when a trigger's permanent filter is a plain "creature" with no subtype / controller / count
 *  restriction — the only attacks shape we can render as a filterless ANY-binding trigger. */
private fun isPlainCreatureFilter(trig: JsonObject): Boolean {
    if ("Creature" !in trig.argWordsTagged("IsCardtype")) return false
    val blob = compact(trig)
    return "IsCreatureType" !in blob && "ControlledByAPlayer" !in blob &&
        "\"Other\"" !in blob && "_Comparison" !in blob && "_Color" !in blob
}

/**
 * A characteristic-defining `CDA_Power` rule (with its matching `CDA_Toughness`) -> a single
 * `dynamicStats(...)` line, when both power and toughness are the same dynamic count (the
 * power-and-toughness-equal-to-the-number-of-X cycle). Differing power/toughness amounts scaffold.
 */
internal fun EmitCtx.cdaStatsBlock(card: JsonObject, rule: JsonObject): List<Stmt>? {
    val toughnessRule = (card["Rules"].asArr ?: JsonArray(emptyList()))
        .filterIsInstance<JsonObject>().firstOrNull { it.strField("_Rule") == "CDA_Toughness" }
    if (toughnessRule == null || compact(rule["args"]) != compact(toughnessRule["args"])) {
        reasons.add("CDA_Power"); return null
    }
    val amount = dynamicAmountExpr(rule["args"]) ?: run { reasons.add("CDA_Power"); return null }
    return listOf(Eval(call("dynamicStats", arg(amount))))
}

/**
 * An `AsPermanentEnters` rule -> `replacementEffect(...)` line(s). The rule's second arg is a list of
 * `_ReplacementActionWouldEnter` nodes (enters tapped, choose a creature type as it enters, ...).
 * Any replacement we can't render exactly downgrades the card to SCAFFOLD rather than guess.
 */
internal fun EmitCtx.asEntersBlock(rule: JsonObject): List<Stmt>? {
    val replacements = (rule["args"].asArr?.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>()
    if (replacements.isNullOrEmpty()) { reasons.add("AsPermanentEnters"); return null }
    // The rule's first arg is the permanent the replacement scopes to. We only render the self case
    // ("~ enters …"), where the counter/tap applies to THIS permanent (`selfOnly`); a group scope
    // ("creatures you control enter …") would need a different rendering, so it scaffolds.
    val onSelf = (rule["args"].asArr?.getOrNull(0) as? JsonObject)?.strField("_Permanent") == "ThisPermanent"
    val stmts = mutableListOf<Stmt>()
    for (rep in replacements) {
        val dsl: Dsl = when (rep.strField("_ReplacementActionWouldEnter")) {
            "EntersTapped" -> call("EntersTapped")
            "ChooseACreatureType" -> call("EntersWithChoice", arg("ChoiceType.CREATURE_TYPE"))
            "EntersWithACounter" -> {
                // "~ enters with a +1/+1 counter on it" — a single fixed counter on this permanent.
                // Only the self-scoped ±1/±1 counter renders; other counter kinds need a CounterTypeFilter
                // we don't map here, and a group scope is unsupported, so both scaffold.
                if (!onSelf) { reasons.add("AsPermanentEnters"); return null }
                val counter = rep["args"] as? JsonObject
                val pt = counter?.get("args").asArr
                if (counter?.strField("_CounterType") != "PTCounter" || pt?.getOrNull(0).asInt() != 1 || pt?.getOrNull(1).asInt() != 1) {
                    reasons.add("AsPermanentEnters"); return null
                }
                call("EntersWithCounters", arg("count", "1"), arg("selfOnly", "true"))
            }
            "EntersWithNumberCounters" -> {
                // "enters with X +1/+1 counters" where X is a dynamic count (Stag Beetle: number of other
                // creatures — as it enters, self isn't on the battlefield, so the plain count IS "other").
                // Only the ±1/±1 counter with a recoverable dynamic amount renders; anything else scaffolds.
                val a = rep["args"].asArr ?: run { reasons.add("AsPermanentEnters"); return null }
                val counter = a.getOrNull(1) as? JsonObject
                val pt = counter?.get("args").asArr
                if (counter?.strField("_CounterType") != "PTCounter" || pt?.getOrNull(0).asInt() != 1 || pt?.getOrNull(1).asInt() != 1) {
                    reasons.add("AsPermanentEnters"); return null
                }
                val amt = dynamicAmountExpr(a.getOrNull(0)) ?: run { reasons.add("AsPermanentEnters"); return null }
                call("EntersWithDynamicCounters", arg("count", amt))
            }
            else -> { reasons.add("AsPermanentEnters"); return null }
        }
        stmts.add(Eval(call("replacementEffect", arg(dsl))))
    }
    return stmts
}

/**
 * A `FromAnyZone { TriggerA { WhenAPlayerCyclesACard(You, this) ... } }` rule -> a triggered ability
 * with `trigger = Triggers.YouCycleThis` ("When you cycle this card, [bonus]"). A lone `you may` bonus
 * becomes `optional = true`, mirroring [triggerBlock].
 */
internal fun EmitCtx.fromAnyZoneBlock(rule: JsonObject): List<Stmt>? {
    val inner = rule["args"] as? JsonObject
    if (inner?.strField("_Rule") != "TriggerA" ||
        !jsonContains(inner, "_Trigger", "WhenAPlayerCyclesACard") ||
        !jsonContains(inner, "_CardInHand", "ThisCardInHand")) { reasons.add("FromAnyZone"); return null }
    val (targets, actions) = extractEnvelope(inner)
    if (actions == null) { reasons.add("FromAnyZone"); return null }
    val (tnode, tvar) = spellTargetExpr(targets, actions) ?: return null
    val mayWrapped = actions.singleOrNull()?.strField("_Action") == "MayAction"
    val effectActions = if (mayWrapped) listOf(innerAction(actions.single()) ?: return null) else actions
    val edsl = renderEffectList(effectActions, tvar) ?: return null
    val stmts = mutableListOf<Stmt>(Assign("trigger", Lit("Triggers.YouCycleThis")))
    if (mayWrapped) stmts.add(Assign("optional", Lit("true")))
    if (tvar != null) stmts.add(targetLocal(tnode!!))
    stmts.add(Assign("effect", edsl))
    return listOf(Sub(Block("triggeredAbility", stmts)))
}

/**
 * A `FromGraveyard { Activated { … } }` rule -> the inner activated ability with
 * `activateFromZone = Zone.GRAVEYARD` ("{cost}, … : Return this card from your graveyard to your hand",
 * Gangrenous Goliath). Only a plain Activated inner renders; anything else scaffolds.
 */
internal fun EmitCtx.fromGraveyardBlock(rule: JsonObject): List<Stmt>? {
    val inner = rule["args"] as? JsonObject
    return when (inner?.strField("_Rule")) {
        "Activated", "ActivatedWithModifiers" -> activatedBlock(inner, activateFromZone = "Zone.GRAVEYARD")
        else -> { reasons.add("FromGraveyard"); return null }
    }
}

/** An Activated / ActivatedWithModifiers rule -> activatedAbility { cost; [target]; effect }. */
internal fun EmitCtx.activatedBlock(rule: JsonObject, activateFromZone: String? = null): List<Stmt>? {
    val args = rule["args"].asArr
    val costNode = args?.firstOrNull() as? JsonObject
    // Recover the exact activation cost. Anything we can't render exactly -> SCAFFOLD (never guess Tap).
    val cost = costNode?.let { abilityCostDsl(it) }
    if (cost == null) { reasons.add("activated-cost"); return null }
    val (targets, actions) = extractEnvelope(rule)
    if (actions == null) { reasons.add("activated-actions"); return null }
    val (tnode, tvar) = spellTargetExpr(targets, actions) ?: return null
    val edsl = renderEffectList(actions, tvar) ?: return null
    val stmts = mutableListOf<Stmt>(Assign("cost", Lit(cost)))
    activationRestrictionLines(rule)?.let { lines -> lines.forEach { stmts.add(RawLine(it)) } } ?: return null
    if (tvar != null) stmts.add(targetLocal(tnode!!))
    stmts.add(Assign("effect", edsl))
    if (activateFromZone != null) stmts.add(Assign("activateFromZone", Lit(activateFromZone)))
    // A ReplaceNextDraw effect ("the next time you would draw … instead") prompts on the replaced draw,
    // not at activation — the activated-ability flag the Words cycle's golden carries.
    if (actions.any { it.strField("_Action") == "CreateFutureReplaceWouldDraw" }) stmts.add(Assign("promptOnDraw", Lit("true")))
    if (isManaAbility(tvar, actions)) {
        stmts.add(Assign("manaAbility", Lit("true")))
        stmts.add(Assign("timing", Lit("TimingRule.ManaAbility")))
    }
    return listOf(Sub(Block("activatedAbility", stmts)))
}

/**
 * mtgish activation-cost `_Cost` node -> the `Costs.*` AbilityCost DSL, or null (-> SCAFFOLD) for any
 * shape we can't render exactly. Recurses on `And` -> `Costs.Composite(...)`. Conservative by design:
 * a wrong cost is worse than a scaffold, so unknown atoms bail rather than approximate.
 */
internal fun EmitCtx.abilityCostDsl(node: JsonElement?): String? {
    val obj = node as? JsonObject ?: return null
    return when (obj.strField("_Cost")) {
        "And" -> {
            val parts = (obj["args"].asArr ?: return null).map { abilityCostDsl(it) ?: return null }
            if (parts.size < 2) return null
            "Costs.Composite(${parts.joinToString(", ")})"
        }
        "PayMana" -> renderMana(obj.field("args")).ifEmpty { null }?.let { "Costs.Mana(\"$it\")" }
        // "{X}{G}{G}" activation cost — args are [symbol-list, the X game number]; render the symbol list
        // (ManaCostX -> "{X}") as the mana cost (Silklash Spider).
        "PayManaX" -> renderMana((obj["args"].asArr)?.getOrNull(0)).ifEmpty { null }?.let { "Costs.Mana(\"$it\")" }
        "DiscardHand" -> "Costs.DiscardHand"  // "Discard your hand" (Slate of Ancestry)
        // "Discard a card at random" — a fixed, no-choice cost (the engine picks): Costs.DiscardAtRandom(1).
        // It carries no value selection / X / inherited choice, so it is safe to render exactly (Canyon Drake).
        "DiscardACardAtRandom" -> "Costs.DiscardAtRandom(1)"
        "TapPermanent" ->
            if (obj.field("args").strField("_Permanent") == "ThisPermanent") "Costs.Tap" else null
        "SacrificePermanent" ->
            if (obj.field("args").strField("_Permanent") == "ThisPermanent") "Costs.SacrificeSelf" else null
        "SacrificeAPermanent" -> costFilterDsl(obj.field("args"))?.let {
            if (it == "GameObjectFilter.Any") "Costs.Sacrifice()" else "Costs.Sacrifice($it)"
        }
        "PayLife" -> (findInteger(obj.field("args")) as? Int)?.let { "Costs.PayLife($it)" }
        "TapNumberPermanents" -> {
            val a = obj["args"].asArr ?: return null
            val n = findInteger(a.getOrNull(0)) as? Int ?: return null
            // "Tap N untapped X you control" — TapPermanents implies untapped + you-control, so only the
            // creature-subtype distinguishes it; bail if there's no recognisable creature-type filter.
            val ctype = creatureTypeIn(a.getOrNull(1)) ?: return null
            "Costs.TapPermanents($n, GameObjectFilter.Creature.withSubtype(\"$ctype\"))"
        }
        else -> null
    }
}

/** A sacrifice/cost permanent filter: the creature-subtype shape the general filter DSL skips, "any
 *  permanent" -> the default Any, otherwise delegate to [gameObjectFilterDsl]. */
private fun EmitCtx.costFilterDsl(node: JsonElement?): String? {
    val obj = node as? JsonObject
    when (obj?.strField("_Permanents")) {
        "IsCreatureType" -> return obj["args"].asStr()?.let { "GameObjectFilter.Creature.withSubtype(\"$it\")" }
        "IsArtifactType" -> return obj["args"].asStr()?.let { "GameObjectFilter.Artifact.withSubtype(\"$it\")" }
        "AnyPermanent" -> return "GameObjectFilter.Permanent"
        "IsToken" -> return "GameObjectFilter.Token"  // "Sacrifice a token" (Fountainport)
    }
    val base = gameObjectFilterDsl(node) ?: return null
    // "Sacrifice a Goblin creature" = And[IsCreatureType X, IsCardtype Creature]: gameObjectFilterDsl
    // sees the Creature cardtype but skips the creature subtype, so re-apply it here.
    val ctype = creatureTypeIn(node)
    return if (ctype != null && base == "GameObjectFilter.Creature") "GameObjectFilter.Creature.withSubtype(\"$ctype\")" else base
}

/** First `IsCreatureType` subtype anywhere in a (possibly `And`-nested) cost filter. */
private fun creatureTypeIn(node: JsonElement?): String? {
    when (node) {
        is JsonObject -> {
            if (node.strField("_Permanents") == "IsCreatureType") return node["args"].asStr()
            node.values.forEach { creatureTypeIn(it)?.let { r -> return r } }
        }
        is JsonArray -> node.forEach { creatureTypeIn(it)?.let { r -> return r } }
        else -> {}
    }
    return null
}

private fun EmitCtx.activationRestrictionLines(rule: JsonObject): List<String>? {
    if (rule.strField("_Rule") != "ActivatedWithModifiers") return emptyList()
    val blob = compact(rule)
    if ("ActivateOnlyIf" in blob && "IsTheirTurn" in blob && "IsBeforeAttackersDeclared" in blob) {
        return listOf(
            "        restrictions = listOf(",
            "            ActivationRestriction.OnlyDuringYourTurn,",
            "            ActivationRestriction.BeforeStep(Step.DECLARE_ATTACKERS)",
            "        )",
        )
    }
    // "Activate no more than N times each turn" (Pit Imp / Phyrexian Battleflies) -> MaxPerTurn(N).
    // The modifier list is the rule's args after the cost + action list; render only when EVERY
    // modifier present is this exact shape, so an unrecognised modifier still scaffolds.
    val modifiers = (rule["args"].asArr ?: emptyList()).filterIsInstance<JsonObject>()
        .filter { it.strField("_ActivateModifier") != null }
    if (modifiers.isNotEmpty() && modifiers.all { it.strField("_ActivateModifier") == "ActivateNoMoreThanNumberTimesEachTurn" }) {
        val lines = modifiers.map { mod ->
            val n = findInteger(mod.field("args")) as? Int ?: return run { reasons.add("activated-modifiers"); null }
            "ActivationRestriction.MaxPerTurn($n)"
        }
        return listOf("        restrictions = listOf(${lines.joinToString(", ")})")
    }
    reasons.add("activated-modifiers")
    return null
}
