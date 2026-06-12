package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.Call
import com.wingedsheep.tooling.coverage.Dsl
import com.wingedsheep.tooling.coverage.Infix
import com.wingedsheep.tooling.coverage.Link
import com.wingedsheep.tooling.coverage.Lit
import com.wingedsheep.tooling.coverage.arg
import com.wingedsheep.tooling.coverage.argWordsTagged
import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.asStr
import com.wingedsheep.tooling.coverage.colorsOf
import com.wingedsheep.tooling.coverage.compact
import com.wingedsheep.tooling.coverage.dot
import com.wingedsheep.tooling.coverage.field
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.firstArgStringTagged
import com.wingedsheep.tooling.coverage.firstArgWordTagged
import com.wingedsheep.tooling.coverage.firstWordAtKey
import com.wingedsheep.tooling.coverage.hasStringValue
import com.wingedsheep.tooling.coverage.hasTag
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.nodesTagged
import com.wingedsheep.tooling.coverage.pascalToUpperSnake
import com.wingedsheep.tooling.coverage.render
import com.wingedsheep.tooling.coverage.strField
import com.wingedsheep.tooling.coverage.subtypes
import com.wingedsheep.tooling.coverage.wordsAtKey
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Target / filter recovery — reads the mtgish target vocabulary the coverage map discards and rebuilds
 * the Argentum target/filter DSL. A filter we can't faithfully render returns null → the card drops
 * to SCAFFOLD rather than emitting a confidently-wrong target.
 *
 * The recovery builds the typed output [Dsl] tree (a base [Lit] + fluent `.method()` [Link]s as a
 * [com.wingedsheep.tooling.coverage.Chain], `or`-joins as an [Infix]); each public `…Dsl` is a thin
 * `…Expr(…)?.let(::render)` wrapper so callers above still receive the historical String.
 */

/**
 * Single source of truth for the filter-predicate suffixes recovered from the mtgish filter IR. The
 * TargetFilter renderer ([creatureFilterExpr]) and the GameObjectFilter renderer ([gameObjectFilterExpr])
 * read the SAME predicate vocabulary onto two parallel fluent surfaces; defining each predicate's
 * IR→DSL recovery ONCE here keeps them from drifting (the regexes were duplicated, and a fix in one
 * renderer had to be mirrored by hand). Each caller still composes these in its own order — the two
 * surfaces are not identical (TargetFilter has no multi-color form, and the renderers append in
 * different orders) — but the per-predicate recovery now lives in one place.
 *
 * Each predicate returns the fluent [Link] (`.tapped()`, `.powerAtLeast(2)`) it recovers from the parsed
 * filter subtree through the typed [IrQuery][hasTag] accessors (bounded by the node, vs. a `compact()`
 * +substring/regex that scanned the whole flattened blob), or null when the clause is absent.
 */
internal object FilterPredicates {
    /** `.powerAtLeast(N)` for a `PowerIs >= N` clause, else null. */
    fun powerAtLeast(node: JsonElement?): Link? = powerBound(node, "GreaterThanOrEqualTo")?.let { Link("powerAtLeast", listOf(arg("$it"))) }

    /** `.powerAtMost(N)` for a `PowerIs <= N` clause, else null. */
    fun powerAtMost(node: JsonElement?): Link? = powerBound(node, "LessThanOrEqualTo")?.let { Link("powerAtMost", listOf(arg("$it"))) }

    /** `.manaValueAtMost(N)` for a `ManaValueIs <= N` clause, else null (Smother's "mana value 3 or less"). */
    fun manaValueAtMost(node: JsonElement?): Link? = manaValueBound(node, "LessThanOrEqualTo")?.let { Link("manaValueAtMost", listOf(arg("$it"))) }

    /** `.manaValueAtLeast(N)` for a `ManaValueIs >= N` clause, else null. */
    fun manaValueAtLeast(node: JsonElement?): Link? = manaValueBound(node, "GreaterThanOrEqualTo")?.let { Link("manaValueAtLeast", listOf(arg("$it"))) }

    fun tapped(node: JsonElement?): Link? = if (node.hasTag("IsTapped")) Link("tapped") else null
    fun untapped(node: JsonElement?): Link? = if (node.hasTag("IsUntapped")) Link("untapped") else null
    fun attacking(node: JsonElement?): Link? = if (node.hasTag("IsAttacking")) Link("attacking") else null

    /** `.nontoken()` for an `IsNonToken` clause ("another nontoken Bird you control"), else null. */
    fun nontoken(node: JsonElement?): Link? = if (node.hasTag("IsNonToken")) Link("nontoken") else null

    /**
     * `.powerOrToughnessAtLeast(N)` for the `Or[PowerIs >= N, ToughnessIs >= N]` shape ("power or
     * toughness 4 or greater"), else null. When this fires, the caller must NOT also append the
     * standalone [powerAtLeast] — the `PowerIs` clause it would match lives inside this Or, and emitting
     * both would narrow the filter to power alone (the Repel Calamity bug). Only the both-bounds-equal
     * `>=` form renders; anything else declines so we never widen "power or toughness" to one stat.
     */
    fun powerOrToughnessAtLeast(node: JsonElement?): Link? {
        val orNode = node.nodesTagged("Or").firstOrNull { or ->
            val kids = or["args"].asArr ?: return@firstOrNull false
            kids.size == 2 &&
                kids.any { it.strField("_Permanents") == "PowerIs" } &&
                kids.any { it.strField("_Permanents") == "ToughnessIs" }
        } ?: return null
        val kids = orNode["args"].asArr!!
        val pn = atLeastBound(kids.first { it.strField("_Permanents") == "PowerIs" })
        val tn = atLeastBound(kids.first { it.strField("_Permanents") == "ToughnessIs" })
        if (pn == null || pn != tn) return null
        return Link("powerOrToughnessAtLeast", listOf(arg("$pn")))
    }

