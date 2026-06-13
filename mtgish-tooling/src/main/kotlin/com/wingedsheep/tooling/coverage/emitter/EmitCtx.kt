package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.Call
import com.wingedsheep.tooling.coverage.Composite
import com.wingedsheep.tooling.coverage.bridge.Bridge
import com.wingedsheep.tooling.coverage.bridge.MappingEntry
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
}

internal val SELF_REFS = setOf(
    "ThisPermanent", "Trigger_ThatCreature", "ThatEnteringPermanent", "Trigger_ThatPermanent",
    "ThatCreature", "ThatPermanent", "Trigger_ThatGraveyardCard", "ThatGraveyardCard",
    // "this card from your graveyard" — the source's own card in the graveyard. Used by self-recursion
    // graveyard activated abilities ("{cost}: Return this card from your graveyard to the battlefield",
    // Teacher's Pest); always resolves to the source itself.
    "ThisGraveyardCard",
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
    impulseExileTopMayPlay(actions)?.let { return it }
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
        "XValue", "X", "ValueX" -> return Lit("DynamicAmount.XValue")
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
            if (jsonContains(node["args"], "_Permanent", "ThisPermanent")) return call("DynamicAmounts.sourcePower")
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
        "LifeTotalOfPlayer" -> {
            val player = if (jsonContains(node, "_Player", "Opponent")) "Player.EachOpponent" else "Player.You"
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
    // "the created token" — the token a preceding CreateTokens action in this same action list just
    // made (Fractal Tender: "create a 0/0 Fractal … and put three +1/+1 counters on it"). The token
    // executor publishes its ids under the CREATED_TOKENS pipeline collection, so the follow-up effect
    // addresses it via PipelineTarget(CREATED_TOKENS, 0).
    if (ref == "TheCreatedToken") return "EffectTarget.PipelineTarget(CREATED_TOKENS, 0)"
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
 */
internal fun EmitCtx.actionConditionDsl(cond: JsonObject?): String? {
    if (cond == null) return null
    if (cond.strField("_Condition") == "PlayerPassesFilter") {
        val args = cond["args"].asArr
        if (args != null && (args.getOrNull(0) as? JsonObject)?.strField("_Player") == "You") {
            val controls = args.getOrNull(1) as? JsonObject
            if (controls?.strField("_Players") == "ControlsA") {
                val filter = gameObjectFilterDsl(controls["args"])
                if (filter != null) return render(call("Conditions.YouControl", arg(Lit(filter))))
            }
        }
    }
    // Other resolution-time intervening-if shapes ("if you gained life this turn", "if you've cast
    // another instant or sorcery this turn", …) reuse the shared condition renderer; declining beats
    // widening. These power the resolution-time `on("If")` action handler.
    return interveningIfDsl(cond)
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
 *   GatherCardsEffect(CardSource.TopOfLibrary(DynamicAmount.Fixed(1)), storeAs = "exiledCard")
 *   MoveCollectionEffect(from = "exiledCard", destination = CardDestination.ToZone(Zone.EXILE))
 *   GrantMayPlayFromExileEffect("exiledCard", MayPlayExpiry.UntilEndOfNextTurn)
 *
 * Only this exact two-action shape with the "until end of your next turn" window collapses; any
 * other player effect, expiration, or extra rider declines (null -> SCAFFOLD) rather than guess.
 */
internal fun EmitCtx.impulseExileTopMayPlay(actions: List<JsonObject>): Dsl? {
    if (actions.size != 2) return null
    if (actions[0].strField("_Action") != "ExileTopCardOfLibrary") return null
    val grant = actions[1]
    if (grant.strField("_Action") != "CreatePlayerEffectUntil") return null
    val blob = compact(grant)
    if ("MayPlayExiledCard" !in blob || "TheCardExiledThisWay" !in blob) return null
    // "Until the end of your next turn" — never expires this turn even on your own turn.
    if (!jsonContains(grant, "_Expiration", "UntilEndOfNextTurn")) return null
    return Composite(
        listOf(
            Lit("GatherCardsEffect(source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)), storeAs = \"exiledCard\")"),
            Lit("MoveCollectionEffect(from = \"exiledCard\", destination = CardDestination.ToZone(Zone.EXILE))"),
            Lit("GrantMayPlayFromExileEffect(\"exiledCard\", MayPlayExpiry.UntilEndOfNextTurn)")
        )
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
