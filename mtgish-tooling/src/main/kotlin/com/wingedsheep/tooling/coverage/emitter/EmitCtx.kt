package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.Call
import com.wingedsheep.tooling.coverage.Composite
import com.wingedsheep.tooling.coverage.bridge.Bridge
import com.wingedsheep.tooling.coverage.bridge.MappingEntry
import com.wingedsheep.tooling.coverage.Dsl
import com.wingedsheep.tooling.coverage.Lit
import com.wingedsheep.tooling.coverage.Raw
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
import com.wingedsheep.tooling.coverage.argWordsTagged
import com.wingedsheep.tooling.coverage.firstArgStringTagged
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

    /**
     * For multi-target spells, the ordered list of target locals per ref kind, so a suffixed
     * `Ref_TargetPermanentN` resolves to the N-th *permanent* target (1-based) — the IR ordinal counts
     * only targets of that kind, not the flat target list (which may interleave players). e.g. Dissection
     * Practice's `[TargetPlayer, UptoOneTargetPermanent, UptoOneTargetPermanent]` -> `Ref_TargetPermanent`
     * maps to `[t2, t3]`, so `Ref_TargetPermanent1` is `t2` and `Ref_TargetPermanent2` is `t3`.
     */
    var targetRefVarsByKind: Map<String, List<String>> = emptyMap()

    /**
     * True while rendering the effect of a trigger whose triggering entity is a *spell* (a
     * `WhenAPlayerCastsASpell` cast trigger). In that context the IR's "that player"
     * (`Trigger_ThatPlayer`) is the spell's controller — the caster — so a damage recipient resolves
     * to `EffectTarget.ControllerOfTriggeringEntity` (Magebane Lizard). For every other trigger
     * ("~ deals combat damage to a player", …) "that player" is the triggering player itself, so the
     * flag stays false and [refTargetFromRef] keeps emitting `PlayerRef(Player.TriggeringPlayer)`.
     * Set/cleared only by [triggerBlock]; default false on every other path.
     */
    var triggeringEntityIsSpell: Boolean = false

    /**
     * True while rendering the effect of an activated ability whose cost sacrifices or exiles the
     * source itself. In that context the source's counters are wiped at cost-payment time
     * (CR 122.2), so "for each counter on this creature" must read the pre-cost count as last-known
     * information (`DynamicAmount.LastKnownSourceCounters`, snapshotted by the engine at activation)
     * rather than `countersOnSelf`, which would read zero. Set/cleared only by [activatedAbilityStmts];
     * default false on every other path (the source is still on the battlefield). Twitching Doll,
     * Lost Isle Calling.
     */
    var sourceSacrificedByCost: Boolean = false

    /**
     * The mtgish `_GameNumber` node bound by a leading `CreateValueX` action, so any later
     * `ValueX` reference in the same action list inlines the *bound* derived amount rather than
     * `DynamicAmount.XValue`. The engine has no separate "set X" step for a triggered ability, so a
     * `CreateValueX(<amount>)` that's reused by N later actions is folded by inlining `<amount>` into
     * each `ValueX` consumer (Arabella, Abandoned Doll: "deals X damage to each opponent and you gain
     * X life, where X is the number of creatures you control with power 2 or less"). Set/cleared only
     * by [createValueXReusedEffect]; default null (so `ValueX` keeps meaning the cast-time X).
     */
    var boundValueX: JsonElement? = null
}

internal val SELF_REFS = setOf(
    "ThisPermanent", "Trigger_ThatCreature", "ThatEnteringPermanent", "Trigger_ThatPermanent",
    "ThatCreature", "ThatPermanent", "Trigger_ThatGraveyardCard", "ThatGraveyardCard",
    // "this card from your graveyard" — the source's own card in the graveyard. Used by self-recursion
    // graveyard activated abilities ("{cost}: Return this card from your graveyard to the battlefield",
    // Teacher's Pest); always resolves to the source itself.
    "ThisGraveyardCard",
)

/**
 * The mtgish derived variables that name "the token(s) a CreateTokens action in this same effect just
 * made". itemfive's mtgish token-creation cleanup (mtgish_grammar-create_tokens.json5) folded the old
 * singular `_Permanent:TheCreatedToken` ref into these plural `_Permanents` derived variables, because a
 * token-creation replacement effect (Doubling Season, "those tokens plus a Squirrel", …) means there is
 * never exactly one created token — only "the tokens created this way". `TheCreatedToken` is kept as a
 * legacy alias so a mixed/older IR snapshot still resolves. All name the same collection, which the token
 * executor publishes under the [CREATED_TOKENS] pipeline slot.
 */
internal val CREATED_TOKEN_REFS = setOf("TheTokensCreatedThisWay", "TheCreatedTokens", "TheCreatedToken")

/** The EffectTarget DSL addressing the just-created token collection. */
internal const val CREATED_TOKENS_TARGET = "EffectTarget.PipelineTarget(CREATED_TOKENS, 0)"

/** [CREATED_TOKENS_TARGET] if [node] is a created-token group/ref sentinel (under `_Permanents` or the
 *  legacy `_Permanent` key), else null — so a "do X to the tokens created this way" action targets the
 *  pipeline collection rather than being mistaken for a battlefield group. */
internal fun createdTokensTarget(node: JsonElement?): String? {
    val o = node as? JsonObject ?: return null
    return if (o.strField("_Permanents") in CREATED_TOKEN_REFS || o.strField("_Permanent") in CREATED_TOKEN_REFS)
        CREATED_TOKENS_TARGET else null
}

/** mtgish refs naming "the permanent a `ManifestDread` action just put onto the battlefield". The
 *  manifest-dread pattern ([Patterns.Library.manifestDread]) stores the manifested creature under this
 *  pipeline collection key, so a follow-up "attach to that creature" addresses that slot. */
internal val MANIFESTED_CREATURE_REFS = setOf("ThePermanentPutOnTheBattlefieldThisWay")

/** The EffectTarget DSL addressing the manifest-dread output collection (Patterns.Library.manifestDread). */
internal const val MANIFESTED_CREATURE_TARGET = "EffectTarget.PipelineTarget(\"manifestDreadManifested\")"

/** [MANIFESTED_CREATURE_TARGET] if [node] is the "permanent put onto the battlefield this way" ref a
 *  ManifestDread action publishes (under `_Permanent`), else null. */
internal fun manifestedCreatureTarget(node: JsonElement?): String? {
    val o = node as? JsonObject ?: return null
    return if (o.strField("_Permanent") in MANIFESTED_CREATURE_REFS) MANIFESTED_CREATURE_TARGET else null
}

// ---------------------------------------------------------------------------
// Dispatch + effect-list assembly.
// ---------------------------------------------------------------------------

/** Render one mtgish action to an Effect [Dsl] node via the [ACTION_HANDLERS] registry. */
internal fun EmitCtx.renderAction(node: JsonObject, tvar: String?): Dsl? =
    ACTION_HANDLERS[node.strField("_Action")]?.invoke(this, node, node["args"], tvar)