    /** The integer of a `_Comparison: GreaterThanOrEqualTo` clause (PowerIs/ToughnessIs share this shape),
     *  or null for a non-`>=` comparison or a non-integer (X) bound. */
    private fun atLeastBound(clause: JsonElement?): Int? =
        clause.field("args").takeIf { it.strField("_Comparison") == "GreaterThanOrEqualTo" }?.let { findInteger(it) as? Int }

    /** `.withoutKeyword(Keyword.FLYING)` for a `DoesntHaveAbility Flying` clause, else null. */
    fun withoutFlying(node: JsonElement?): Link? =
        if (jsonContains(node, "_Permanents", "DoesntHaveAbility") && node.hasStringValue("Flying"))
            Link("withoutKeyword", listOf(arg("Keyword.FLYING"))) else null

    /** `.withKeyword(Keyword.FLYING)` for a plain `Flying` clause, else null. */
    fun withFlying(node: JsonElement?): Link? =
        if (node.hasStringValue("Flying")) Link("withKeyword", listOf(arg("Keyword.FLYING"))) else null

    /** The integer bound of a `PowerIs` clause whose `args` is `{ _Comparison: <comparison>, args: Integer }`,
     *  scoped to the matching `PowerIs` node so a power range's two bounds stay distinct. */
    private fun powerBound(node: JsonElement?, comparison: String): Int? =
        node.nodesTagged("PowerIs")
            .firstOrNull { it["args"].strField("_Comparison") == comparison }
            ?.let { findInteger(it["args"]) as? Int }

    /** The integer bound of a `ManaValueIs` clause whose `args` is `{ _Comparison, args: Integer }`; null for
     *  a non-integer (e.g. X) bound, leaving the filter unrestricted rather than emitting a wrong number. */
    private fun manaValueBound(node: JsonElement?, comparison: String): Int? =
        node.nodesTagged("ManaValueIs")
            .firstOrNull { it["args"].strField("_Comparison") == comparison }
            ?.let { findInteger(it["args"]) as? Int }
}

internal fun EmitCtx.creatureFilterDsl(filterNode: JsonElement?): String? = creatureFilterExpr(filterNode)?.let(::render)

internal fun EmitCtx.creatureFilterExpr(filterNode: JsonElement?): Dsl? {
    val blob = compact(filterNode)
    // "creature blocking <source>" / "blocked by <source>" (IsBlockingAttacker / IsBlockedByDefender,
    // bound to ThisPermanent) is relative to the source creature — no static GroupFilter expresses it,
    // and the bare "IsBlocking" substring check below would otherwise mis-catch IsBlockingAttacker as
    // the generic "blocking creature" constant, silently dropping the source relation. Decline so it
    // scaffolds (Cromat's {W}{B} ability).
    if ("IsBlockingAttacker" in blob || "IsBlockedByDefender" in blob) return null
    // "creature that crewed/saddled it this turn" (CrewedOrSaddledSourceThisTurn) — a source-relative
    // Mount/Vehicle payoff filter (Giant Beaver: "put a +1/+1 counter on target creature that saddled it
    // this turn"). mtgish tags it `SaddledPermanentThisTurn` bound to the trigger's permanent. It composes
    // onto the plain creature filter via .crewedOrSaddledSourceThisTurn(); a controller restriction on top
    // of it isn't a shape we render, so decline rather than widen.
    if ("SaddledPermanentThisTurn" in blob || "CrewedPermanentThisTurn" in blob) {
        if ("ControlledByAPlayer" in blob) return null
        return Call("TargetFilter", listOf(arg(Lit("GameObjectFilter.Creature").dot("crewedOrSaddledSourceThisTurn"))))
    }
    // "...that was dealt damage this turn" (Rooftop Assassin) renders via `.dealtDamageThisTurn()` on the
    // plain-creature path below (composed alongside the controller suffix). The special-shape branches
    // (attacking/blocking/subtype/face-down/non-cardtype) can't compose it, so if such a predicate is also
    // present, decline rather than silently drop the dealt-damage restriction (which would widen the kill).
    if ("WasDealtDamageThisTurn" in blob) {
        val unrenderableAlongside = listOf(
            "IsAttacking", "IsBlocking", "IsFaceDown", "IsCreatureType", "IsNonCreatureType",
            "IsNonCardtype", "Other", "HasACounterOfType",
        )
        if (unrenderableAlongside.any { it in blob }) return null
    }
    // "non-Mount creature" / "non-<creature-type> creature" (Sterling Keykeeper): a negated creature
    // subtype (IsNonCreatureType). TargetFilter has no `.notSubtype` passthrough, so render the
    // GameObjectFilter form wrapped in TargetFilter. Only the bare negated subtype (optionally a
    // You/Opponent controller) is modeled; any other predicate alongside it would be silently dropped, so
    // decline rather than widen.
    val nonCreatureSubs = filterNode.argWordsTagged("IsNonCreatureType")
    if (nonCreatureSubs.isNotEmpty()) {
        val otherPredicates = listOf(
            "IsTapped", "IsUntapped", "IsAttacking", "IsBlocking", "PowerIs", "ToughnessIs", "ManaValueIs",
            "IsColor", "IsNonColor", "HasAbility", "DoesntHaveAbility", "IsNonToken", "IsCreatureType",
            "IsNonCardtype", "Other",
        )
        if (otherPredicates.any { it in blob }) return null
        var g: Dsl = Lit("GameObjectFilter.Creature")
        nonCreatureSubs.forEach { g = g.dot("notSubtype", arg("Subtype(\"$it\")")) }
        when {
            "\"You\"" in blob -> g = g.dot("youControl")
            "\"Opponent\"" in blob -> g = g.dot("opponentControls")
            "ControlledByAPlayer" in blob -> return null  // an unrenderable controller ref -> SCAFFOLD
        }
        return Call("TargetFilter", listOf(arg(g)))
    }
    // "nonartifact creature" (the Terror template) renders via .nonartifact(); any OTHER non-cardtype
    // restriction has no faithful filter rendering yet, so drop to SCAFFOLD rather than omit it.
    val nonCardtypes = filterNode.argWordsTagged("IsNonCardtype")
    if (nonCardtypes.any { it != "Artifact" }) return null
    // "ANOTHER target creature you control" — an `Other(ThisPermanent)` self-exclusion. The TargetFilter
    // surface built here doesn't compose excludeSelf, so silently dropping it would let the source target
    // itself (an illegal target). Decline (-> SCAFFOLD) rather than emit a target missing the "another"
    // restriction (Deserter's Disciple). GroupFilter's excludeSelf path is separate (groupFilterExpr).
    if (jsonContains(filterNode, "_Permanents", "Other")) return null
    // "target creature you control" / "...an opponent controls" — the controller restriction is a
    // ControlledByAPlayer clause. Preserve it as a `.youControl()` / `.opponentControls()` suffix; never
    // drop it (an unrestricted target would let the spell hit any creature). Only the plain-creature
    // path below can compose it, so the special shapes scaffold when a controller clause is present.
    val controller: Link? = when {
        "ControlledByAPlayer" !in blob -> null
        "\"Opponent\"" in blob -> Link("opponentControls")
        "\"You\"" in blob -> Link("youControl")
        // "a creature that player controls" in a combat-damage trigger: the player ~ dealt damage to is an
        // opponent (your creature dealt them combat damage), so it's that opponent's creature (Skirk Commando).
        "\"Trigger_ThatPlayer\"" in blob -> Link("opponentControls")
        else -> return null
    }
    val hasController = "ControlledByAPlayer" in blob
    // Whole-creature shapes whose helpers live on GameObjectFilter (not TargetFilter), or are a named
    // TargetFilter constant. ONS targets use these in isolation, so render them as the whole filter.
    if ("IsAttacking" in blob && "IsBlocking" in blob) {
        if (hasController) return null
        // "...with flying" composes onto the attacking-or-blocking base (Venomspout Brackus).
        return if ("\"Flying\"" in blob)
            Call("TargetFilter", listOf(arg(Lit("GameObjectFilter.Creature").dot("attackingOrBlocking").dot("withKeyword", arg("Keyword.FLYING")))))
        else Lit("TargetFilter.AttackingOrBlockingCreature")
    }
    if ("IsBlocking" in blob && "IsAttacking" !in blob) {
        if (hasController) return null
        // "...with flying" composes onto the blocking base; bare "blocking creature" uses the constant.
        return if ("\"Flying\"" in blob)
            Call("TargetFilter", listOf(arg(Lit("GameObjectFilter.Creature").dot("blocking").dot("withKeyword", arg("Keyword.FLYING")))))
        else Lit("TargetFilter.BlockingCreature")
    }
    if ("IsFaceDown" in blob) {
        if (hasController) return null
        return Call("TargetFilter", listOf(arg(Lit("GameObjectFilter.Creature").dot("faceDown"))))
    }
    // "Goblin creature" / "Elf or Soldier creature": one subtype -> withSubtype; several -> an Or of
    // per-subtype creature filters (matches golden's distributed Or[And[IsCreature, HasSubtype X]…]).
    val subs = filterNode.argWordsTagged("IsCreatureType")
    if (subs.isNotEmpty()) {
        if (hasController) return null
        return Call("TargetFilter", listOf(arg(Infix("or", subs.map { Lit("GameObjectFilter.Creature").dot("withSubtype", arg("\"$it\"")) }))))
    }
    var node: Dsl = Lit("TargetFilter.Creature")
    if ("Artifact" in nonCardtypes) node = node.dot("nonartifact")
    // Color recovery, IsColor/IsNonColor-scoped: a single colour -> .withColor / .notColor; several
    // colours under an Or ("white or black creature") -> .withAnyColor so the extra colours aren't
    // dropped. Multiple excluded colours chain as .notColor (AND-of-not = "neither X nor Y").
    filterNode.colorsOf("IsNonColor").forEach { node = node.dot("notColor", arg("Color.${it.uppercase()}")) }
    val colors = filterNode.colorsOf("IsColor")
    when {
        colors.size == 1 -> node = node.dot("withColor", arg("Color.${colors[0].uppercase()}"))
        colors.size > 1 -> node = node.dot("withAnyColor", *colors.map { arg("Color.${it.uppercase()}") }.toTypedArray())
    }
    // "...with flying" / "...without flying" — withoutFlying first, since a DoesntHaveAbility Flying
    // clause also satisfies the plain-Flying string check (mirrors gameObjectFilterExpr).
    (FilterPredicates.withoutFlying(filterNode) ?: FilterPredicates.withFlying(filterNode))?.let { node = node.dot(it) }
    FilterPredicates.tapped(filterNode)?.let { node = node.dot(it) }
    FilterPredicates.attacking(filterNode)?.let { node = node.dot(it) }
    FilterPredicates.nontoken(filterNode)?.let { node = node.dot(it) }
    // "power or toughness N or greater" takes the whole Or; suppress the standalone power bounds it
    // would otherwise also match (the PowerIs clause inside the Or) — see [powerOrToughnessAtLeast].
    val powerOrToughness = FilterPredicates.powerOrToughnessAtLeast(filterNode)
    if (powerOrToughness != null) node = node.dot(powerOrToughness) else {
        FilterPredicates.powerAtMost(filterNode)?.let { node = node.dot(it) }
        FilterPredicates.powerAtLeast(filterNode)?.let { node = node.dot(it) }
    }
    FilterPredicates.manaValueAtMost(filterNode)?.let { node = node.dot(it) }
    FilterPredicates.manaValueAtLeast(filterNode)?.let { node = node.dot(it) }
    // "...that was dealt damage this turn" (Rooftop Assassin) — composes onto the plain-creature filter
    // via .dealtDamageThisTurn(). Special shapes that carry this predicate already declined above.
    if ("WasDealtDamageThisTurn" in blob) node = node.dot("dealtDamageThisTurn")
    // Keyword-ability restrictions ("attacking creature with shadow", Maze of Shadows): a
    // HasAbility / DoesntHaveAbility clause carrying a `_CheckHasable` keyword -> .withKeyword /
    // .withoutKeyword. Flying is already composed above (string match), so skip it here. If any
    // HasAbility clause names an ability we can't map to a known SDK Keyword, decline (-> SCAFFOLD)
    // rather than silently dropping the restriction and widening the target.
    val keywordLinks = abilityKeywordLinks(filterNode) ?: return null
    keywordLinks.forEach { node = node.dot(it) }
    controller?.let { node = node.dot(it) }
    return node
}