/** Render a list of mtgish actions to one Effect ([Composite] if >1). Null if any can't render. */
internal fun EmitCtx.renderEffectList(actions: List<JsonObject>, tvar: String?): Dsl? {
    echoEffect(actions)?.let { return it }
    counterUnlessPaysEffect(actions)?.let { return it }
    counterUnlessPaysWithRiderEffect(actions)?.let { return it }
    mayCostReflexiveDamageEffect(actions)?.let { return it }
    mayCostReflexiveDestroyRoomEffect(actions)?.let { return it }
    erodeDestroyControllerFetchEffect(actions)?.let { return it }
    heatedArgumentExileRiderEffect(actions)?.let { return it }
    manaSculptCounterWizardManaEffect(actions)?.let { return it }
    discardHandThenDrawEffect(actions)?.let { return it }
    discardAnyNumberThenDrawEffect(actions)?.let { return it }
    renderSpeechlessDiscardCountersEffect(actions)?.let { return it }
    runBehindOwnerTopOrBottomEffect(actions, tvar)?.let { return it }
    dinasGuidanceSearchHandOrGraveyardEffect(actions)?.let { return it }
    becomeCreatureTypeEffect(actions, tvar)?.let { return it }
    chooseAbilityGrantEffect(actions, tvar)?.let { return it }
    chooseTypeModifyStatsEffect(actions)?.let { return it }
    chooseCreatureTypeRevealTopEffect(actions)?.let { return it }
    impulseExileTopMayPlay(actions)?.let { return it }
    lookExileCastFree(actions)?.let { return it }
    improvisationExileUntilCastFreeEffect(actions)?.let { return it }
    createValueXReusedEffect(actions, tvar)?.let { return it }
    val rendered = mutableListOf<Dsl>()
    var i = 0
    while (i < actions.size) {
        val act = actions[i]
        // "You may pay <cost>. If you do, <then>." — a MayCost action immediately followed by an
        // If(CostWasPaid, [then]) gate (Pursue the Past's "you may discard a card. If you do, draw two
        // cards"). Collapse the pair into one MayEffect(IfYouDoEffect(action = <cost>, ifYouDo = <then>)),
        // the engine's loot idiom. Only renderable cost/then shapes collapse; anything else falls through
        // to the normal per-action path (which renders MayCost/If, or declines).
        if (act.strField("_Action") == "MayCost") {
            val next = actions.getOrNull(i + 1)
            val collapsed = if (next != null) mayCostIfYouDoEffect(act, next, tvar) else null
            if (collapsed != null) {
                if (collapsed is Composite) rendered.addAll(collapsed.parts) else rendered.add(collapsed)
                i += 2
                continue
            }
        }
        val r = renderAction(act, tvar)
        if (r == null) { reasons.add(act.strField("_Action") ?: act.strField("_Rule") ?: "unknown-action"); return null }
        // Splice a sub-action's own Composite into this sequence rather than nesting it — the engine's
        // `.then()` chain (the hand-authored idiom) is flat, so an action that itself renders a Composite
        // (e.g. a layer effect granting +P/+T and a keyword) would otherwise double-wrap into
        // Composite([Composite([…]), …]) and diverge from golden (High Stride, Shore Up).
        if (r is Composite) rendered.addAll(r.parts) else rendered.add(r)
        i += 1
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
    // Inside a CreateValueX-bound action list, an X reference is the derived bound amount, not the
    // cast-time X — route it through the dynamic path so the bound DynamicAmount inlines. mtgish is
    // inconsistent about which token the consumers use (`ValueX` for Arabella's gain-life arm but the
    // bare `X` symbol for her deal-damage arm), so inline all three spellings here; the dynamic path
    // falls back to cast-time X only when nothing is bound.
    if (boundValueX != null && findInteger(node) == "X") {
        return boundValueX?.let { dynamicAmountExpr(it) } ?: Lit("DynamicAmount.XValue")
    }
    if (boundValueX != null && (node as? JsonObject)?.strField("_GameNumber") == "ValueX") {
        return dynamicAmountExpr(node)
    }
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
        "Integer" -> amountNode["args"].asInt()?.toString()
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

/**
 * The card name behind a "cards named ~ in your graveyard" count filter, or null if the filter
 * carries any constraint beyond the name + You-graveyard scope (so the caller declines rather than
 * over-count). The expected shape is `And(IsNamed(NamedCard "<name>"), InAPlayersGraveyard(You))`:
 * exactly two `_CardsInGraveyard` clauses, one an `IsNamed`, the other an `InAPlayersGraveyard`. Any
 * extra cardtype/subtype/colour clause means the bare-name `GameObjectFilter.Any.named(...)` would be
 * wrong, so we return null.
 */
internal fun namedCardInGraveyardOnly(filterNode: JsonElement?): String? {
    val and = filterNode as? JsonObject ?: return null
    if (and.strField("_CardsInGraveyard") != "And") return null
    val arms = and["args"].asArr?.filterIsInstance<JsonObject>() ?: return null
    if (arms.size != 2) return null
    val namedArm = arms.firstOrNull { it.strField("_CardsInGraveyard") == "IsNamed" } ?: return null
    val scopeArm = arms.firstOrNull { it.strField("_CardsInGraveyard") == "InAPlayersGraveyard" } ?: return null
    if (!jsonContains(scopeArm, "_Player", "You")) return null
    return namedArm.firstArgStringTagged("NamedCard")
}

/** The [Dsl] node behind [dynamicAmount]. Filters it embeds are still carried as [Lit] text (they are
 *  migrated in the filter layer); the structure around them is typed. */
internal fun EmitCtx.dynamicAmountExpr(node: JsonElement?): Dsl? {
    if (node !is JsonObject) return null
    val gn = node.strField("_GameNumber")
    when (gn) {
        "Integer" -> return call("DynamicAmount.Fixed", arg("${node["args"].asInt()}"))
        "XValue", "X" -> return Lit("DynamicAmount.XValue")
        // `ValueX` is the cast-time X by default, but inside a `CreateValueX`-bound action list it
        // refers to the derived amount that CreateValueX computed — inline that bound amount so the
        // reuse renders exactly (Arabella reuses one power<=2 creature count in both damage and life).
        "ValueX" -> return boundValueX?.let { dynamicAmountExpr(it) } ?: Lit("DynamicAmount.XValue")
        // "that much" in a damage trigger — the amount of damage the trigger fired on (Doubtless One's
        // "gain that much life", Thrashing Mudspawn's "lose that much life").
        "Trigger_AmountOfDamageDealt" ->
            return call("DynamicAmount.ContextProperty", arg("ContextPropertyKey.TRIGGER_DAMAGE_AMOUNT"))
        // "the amount of mana spent to cast that spell" on a `WhenAPlayerCastsASpell` trigger
        // (Aberrant Manawurm's "+X/+0 ... where X is the amount of mana spent to cast that spell").
        // Only the triggering-spell subject (`Trigger_ThatSpell`) maps to the context key; any other
        // spell subject declines -> SCAFFOLD rather than misread a different cast's mana.
        "AmountOfManaSpentToCastSpell" ->
            return if (jsonContains(node["args"], "_Spell", "Trigger_ThatSpell"))
                call("DynamicAmount.ContextProperty", arg("ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL"))
            else null
        // Converge — "the number of colors of mana spent to cast it" reading the entering/resolving
        // permanent's own cast (the dominant Converge "Archaic" shape, also Sunburst). Maps to the
        // source-relative facade `DynamicAmounts.colorsOfManaSpent()` (DistinctColorsManaSpent).
        "NumColorsManaSpentToCastEnteringPermanent" ->
            return call("DynamicAmounts.colorsOfManaSpent")
        // "the number of colors of mana spent to cast that spell" on a `WhenAPlayerCastsASpell`
        // trigger (Magmablood Archaic's "+1/+0 ... for each color of mana spent to cast that spell").
        // Only the triggering-spell subject (`Trigger_ThatSpell`) maps to the context key; any other
        // spell subject declines -> SCAFFOLD rather than misread a different cast's colors.
        "NumColorsManaSpentToCastSpell" ->
            return if (jsonContains(node["args"], "_Spell", "Trigger_ThatSpell"))
                call("DynamicAmount.ContextProperty", arg("ContextPropertyKey.COLORS_SPENT_ON_TRIGGERING_SPELL"))
            else null
        // "X" on a `WhenAPlayerCastsASpell(You, HasXInCost)` trigger — the value chosen for {X} on the
        // triggering spell (Geometer's Arthropod's "look at the top X cards of your library"). Reads the
        // triggering-spell X via the context key; the `Trigger_` prefix already scopes it to that spell.
        "Trigger_ValueXOfThatSpell" ->
            return call("DynamicAmounts.xValueOfTriggeringSpell")
        "PowerOfTheSacrificedCreature" -> return call("DynamicAmounts.sacrificedPower")
        // "the number of [filter] cards in your graveyard" (Rise of the Varmints' Varmint count). The
        // count's args are a `_CardsInGraveyard` filter — typically `And(IsCardtype Creature,
        // InAPlayersGraveyard(You))`. Render as a resolution-time `DynamicAmount.Count` over the You
        // graveyard with the recovered filter. Only the You-scoped graveyard renders; any other player
        // scope (an opponent's graveyard, "each player's") declines -> SCAFFOLD rather than miscount.
        "TheNumberOfGraveyardCards" -> {
            if (!jsonContains(node["args"], "_Player", "You")) return null
            // "the number of cards named ~ in your graveyard" (Ancestral Anger): the only constraint
            // beyond the You-graveyard scope is an `IsNamed(NamedCard "<name>")` clause. Render the
            // name as a `CardPredicate.NameEquals` filter. Only this bare named-card shape renders; a
            // named clause combined with any other cardtype/subtype/colour constraint declines, since
            // gameObjectFilterDsl can't carry the name and the combination would silently over-count.
            if ("IsNamed" in compact(node["args"])) {
                val name = namedCardInGraveyardOnly(node["args"]) ?: return null
                return call(
                    "DynamicAmount.Count",
                    arg("Player.You"), arg("Zone.GRAVEYARD"),
                    arg(Lit("GameObjectFilter.Any.named(\"$name\")")),
                )
            }
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
        "PowerOfPermanent" -> {
            // "its power" where "it" is the source: either the static ThisPermanent or — in a dies
            // trigger — Trigger_ThatPermanent (the permanent that died, i.e. this source via LKI).
            // Both read the source's last-known power (Goldvein Hydra, Heartfire Hero).
            if (jsonContains(node["args"], "_Permanent", "ThisPermanent") ||
                jsonContains(node["args"], "_Permanent", "Trigger_ThatPermanent")
            ) return call("DynamicAmounts.sourcePower")
            // "equal to its power" where "it" is a bound target (Burrog Barrage's "deals damage equal to
            // its power" — the buffed target creature). Resolve the ref to its declaration-order index.
            val idx = targetFlatIndexForRef(findRef(node["args"]))
            return if (idx != null) call("DynamicAmounts.targetPower", arg("$idx")) else null
        }
        // "the greatest power among creatures you control" (Tumbleweed Rising's X/X token). The args are a
        // permanent filter — only the exact "creatures you control" (And(IsCardtype Creature,
        // ControlledByAPlayer You)) shape maps to the battlefield MAX-power facade; any other filter (a
        // subtype, an opponent's creatures, "on the battlefield") declines -> SCAFFOLD rather than miscount.
        "TheGreatestPowerAmongPermanents" -> {
            val filterBlob = compact(node["args"])
            val creaturesYouControl = node.firstArgWordTagged("IsCardtype") == "Creature" &&
                "ControlledByAPlayer" in filterBlob && jsonContains(node["args"], "_Player", "You") &&
                "IsCreatureType" !in filterBlob && "_Color" !in filterBlob && "\"Other\"" !in filterBlob &&
                "Opponent" !in filterBlob
            return if (creaturesYouControl) {
                call("DynamicAmounts.battlefield", arg("Player.You"), arg("GameObjectFilter.Creature")).dot("maxPower")
            } else null
        }
        // "the number of counters on this creature" (Twitching Doll's "a Spider token for each counter
        // on this creature"). When no counter kind is named the count spans ALL counter kinds. The
        // source-relative reader depends on whether the enclosing ability sacrifices/exiles the source:
        // when it does (Twitching Doll's "{T}, Sacrifice this creature"), the cost wipes the counters
        // before the effect resolves, so the count must read the pre-cost snapshot via
        // DynamicAmounts.lastKnownSourceCounters; otherwise the source is still present and
        // DynamicAmounts.countersOnSelf reads the live count. Only the ThisPermanent subject with no
        // named counter type renders; a specific counter kind or any other permanent subject declines
        // (-> SCAFFOLD) rather than miscount.
        "NumCountersOnPermanent" -> {
            if (!jsonContains(node["args"], "_Permanent", "ThisPermanent") ||
                "_CounterType" in compact(node["args"])
            ) return null
            val reader = if (sourceSacrificedByCost) "DynamicAmounts.lastKnownSourceCounters"
                         else "DynamicAmounts.countersOnSelf"
            return call(reader, arg("CounterTypeFilter.Any"))
        }
        "LifeTotalOfPlayer" -> {
            val player = if (jsonContains(node, "_Player", "Opponent")) "Player.EachOpponent" else "Player.You"
            return call("DynamicAmount.LifeTotal", arg(player))
        }
        // "the number of cards you've drawn this turn" (Fractal Anomaly, Duelist of the Mind). Backed by
        // the CARDS_DRAWN turn tracker. Only the You scope maps to the facade; an opponent / each-player
        // scope declines (-> SCAFFOLD) rather than read the wrong player's draw count.
        "NumCardsDrawnByPlayerThisTurn" ->
            return if (jsonContains(node["args"], "_Player", "You"))
                call("DynamicAmount.TurnTracking", arg("Player.You"), arg("TurnTracker.CARDS_DRAWN"))
            else null
        // "the number of [type] spells <player> has cast this turn" (Magebane Lizard). args = a spell
        // filter + a player ref. Both must map to an EXACT category/scope we can render — decline
        // (-> SCAFFOLD) on any other spell filter or player scope rather than under-counting.
        "NumSpellsCastByPlayerThisTurn" -> {
            val argv = node["args"].asArr ?: return null
            val spellsNode = argv.firstOrNull { (it as? JsonObject)?.containsKey("_Spells") == true } as? JsonObject
            val playerNode = argv.firstOrNull { (it as? JsonObject)?.containsKey("_Player") == true } as? JsonObject
            // "the number of spells you've cast this turn other than the first" (Outlaw Stitcher) —
            // mtgish models the "other than the first" exclusion as `Other(TheNthSpellCastByPlayerThisTurn(
            // 1, AnySpell, <player>))`. The engine has no "Nth-spell" exclusion, so render it as the
            // arithmetic equivalent: max(spellsCastThisTurn - 1, 0). Only the You-scoped, any-spell,
            // exclude-the-FIRST shape collapses; any other Nth / filter / scope declines (-> SCAFFOLD).
            spellsCastExcludingFirstAmount(spellsNode, playerNode)?.let { return it }
            val filter = spellsCastThisTurnFilter(spellsNode) ?: return null
            val player = spellsCastThisTurnPlayer(playerNode) ?: return null
            return call("DynamicAmount.SpellsCastThisTurn", arg(player), arg(filter))
        }
        // "the number of other spells you've cast this turn" (Thunder Salvo). The single `_Spells` arg is
        // an `And` of cast-history clauses: `Other(ThisSpell)` -> excludeSelf, `CastByPlayer(<player>)`
        // -> the scope, and an optional cardtype/noncreature clause -> the spell filter. Any other clause
        // (or a non-And single clause we don't model) declines (-> SCAFFOLD) rather than miscount.
        "NumSpellsCastThisTurn" -> return spellsCastThisTurnAmount(node["args"] as? JsonObject)
        // "the number of permanents you sacrificed this turn" — NumberOfPermanentsSacrificedByPlayerThisTurn(
        // [IsPermanent], You) -> DynamicAmounts.permanentsSacrificedThisTurn() ("that much damage", Sawblade
        // Skinripper). Only the You scope over a *bare* IsPermanent filter (any permanent, no narrower
        // type/subtype clause) renders — the per-player tracker is any-permanent, so a narrower count would
        // be wrong; decline -> SCAFFOLD.
        "NumberOfPermanentsSacrificedByPlayerThisTurn" -> {
            val arr = node["args"].asArr
            val filt = arr?.firstOrNull { (it as? JsonObject)?.strField("_Permanents") != null } as? JsonObject
            val bareAnyPermanent = filt?.strField("_Permanents") == "IsPermanent" && filt.size == 1
            return if (bareAnyPermanent && jsonContains(node["args"], "_Player", "You"))
                call("DynamicAmounts.permanentsSacrificedThisTurn")
            else null
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
    if (gn == "TheNumberOfCardtypesAmongGraveyardCards") {
        val player = when {
            jsonContains(node, "_Player", "You") || jsonContains(node, "_Players", "You") -> "Player.You"
            jsonContains(node, "_Player", "Opponent") || jsonContains(node, "_Players", "Opponent") -> "Player.EachOpponent"
            else -> return null
        }
        return call(
            "DynamicAmount.AggregateZone",
            arg(player), arg("Zone.GRAVEYARD"), arg("GameObjectFilter.Any"), arg("Aggregation.DISTINCT_TYPES"),
        )
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
        // "for each creature blocking it" — an IsBlockingAttacker combat-relationship predicate scoped to
        // the triggering attacker (Elvish Berserker's "+1/+1 for each creature blocking it"). A flat
        // battlefield aggregate has no notion of "blocking <that creature>"; the search filter would
        // silently drop the predicate and tally EVERY creature on the battlefield. Decline -> SCAFFOLD
        // rather than misrender.
        if ("IsBlockingAttacker" in compact(node)) return null
        // "for each creature that crewed it this turn" — a CrewedVehicleThisTurn predicate scoped to the
        // source Vehicle (Luxurious Locomotive). This is a per-source set-tracker count, not a flat
        // battlefield aggregate; the search filter below would silently drop the CrewedVehicleThisTurn
        // clause and over-count every creature. Render the dedicated source-relative DynamicAmount instead
        // (which retains contributors that have since left, matching the ruling). Only the source-Vehicle
        // subject (Trigger_ThatCreature / ThisPermanent) is recognised; any other subject declines.
        if ("CrewedVehicleThisTurn" in compact(node)) {
            val subj = compact(node)
            return if ("Trigger_ThatCreature" in subj || "ThisPermanent" in subj)
                Lit("DynamicAmount.CreaturesThatCrewedOrSaddledThisTurn")
            else null
        }
        val oracle = oracleText?.lowercase() ?: ""
        // The count filter is recovered by landSearchFilterExpr, which renders only type/subtype/color/land
        // shapes — it silently widens anything else (its `else` arm is GameObjectFilter.Any) and its
        // oracle-text fallbacks can misfire on a clause that belongs to the card's TARGET rather than its
        // count. Decline (-> SCAFFOLD) for count filters carrying predicates that path can't faithfully
        // render:
        //   IsArtifactType  — "number of Equipment you control" (Armed Response): no artifact-subtype
        //     rendering, and the oracle fallback wrongly latches onto the card's "attacking creature" TARGET.
        //   a tapped/untapped LAND — only "tapped creature" is recovered, via the oracle text (Theft of
        //     Dreams, correctly); "tapped land" (Mana Geyser) would silently drop the restriction.
        val countBlob = compact(node)
        // "for each of those creatures" — a `HadCountersPutOnItThisWay` permanent set scopes to the
        // creatures THIS resolution just put counters on (Bounding Felidar's "gain 1 life for each of
        // those creatures"), not a current battlefield tally. The land/type search filter can't express
        // it and would silently widen to GameObjectFilter.Any (every permanent). Decline -> SCAFFOLD.
        if ("HadCountersPutOnItThisWay" in countBlob) return null
        if ("IsArtifactType" in countBlob) return null
        if (("IsTapped" in countBlob && "tapped creature" !in oracle) || "IsUntapped" in countBlob) return null
        //   IsNamed — "the number of creatures named ~ on the battlefield" (Plague Rats): the search
        //     filter can't express a name predicate and would silently widen to every creature.
        if ("IsNamed" in countBlob) return null
        //   IsNonCreatureType — "non-Wall creatures you control" (Keldon Warlord): a negated creature-type
        //     exclusion the search filter drops, over-counting. Decline rather than miscount.
        if ("IsNonCreatureType" in countBlob) return null
        //   Trigger_ThatPlayer controller — "...Swamps they control" in a per-player upkeep (Karma): the
        //     count scopes to the triggering player, but an aggregate has no triggering-player scope here,
        //     so the resolver below would wrongly tally the source controller's (You) permanents.
        if (jsonContains(node, "_Player", "Trigger_ThatPlayer")) return null
        // The hand/"in it" guard catches a generic "NumberOf" count that's really about hand cards. It must
        // NOT fire for an explicit battlefield count (TheNumberOfPermanentsOnTheBattlefield) — a card may
        // mention "hand" elsewhere in its text (e.g. Slate of Ancestry's "Discard your hand" cost) while the
        // count itself is unambiguously a battlefield tally.
        val battlefieldCount = gn == "TheNumberOfPermanentsOnTheBattlefield"
        if (!battlefieldCount && (" hand" in oracle || " in it" in oracle)) return null
        val player = when {
            "attacking you" in oracle -> "Player.EachOpponent"
            "on the battlefield" in oracle -> "Player.Each"
            "target opponent controls" in oracle || jsonContains(node, "_Player", "Ref_TargetOpponent") -> "Player.TargetOpponent"
            "target player controls" in oracle || jsonContains(node, "_Player", "Ref_TargetPlayer") -> "Player.TargetPlayer"
            // A plain "an opponent controls" controller clause is `ControlledByAPlayer{_Players: Opponent}`
            // — the key is plural `_Players`, so the singular `_Player` probe above misses it and the count
            // would wrongly default to Player.You (Pygmy Kavu's "each black creature your opponents control").
            jsonContains(node, "_Player", "Opponent") || jsonContains(node, "_Players", "Opponent") -> "Player.EachOpponent"
            // An explicit "you control" controller predicate (Fire Dragon's "Mountains you control").
            "ControlledByAPlayer" in compact(node) -> "Player.You"
            // A global battlefield tally with NO controller predicate ("the number of attacking
            // creatures", Divine Retribution) counts every player's permanents — Each, not the You
            // default the controller-scoped NumberOf forms want.
            battlefieldCount -> "Player.Each"
            else -> "Player.You"
        }
        // "for each Goblin/Bird/Elf on the battlefield": a creature subtype, which the land-oriented
        // search filter misses; otherwise fall back to the land/type search filter. Only a SINGLE creature
        // subtype is modeled — "Wolves and Werewolves you control" (Runebound Wolf) carries two, and
        // rendering only the first under-counts, so decline rather than drop a subtype.
        if (node.argWordsTagged("IsCreatureType").size > 1) return null
        val subtype = node.firstArgWordTagged("IsCreatureType")
        // "for each Shrine you control" — an enchantment subtype the land/type search filter can't express;
        // it would silently widen the count's filter to GameObjectFilter.Any and over-count every permanent.
        // Decline (-> SCAFFOLD) rather than misrender (The Spirit Oasis's "draw a card for each Shrine").
        if (subtype == null && node.firstArgWordTagged("IsEnchantmentType") != null) return null
        // A bare "number of LANDS [you control]" battlefield tally (Dance of the Tumbleweeds) is an
        // explicit `IsCardtype Land` with no land subtype or basic-land signal. Render
        // `GameObjectFilter.Land` directly: `landSearchFilterExpr` is search-oriented and its
        // "basic land" oracle fallback would narrow a *lands* count to basics whenever the card's text
        // also mentions a basic-land search elsewhere (Dance's mode 0). Land subtypes / basic-land
        // signals still route through the search helper below.
        val bareLandCount = subtype == null && "\"Land\"" in countBlob &&
            "IsLandType" !in countBlob && "IsBasicLand" !in countBlob && "IsSupertype" !in countBlob
        var filter = when {
            subtype != null -> Lit("GameObjectFilter.Creature").dot("withSubtype", arg("\"$subtype\""))
            bareLandCount -> Lit("GameObjectFilter.Land")
            else -> landSearchFilterExpr(node)
        }
        // "untapped Mountains you control" (Ben-Ben, Akki Hermit): an IsUntapped predicate on the counted
        // group. Compose .untapped() so the tally isn't silently widened to include tapped permanents (the
        // land/type filter above has no untapped path). The tapped case is already handled inside
        // landSearchFilterExpr via the oracle "tapped creature" cue (Theft of Dreams), so don't re-apply it.
        if ("IsUntapped" in compact(node)) filter = filter.dot("untapped")
        // "creatures you control with power 2 or less" — a `PowerIs <= N` clause on the counted group
        // (Arabella, Abandoned Doll). landSearchFilterExpr doesn't carry power bounds, so compose
        // `.powerAtMost(N)` here. If a `PowerIs` clause is present but it isn't the `<=` form we can
        // compose (e.g. `>=`, or `>` ranges), decline (-> SCAFFOLD) rather than silently drop it and
        // over-count.
        if ("PowerIs" in countBlob) {
            val link = FilterPredicates.powerAtMost(node) ?: return null
            filter = filter.dot(link)
        }
        return call("DynamicAmount.AggregateBattlefield", arg(player), arg(filter))
    }
    return null
}

/**
 * A `DynamicAmount.SpellsCastThisTurn(player, filter, excludeSelf)` for a [NumSpellsCastThisTurn] count
 * whose single `_Spells` arg is an `And` of cast-history clauses (Thunder Salvo's "other spells you've
 * cast this turn"). The recognised clauses are:
 *  - `Other(ThisSpell)`        -> `excludeSelf = true` (the resolving spell isn't itself counted)
 *  - `CastByPlayer(<player>)`  -> the player scope (You / Opponent)
 *  - an optional `IsCardtype Creature` / `IsNonCardtype Creature` -> the spell filter
 * Any other clause, a missing `CastByPlayer`, or a player/filter we don't render declines (-> SCAFFOLD)
 * rather than miscount.
 */
internal fun EmitCtx.spellsCastThisTurnAmount(spells: JsonObject?): Dsl? {
    if (spells == null) return null
    val clauses: List<JsonObject> = when (spells.strField("_Spells")) {
        "And" -> spells["args"].asArr?.filterIsInstance<JsonObject>() ?: return null
        else -> listOf(spells)
    }
    var excludeSelf = false
    var player: String? = null
    var filter = "GameObjectFilter.Any"
    for (clause in clauses) {
        when (clause.strField("_Spells")) {
            "Other" -> if (jsonContains(clause["args"], "_Spell", "ThisSpell")) excludeSelf = true else return null
            "CastByPlayer" -> player = spellsCastThisTurnPlayer(clause["args"] as? JsonObject) ?: return null
            "IsCardtype" -> filter = if (clause.field("args").asStr() == "Creature") "GameObjectFilter.Creature" else return null
            "IsNonCardtype" -> filter = if (clause.field("args").asStr() == "Creature") "GameObjectFilter.Noncreature" else return null
            "AnySpell" -> {}
            else -> return null
        }
    }
    val scope = player ?: return null
    val args = mutableListOf(arg(scope), arg(filter))
    if (excludeSelf) args.add(arg("excludeSelf", "true"))
    return Call("DynamicAmount.SpellsCastThisTurn", args)
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

/**
 * "the number of spells <player> has cast this turn other than the first" -> a
 * `DynamicAmount.Max(DynamicAmount.Subtract(DynamicAmount.SpellsCastThisTurn(<player>), Fixed(1)),
 * Fixed(0))` expression (Outlaw Stitcher), or null when the shape isn't exactly
 * `Other(TheNthSpellCastByPlayerThisTurn(1, AnySpell, <player>))`.
 *
 * mtgish has no "all spells except the Nth" count, so it nests the exclusion as an `Other` of the
 * first (Nth = 1) cast. The engine's `SpellsCastThisTurn` counts ALL casts (it has no Nth concept),
 * so "all but the first" is `total - 1`, clamped at 0 (a turn with zero or one spell yields 0). Only
 * the exclude-the-FIRST, any-spell shape with a [spellsCastThisTurnPlayer]-renderable scope collapses;
 * excluding any other ordinal, a filtered spell set, or a mismatched player declines (-> SCAFFOLD).
 */
private fun EmitCtx.spellsCastExcludingFirstAmount(spells: JsonObject?, playerNode: JsonObject?): Dsl? {
    if (spells?.strField("_Spells") != "Other") return null
    val nth = spells["args"] as? JsonObject ?: return null
    if (nth.strField("_Spell") != "TheNthSpellCastByPlayerThisTurn") return null
    val nthArgs = nth["args"].asArr ?: return null
    // [ordinal, spell filter, player]. The excluded ordinal must be the FIRST (1) and the set AnySpell.
    val ordinal = (nthArgs.getOrNull(0) as? JsonObject)
        ?.takeIf { it.strField("_GameNumber") == "Integer" }?.get("args").asInt()
    if (ordinal != 1) return null
    if ((nthArgs.getOrNull(1) as? JsonObject)?.strField("_Spells") != "AnySpell") return null
    // The Nth-spell's caster and the outer count's player must be the same renderable scope.
    val innerPlayer = nthArgs.getOrNull(2) as? JsonObject
    val player = spellsCastThisTurnPlayer(playerNode) ?: return null
    if (spellsCastThisTurnPlayer(innerPlayer) != player) return null
    return call(
        "DynamicAmount.Max",
        arg(call(
            "DynamicAmount.Subtract",
            arg(call("DynamicAmount.SpellsCastThisTurn", arg(player), arg("GameObjectFilter.Any"))),
            arg(call("DynamicAmount.Fixed", arg("1"))),
        )),
        arg(call("DynamicAmount.Fixed", arg("0"))),
    )
}

/** The `Player.*` scope for a [NumSpellsCastByPlayerThisTurn] player ref, or null for a scope we don't
 *  render. "that player" in a cast trigger is the triggering caster; an explicit You/Opponent map too. */
private fun spellsCastThisTurnPlayer(player: JsonObject?): String? = when (player?.strField("_Player")) {
    "Trigger_ThatPlayer" -> "Player.TriggeringPlayer"
    "You" -> "Player.You"
    "Opponent" -> "Player.EachOpponent"
    else -> null
}

/** Resolve an action's subject ref to an EffectTarget DSL (the bound `tvar`, or EffectTarget.Self). */
internal fun EmitCtx.refTarget(args: JsonElement?, tvar: String?): String? {
    val ref = findRef(args)
    return refTargetFromRef(ref, tvar)
}

/** The flat declaration-order index of a bound-target ref (its position in [targetVars]) — the index
 *  `DynamicAmounts.targetPower(n)` / `EffectTarget.ContextTarget(n)` use. Returns null for a ref that
 *  isn't a bound target (-> the caller scaffolds). In a single-target spell the only target is index 0. */
internal fun EmitCtx.targetFlatIndexForRef(ref: String?): Int? {
    if (ref == null) return null
    val resolved = refTargetFromRef(ref, targetVars.firstOrNull()) ?: return null
    if (targetVars.isEmpty()) return 0  // single-target spell: the lone bound target is index 0
    val idx = targetVars.indexOf(resolved)
    return if (idx >= 0) idx else null
}

/** Resolve the ref under a marked subtree, such as `_DamageRecipient`, to an EffectTarget DSL. */
internal fun EmitCtx.refTargetIn(args: JsonElement?, markerKey: String, tvar: String?): String? {
    return refTargetFromRef(findRefIn(args, markerKey), tvar)
}

internal fun EmitCtx.refTargetFromRef(ref: String?, tvar: String?): String? {
    // A suffixed multi-target ref (Ref_TargetPermanent1 / Ref_TargetPermanent2 / …) indexes the
    // per-KIND ordered list of target locals (1-based). The IR ordinal counts only targets of that
    // kind, so it must not index the flat target list — that would skew when an earlier slot is a
    // player (Dissection Practice: t1 is the opponent, so Ref_TargetPermanent1 is t2, not t1).
    // The same suffixing applies to graveyard-card refs (Ref_TargetGraveyardCard1 /
    // Ref_TargetGraveyardCard2) on spells with two graveyard targets (Badlands Revival's two
    // "up to one target … card from your graveyard" slots); each action must bind to its own slot.
    if (ref != null && targetVars.isNotEmpty()) {
        Regex("^(Ref_TargetPermanent|Ref_TargetGraveyardCard)(\\d+)$").matchEntire(ref)?.let { m ->
            val kind = m.groupValues[1]
            val idx = m.groupValues[2].toInt() - 1
            return targetRefVarsByKind[kind]?.getOrNull(idx)
                ?: targetVars.getOrNull(idx)
        }
    }
    if (ref in setOf("Ref_TargetPermanent", "Ref_TargetPlayer", "Ref_TargetGraveyardCard")) {
        if (targetVars.isEmpty()) return tvar
        // Multi-target spell: an unsuffixed ref resolves to the single target of its kind. When the IR
        // mixes an unsuffixed ref with suffixed siblings of the SAME kind (Burrog Barrage's +1/+0 on
        // `Ref_TargetPermanent` alongside the damage's `Ref_TargetPermanent1`/`2`), the unsuffixed ref
        // means the first target of that kind. A genuinely ambiguous case (no first-of-kind) declines.
        return targetRefVars[ref] ?: targetRefVarsByKind[ref]?.firstOrNull()
    }
    if (ref in SELF_REFS) return "EffectTarget.Self"
    // "the tokens created this way" / "the created tokens" — the tokens a preceding CreateTokens action
    // in this same action list just made (Fractal Tender: "create a 0/0 Fractal … and put three +1/+1
    // counters on it"). The token executor publishes their ids under the CREATED_TOKENS pipeline
    // collection, so a follow-up effect addresses them via PipelineTarget(CREATED_TOKENS, 0). See
    // [CREATED_TOKEN_REFS] for why these are plural — mtgish's token cleanup folded the old singular
    // `TheCreatedToken` ref into the plural derived variables.
    if (ref in CREATED_TOKEN_REFS) return CREATED_TOKENS_TARGET
    // "that player" in a trigger. In a spell-cast trigger the triggering entity is the spell, so "that
    // player" is its controller — the caster — modeled as ControllerOfTriggeringEntity (Magebane Lizard).
    // In every other trigger ("the player ~ dealt combat damage to") it's the triggering player itself.
    if (ref == "Trigger_ThatPlayer") return if (triggeringEntityIsSpell)
        "EffectTarget.ControllerOfTriggeringEntity"
    else "EffectTarget.PlayerRef(Player.TriggeringPlayer)"
    // A plain player reference (no target) — the controller / "you" or an opponent. The pain-land idiom
    // "{T}: Add {C}. This land deals N damage to you" carries a `_DamageRecipient: Player{You}` recipient
    // that is the controller, not a chosen target (Adarkar Wastes, Caldera Lake, Ancient Tomb).
    if (ref == "You") return "EffectTarget.PlayerRef(Player.You)"
    // A bare non-targeted "Opponent" recipient has no single multiplayer meaning
    // (defending player / each opponent / a chosen one) — decline -> SCAFFOLD.
    if (ref == "Opponent") return null
    return tvar
}

internal fun EmitCtx.keywordOf(node: JsonElement?): String? {
    for (m in Regex("\"(\\w+)\"").findAll(compact(node))) {
        // A keyword pinned `unsupported` in the bridge (an engine-inert enum member, e.g. Intimidate)
        // must not render — granting it would be a silent no-op the probe deliberately blocks.
        if (Bridge[m.groupValues[1]] is MappingEntry.Unsupported) continue
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
 *  - `PlayerPassesFilter(You, NumCardsInHandIs(EqualTo 0))` ("if you have no cards in hand") ->
 *    `Conditions.EmptyHand` (Plan the Heist: "Surveil 3 if you have no cards in hand.").
 */
internal fun EmitCtx.actionConditionDsl(cond: JsonObject?): String? {
    if (cond == null) return null
    if (cond.strField("_Condition") == "PlayerPassesFilter") {
        val args = cond["args"].asArr
        if (args != null && (args.getOrNull(0) as? JsonObject)?.strField("_Player") == "You") {
            val controls = args.getOrNull(1) as? JsonObject
            if (controls?.strField("_Players") == "ControlsA") {
                // "you control ANOTHER <filter>" (Splitskin Doll's "another creature with power 2 or
                // less"): the filter's `And` carries an `Other(<self>)` clause. The flat GameObjectFilter
                // can't carry excludeSelf, so peel that clause off and render the remaining filter under
                // Conditions.YouControl(filter, excludeSelf = true). Without this the "another" restriction
                // would be silently dropped.
                val (innerFilter, excludeSelf) = stripSelfOtherClause(controls["args"])
                val filter = gameObjectFilterDsl(innerFilter)
                if (filter != null) {
                    return render(
                        if (excludeSelf)
                            call("Conditions.YouControl", arg(Lit(filter)), arg("excludeSelf", "true"))
                        else call("Conditions.YouControl", arg(Lit(filter)))
                    )
                }
            }
            // "if you control no <filter>" -> Conditions.YouControl(<filter>, negate = true)
            // (Glimmer Seeker: "If you don't control a Glimmer creature, create a … token.").
            if (controls?.strField("_Players") == "ControlsNo") {
                val filter = gameObjectFilterDsl(controls["args"])
                if (filter != null) return render(call(
                    "Conditions.YouControl", arg(Lit(filter)), arg("negate", "true")
                ))
            }
            // "if you have no cards in hand" -> Conditions.EmptyHand. Only the exact
            // `NumCardsInHandIs(EqualTo 0)` shape renders; any other comparator/count declines.
            if (controls?.strField("_Players") == "NumCardsInHandIs") {
                val cmp = controls["args"] as? JsonObject
                if (cmp?.strField("_Comparison") == "EqualTo") {
                    val n = cmp["args"].asInt() ?: ((cmp["args"] as? JsonObject)?.get("args").asInt())
                    if (n == 0) return "Conditions.EmptyHand"
                }
            }
            // Delirium — "if there are [N] or more card types among cards in your graveyard"
            // (`NumCardTypesInGraveyardIs(GreaterThanOrEqualTo N)`) -> Conditions.Delirium(N).
            // The printed threshold is always four (Impossible Inferno), but the count is read
            // from the IR so any GTE-N variant renders. Only the GTE comparator renders; any other
            // comparator declines (-> SCAFFOLD) rather than misread the gate.
            if (controls?.strField("_Players") == "NumCardTypesInGraveyardIs") {
                val cmp = controls["args"] as? JsonObject
                if (cmp?.strField("_Comparison") == "GreaterThanOrEqualTo") {
                    val n = cmp["args"].asInt() ?: ((cmp["args"] as? JsonObject)?.get("args").asInt())
                    if (n != null) return "Conditions.Delirium($n)"
                }
            }
        }
    }
    // "if this spell was cast from anywhere other than your hand" (Antiquities on the Loose) — a
    // resolution-time gate on the resolving spell's cast-origin zone. The IR is
    // `CastSpellPassesFilter(WasntCastFromTheirHand)`: cast from any zone but the caster's hand, which
    // is exactly `Not(WasCastFromHand)` (true for a flashback/graveyard cast, false for a normal hand
    // cast). Only this exact spell-origin filter renders; any other CastSpellPassesFilter shape
    // declines (-> SCAFFOLD).
    if (cond.strField("_Condition") == "CastSpellPassesFilter") {
        if (cond["args"].strField("_Spells") == "WasntCastFromTheirHand") {
            return render(call("Conditions.Not", arg("Conditions.WasCastFromHand")))
        }
        return null
    }
    // "If [N] or more mana was spent to cast that spell, …" — the Opus 5+ mana tier
    // (Elemental Mascot, Expressive Firedancer, Muse Seeker). Renders a Compare over the triggering
    // spell's mana. See [spellManaSpentConditionDsl] for the exact shape constraints.
    spellManaSpentConditionDsl(cond)?.let { return it }
    // Other resolution-time intervening-if shapes ("if you gained life this turn", "if you've cast
    // another instant or sorcery this turn", …) reuse the shared condition renderer; declining beats
    // widening. These power the resolution-time `on("If")` action handler.
    return interveningIfDsl(cond)
}

/**
 * Peel a self-`Other` clause off a permanent filter. A filter for "another <thing>" carries an
 * `Other(<self ref>)` clause (e.g. `And(Other(ThatEnteringPermanent), IsCardtype Creature, …)`) the
 * flat GameObjectFilter can't represent. Returns the filter with that clause removed and `true` when an
 * `And` contained exactly one self-`Other` clause (so the caller can render `excludeSelf = true`);
 * otherwise returns the filter unchanged with `false`. Any non-self `Other` (e.g. `Other(You)`, handled
 * elsewhere as a controller predicate) is left in place so it isn't misread as excludeSelf.
 */
internal fun stripSelfOtherClause(filterNode: JsonElement?): Pair<JsonElement?, Boolean> {
    val obj = filterNode as? JsonObject ?: return filterNode to false
    if (obj.strField("_Permanents") != "And") return filterNode to false
    val arms = obj["args"].asArr ?: return filterNode to false
    fun isSelfOther(arm: JsonElement?): Boolean {
        val a = arm as? JsonObject ?: return false
        return a.strField("_Permanents") == "Other" &&
            (a["args"] as? JsonObject)?.strField("_Permanent") in SELF_REFS
    }
    if (arms.none { isSelfOther(it) }) return filterNode to false
    val remaining = arms.filterNot { isSelfOther(it) }
    val rebuilt = buildJsonObject {
        put("_Permanents", JsonPrimitive("And"))
        put("args", buildJsonArray { remaining.forEach { add(it) } })
    }
    return rebuilt to true
}

/**
 * "[N] or more mana was spent to cast that spell" — a `SpellPassesFilter(Trigger_ThatSpell,
 * AnAmountOfManaWasSpentToCastIt{GreaterThanOrEqualTo Integer N})` gate on a spell-cast trigger's
 * triggering spell (the Opus 5+ mana tier). Renders the `Compare(ContextProperty(
 * MANA_SPENT_ON_TRIGGERING_SPELL), GTE, Fixed(N))` condition DSL, or null (-> SCAFFOLD) for any other
 * spell subject / comparator so the threshold is never misread.
 */
internal fun spellManaSpentConditionDsl(cond: JsonObject?): String? {
    if (cond?.strField("_Condition") != "SpellPassesFilter") return null
    val cargs = cond["args"].asArr ?: return null
    if ((cargs.getOrNull(0) as? JsonObject)?.strField("_Spell") != "Trigger_ThatSpell") return null
    val spellFilter = cargs.getOrNull(1) as? JsonObject ?: return null
    if (spellFilter.strField("_Spells") != "AnAmountOfManaWasSpentToCastIt") return null
    val cmp = spellFilter["args"] as? JsonObject ?: return null
    if (cmp.strField("_Comparison") != "GreaterThanOrEqualTo") return null
    val n = cmp["args"].asInt() ?: ((cmp["args"] as? JsonObject)?.get("args").asInt()) ?: return null
    return "Compare(DynamicAmount.ContextProperty(ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL), " +
        "ComparisonOperator.GTE, DynamicAmount.Fixed($n))"
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
    if (kind == "PayMana") {
        // "unless you pay {cost}" carries an EXPLICIT mana cost in the IR ({B}{B}{B}{B} for Kuro,
        // Pitlord). That is rarely the card's own printed cost, so render the literal symbols rather
        // than collapsing to OwnManaCost (which resolves against the source's printed cost). Decline
        // if any symbol is unrecognised so we scaffold instead of emitting a wrong cost.
        val mana = renderMana(costNode["args"])
        if (mana.isBlank() || "{?}" in mana) return null
        return "Costs.pay.Mana(\"$mana\")"
    }
    if ("Mana" in blob) {
        // A specific printed mana cost — an explicit `_ManaSymbol` list, e.g. Drifting Djinn's
        // "...unless you pay {1}{U}" or Phantasmal Forces' {U} — is NOT the permanent's own mana cost,
        // so OwnManaCost mis-charges it. There is no audited rendering for an arbitrary alternative mana
        // payment in this gate, so decline (-> SCAFFOLD) rather than emit the wrong cost. A genuine
        // "pay its mana cost" carries no mana symbols and still renders as OwnManaCost.
        if ("_ManaSymbol" in blob) return null
        return "Costs.pay.OwnManaCost"
    }
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
 * [ChooseAnAbility([kw1, kw2, …]), CreatePermanentLayerEffectUntil(Ref_TargetPermanent,
 * [AddAbilityVariable(TheChosenAbility)], UntilEndOfTurn)] -> a single
 * `ModalEffect.chooseOne(Mode.noTarget(GrantKeywordEffect(Keyword.X, <target>, Duration.EndOfTurn),
 * "X"), …)` ("target … gains your choice of <kw1> or <kw2> until end of turn", Rattleback Apothecary).
 *
 * mtgish models "your choice of keyword X or Y" as a `ChooseAnAbility` action listing the keyword
 * options, then a single layer effect that grants `TheChosenAbility`. The engine has no "chosen
 * ability variable", so the choice is reified as a `ModalEffect.chooseOne` over one
 * [GrantKeywordEffect] per option — each granting its keyword to the same target for end of turn. The
 * mode label is the option's display name (the IR `_Rule` string, e.g. "Menace"), matching the engine's
 * `Keyword.displayName`.
 *
 * Renders only the exact shape: every option a bare keyword the SDK knows, the layer effect a lone
 * `AddAbilityVariable(TheChosenAbility)` collapsing at end of turn, on a recoverable target. Anything
 * else (a non-keyword option, a riding +P/T, a non-EOT window, a richer layer-effect list) declines
 * (null -> SCAFFOLD) rather than emit a lossy grant.
 */
internal fun EmitCtx.chooseAbilityGrantEffect(actions: List<JsonObject>, tvar: String?): Dsl? {
    val chooser = actions.firstOrNull { it.strField("_Action") == "ChooseAnAbility" } ?: return null
    val layer = actions.firstOrNull {
        it.strField("_Action") in setOf("CreatePermanentLayerEffectUntil", "CreateEachPermanentLayerEffectUntil")
    } ?: return null
    // The pair must be the WHOLE effect — a third action would be dropped.
    if (actions.size != 2) return null
    // Layer effect: exactly the chosen-ability grant, collapsing at end of turn.
    if (!jsonContains(layer, "_LayerEffect", "AddAbilityVariable")) return null
    if (!jsonContains(layer, "_AbilityVariable", "TheChosenAbility")) return null
    if (jsonContains(layer, "_LayerEffect", "AdjustPT")) return null
    if (!jsonContains(layer, "_Expiration", "UntilEndOfTurn")) return null
    val layerEffects = (layer["args"].asArr)?.getOrNull(1) as? JsonArray ?: return null
    if (layerEffects.size != 1) return null  // a lone chosen-ability grant; a riding effect declines
    val target = refTarget(layer["args"], tvar) ?: return null
    // The options: each a bare keyword rule the SDK knows. A non-keyword or parameterized option declines.
    val options = chooser["args"].asArr?.filterIsInstance<JsonObject>() ?: return null
    if (options.size < 2) return null
    val modes = options.map { opt ->
        if (opt["args"] != null) return null  // a parameterized ability, not a bare keyword
        val rule = opt.strField("_Rule") ?: return null
        val kw = pascalToUpperSnake(rule).takeIf { it in keywords } ?: return null
        call(
            "Mode.noTarget",
            arg(call(
                "GrantKeywordEffect",
                arg("Keyword.$kw"),
                arg(Lit(target)),
                arg("Duration.EndOfTurn"),
            )),
            arg(Lit("\"${ktStr(rule)}\"")),
        )
    }
    return Call("ModalEffect.chooseOne", modes.map { arg(it) })
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

/**
 * [ExileTopCardOfLibrary, CreatePlayerEffectUntil(You, [MayPlayExiledCard(TheCardExiledThisWay)],
 * UntilEndOfNextTurn)] -> the impulse-draw composite (Alania's Pathmaker, Gila Courser): exile the
 * top card of your library into exile, then grant "you may play that card until the end of your next
 * turn". Renders the foundational gather → move → grant pipeline:
 *
 *   GatherCardsEffect(CardSource.TopOfLibrary(DynamicAmount.Fixed(1)), storeAs = "impulseExiled")
 *   MoveCollectionEffect(from = "impulseExiled", destination = CardDestination.ToZone(Zone.EXILE))
 *   GrantMayPlayFromExileEffect("impulseExiled", MayPlayExpiry.UntilEndOfNextTurn)
 *
 * Only this exact two-action shape with the "until end of your next turn" window collapses; any
 * other player effect, expiration, or extra rider declines (null -> SCAFFOLD) rather than guess.
 */
/**
 * "Exile the top card of your library" — recognises both IR encodings of the same action:
 *  - the dedicated `ExileTopCardOfLibrary` action (the common shape, 100+ cards), and
 *  - the generic `Exile([TheTopCardOfPlayersLibrary(You)])` action (Elemental Mascot's one-off).
 * Only the You-scoped library renders; any other player scope declines.
 */
private fun isExileTopOfYourLibrary(action: JsonObject): Boolean {
    if (action.strField("_Action") == "ExileTopCardOfLibrary") return true
    if (action.strField("_Action") != "Exile") return false
    val exilable = action["args"].asArr?.singleOrNull() as? JsonObject ?: return false
    return exilable.strField("_Exilable") == "TheTopCardOfPlayersLibrary" &&
        jsonContains(exilable, "_Player", "You")
}

internal fun EmitCtx.impulseExileTopMayPlay(actions: List<JsonObject>): Dsl? {
    if (actions.size != 2) return null
    if (!isExileTopOfYourLibrary(actions[0])) return null
    val grant = actions[1]
    if (grant.strField("_Action") != "CreatePlayerEffectUntil") return null
    val blob = compact(grant)
    if ("MayPlayExiledCard" !in blob || "TheCardExiledThisWay" !in blob) return null
    // "Until the end of your next turn" — never expires this turn even on your own turn. mtgish
    // encodes this window three equivalent ways for the may-play permission: the dedicated
    // `UntilEndOfNextTurn`, `UntilPlayersNextTurn(You)` (Elemental Mascot, Light Up the Stage,
    // Reckless Impulse), and `UntilTheEndOfPlayersNextTurn(You)` (Duskmourn — Clockwork
    // Percussionist, Impossible Inferno). All three are the same window and map to
    // MayPlayExpiry.UntilEndOfNextTurn; any other window declines -> SCAFFOLD.
    val untilEndOfNextTurn = jsonContains(grant, "_Expiration", "UntilEndOfNextTurn") ||
        ((jsonContains(grant, "_Expiration", "UntilPlayersNextTurn") ||
            jsonContains(grant, "_Expiration", "UntilTheEndOfPlayersNextTurn")) &&
            jsonContains(grant, "_Player", "You"))
    if (!untilEndOfNextTurn) return null
    // Use the shared "impulseExiled" pipeline-variable name — the same key `Patterns.Exile.impulse`
    // (the hand-authored idiom) writes, so the emitted tree is identical to a card built with that
    // facade. Diverging on the slot name (e.g. "exiledCard") would be gameplay-equivalent but trip
    // the fidelity gate's literal tree diff against the impulse golden (Elemental Mascot).
    return Composite(
        listOf(
            Lit("GatherCardsEffect(source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)), storeAs = \"impulseExiled\")"),
            Lit("MoveCollectionEffect(from = \"impulseExiled\", destination = CardDestination.ToZone(Zone.EXILE))"),
            Lit("GrantMayPlayFromExileEffect(\"impulseExiled\", MayPlayExpiry.UntilEndOfNextTurn)")
        )
    )
}

/**
 * `[LookAtTheTopNumberCardsOfLibrary(<count>, [MayExileACardOfType(nonland),
 * PutTheRemainingCardsOnTheBottomOfLibraryInARandomOrder]), MayAction(CastExiledCardWithoutPaying(
 * TheCardExiledThisWay))]` -> the "look at N, may exile a nonland, bottom the rest at random, then
 * cast the exiled card for free" pipeline (The Key to the Vault). Spans two actions — the look (which
 * names the exiled card collection) and the free cast — so it's recognised here as a whole action list
 * rather than per-action.
 *
 * Renders the Sunbird's-Invocation pipeline: Gather(top N, private) -> SelectFromCollection(up to 1,
 * nonland) -> MoveCollection(chosen -> exile, storeMovedAs) -> MoveCollection(rest -> library bottom,
 * random) -> CastFromCollectionWithoutPayingCost(exiled). Only this exact two-action shape with a
 * nonland exile filter and the random-bottom remainder renders; any other filter, remainder
 * destination, or exiled-card rider declines (-> SCAFFOLD) rather than dropping a constraint.
 */
internal fun EmitCtx.lookExileCastFree(actions: List<JsonObject>): Dsl? {
    if (actions.size != 2) return null
    val look = actions[0]
    if (look.strField("_Action") != "LookAtTheTopNumberCardsOfLibrary") return null
    val cast = actions[1]
    // The free cast is wrapped in a `MayAction` envelope ("you may cast …").
    val castInner = (cast["args"] as? JsonObject)?.takeIf { cast.strField("_Action") == "MayAction" } ?: return null
    if (castInner.strField("_Action") != "CastExiledCardWithoutPaying") return null
    if (!jsonContains(castInner["args"], "_CardInExile", "TheCardExiledThisWay")) return null

    val lookArgs = look["args"].asArr ?: return null
    val countExpr = render(dynamicAmountExpr(lookArgs.getOrNull(0)) ?: return null)
    val subActions = lookArgs.getOrNull(1).asArr?.filterIsInstance<JsonObject>() ?: return null
    // Exactly the may-exile-nonland + bottom-the-rest-at-random sub-actions, nothing else.
    val exileNode = subActions.firstOrNull { it.strField("_LookAtTopOfLibraryAction") == "MayExileACardOfType" } ?: return null
    val hasBottomRandom = subActions.any {
        it.strField("_LookAtTopOfLibraryAction") == "PutTheRemainingCardsOnTheBottomOfLibraryInARandomOrder"
    }
    if (!hasBottomRandom || subActions.size != 2) return null
    val exileFilter = exileNode["args"] as? JsonObject ?: return null
    val filterExpr = when {
        exileFilter.strField("_Cards") == "IsNonCardtype" && exileFilter.field("args").asStr() == "Land" -> "GameObjectFilter.Nonland"
        else -> return null
    }
    return Composite(listOf(
        Lit("GatherCardsEffect(source = CardSource.TopOfLibrary($countExpr), storeAs = \"looked\", revealed = false)"),
        Raw(
            "SelectFromCollectionEffect(\n" +
                "                from = \"looked\",\n" +
                "                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),\n" +
                "                filter = $filterExpr,\n" +
                "                showAllCards = true,\n" +
                "                storeSelected = \"exiledChosen\",\n" +
                "                storeRemainder = \"toBottom\",\n" +
                "                selectedLabel = \"Exile\",\n" +
                "                remainderLabel = \"Put on the bottom of your library\"\n" +
                "            )",
        ),
        Lit("MoveCollectionEffect(from = \"exiledChosen\", destination = CardDestination.ToZone(Zone.EXILE), storeMovedAs = \"exiledCard\")"),
        Lit("MoveCollectionEffect(from = \"toBottom\", destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom), order = CardOrder.Random)"),
        Lit("CastFromCollectionWithoutPayingCostEffect(from = \"exiledCard\")"),
    ))
}

/**
 * `[CreateValueX(<amount>), <consumer…>]` where each consumer spends `ValueX` -> render the consumers
 * with the bound derived `<amount>` inlined into every `ValueX` reference.
 *
 * mtgish models a derived X as a separate `CreateValueX` action that binds X, then later actions that
 * spend `ValueX`. The engine has no separate "set X" step for a triggered/one-shot ability — it inlines
 * the computed [DynamicAmount] straight into each consumer. So fold the list: bind `<amount>` on the
 * context ([EmitCtx.boundValueX]) and render the remaining actions, so each `ValueX` resolves to that
 * amount via [dynamicAmountExpr]. Two shapes this unlocks:
 *  - one consumer — Thunder Salvo's "deals X damage to target creature, where X is 2 plus the number of
 *    other spells you've cast this turn" ([SpellDealsDamage]);
 *  - multiple consumers reusing the SAME X — Arabella, Abandoned Doll's "it deals X damage to each
 *    opponent and you gain X life, where X is the number of creatures you control with power 2 or less"
 *    ([PermanentDealsDamage] + [GainLife], both spending the one derived count).
 *
 * The bound amount must render exactly via [dynamicAmountExpr], every consumer must render, and at least
 * one consumer must actually spend `ValueX` — otherwise decline (-> SCAFFOLD) rather than drop the X
 * derivation or emit a CreateValueX with no consumer.
 */
internal fun EmitCtx.createValueXReusedEffect(actions: List<JsonObject>, tvar: String?): Dsl? {
    if (actions.size < 2) return null
    val bind = actions.first()
    if (bind.strField("_Action") != "CreateValueX") return null
    val consumers = actions.drop(1)
    // The bound amount must render exactly; a bare DynamicAmount.XValue would mean we failed to
    // recover the derivation, so decline rather than inline a meaningless self-reference.
    val boundExpr = dynamicAmountExpr(bind["args"]) ?: return null
    if (render(boundExpr) == "DynamicAmount.XValue") return null
    // At least one consumer must spend ValueX (else the CreateValueX was pointless / mis-parsed).
    if (consumers.none { "ValueX" in compact(it) }) return null
    val saved = boundValueX
    boundValueX = bind["args"]
    val rendered: List<Dsl>? = try {
        val out = ArrayList<Dsl>(consumers.size)
        for (c in consumers) {
            val r = renderAction(c, tvar) ?: return null  // finally restores boundValueX
            // Inside a CreateValueX-bound fold there is no real cast-time X. A residual
            // `DynamicAmount.XValue` means a consumer referenced the derived value through a token we
            // failed to inline (e.g. Graveborn Muse's draw arm), so the render would silently swap the
            // derived count for a meaningless cast-time X. Decline (-> SCAFFOLD) rather than misrender.
            if ("DynamicAmount.XValue" in render(r)) return null
            out.add(r)
        }
        out
    } finally {
        boundValueX = saved
    }
    return if (rendered!!.size == 1) rendered.single() else Composite(rendered)
}

/**
 * `MayCost(cost)` + `If(CostWasPaid, [then…])` -> `MayEffect(IfYouDoEffect(action = <cost>, ifYouDo =
 * <then>))` — the engine's "you may pay <cost>. If you do, <then>." loot idiom (Pursue the Past's "you
 * may discard a card. If you do, draw two cards"). Only renderable shapes collapse: the cost must be one
 * we can express as an *effect* (currently the self-discard `DiscardACard` -> `Patterns.Hand.discardCards(1)`),
 * the gate must be exactly `If(CostWasPaid, [then])` with no else-branch, and the then-actions must render.
 * Anything else returns null so the caller falls through to the normal per-action path (no lossy emit).
 *
 * The "you may draw a card. If you do, discard a card." loot (`MayCost(DrawACard)` +
 * `If(CostWasPaid, [DiscardACard])`, e.g. Stadium Tidalmage / Jeskai Elder) is special-cased to the
 * canonical `MayEffect(Patterns.Hand.loot())` — the exact gameplay tree the hand-authored cards use —
 * rather than the generic IfYouDo form, since `loot()` is the named MTG loot mechanic.
 */
internal fun EmitCtx.mayCostIfYouDoEffect(may: JsonObject, ifAct: JsonObject, tvar: String?): Dsl? {
    if (may.strField("_Action") != "MayCost" || ifAct.strField("_Action") != "If") return null
    // The gate must be exactly If(CostWasPaid, [then]) — no else-branch (a single IfYouDo can't fork).
    val ifArgs = ifAct["args"].asArr ?: return null
    if (!jsonContains(ifArgs.getOrNull(0), "_Condition", "CostWasPaid")) return null
    if (ifArgs.getOrNull(2) != null) return null
    val thenActions = (ifArgs.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>() ?: return null
    if (thenActions.isEmpty()) return null
    val cost = (may["args"] as? JsonObject)?.strField("_Cost")
    // "you may draw a card. If you do, discard a card." — the canonical loot mechanic.
    if (cost == "DrawACard" && thenActions.singleOrNull()?.strField("_Action") == "DiscardACard") {
        return call("MayEffect", arg(call("Patterns.Hand.loot")))
    }
    // Render the paid cost as the IfYouDo's `action`. Only the self-discard cost is modeled today; any
    // other cost (sacrifice, pay life, exile, mana) declines -> the pair stays uncollapsed.
    val costAction = when (cost) {
        "DiscardACard" -> call("Patterns.Hand.discardCards", arg("1"))
        else -> return null
    }
    val thenEffect = renderEffectList(thenActions, tvar) ?: return null
    return call(
        "MayEffect",
        arg("effect", call("IfYouDoEffect", arg("action", costAction), arg("ifYouDo", thenEffect))),
    )
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

/**
 * `[PlayerMayCost(ControllerOfSpell, PayMana {N}), Unless(CostWasPaid, [CounterSpell])]`
 * -> `CounterEffect(condition = CounterCondition.UnlessPaysMana(ManaCost.parse("{N}")))`
 * — "counter target spell unless its controller pays {N}" (Phantom Interference mode 2). The target
 * (`TargetSpell`) is recovered by the enclosing `Targeted` envelope; the `CounterEffect`'s default
 * `CounterTarget.Spell` reads that `ContextTarget(0)`. Only a *generic* mana cost ({N}) is rendered;
 * any other cost shape declines (-> SCAFFOLD) rather than approximate.
 */
internal fun EmitCtx.counterUnlessPaysEffect(actions: List<JsonObject>): Dsl? {
    if (actions.size != 2) return null
    val (a0, a1) = actions
    if (a0.strField("_Action") != "PlayerMayCost" || a1.strField("_Action") != "Unless") return null
    if (!jsonContains(a1, "_Condition", "CostWasPaid") || !jsonContains(a1, "_Action", "CounterSpell")) return null
    // The payer must be the controller of the targeted spell (the only shape CounterUnlessPays models).
    if (!jsonContains(a0["args"], "_Player", "ControllerOfSpell")) return null
    // Recover the mana cost from the PayMana sub-node. Only a plain generic-or-coloured printed mana
    // cost is modeled (CounterUnlessPays takes a mana string); decline for anything else (-> SCAFFOLD).
    val payMana = (a0["args"].asArr ?: return null).firstOrNull { it.strField("_Cost") == "PayMana" } ?: return null
    val cost = renderMana(payMana.field("args"))
    if (cost.isEmpty() || "{?}" in cost || "{X}" in cost) return null
    return call(
        "CounterEffect",
        arg("condition", Lit("CounterCondition.UnlessPaysMana(ManaCost.parse(\"$cost\"))"))
    )
}

/**
 * `[PlayerMayCost(ControllerOfSpell, PayMana {N}),
 *   IfElse(CostWasPaid, [<onPaid actions>], [CounterSpell(Ref_TargetSpell)])]`
 * -> `CounterEffect(condition = CounterCondition.UnlessPaysMana(ManaCost.parse("{N}"), <onPaid>))`
 *
 * "Counter target spell unless its controller pays {N}. If they do, <onPaid>." (Don't Make a Sound:
 * `<onPaid>` is `Surveil 2`; Divert Disaster: create a Lander). The else-branch must be exactly the
 * `CounterSpell` of the same targeted spell — the negative side of "unless they pay" — so the whole
 * `IfElse` collapses into the engine's `onPaid` rider rather than two separate forks. The then-branch
 * (the rider) renders through the normal effect-list path; if it can't render whole, decline (-> SCAFFOLD).
 * Only a *generic* mana cost is modeled (sibling of [counterUnlessPaysEffect]); anything else declines.
 */
internal fun EmitCtx.counterUnlessPaysWithRiderEffect(actions: List<JsonObject>): Dsl? {
    if (actions.size != 2) return null
    val (a0, a1) = actions
    if (a0.strField("_Action") != "PlayerMayCost" || a1.strField("_Action") != "IfElse") return null
    // The payer must be the controller of the targeted spell.
    if (!jsonContains(a0["args"], "_Player", "ControllerOfSpell")) return null
    val ifArgs = a1["args"].asArr ?: return null
    if (ifArgs.size != 3) return null
    // The gate must be exactly CostWasPaid.
    if (!jsonContains(ifArgs[0], "_Condition", "CostWasPaid")) return null
    val thenActions = ifArgs[1].asArr?.filterIsInstance<JsonObject>() ?: return null
    val elseActions = ifArgs[2].asArr?.filterIsInstance<JsonObject>() ?: return null
    // The else-branch is the "they didn't pay" outcome: a lone CounterSpell of the targeted spell.
    if (elseActions.size != 1 || elseActions[0].strField("_Action") != "CounterSpell") return null
    if (!jsonContains(elseActions[0]["args"], "_Spell", "Ref_TargetSpell")) return null
    // The then-branch is the "they paid" rider; render it whole or decline.
    if (thenActions.isEmpty()) return null
    val onPaid = renderEffectList(thenActions, null) ?: return null
    val payMana = (a0["args"].asArr ?: return null).firstOrNull { it.strField("_Cost") == "PayMana" } ?: return null
    val cost = renderMana(payMana.field("args"))
    if (cost.isEmpty() || "{?}" in cost || "{X}" in cost) return null
    return call(
        "CounterEffect",
        arg("condition", Lit("CounterCondition.UnlessPaysMana(ManaCost.parse(\"$cost\"), ${render(onPaid)})"))
    )
}