/**
 * `.withKeyword(Keyword.X)` / `.withoutKeyword(Keyword.X)` links for every HasAbility / DoesntHaveAbility
 * clause in [filterNode], or null if any such clause names an ability the SDK has no Keyword for (so the
 * caller declines rather than dropping it). Flying is handled by the caller's string-based path, so it is
 * skipped here to avoid a duplicate link.
 */
private fun EmitCtx.abilityKeywordLinks(filterNode: JsonElement?): List<Link>? {
    val links = mutableListOf<Link>()
    for ((tag, method) in listOf("HasAbility" to "withKeyword", "DoesntHaveAbility" to "withoutKeyword")) {
        for (node in filterNode.nodesTagged(tag)) {
            val kwName = node.firstWordAtKey("_CheckHasable") ?: return null  // non-keyword hasable -> decline
            if (kwName == "Flying") continue  // composed by the caller's "Flying" string match
            val kw = pascalToUpperSnake(kwName)
            if (kw !in keywords) return null  // unknown ability -> decline (don't widen the filter)
            links.add(Link(method, listOf(arg("Keyword.$kw"))))
        }
    }
    return links
}

private fun targetTypes(args: JsonElement?): Set<String> = args.argWordsTagged("IsCardtype").toSet()

/**
 * True for a `ManaValueIs { _Comparison: EqualTo, args: { _GameNumber: ValueX } }` clause — "mana value
 * X", where X is the value chosen for the source spell's `{X}…` cost (Repeal). Renders as
 * `.manaValueEqualsX()`. Any other ManaValueIs shape (a fixed integer, a different comparison) is NOT
 * matched here, so the caller declines rather than mis-rendering.
 */
private fun manaValueEqualsXClause(args: JsonElement?): Boolean =
    args.nodesTagged("ManaValueIs").any { mv ->
        val cmp = mv["args"]
        cmp.strField("_Comparison") == "EqualTo" && "ValueX" in compact(cmp)
    }

internal fun EmitCtx.targetDsl(tnode: JsonObject, actionContext: List<JsonObject>? = null): String? =
    targetExpr(tnode, actionContext)?.let(::render)

/** Faithful Argentum target DSL node, or null if the filter can't be rendered (-> not AUTO). */
internal fun EmitCtx.targetExpr(tnode: JsonObject, actionContext: List<JsonObject>? = null): Dsl? {
    val ttype = tnode.strField("_Target")
    val args = tnode["args"]
    val countInt = findInteger(tnode)
    if (ttype == "TargetPlayer") {
        return if (jsonContains(tnode, "_Players", "Opponent")) Call("TargetOpponent") else Call("TargetPlayer")
    }
    if (ttype == "AnyTarget" || ttype == "TargetPlayerOrPermanent") {
        val blob = compact(tnode)
        if ("Planeswalker" in blob && "Player" in blob && "Opponent" in blob) return Call("TargetOpponentOrPlaneswalker")
        if ("Planeswalker" in blob && "Player" in blob) return Call("TargetPlayerOrPlaneswalker")
        if ("Planeswalker" in blob && "Creature" in blob) return Call("TargetCreatureOrPlaneswalker")
        if (actionContext != null && actionContext.consumesOnlyTargetPlayer()) return Call("TargetPlayer")
        return Call("AnyTarget")
    }
    if (ttype in setOf("TargetPermanent", "NumberTargetPermanents", "UptoNumberTargetPermanents", "OneOrTwoTargetPermanents")) {
        val types = targetTypes(args)
        val blob = compact(args)
        // "the permanent with the lowest/highest mana value" (Culling Scales) is a global superlative the
        // SDK has no target filter for. Dropping it would let the spell hit ANY matching permanent, so
        // decline -> SCAFFOLD rather than emit an unrestricted target.
        if ("WithTheLowestManaValue" in blob || "WithTheHighestManaValue" in blob) return null
        // "target Equipment" / "target Vehicle" — an artifact-subtype restriction (IsArtifactType) that
        // carries no IsCardtype. Render the artifact filter narrowed by the subtype so it isn't silently
        // dropped to "any permanent". Only the bare subtype (optionally a controller clause) is modeled;
        // anything else declines (Rustspore Ram, Turn to Dust).
        val artifactSubtype = args.firstArgWordTagged("IsArtifactType")
        if (artifactSubtype != null) {
            if (types.isNotEmpty() || "IsNonCardtype" in blob || "IsCreatureType" in blob) return null
            val controller: Link? = when {
                "ControlledByAPlayer" !in blob -> null
                "\"You\"" in blob -> Link("youControl")
                "\"Opponent\"" in blob -> Link("opponentControls")
                else -> return null
            }
            var base: Dsl = Lit("GameObjectFilter.Artifact").dot("withSubtype", arg(subtypeArg(artifactSubtype)))
            controller?.let { base = base.dot(it) }
            val parts = mutableListOf(arg("filter", Call("TargetFilter", listOf(arg(base)))))
            if (ttype in setOf("NumberTargetPermanents", "UptoNumberTargetPermanents") && countInt is Int) parts.add(0, arg("count", "$countInt"))
            if (ttype == "UptoNumberTargetPermanents") parts.add(0, arg("optional", "true"))
            return Call("TargetPermanent", parts)
        }
        // A creature-subtype restriction ("target Wall") implies a creature target even with no explicit
        // IsCardtype Creature; route it through the creature filter so the subtype isn't dropped (Tunnel).
        val creatureTarget = types == setOf("Creature") || (types.isEmpty() && "IsCreatureType" in blob)
        if (creatureTarget) {
            val filter = creatureFilterExpr(args) ?: return null
            val parts = mutableListOf(arg("filter", filter))
            if (ttype in setOf("NumberTargetPermanents", "UptoNumberTargetPermanents") && countInt is Int) parts.add(0, arg("count", "$countInt"))
            if (ttype == "OneOrTwoTargetPermanents") { parts.add(0, arg("minCount", "1")); parts.add(0, arg("count", "2")) }
            if (ttype == "UptoNumberTargetPermanents") parts.add(0, arg("optional", "true"))
            return Call("TargetCreature", parts)
        }
        val singleType = mapOf("Land" to "TargetFilter.Land", "Artifact" to "TargetFilter.Artifact", "Enchantment" to "TargetFilter.Enchantment")
        if (types.size == 1 && types.first() in singleType) {
            // "noncreature artifact" (Blinkmoth Well): an IsNonCardtype restriction on top of the single
            // cardtype that no land/artifact/enchantment target filter expresses (there is no .noncreature()).
            // Decline rather than widen to "any artifact".
            if ("IsNonCardtype" in blob) return null
            // "target land you control" / "...an opponent controls" — the controller restriction is a
            // ControlledByAPlayer clause. Preserve it as a `.youControl()` / `.opponentControls()` suffix
            // (mirrors the creature path); a controller clause we can't render exactly declines (-> SCAFFOLD)
            // rather than silently widening to any land/artifact/enchantment.
            val controller: Link? = when {
                "ControlledByAPlayer" !in blob -> null
                "\"You\"" in blob -> Link("youControl")
                "\"Opponent\"" in blob -> Link("opponentControls")
                else -> return null
            }
            var f: Dsl = Lit(singleType.getValue(types.first()))
            controller?.let { f = f.dot(it) }
            val parts = mutableListOf(arg("filter", f))
            if (ttype in setOf("NumberTargetPermanents", "UptoNumberTargetPermanents") && countInt is Int) parts.add(0, arg("count", "$countInt"))
            if (ttype == "UptoNumberTargetPermanents") parts.add(0, arg("optional", "true"))
            return Call("TargetPermanent", parts)
        }
        // "target nonland permanent" — an IsNonCardtype "Land" with no positive cardtype restriction
        // (Thistledown Players' "untap target nonland permanent"). An optional "with mana value X"
        // clause (ManaValueIs EqualTo ValueX, from an {X}… cast cost) renders as .manaValueEqualsX()
        // (Repeal). A ManaValueIs clause we DON'T render (any other shape) must decline rather than
        // silently drop the restriction — an unrestricted bounce would hit any permanent.
        if (types.isEmpty() && args.argWordsTagged("IsNonCardtype") == listOf("Land") && "IsCreatureType" !in blob) {
            val manaValueX = manaValueEqualsXClause(args)
            if ("ManaValueIs" in blob && !manaValueX) return null  // unrendered MV restriction -> SCAFFOLD
            var f: Dsl = Lit("TargetFilter.NonlandPermanent")
            if (manaValueX) f = f.dot("manaValueEqualsX")
            val parts = mutableListOf(arg("filter", f))
            if (ttype in setOf("NumberTargetPermanents", "UptoNumberTargetPermanents") && countInt is Int) parts.add(0, arg("count", "$countInt"))
            if (ttype == "UptoNumberTargetPermanents") parts.add(0, arg("optional", "true"))
            return Call("TargetPermanent", parts)
        }
        if (types.isEmpty() && "IsCardtype" !in blob && "IsCreatureType" !in blob && "IsNonCardtype" !in blob &&
            "IsArtifactType" !in blob && "IsLandType" !in blob && "IsEnchantmentType" !in blob) {
            // "untap ANOTHER target permanent YOU CONTROL" (North Pole Patrol): a bare TargetPermanent would
            // silently drop the controller (ControlledByAPlayer) and self-exclusion (Other) restrictions,
            // widening it to any permanent. Decline (-> SCAFFOLD) rather than emit a too-broad target.
            if ("ControlledByAPlayer" in blob || jsonContains(args, "_Permanents", "Other")) return null
            return Call("TargetPermanent")
        }
        val multiType = mapOf(
            setOf("Creature", "Land") to "TargetFilter.CreatureOrLandPermanent",
            setOf("Creature", "Artifact") to "TargetFilter.CreatureOrArtifact",
            setOf("Creature", "Enchantment") to "TargetFilter.CreatureOrEnchantment",
            setOf("Artifact", "Enchantment") to "TargetFilter.ArtifactOrEnchantment",
        )
        multiType[types]?.let {
            return Call("TargetPermanent", listOf(arg("filter", it)))
        }
        return null  // unusual filters: not rendered yet -> SCAFFOLD
    }
    if (ttype == "TargetSpell") {
        val blob = compact(args)
        // "counter target spell that targets a creature/player" (TargetsAPermanent / TargetsAPlayer):
        // the nested IsCardtype describes the spell's *target*, not the spell's own type, so targetTypes
        // would mis-read it as a "creature spell". No SDK filter expresses the relation — decline so it
        // scaffolds rather than counter the wrong spells (Confound).
        if ("TargetsA" in blob) return null
        val types = targetTypes(args)
        val colors = args.colorsOf("IsColor")
        val nonColors = args.colorsOf("IsNonColor")
        // "counter target nonblue spell" (Frazzle) — an IsNonColor clause on a stack spell. AND-of-not =
        // "neither X nor Y"; each excluded colour chains as .notColor. SDK supports it on SpellOnStack.
        if (types.isEmpty() && colors.isEmpty() && nonColors.isNotEmpty()) {
            var f: Dsl = Lit("TargetFilter.SpellOnStack")
            nonColors.forEach { f = f.dot("notColor", arg("Color.${it.uppercase()}")) }
            return Call("TargetSpell", listOf(arg("filter", f)))
        }
        if (nonColors.isNotEmpty()) return null  // nonColor + (type | positive colour) combo not rendered -> SCAFFOLD
        // "counter target blue spell" — a colour-restricted spell on the stack.
        if (types.isEmpty() && colors.isNotEmpty()) {
            var f: Dsl = Lit("TargetFilter.SpellOnStack")
            f = if (colors.size == 1) f.dot("withColor", arg("Color.${colors[0].uppercase()}"))
                else f.dot("withAnyColor", *colors.map { arg("Color.${it.uppercase()}") }.toTypedArray())
            return Call("TargetSpell", listOf(arg("filter", f)))
        }
        if (colors.isNotEmpty()) return null  // colour + type combo not rendered yet -> SCAFFOLD
        if (types == setOf("Creature", "Sorcery")) return Call("TargetSpell", listOf(arg("filter", "TargetFilter.CreatureOrSorcerySpellOnStack")))
        if (types == setOf("Instant", "Sorcery")) return Call("TargetSpell", listOf(arg("filter", "TargetFilter.InstantOrSorcerySpellOnStack")))
        if (types == setOf("Creature")) return Call("TargetSpell", listOf(arg("filter", "TargetFilter.CreatureSpellOnStack")))
        if (types.isEmpty()) return Call("TargetSpell")
        return null
    }
    if (ttype == "TargetGraveyardCard" || ttype == "UptoOneTargetGraveyardCard") {
        val blob = compact(args)
        val types = targetTypes(args)
        val filt: Dsl = when {
            types.isEmpty() && "IsCardtype" !in blob -> Lit("TargetFilter.CardInGraveyard")
            types == setOf("Creature") -> when {
                "\"You\"" in blob -> Lit("TargetFilter.CreatureInYourGraveyard")
                "\"Opponent\"" in blob -> graveyardFilter("Creature", blob)  // "from an opponent's graveyard" (Ashen Powder)
                else -> Lit("TargetFilter.CreatureInGraveyard")
            }
            types == setOf("Instant", "Sorcery") -> graveyardFilter("InstantOrSorcery", blob)
            types.size == 1 && types.first() in graveyardSingleTypeFilters -> graveyardFilter(graveyardSingleTypeFilters.getValue(types.first()), blob)
            else -> return null
        }
        // "Return up to one target … card from your graveyard …" (Mourner's Surprise) — the optional
        // (zero-or-one) variant renders the same filter under `optional = true`.
        val parts = mutableListOf(arg("filter", filt))
        if (ttype == "UptoOneTargetGraveyardCard") parts.add(0, arg("optional", "true"))
        return Call("TargetObject", parts)
    }
    return null
}

private val graveyardSingleTypeFilters = mapOf(
    "Artifact" to "Artifact",
    "Enchantment" to "Enchantment",
    "Instant" to "Instant",
    "Land" to "Land",
    "Sorcery" to "Sorcery",
)

private fun graveyardFilter(gameObjectFilter: String, blob: String): Dsl {
    val owner: Link? = when {
        "\"You\"" in blob -> Link("ownedByYou")
        "\"Opponent\"" in blob -> Link("ownedByOpponent")
        else -> null
    }
    var base: Dsl = Lit("GameObjectFilter.$gameObjectFilter")
    owner?.let { base = base.dot(it) }
    return Call("TargetFilter", listOf(arg(base), arg("zone", "Zone.GRAVEYARD")))
}

private fun List<JsonObject>.consumesOnlyTargetPlayer(): Boolean {
    val targetPlayer = any { jsonContains(it, "_Player", "Ref_TargetPlayer") }
    val targetPermanent = any { jsonContains(it, "_Permanent", "Ref_TargetPermanent") }
    val targetGraveyardCard = any { jsonContains(it, "_GraveyardCard", "Ref_TargetGraveyardCard") }
    return targetPlayer && !targetPermanent && !targetGraveyardCard
}

/** GroupFilter for mass effects. If we can't preserve the filter, scaffold rather than widen. */
internal fun EmitCtx.groupFilterDsl(filterNode: JsonElement?): String? = groupFilterExpr(filterNode)?.let(::render)

internal fun EmitCtx.groupFilterExpr(filterNode: JsonElement?): Dsl? {
    val filtered = gameObjectFilterExpr(filterNode) ?: return null
    val oracle = oracleText?.lowercase() ?: ""
    val args = mutableListOf(arg(filtered))
    // The IR's `Other(ThisPermanent)` is the authoritative "excludeSelf" signal; the oracle phrasing
    // ("all other" / "each other" / "other ... creatures") is the fallback for shapes without it.
    if (jsonContains(filterNode, "_Permanents", "Other") ||
        "all other" in oracle || "each other" in oracle) args.add(arg("excludeSelf", "true"))
    return Call("GroupFilter", args)
}

internal fun EmitCtx.gameObjectFilterDsl(filterNode: JsonElement?): String? = gameObjectFilterExpr(filterNode)?.let(::render)

internal fun EmitCtx.gameObjectFilterExpr(filterNode: JsonElement?): Dsl? {
    val blob = compact(filterNode)
    // "shares a color with [the target]" (Radiance) is a group predicate bound to the resolved target's
    // colors — we can't express it in a static GroupFilter, and silently dropping it would widen the
    // effect to every creature on the battlefield. Decline so radiance scaffolds rather than emitting a
    // confidently-wrong mass effect (Cleansing Beam, Surge of Zeal, the Wojek pingers).
    if ("SharesAColorWithPermanent" in blob) return null
    // "...that was dealt damage this turn" has no GroupFilter helper — decline rather than widen the group.
    if ("WasDealtDamageThisTurn" in blob) return null
    // "creatures you control WITH +1/+1 counters on them" (Badgermole's trample lord): a
    // `HasACounterOfType` predicate this flat GroupFilter can't express. Dropping it would widen the
    // grant to every creature you control, so decline (-> SCAFFOLD) rather than misrender.
    if ("HasACounterOfType" in blob) return null
    // An enchantment subtype ("another Shrine you control", "each Shrine") has no rendering on this
    // surface — widening it to GameObjectFilter.Enchantment/Permanent would silently drop the subtype.
    // Decline rather than emit a too-broad filter (The Spirit Oasis / Kyoshi Island Plaza Shrine triggers).
    if (filterNode.argWordsTagged("IsEnchantmentType").isNotEmpty()) return null
    val types = targetTypes(filterNode)
    val subs = subtypes(filterNode)
    // Creature subtypes come from IsCreatureType (subtypes() only collects land/card subtypes).
    val creatureSubs = filterNode.argWordsTagged("IsCreatureType")
    // "Each land and Ally you control" — a creature subtype unioned with a non-creature cardtype (the
    // "land" half). The single-creature-subtype branch below would render only the Ally clause and
    // silently drop the other half, so decline the compound rather than emit half the filter
    // (Great Divide Guide).
    if (creatureSubs.isNotEmpty() && (types - "Creature").any { it in setOf("Land", "Artifact", "Enchantment", "Planeswalker") }) return null
    var node: Dsl = when {
        // "outlaw" (IsAnOutlaw): Assassin/Mercenary/Pirate/Rogue/Warlock. Render the outlaw creature group
        // (matches Filters.OutlawCreature) rather than widening to any permanent (Vial Smasher).
        "IsAnOutlaw" in blob -> Lit("GameObjectFilter.Creature").dot("withAnyOfSubtypes", arg("Subtype.OUTLAW_TYPES"))
        subs.isNotEmpty() && ("Land" in types || "IsLandType" in blob || "\"Land\"" in blob) ->
            Lit("GameObjectFilter.Land").dot("withSubtype", arg(subtypeArg(subs[0])))
        // A creature subtype always implies creature, so render Creature.withSubtype even when there's no
        // explicit IsCardtype Creature (the "other Merfolk"/"other Goblins" lord groups) — otherwise the
        // ThisPermanent marker below would wrongly widen it to GameObjectFilter.Permanent.
        creatureSubs.size == 1 ->
            Lit("GameObjectFilter.Creature").dot("withSubtype", arg(subtypeArg(creatureSubs[0])))
        // Several creature subtypes -> withAnyOfSubtypes ("another Frog, Rabbit, Raccoon, or Squirrel"),
        // but ONLY for an explicit Or; an And of subtypes ("Goblin Wizard", all-of) has different
        // semantics, so decline rather than emit a wrong OR.
        creatureSubs.size > 1 ->
            (orCreatureSubtypes(filterNode) ?: return null)
                .let { Lit("GameObjectFilter.Creature").dot("withAnyOfSubtypes", arg(subtypeListArg(it))) }
        subs.isNotEmpty() && ("Creature" in types || "\"Creature\"" in blob) ->
            Lit("GameObjectFilter.Creature").dot("withSubtype", arg(subtypeArg(subs[0])))
        types == setOf("Creature", "Land") -> Lit("GameObjectFilter.CreatureOrLand")
        types == setOf("Creature", "Artifact") -> Lit("GameObjectFilter.CreatureOrArtifact")
        types == setOf("Creature", "Enchantment") -> Lit("GameObjectFilter.CreatureOrEnchantment")
        types == setOf("Artifact", "Enchantment") -> Lit("GameObjectFilter.ArtifactOrEnchantment")
        "Creature" in types || "\"Creature\"" in blob -> Lit("GameObjectFilter.Creature")
        "Land" in types || "\"Land\"" in blob -> Lit("GameObjectFilter.Land")
        "Artifact" in types || "\"Artifact\"" in blob -> Lit("GameObjectFilter.Artifact")
        "Enchantment" in types || "\"Enchantment\"" in blob -> Lit("GameObjectFilter.Enchantment")
        "Permanent" in blob -> Lit("GameObjectFilter.Permanent")
        else -> return null
    }
    val colors = filterNode.wordsAtKey("_Color").map { it.uppercase() }.distinct()
    if (colors.size > 1 && "\"Or\"" in blob) {
        node = node.dot("withAnyColor", *colors.map { arg("Color.$it") }.toTypedArray())
    } else if (colors.size == 1) {
        node = if (filterNode.hasTag("IsNonColor")) node.dot("notColor", arg("Color.${colors[0]}")) else node.dot("withColor", arg("Color.${colors[0]}"))
    }
    (FilterPredicates.withoutFlying(filterNode) ?: FilterPredicates.withFlying(filterNode))?.let { node = node.dot(it) }
    val powerOrToughness = FilterPredicates.powerOrToughnessAtLeast(filterNode)
    if (powerOrToughness != null) node = node.dot(powerOrToughness) else {
        FilterPredicates.powerAtLeast(filterNode)?.let { node = node.dot(it) }
        FilterPredicates.powerAtMost(filterNode)?.let { node = node.dot(it) }
    }
    FilterPredicates.manaValueAtMost(filterNode)?.let { node = node.dot(it) }
    FilterPredicates.manaValueAtLeast(filterNode)?.let { node = node.dot(it) }
    FilterPredicates.tapped(filterNode)?.let { node = node.dot(it) }
    FilterPredicates.untapped(filterNode)?.let { node = node.dot(it) }
    FilterPredicates.attacking(filterNode)?.let { node = node.dot(it) }
    FilterPredicates.nontoken(filterNode)?.let { node = node.dot(it) }
    // The `.youControl()`/`.opponentControls()` suffix is a *controller* predicate — only a
    // `ControlledByAPlayer` clause carries it. A bare `"You"` elsewhere in the blob (e.g. a graveyard
    // count's `InAPlayersGraveyard(You)` ownership clause, whose player scope is carried separately by
    // the enclosing DynamicAmount.Count) must NOT be misread as control of a battlefield permanent.
    val hasControllerClause = "ControlledByAPlayer" in blob
    if (hasControllerClause && "\"You\"" in blob) node = node.dot("youControl")
    if (hasControllerClause && "\"Opponent\"" in blob) node = node.dot("opponentControls")
    // A ControlledByAPlayer clause naming a player we can't render (e.g. Ref_TargetPlayer — "creatures
    // target opponent controls") must decline rather than silently widen to every creature on the
    // battlefield (Neutralize the Guards).
    if (hasControllerClause && "\"You\"" !in blob && "\"Opponent\"" !in blob) return null
    return node
}

/** Subtypes from an `Or[IsCreatureType…]` node ("Frog, Rabbit, Raccoon, or Squirrel"), in document
 *  order, or null when the creature subtypes aren't a flat OR (a lone subtype, or an `And` such as a
 *  "Goblin Wizard" all-of). Distinguishes the any-of tribal group from an all-of compound type. */
private fun orCreatureSubtypes(filterNode: JsonElement?): List<String>? {
    val orNode = filterNode.nodesTagged("Or").firstOrNull { or ->
        val kids = or["args"].asArr ?: return@firstOrNull false
        kids.isNotEmpty() && kids.all { it.strField("_Permanents") == "IsCreatureType" }
    } ?: return null
    return (orNode["args"].asArr ?: return null).mapNotNull { it.field("args").asStr() }
}

internal fun EmitCtx.revealedHandFilterDsl(filterNode: JsonElement?): String? = revealedHandFilterExpr(filterNode)?.let(::render)

internal fun EmitCtx.revealedHandFilterExpr(filterNode: JsonElement?): Dsl? {
    val landType = filterNode.firstArgStringTagged("IsLandType")
    val color = filterNode.firstWordAtKey("_Color")
    if (landType == null && color == null) return null
    val parts = mutableListOf<Dsl>()
    if (landType != null) parts.add(Lit("GameObjectFilter.Land").dot("withSubtype", arg(subtypeArg(landType))))
    if (color != null) parts.add(Lit("GameObjectFilter.Any").dot("withColor", arg("Color.${color.uppercase()}")))
    return Infix("or", parts, parenthesized = parts.size > 1)
}

internal fun EmitCtx.landSearchFilterDsl(filterNode: JsonElement?): String = render(landSearchFilterExpr(filterNode))

/**
 * The "basic land card and/or <subtype> card" union (Map the Frontier, Silver Deputy) rendered as
 * `GameObjectFilter.BasicLand or GameObjectFilter.Land.withSubtype("<subtype>")`, or null when the
 * filter isn't that exact union. The mtgish shape is an `Or` with a basic-land arm
 * (`And[IsSupertype "Basic", IsCardtype "Land"]`) and a land-subtype arm (`IsLandType "<subtype>"`).
 * Returning the faithful union (instead of either single arm) keeps the search from silently dropping
 * "basic land" or narrowing to the subtype only.
 */
private fun basicLandOrSubtypeUnion(filterNode: JsonElement?): Dsl? {
    // Must be an Or whose arms include a basic-land supertype clause.
    if (filterNode.nodesTagged("Or").isEmpty()) return null
    val hasBasic = filterNode.argWordsTagged("IsSupertype").any { it == "Basic" } &&
        filterNode.argWordsTagged("IsCardtype").any { it == "Land" }
    if (!hasBasic) return null
    val landSubtype = filterNode.firstArgStringTagged("IsLandType") ?: return null
    return Infix(
        "or",
        listOf(
            Lit("GameObjectFilter.BasicLand"),
            Lit("GameObjectFilter.Land").dot("withSubtype", arg(subtypeArg(landSubtype))),
        ),
        parenthesized = false,
    )
}

internal fun EmitCtx.landSearchFilterExpr(filterNode: JsonElement?): Dsl {
    // "basic land card and/or <subtype> card" (Map the Frontier, Silver Deputy): an Or unioning the
    // basic-land supertype (And[IsSupertype Basic, IsCardtype Land]) with a land subtype (IsLandType).
    // Render the faithful union `BasicLand or Land.withSubtype("<subtype>")` rather than collapsing to
    // either arm (the bare `subs` branch below would silently drop "basic land" and Desert-only it).
    basicLandOrSubtypeUnion(filterNode)?.let { return it }
    val subs = subtypes(filterNode)
    // Dual-land fetch ("a Swamp or Mountain card") -> Land + Or[HasSubtype…], i.e. withAnySubtype;
    // golden factors IsLand out (unlike the distributed creature-subtype form).
    if (subs.size >= 2) return Lit("GameObjectFilter.Land").dot("withAnySubtype", *subs.map { arg("\"$it\"") }.toTypedArray())
    if (subs.isNotEmpty()) return Lit("GameObjectFilter.Land").dot("withSubtype", arg(subtypeArg(subs[0])))
    val blob = compact(filterNode)
    val oracle = oracleText?.lowercase() ?: ""
    return when {
        "basic land" in oracle || "IsBasicLand" in blob -> Lit("GameObjectFilter.BasicLand")
        "sorcery card" in oracle || "\"Sorcery\"" in blob -> Lit("GameObjectFilter.Sorcery")
        "instant card" in oracle || "\"Instant\"" in blob -> Lit("GameObjectFilter.Instant")
        // "an artifact card" (Fabricate) / "an enchantment card": a single positive cardtype with no
        // creature clause. Render the matching filter — checked after the more specific types above and
        // before the plain Land/Creature fallthrough; "Creature" present means an artifact-creature
        // compound the Creature arm handles, so guard against it.
        ("\"Artifact\"" in blob) && "\"Creature\"" !in blob -> Lit("GameObjectFilter.Artifact")
        ("\"Enchantment\"" in blob) && "\"Creature\"" !in blob -> Lit("GameObjectFilter.Enchantment")
        "\"Land\"" in blob -> Lit("GameObjectFilter.Land")
        "\"Creature\"" in blob || "creature" in oracle -> {
            var out: Dsl = Lit("GameObjectFilter.Creature")
            // "black and/or red creature" -> withAnyColor; a single colour -> withColor; fall back to the
            // oracle's "black creature" wording only when the filter node carries no structured colour.
            val colors = filterNode.colorsOf("IsColor")
            when {
                colors.size > 1 -> out = out.dot("withAnyColor", *colors.map { arg("Color.${it.uppercase()}") }.toTypedArray())
                colors.size == 1 -> out = out.dot("withColor", arg("Color.${colors[0].uppercase()}"))
                "black creature" in oracle -> out = out.dot("withColor", arg("Color.BLACK"))
            }
            if ("tapped creature" in oracle) out = out.dot("tapped")
            if ("attacking" in oracle) out = out.dot("attacking")
            out
        }
        else -> Lit("GameObjectFilter.Any")
    }
}
