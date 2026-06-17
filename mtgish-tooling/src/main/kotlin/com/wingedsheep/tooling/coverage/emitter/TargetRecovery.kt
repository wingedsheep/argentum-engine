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
import kotlinx.serialization.json.JsonArray
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

/** The SDK [com.wingedsheep.sdk.core.Color] enum names — the only colours that render as a static
 *  `Color.X`. A filter IR colour token outside this set (e.g. "the chosen color" / "that color") has no
 *  static representation. */
private val SDK_COLOR_NAMES = setOf("WHITE", "BLUE", "BLACK", "RED", "GREEN")

/** The uppercased `Color.X` enum names for each colour [token], or null if ANY token is not one of the
 *  five real colours. Returning null forces the caller to decline (→ SCAFFOLD) rather than emit an
 *  invalid enum like `Color.THECHOSENCOLOR` (chosen-colour shapes) or silently drop the colour clause
 *  (which would wrongly widen the filter to every colour). */
internal fun colorEnumNames(tokens: List<String>): List<String>? =
    tokens.map { it.uppercase() }.takeIf { upper -> upper.all { it in SDK_COLOR_NAMES } }

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

    /** `.toughnessAtLeast(N)` for a `ToughnessIs >= N` clause, else null ("toughness 4 or greater"). */
    fun toughnessAtLeast(node: JsonElement?): Link? = toughnessBound(node, "GreaterThanOrEqualTo")?.let { Link("toughnessAtLeast", listOf(arg("$it"))) }

    /** `.toughnessAtMost(N)` for a `ToughnessIs <= N` clause, else null ("toughness 2 or less"). */
    fun toughnessAtMost(node: JsonElement?): Link? = toughnessBound(node, "LessThanOrEqualTo")?.let { Link("toughnessAtMost", listOf(arg("$it"))) }

    /** `.manaValueAtMost(N)` for a `ManaValueIs <= N` clause, else null (Smother's "mana value 3 or less"). */
    fun manaValueAtMost(node: JsonElement?): Link? = manaValueBound(node, "LessThanOrEqualTo")?.let { Link("manaValueAtMost", listOf(arg("$it"))) }

    /** `.manaValueAtLeast(N)` for a `ManaValueIs >= N` clause, else null. */
    fun manaValueAtLeast(node: JsonElement?): Link? = manaValueBound(node, "GreaterThanOrEqualTo")?.let { Link("manaValueAtLeast", listOf(arg("$it"))) }

    fun tapped(node: JsonElement?): Link? = if (node.hasTag("IsTapped")) Link("tapped") else null
    fun untapped(node: JsonElement?): Link? = if (node.hasTag("IsUntapped")) Link("untapped") else null
    fun attacking(node: JsonElement?): Link? = if (node.hasTag("IsAttacking")) Link("attacking") else null
    fun blocking(node: JsonElement?): Link? = if (node.hasTag("IsBlocking")) Link("blocking") else null

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

    /** The integer bound of a `ToughnessIs` clause whose `args` is `{ _Comparison, args: Integer }`,
     *  scoped to the matching `ToughnessIs` node so a toughness range's two bounds stay distinct. */
    private fun toughnessBound(node: JsonElement?, comparison: String): Int? =
        node.nodesTagged("ToughnessIs")
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
    // "nonlegendary creature" (Blind with Anger) — a negated supertype (IsNonSupertype "Legendary").
    // Only Legendary maps to a filter helper (.nonlegendary()); any other negated supertype has no
    // rendering, so decline rather than silently drop it. The special creature shapes below (attacking/
    // blocking/face-down/subtype/non-cardtype) can't compose .nonlegendary(), so if such a predicate is
    // also present, decline rather than widen — only the plain-creature path composes it (see below).
    val nonSupertypes = filterNode.argWordsTagged("IsNonSupertype")
    if (nonSupertypes.any { it != "Legendary" }) return null
    val nonlegendary = nonSupertypes.contains("Legendary")
    if (nonlegendary) {
        val unrenderableAlongside = listOf(
            "IsAttacking", "IsBlocking", "IsFaceDown", "IsCreatureType", "IsNonCreatureType", "Other",
        )
        if (unrenderableAlongside.any { it in blob }) return null
    }
    // "legendary creature" (Shinka / Shizo / Okina target abilities) — a positive supertype (IsSupertype
    // "Legendary"). Only Legendary maps to a filter helper (.legendary()); any other supertype declines.
    // As with nonlegendary, the special creature shapes below can't compose it, so decline if present.
    val supertypes = filterNode.argWordsTagged("IsSupertype")
    if (supertypes.any { it != "Legendary" }) return null
    val legendary = supertypes.contains("Legendary")
    if (legendary) {
        val unrenderableAlongside = listOf(
            "IsAttacking", "IsBlocking", "IsFaceDown", "IsCreatureType", "IsNonCreatureType", "Other",
        )
        if (unrenderableAlongside.any { it in blob }) return null
    }
    // "creature that dealt damage to you this turn" (Reciprocate) — a relational predicate with no SDK
    // target filter. Dropping it would let the spell hit any creature, so decline (-> SCAFFOLD).
    if ("DealtDamageToPlayerThisTurn" in blob) return null
    // "non-outlaw creature" (Shoot the Sheriff): IsNonOutlaw excludes the five outlaw creature types
    // (Assassin, Mercenary, Pirate, Rogue, Warlock) at once. Render the exact filter
    // Targets.NonOutlawCreature compiles to — Creature.notAnyOfSubtypes(Subtype.OUTLAW_TYPES). Only the
    // bare shape (optionally a You/Opponent controller) is modeled; any other predicate alongside it
    // would be silently dropped, so decline rather than widen.
    if ("IsNonOutlaw" in blob) {
        val otherPredicates = listOf(
            "IsTapped", "IsUntapped", "IsAttacking", "IsBlocking", "PowerIs", "ToughnessIs", "ManaValueIs",
            "IsColor", "IsNonColor", "HasAbility", "DoesntHaveAbility", "IsNonToken", "IsCreatureType",
            "IsNonCreatureType", "IsNonCardtype", "Other", "HasACounterOfType", "WasDealtDamageThisTurn",
        )
        if (otherPredicates.any { it in blob }) return null
        var g: Dsl = Lit("GameObjectFilter.Creature").dot("notAnyOfSubtypes", arg("Subtype.OUTLAW_TYPES"))
        when {
            "\"You\"" in blob -> g = g.dot("youControl")
            "\"Opponent\"" in blob -> g = g.dot("opponentControls")
            "ControlledByAPlayer" in blob || "ControlledByPlayer" in blob -> return null
        }
        return Call("TargetFilter", listOf(arg(g)))
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
    // "ANOTHER target creature you control" — `And(Other(<self>), IsCardtype Creature, ControlledByAPlayer
    // You)`. This exact self-exclusion + you-control shape has a named TargetFilter constant
    // (OtherCreatureYouControl) that composes excludeSelf, so render it directly (Cunning Coyote, Sterling
    // Supplier's ETB +1/+1). Guard strictly: only when the sole predicates are Other + Creature + You, with
    // no extra subtype / colour / power / keyword / state clause that the constant wouldn't carry.
    run {
        val otherYouControlExtras = listOf(
            "IsCreatureType", "IsNonCreatureType", "IsArtifactType", "IsLandType", "IsEnchantmentType",
            "IsNonCardtype", "IsColor", "IsNonColor", "PowerIs", "ToughnessIs", "ManaValueIs", "HasAbility",
            "DoesntHaveAbility", "IsTapped", "IsUntapped", "IsAttacking", "IsBlocking", "IsFaceDown",
            "IsNonToken", "HasACounterOfType", "WasDealtDamageThisTurn",
        )
        if (jsonContains(filterNode, "_Permanents", "Other") &&
            "Creature" in filterNode.argWordsTagged("IsCardtype") &&
            "\"You\"" in blob && "ControlledByAPlayer" in blob &&
            otherYouControlExtras.none { it in blob }
        ) {
            return Lit("TargetFilter.OtherCreatureYouControl")
        }
    }
    // "up to one OTHER target creature" with no controller restriction — `And(Other(<self/entering>),
    // IsCardtype Creature)`. The named TargetFilter constant `OtherCreature` composes excludeSelf, so
    // render it directly (Rimekin Recluse, Matterbending Mage). Guard strictly: only when the sole
    // predicates are Other + Creature, with no controller clause or extra subtype / colour / power /
    // keyword / state clause that the bare constant wouldn't carry — anything else falls through to the
    // strict decline below.
    run {
        val otherCreatureExtras = listOf(
            "IsCreatureType", "IsNonCreatureType", "IsArtifactType", "IsLandType", "IsEnchantmentType",
            "IsNonCardtype", "IsColor", "IsNonColor", "PowerIs", "ToughnessIs", "ManaValueIs", "HasAbility",
            "DoesntHaveAbility", "IsTapped", "IsUntapped", "IsAttacking", "IsBlocking", "IsFaceDown",
            "IsNonToken", "HasACounterOfType", "WasDealtDamageThisTurn", "ControlledByAPlayer", "ControlledByPlayer",
        )
        if (jsonContains(filterNode, "_Permanents", "Other") &&
            "Creature" in filterNode.argWordsTagged("IsCardtype") &&
            otherCreatureExtras.none { it in blob }
        ) {
            return Lit("TargetFilter.OtherCreature")
        }
    }
    // "ANOTHER target creature you control" — an `Other(ThisPermanent)` self-exclusion. The TargetFilter
    // surface built here doesn't compose excludeSelf, so silently dropping it would let the source target
    // itself (an illegal target). Decline (-> SCAFFOLD) rather than emit a target missing the "another"
    // restriction (Deserter's Disciple). GroupFilter's excludeSelf path is separate (groupFilterExpr).
    if (jsonContains(filterNode, "_Permanents", "Other")) return null
    // "target creature you control" / "...an opponent controls" — the controller restriction is a
    // ControlledByAPlayer clause. Preserve it as a `.youControl()` / `.opponentControls()` suffix; never
    // drop it (an unrestricted target would let the spell hit any creature). Only the plain-creature
    // path below can compose it, so the special shapes scaffold when a controller clause is present.
    // `ControlledByAPlayer` is the generic "you / an opponent" controller clause; `ControlledByPlayer`
    // names a specific player ref (e.g. the attack trigger's defending player). Either is a controller
    // restriction that must be preserved or the card declines — never silently widened.
    val hasController = "ControlledByAPlayer" in blob || "ControlledByPlayer" in blob
    val controller: Link? = when {
        !hasController -> null
        // "target creature defending player controls" in an attack trigger (Spring Splasher): the
        // defending player is by definition an opponent of the attacking source's controller.
        "\"Trigger_DefendingPlayer\"" in blob -> Link("opponentControls")
        "\"Opponent\"" in blob -> Link("opponentControls")
        "\"You\"" in blob -> Link("youControl")
        // "a creature that player controls" in a combat-damage trigger: the player ~ dealt damage to is an
        // opponent (your creature dealt them combat damage), so it's that opponent's creature (Skirk Commando).
        "\"Trigger_ThatPlayer\"" in blob -> Link("opponentControls")
        else -> return null
    }
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
        // The subtype-Or form composes only the subtypes. If the filter ALSO carries a state predicate
        // (e.g. "attacking Wolf or Werewolf" — Ulrich's Kindred), this branch would silently drop it and
        // widen the target. Decline so the card scaffolds rather than emit a too-broad filter.
        val statePredicates = listOf(
            "IsAttacking", "IsBlocking", "IsTapped", "IsUntapped", "PowerIs", "ToughnessIs", "ManaValueIs",
            "IsColor", "IsNonColor", "HasAbility", "DoesntHaveAbility", "IsNonToken", "HasACounterOfType",
            "WasDealtDamageThisTurn",
        )
        if (statePredicates.any { it in blob }) return null
        return Call("TargetFilter", listOf(arg(Infix("or", subs.map { Lit("GameObjectFilter.Creature").dot("withSubtype", arg("\"$it\"")) }))))
    }
    var node: Dsl = Lit("TargetFilter.Creature")
    if (nonlegendary) node = node.dot("nonlegendary")
    if (legendary) node = node.dot("legendary")
    if ("Artifact" in nonCardtypes) node = node.dot("nonartifact")
    // Color recovery, IsColor/IsNonColor-scoped: a single colour -> .withColor / .notColor; several
    // colours under an Or ("white or black creature") -> .withAnyColor so the extra colours aren't
    // dropped. Multiple excluded colours chain as .notColor (AND-of-not = "neither X nor Y").
    (colorEnumNames(filterNode.colorsOf("IsNonColor")) ?: return null).forEach { node = node.dot("notColor", arg("Color.$it")) }
    val colors = colorEnumNames(filterNode.colorsOf("IsColor")) ?: return null
    when {
        colors.size == 1 -> node = node.dot("withColor", arg("Color.${colors[0]}"))
        colors.size > 1 -> node = node.dot("withAnyColor", *colors.map { arg("Color.$it") }.toTypedArray())
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
        FilterPredicates.toughnessAtMost(filterNode)?.let { node = node.dot(it) }
        FilterPredicates.toughnessAtLeast(filterNode)?.let { node = node.dot(it) }
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
        // "target player dealt damage by this creature this turn" (Wicked Akuba) is a source-relative
        // restriction with no SDK target filter — decline rather than emit an unrestricted target player.
        if ("DealtDamage" in compact(tnode)) return null
        return if (jsonContains(tnode, "_Players", "Opponent")) Call("TargetOpponent") else Call("TargetPlayer")
    }
    // "any number of target players" / "any number of target opponents" — an unbounded player target
    // (Tinybones Joins Up). Maps to TargetPlayer/TargetOpponent with `unlimited = true`; pairs with a
    // ForEachTargetEffect body to apply a per-player effect.
    if (ttype == "AnyNumberOfTargetPlayers") {
        return if (jsonContains(tnode, "_Players", "Opponent"))
            Call("TargetOpponent", listOf(arg("unlimited", "true")))
        else Call("TargetPlayer", listOf(arg("unlimited", "true")))
    }
    if (ttype == "AnyTarget" || ttype == "TargetPlayerOrPermanent") {
        val blob = compact(tnode)
        if ("Planeswalker" in blob && "Player" in blob && "Opponent" in blob) return Call("TargetOpponentOrPlaneswalker")
        if ("Planeswalker" in blob && "Player" in blob) return Call("TargetPlayerOrPlaneswalker")
        if ("Planeswalker" in blob && "Creature" in blob) return Call("TargetCreatureOrPlaneswalker")
        if (actionContext != null && actionContext.consumesOnlyTargetPlayer()) return Call("TargetPlayer")
        return Call("AnyTarget")
    }
    if (ttype in setOf("TargetPermanent", "NumberTargetPermanents", "UptoNumberTargetPermanents", "OneOrTwoTargetPermanents", "UptoOneTargetPermanent")) {
        val types = targetTypes(args)
        val blob = compact(args)
        // "up to one target …" — a single optional permanent target (Peerless Ropemaster, Nurturing
        // Pixie). It renders the same filter as a plain TargetPermanent/TargetCreature with `optional =
        // true` (no count: "up to one" is exactly one-or-zero). The flag is appended in every sub-branch
        // below via [optionalArg].
        val isUpToOne = ttype == "UptoOneTargetPermanent"
        // A creature-subtype clause (IsCreatureType) must be matched as a whole token, not a substring of
        // IsNonCreatureType ("non-Faerie permanent"): the latter is a NEGATED subtype on a non-creature
        // permanent target, and a naive `"IsCreatureType" in blob` would mis-route it to the creature path.
        val hasCreatureSubtype = args.argWordsTagged("IsCreatureType").isNotEmpty()
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
            if (types.isNotEmpty() || "IsNonCardtype" in blob || hasCreatureSubtype) return null
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
            if (ttype == "UptoNumberTargetPermanents" || isUpToOne) parts.add(0, arg("optional", "true"))
            return Call("TargetPermanent", parts)
        }
        // A creature-subtype restriction ("target Wall") implies a creature target even with no explicit
        // IsCardtype Creature; route it through the creature filter so the subtype isn't dropped (Tunnel).
        val creatureTarget = types == setOf("Creature") || (types.isEmpty() && hasCreatureSubtype)
        if (creatureTarget) {
            val filter = creatureFilterExpr(args) ?: return null
            val parts = mutableListOf(arg("filter", filter))
            if (ttype in setOf("NumberTargetPermanents", "UptoNumberTargetPermanents") && countInt is Int) parts.add(0, arg("count", "$countInt"))
            if (ttype == "OneOrTwoTargetPermanents") { parts.add(0, arg("minCount", "1")); parts.add(0, arg("count", "2")) }
            if (ttype == "UptoNumberTargetPermanents" || isUpToOne) parts.add(0, arg("optional", "true"))
            return Call("TargetCreature", parts)
        }
        // "target creature or planeswalker[, an opponent controls]" (Annie Joins Up's ETB damage):
        // an Or unioning the two cardtypes Creature and Planeswalker, optionally narrowed by a
        // controller clause. TargetCreatureOrPlaneswalker carries no filter field, so the controller
        // restriction is rendered as TargetObject(filter = TargetFilter(
        // GameObjectFilter.CreatureOrPlaneswalker[.youControl()/.opponentControls()])). Only the bare
        // two-cardtype Or (optionally one You/Opponent controller clause, no count, subtype, or other
        // predicate) renders; anything else declines (-> SCAFFOLD) rather than dropping a restriction.
        run {
            if (types != setOf("Creature", "Planeswalker")) return@run
            if (hasCreatureSubtype || "IsNonCardtype" in blob || "IsNonCreatureType" in blob ||
                jsonContains(args, "_Permanents", "Other")) return@run
            // Only the union arms (Creature/Planeswalker) and an optional ControlledByAPlayer clause
            // may be present; any other restrictive predicate must decline.
            val extras = listOf(
                "IsTapped", "IsUntapped", "IsAttacking", "IsBlocking", "PowerIs", "ToughnessIs",
                "ManaValueIs", "IsColor", "IsNonColor", "HasAbility", "DoesntHaveAbility", "IsNonToken",
                "IsToken", "IsSupertype", "IsArtifactType", "ManaValueAtMost", "ManaValueAtLeast",
            )
            if (extras.any { it in blob }) return@run
            val controller: Link? = when {
                "ControlledByAPlayer" !in blob -> null
                "\"You\"" in blob -> Link("youControl")
                "\"Opponent\"" in blob -> Link("opponentControls")
                else -> return null
            }
            if (controller == null) {
                // No controller restriction — the named TargetCreatureOrPlaneswalker is exact.
                val parts = listOfNotNull(
                    if (ttype in setOf("NumberTargetPermanents", "UptoNumberTargetPermanents") && countInt is Int) arg("count", "$countInt") else null,
                    if (ttype == "UptoNumberTargetPermanents" || isUpToOne) arg("optional", "true") else null,
                )
                return Call("TargetCreatureOrPlaneswalker", parts)
            }
            var g: Dsl = Lit("GameObjectFilter.CreatureOrPlaneswalker").dot(controller.method)
            val filt = Call("TargetFilter", listOf(arg(g)))
            val parts = mutableListOf(arg("filter", filt))
            if (ttype in setOf("NumberTargetPermanents", "UptoNumberTargetPermanents") && countInt is Int) parts.add(0, arg("count", "$countInt"))
            if (ttype == "UptoNumberTargetPermanents" || isUpToOne) parts.add(0, arg("optional", "true"))
            return Call("TargetObject", parts)
        }
        // "target Spirit or enchantment" (Urgent Exorcism): an Or unioning a creature subtype with a
        // single non-creature cardtype. The single-type branch below would render the cardtype arm
        // alone, silently dropping the subtype arm — render the faithful union instead. Only the bare
        // two-arm Or on a plain TargetPermanent (no count, controller, or self-exclusion) is modeled.
        run {
            val orNode = args.nodesTagged("Or").firstOrNull { or ->
                val kids = or["args"].asArr ?: return@firstOrNull false
                kids.size == 2 && kids.count { it.strField("_Permanents") == "IsCreatureType" } == 1 &&
                    kids.count { it.strField("_Permanents") == "IsCardtype" } == 1
            } ?: return@run
            val kids = orNode["args"].asArr!!
            val sub = kids.first { it.strField("_Permanents") == "IsCreatureType" }.field("args").asStr() ?: return null
            val type = kids.first { it.strField("_Permanents") == "IsCardtype" }.field("args").asStr() ?: return null
            val typeFilter = mapOf(
                "Enchantment" to "GameObjectFilter.Enchantment",
                "Artifact" to "GameObjectFilter.Artifact",
                "Land" to "GameObjectFilter.Land",
            )[type] ?: return null
            if (ttype != "TargetPermanent" || "ControlledByAPlayer" in blob || jsonContains(args, "_Permanents", "Other")) return null
            val union = Infix("or", listOf(Lit("GameObjectFilter.Creature").dot("withSubtype", arg(subtypeArg(sub))), Lit(typeFilter)))
            return Call("TargetPermanent", listOf(arg("filter", Call("TargetFilter", listOf(arg(union))))))
        }
        val singleType = mapOf("Land" to "TargetFilter.Land", "Artifact" to "TargetFilter.Artifact", "Enchantment" to "TargetFilter.Enchantment")
        if (types.size == 1 && types.first() in singleType) {
            // "noncreature artifact" (Blinkmoth Well): an IsNonCardtype restriction on top of the single
            // cardtype that no land/artifact/enchantment target filter expresses (there is no .noncreature()).
            // Decline rather than widen to "any artifact".
            if ("IsNonCardtype" in blob) return null
            // An unrendered creature-subtype clause alongside the single cardtype (an Or arm the union
            // branch above didn't model) must decline rather than silently drop it (-> SCAFFOLD).
            if ("IsCreatureType" in blob) return null
            // "target nonbasic land" (Rocket Volley, Encroaching Wastes): an IsNonSupertype "Basic" clause
            // on the single Land cardtype. Render `.nonbasic()`; any OTHER negated supertype (or any
            // positive supertype) on a land/artifact/enchantment has no rendering, so decline rather than
            // silently drop it and widen the target (e.g. to all lands, including basics).
            val nonSupertypes = args.argWordsTagged("IsNonSupertype")
            val nonbasic = nonSupertypes == listOf("Basic") && types.first() == "Land"
            if (nonSupertypes.any { !(it == "Basic" && nonbasic) }) return null
            if (args.argWordsTagged("IsSupertype").isNotEmpty()) return null
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
            if (nonbasic) f = f.dot("nonbasic")
            val parts = mutableListOf(arg("filter", f))
            if (ttype in setOf("NumberTargetPermanents", "UptoNumberTargetPermanents") && countInt is Int) parts.add(0, arg("count", "$countInt"))
            if (ttype == "UptoNumberTargetPermanents" || isUpToOne) parts.add(0, arg("optional", "true"))
            return Call("TargetPermanent", parts)
        }
        // "non-<creature-type>, nonland permanent you control" (Nurturing Pixie: "non-Faerie, nonland
        // permanent you control") — an IsNonCardtype "Land" + a single negated creature subtype
        // (IsNonCreatureType), optionally a controller clause. There's no named TargetFilter constant for
        // it, so render GameObjectFilter.NonlandPermanent.notSubtype(Subtype(...))[.youControl()] wrapped
        // in TargetFilter. Only this exact shape (one negated subtype, optional You/Opponent controller,
        // nothing else) renders; any extra predicate declines rather than silently dropping it.
        run {
            val negSubs = args.argWordsTagged("IsNonCreatureType")
            if (types.isEmpty() && args.argWordsTagged("IsNonCardtype") == listOf("Land") && negSubs.size == 1) {
                val extras = listOf(
                    "IsTapped", "IsUntapped", "IsAttacking", "IsBlocking", "PowerIs", "ToughnessIs",
                    "ManaValueIs", "IsColor", "IsNonColor", "HasAbility", "DoesntHaveAbility", "IsNonToken",
                    "IsCreatureType", "IsArtifactType", "IsLandType", "IsEnchantmentType", "Other",
                    "HasACounterOfType",
                )
                if (extras.any { it in blob }) return null
                val controller: Link? = when {
                    "ControlledByAPlayer" !in blob -> null
                    "\"You\"" in blob -> Link("youControl")
                    "\"Opponent\"" in blob -> Link("opponentControls")
                    else -> return null
                }
                var g: Dsl = Lit("GameObjectFilter.NonlandPermanent").dot("notSubtype", arg("Subtype(\"${negSubs[0]}\")"))
                controller?.let { g = g.dot(it) }
                val parts = mutableListOf(arg("filter", Call("TargetFilter", listOf(arg(g)))))
                if (ttype in setOf("NumberTargetPermanents", "UptoNumberTargetPermanents") && countInt is Int) parts.add(0, arg("count", "$countInt"))
                if (ttype == "UptoNumberTargetPermanents" || isUpToOne) parts.add(0, arg("optional", "true"))
                return Call("TargetPermanent", parts)
            }
        }
        // "target nonland permanent" — an IsNonCardtype "Land" with no positive cardtype restriction
        // (Thistledown Players' "untap target nonland permanent"). An optional "with mana value X"
        // clause (ManaValueIs EqualTo ValueX, from an {X}… cast cost) renders as .manaValueEqualsX()
        // (Repeal). A ManaValueIs clause we DON'T render (any other shape) must decline rather than
        // silently drop the restriction — an unrestricted bounce would hit any permanent. (The
        // IsCreatureType substring guard also excludes IsNonCreatureType, which the branch above handles.)
        if (types.isEmpty() && args.argWordsTagged("IsNonCardtype") == listOf("Land") && "IsCreatureType" !in blob) {
            val manaValueX = manaValueEqualsXClause(args)
            if ("ManaValueIs" in blob && !manaValueX) return null  // unrendered MV restriction -> SCAFFOLD
            // "target nonland permanent AN OPPONENT CONTROLS" (Lassoed by the Law) — the controller
            // restriction is a ControlledByAPlayer clause. Opponent maps to the named
            // TargetFilter.NonlandPermanentOpponentControls; You composes
            // TargetFilter(GameObjectFilter.NonlandPermanent.youControl()) (no named constant exists). A
            // controller clause we can't render exactly declines (-> SCAFFOLD) rather than silently
            // widening to any nonland permanent, and a ManaValueIs+controller combo we don't model declines.
            val controller: Link? = when {
                "ControlledByAPlayer" !in blob -> null
                manaValueX -> return null  // .manaValueEqualsX() + controller has no composed shape here
                "\"You\"" in blob -> Link("youControl")
                "\"Opponent\"" in blob -> Link("opponentControls")
                else -> return null
            }
            var f: Dsl = when {
                controller == null -> Lit("TargetFilter.NonlandPermanent")
                controller.method == "opponentControls" -> Lit("TargetFilter.NonlandPermanentOpponentControls")
                else -> Call("TargetFilter", listOf(arg(Lit("GameObjectFilter.NonlandPermanent").dot(controller))))
            }
            if (manaValueX) f = f.dot("manaValueEqualsX")
            val parts = mutableListOf(arg("filter", f))
            if (ttype in setOf("NumberTargetPermanents", "UptoNumberTargetPermanents") && countInt is Int) parts.add(0, arg("count", "$countInt"))
            if (ttype == "UptoNumberTargetPermanents" || isUpToOne) parts.add(0, arg("optional", "true"))
            return Call("TargetPermanent", parts)
        }
        if (types.isEmpty() && "IsCardtype" !in blob && "IsCreatureType" !in blob && "IsNonCardtype" !in blob &&
            "IsArtifactType" !in blob && "IsLandType" !in blob && "IsEnchantmentType" !in blob) {
            // "untap ANOTHER target permanent YOU CONTROL" (North Pole Patrol): a bare TargetPermanent would
            // silently drop the controller (ControlledByAPlayer) and self-exclusion (Other) restrictions,
            // widening it to any permanent. Decline (-> SCAFFOLD) rather than emit a too-broad target.
            if ("ControlledByAPlayer" in blob || jsonContains(args, "_Permanents", "Other")) return null
            // "target legendary permanent" (Minamo, School at Water's Edge) — a positive supertype on an
            // otherwise-any permanent. Render TargetFilter.Permanent.legendary(); any other supertype has
            // no rendering, so decline rather than silently drop it.
            val supertypes = args.argWordsTagged("IsSupertype")
            if (supertypes.any { it != "Legendary" }) return null
            if (supertypes.contains("Legendary"))
                return Call("TargetPermanent", listOf(arg("filter", Lit("TargetFilter.Permanent").dot("legendary"))))
            return Call("TargetPermanent")
        }
        // A two-cardtype union ("artifact or creature", "creature or enchantment"). The base maps to a
        // named TargetFilter constant; a controller clause ("...an opponent controls", Mystical Tether)
        // narrows the underlying GameObjectFilter union via .opponentControls()/.youControl() wrapped in
        // TargetFilter (no named controller-narrowed union constant exists). A controller clause we can't
        // render exactly declines (-> SCAFFOLD) rather than silently dropping the "an opponent controls".
        val multiTypeFilter = mapOf(
            setOf("Creature", "Land") to "CreatureOrLand",
            setOf("Creature", "Artifact") to "CreatureOrArtifact",
            setOf("Creature", "Enchantment") to "CreatureOrEnchantment",
            setOf("Artifact", "Enchantment") to "ArtifactOrEnchantment",
        )
        val namedTargetFilter = mapOf(
            setOf("Creature", "Land") to "TargetFilter.CreatureOrLandPermanent",
            setOf("Creature", "Artifact") to "TargetFilter.CreatureOrArtifact",
            setOf("Creature", "Enchantment") to "TargetFilter.CreatureOrEnchantment",
            setOf("Artifact", "Enchantment") to "TargetFilter.ArtifactOrEnchantment",
        )
        multiTypeFilter[types]?.let { union ->
            val controller: Link? = when {
                "ControlledByAPlayer" !in blob -> null
                "\"You\"" in blob -> Link("youControl")
                "\"Opponent\"" in blob -> Link("opponentControls")
                else -> return null
            }
            val filter: Dsl = if (controller == null) {
                Lit(namedTargetFilter.getValue(types))
            } else {
                Call("TargetFilter", listOf(arg(Lit("GameObjectFilter.$union").dot(controller))))
            }
            return Call("TargetPermanent", listOf(arg("filter", filter)))
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
        val colors = colorEnumNames(args.colorsOf("IsColor")) ?: return null
        val nonColors = colorEnumNames(args.colorsOf("IsNonColor")) ?: return null
        val nonTypes = args.argWordsTagged("IsNonCardtype")
        if (types.isEmpty() && colors.isEmpty() && nonColors.isEmpty() && nonTypes == listOf("Creature")) {
            return Call("TargetSpell", listOf(arg("filter", "TargetFilter.NoncreatureSpellOnStack")))
        }
        if (nonTypes.isNotEmpty()) return null  // non-type + extra predicates not rendered -> SCAFFOLD
        // "counter target nonblue spell" (Frazzle) — an IsNonColor clause on a stack spell. AND-of-not =
        // "neither X nor Y"; each excluded colour chains as .notColor. SDK supports it on SpellOnStack.
        if (types.isEmpty() && colors.isEmpty() && nonColors.isNotEmpty()) {
            var f: Dsl = Lit("TargetFilter.SpellOnStack")
            nonColors.forEach { f = f.dot("notColor", arg("Color.$it")) }
            return Call("TargetSpell", listOf(arg("filter", f)))
        }
        if (nonColors.isNotEmpty()) return null  // nonColor + (type | positive colour) combo not rendered -> SCAFFOLD
        // "counter target blue spell" — a colour-restricted spell on the stack.
        if (types.isEmpty() && colors.isNotEmpty()) {
            var f: Dsl = Lit("TargetFilter.SpellOnStack")
            f = if (colors.size == 1) f.dot("withColor", arg("Color.${colors[0]}"))
                else f.dot("withAnyColor", *colors.map { arg("Color.$it") }.toTypedArray())
            return Call("TargetSpell", listOf(arg("filter", f)))
        }
        if (colors.isNotEmpty()) return null  // colour + type combo not rendered yet -> SCAFFOLD
        if (types == setOf("Creature", "Sorcery")) return Call("TargetSpell", listOf(arg("filter", "TargetFilter.CreatureOrSorcerySpellOnStack")))
        if (types == setOf("Instant", "Sorcery")) return Call("TargetSpell", listOf(arg("filter", "TargetFilter.InstantOrSorcerySpellOnStack")))
        if (types == setOf("Creature")) return Call("TargetSpell", listOf(arg("filter", "TargetFilter.CreatureSpellOnStack")))
        if (types.isEmpty()) {
            // "counter target spell with mana value N or less" (Thoughtbind) -> SpellOnStack.manaValueAtMost(N).
            FilterPredicates.manaValueAtMost(args)?.let {
                return Call("TargetSpell", listOf(arg("filter", Lit("TargetFilter.SpellOnStack").dot(it))))
            }
            // A bare "target spell" is only correct with no further restriction. A creature-type or
            // spell-subtype restriction ("Spirit or Arcane spell", Hisoka's Defiance), or any other mana-value
            // comparison, has no SDK stack-spell filter — decline (-> SCAFFOLD) rather than emit an
            // unrestricted counter that hits any spell.
            if ("IsCreatureType" in blob || "IsSpellType" in blob || "IsSupertype" in blob || "ManaValueIs" in blob) return null
            return Call("TargetSpell")
        }
        return null
    }
    if (ttype == "TargetSpellOrAbility") {
        val blob = compact(args)
        // "...with a single target" (Return the Favor mode 2 / Willbender) -> the single-target
        // spell-or-ability requirement that the change-target effect consumes.
        if ("HasASingleTarget" in blob) return Lit("Targets.SpellOrAbilityWithSingleTarget")
        // "instant spell, sorcery spell, activated ability, or triggered ability" — the four-way Or
        // (Return the Favor mode 1). Match it exactly; any other spell/ability union declines.
        val isInstant = jsonContains(args, "_Spells", "IsCardtype") && "\"Instant\"" in blob
        val isSorcery = "\"Sorcery\"" in blob
        val hasActivated = "ActivatedAbility" in blob
        val hasTriggered = "TriggeredAbility" in blob
        if ("\"Or\"" in blob && isInstant && isSorcery && hasActivated && hasTriggered) {
            return Lit("Targets.InstantSorcerySpellOrAbility")
        }
        return null
    }
    if (ttype == "TargetGraveyardCard" || ttype == "UptoOneTargetGraveyardCard" ||
        ttype == "UptoNumberTargetGraveyardCards") {
        val blob = compact(args)
        // "up to N target … cards from your graveyard" (Pull from the Grave): a bounded, optional
        // multi-target graveyard slot — args is `[count, filter]`. It renders the same recovered filter
        // as the single/up-to-one variants, plus `count = N` and `optional = true` (minCount becomes 0).
        // The companion PutEachGraveyardCardIntoHand action moves each chosen card via ForEachTargetEffect.
        val isUptoNumberGrave = ttype == "UptoNumberTargetGraveyardCards"
        // The bound is either a fixed top-level `Integer` count, or a cast-time `X` ("up to X target
        // instant and/or sorcery cards" — Divergent Equation's `{X}{X}{U}`). The X bound maps to
        // `dynamicMaxCount = DynamicAmount.XValue`, which the engine evaluates as the spell goes on the
        // stack to cap the target overlay (Distorting Wake / Restock pattern). A DIFFERENT derived bound
        // ("up to X … where X is the number of black permanents an opponent controls" — Reap, encoded as
        // `TheNumberOfPermanentsOnTheBattlefield`) is NOT a simple {X} value the target shape can carry, so
        // decline -> SCAFFOLD rather than silently drop the cap and let the spell return any number of cards.
        var graveDynamicMax: String? = null
        if (isUptoNumberGrave) {
            val countNode = (args as? JsonArray)?.getOrNull(0) as? JsonObject
            when (countNode?.strField("_GameNumber")) {
                "Integer" -> {}
                "X", "XValue", "ValueX" -> graveDynamicMax = "DynamicAmount.XValue"
                else -> return null
            }
        }
        val graveCount = if (isUptoNumberGrave && graveDynamicMax == null) (countInt as? Int) else null
        // Prepend `count`/`optional`/`dynamicMaxCount` to a graveyard TargetObject's args, covering the
        // up-to-one (optional only), bounded up-to-N (count + optional), and X-clamped up-to-X
        // (dynamicMaxCount + optional) graveyard target variants.
        fun withGraveCounting(parts: MutableList<com.wingedsheep.tooling.coverage.Arg>): MutableList<com.wingedsheep.tooling.coverage.Arg> {
            graveDynamicMax?.let { parts.add(0, arg("dynamicMaxCount", Lit(it))) }
            if (graveCount != null) parts.add(0, arg("count", "$graveCount"))
            if (ttype == "UptoOneTargetGraveyardCard" || isUptoNumberGrave) parts.add(0, arg("optional", "true"))
            return parts
        }
        // "target BASIC land card from your graveyard" (Groundskeeper): a supertype restriction none of the
        // graveyard branches below compose. Decline rather than drop "basic" and widen to any land.
        if ("IsSupertype" in blob) return null
        // "target permanent card with mana value N or less from your graveyard" (Shepherd of the Clouds):
        // an IsPermanent type check + an optional ManaValueIs <= N cap + an ownership clause. There's no
        // named graveyard TargetFilter constant for "permanent card", so compose
        // GameObjectFilter.Permanent[.ownedBy…].manaValueAtMost(N) in the GRAVEYARD zone. Only this exact
        // shape (IsPermanent, at most one `<=` MV cap, optional You/Opponent ownership, no creature
        // subtype or other predicate) renders; anything else falls through to the branches below or
        // declines, so we never silently drop a restriction.
        if ("IsPermanent" in blob && "IsCreatureType" !in blob && targetTypes(args).isEmpty()) {
            val mvCap = FilterPredicates.manaValueAtMost(args)
            // A ManaValueIs clause we couldn't render as a `<=` cap (any other comparison/X) must decline
            // rather than widen the target to any permanent card in the graveyard.
            if ("ManaValueIs" in blob && mvCap == null) return null
            // "target NONLAND permanent card from your graveyard" (Moment of Reckoning's reanimate mode)
            // arrives as `IsPermanent` AND `IsNonCardtype "Land"`. Render the nonland-permanent group so
            // the "nonland" restriction isn't silently dropped (which would let it reanimate lands); any
            // other negated cardtype on a permanent-card target has no rendering here, so decline.
            val isNonland = args.argWordsTagged("IsNonCardtype") == listOf("Land")
            if (args.hasTag("IsNonCardtype") && !isNonland) return null
            var g: Dsl = if (isNonland) Lit("GameObjectFilter.NonlandPermanent") else Lit("GameObjectFilter.Permanent")
            when {
                "\"You\"" in blob -> g = g.dot("ownedByYou")
                "\"Opponent\"" in blob -> g = g.dot("ownedByOpponent")
            }
            mvCap?.let { g = g.dot(it) }
            val filt = Call("TargetFilter", listOf(arg(g), arg("zone", "Zone.GRAVEYARD")))
            val parts = mutableListOf(arg("filter", filt))
            return Call("TargetObject", withGraveCounting(parts))
        }
        // "target artifact or creature card with mana value N or less from your graveyard" (Lorehold
        // Charm): an `Or[IsCardtype X, IsCardtype Y]` over two single-card-types, plus a `<=` MV cap and
        // an optional ownership clause, no creature subtype. The typed branches below handle a single
        // card type only, so compose the OR explicitly:
        // `GameObjectFilter.X[.ownedBy…].manaValueAtMost(N).or(GameObjectFilter.Y[.ownedBy…].manaValueAtMost(N))`.
        // Only the two-type OR with both types in [graveyardCardtypeFilters] renders; anything else falls
        // through (a single-type ManaValueIs cap declines at the guard below), so no restriction is dropped.
        run {
            val ts = targetTypes(args).toList()
            if (ts.size == 2 && ts.all { it in graveyardCardtypeFilters } &&
                "IsCreatureType" !in blob && "\"Or\"" in blob) {
                val mvCap = FilterPredicates.manaValueAtMost(args)
                if ("ManaValueIs" in blob && mvCap == null) return null
                val owner: Link? = when {
                    "\"You\"" in blob -> Link("ownedByYou")
                    "\"Opponent\"" in blob -> Link("ownedByOpponent")
                    else -> null
                }
                fun typeFilter(t: String): Dsl {
                    var g: Dsl = Lit("GameObjectFilter.${graveyardCardtypeFilters.getValue(t)}")
                    owner?.let { g = g.dot(it) }
                    mvCap?.let { g = g.dot(it) }
                    return g
                }
                val combined = typeFilter(ts[0]).dot("or", arg(typeFilter(ts[1])))
                val filt = Call("TargetFilter", listOf(arg(combined), arg("zone", "Zone.GRAVEYARD")))
                val parts = mutableListOf(arg("filter", filt))
                return Call("TargetObject", withGraveCounting(parts))
            }
        }
        // "target artifact card WITH MANA VALUE 1 OR LESS from your graveyard" (Auriok Salvagers, Leonin
        // Squire): a ManaValueIs cap the graveyard filters below don't compose — emitting without it would
        // widen the target to any artifact in the graveyard. Decline (-> SCAFFOLD) rather than drop it.
        if ("ManaValueIs" in blob) return null
        val types = targetTypes(args)
        // "target Mount or Vehicle card" / "target Aura or Equipment card" from a graveyard (One Last
        // Job modes 2 & 3): an `Or` of subtype clauses drawn from MORE THAN ONE type category
        // (IsCreatureType Mount + IsArtifactType Vehicle; IsEnchantmentType Aura + IsArtifactType
        // Equipment). The single-category `graveyardSubs` branch below only sees the creature subtypes
        // and would silently drop the others, so handle the cross-category `Or` here as
        // `GameObjectFilter.Any.withAnySubtype(…)`. Only the plain Or-of-subtypes shape (no IsCardtype,
        // no mana-value cap) renders; anything else falls through.
        run {
            val anySubs = listOf("IsCreatureType", "IsArtifactType", "IsEnchantmentType", "IsLandType")
                .flatMap { args.argWordsTagged(it) }
            if (anySubs.size >= 2 && "\"Or\"" in blob && "IsCardtype" !in blob && "ManaValueIs" !in blob) {
                // withAnySubtype takes vararg String (not Subtype), so emit quoted subtype names.
                val subtypeArgs = anySubs.map { arg(Lit("\"${ktStr(it)}\"")) }.toTypedArray()
                val base = Lit("GameObjectFilter.Any").dot("withAnySubtype", *subtypeArgs)
                val filt = graveyardFilter(base, blob)
                val parts = mutableListOf(arg("filter", filt))
                return Call("TargetObject", withGraveCounting(parts))
            }
        }
        // "target Spirit card from your graveyard" (Angel of Flight Alabaster): a creature-subtype
        // restriction, usually with an ownership clause. The bare CardInGraveyard constant used to
        // swallow this shape, silently dropping BOTH constraints (any card, any graveyard).
        val graveyardSubs = args.argWordsTagged("IsCreatureType")
        val filt: Dsl = when {
            types.isEmpty() && "IsCardtype" !in blob -> when {
                // "target Arcane card from your graveyard" (Hana Kami) — a spell-subtype or supertype
                // restriction with no graveyard target filter. Decline (-> SCAFFOLD) rather than let the
                // spell return any card from the graveyard. (Creature subtypes ARE handled below.)
                "IsSpellType" in blob || "IsSupertype" in blob -> return null
                graveyardSubs.size == 1 ->
                    graveyardFilter(Lit("GameObjectFilter.Any").dot("withSubtype", arg(subtypeArg(graveyardSubs[0]))), blob)
                graveyardSubs.isEmpty() && ("\"You\"" in blob || "\"Opponent\"" in blob) ->
                    graveyardFilter(Lit("GameObjectFilter.Any"), blob)  // "from your graveyard" — keep ownership
                graveyardSubs.isEmpty() -> Lit("TargetFilter.CardInGraveyard")
                else -> return null
            }
            // A creature-subtype clause the typed branches below don't compose ("target Zombie creature
            // card") must decline rather than widen to any creature/type card.
            graveyardSubs.isNotEmpty() -> return null
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
        // (zero-or-one) variant renders the same filter under `optional = true`; the bounded
        // "up to N" variant (Pull from the Grave) adds `count = N` as well.
        val parts = mutableListOf(arg("filter", filt))
        return Call("TargetObject", withGraveCounting(parts))
    }
    return null
}

/** Card types that map to a same-named `GameObjectFilter.<Type>` constant — for composing an
 *  `Or`-of-card-types graveyard target (e.g. "artifact or creature card …", Lorehold Charm). */
private val graveyardCardtypeFilters = mapOf(
    "Artifact" to "Artifact",
    "Creature" to "Creature",
    "Enchantment" to "Enchantment",
    "Instant" to "Instant",
    "Land" to "Land",
    "Sorcery" to "Sorcery",
)

private val graveyardSingleTypeFilters = mapOf(
    "Artifact" to "Artifact",
    "Enchantment" to "Enchantment",
    "Instant" to "Instant",
    "Land" to "Land",
    "Sorcery" to "Sorcery",
)

private fun graveyardFilter(gameObjectFilter: String, blob: String): Dsl =
    graveyardFilter(Lit("GameObjectFilter.$gameObjectFilter"), blob)

private fun graveyardFilter(base: Dsl, blob: String): Dsl {
    val owner: Link? = when {
        "\"You\"" in blob -> Link("ownedByYou")
        "\"Opponent\"" in blob -> Link("ownedByOpponent")
        else -> null
    }
    var b = base
    owner?.let { b = b.dot(it) }
    return Call("TargetFilter", listOf(arg(b), arg("zone", "Zone.GRAVEYARD")))
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
    // "each of the target permanents" (Felidar Savior's "+1/+1 counter on each of up to two target
    // creatures") arrives as a `Ref_TargetPermanent(s)` reference, NOT a battlefield filter. The bare
    // `"Permanent" in blob` arm below would misread it (the substring is inside `Ref_TargetPermanents`)
    // as GameObjectFilter.Permanent and widen the effect to EVERY permanent, so decline (-> SCAFFOLD):
    // a target-reference group has no static GroupFilter rendering.
    if ("Ref_TargetPermanent" in blob) return null
    // "the permanents tapped this way" (`ThePermanentsTappedThisWay`, e.g. Homesickness's "put a stun
    // counter on each of them") is a PIPELINE reference to the just-tapped permanents, NOT a static
    // battlefield filter. The bare `"Permanent" in blob` arm below would misread it (the substring is
    // inside `ThePermanentsTappedThisWay`) as GameObjectFilter.Permanent and widen the effect to EVERY
    // permanent on the battlefield. There is no static GroupFilter rendering for a "this way" pipeline
    // group, so decline (-> SCAFFOLD) rather than emit a confidently-wrong board-wide effect.
    if ("ThePermanentsTappedThisWay" in blob) return null
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
    // An artifact subtype ("an Equipment", "each Vehicle you control") likewise has no rendering here;
    // widening to GameObjectFilter.Artifact/Permanent would silently drop the subtype (the bare
    // `"Permanent" in blob` arm below even matches the `_Permanents` key). Decline rather than emit a
    // too-broad filter (Strength of Arms' "if you control an Equipment").
    if (filterNode.argWordsTagged("IsArtifactType").isNotEmpty()) return null
    // "destroy each NONLAND artifact" (Granulate): a negated cardtype (IsNonCardtype). This surface has
    // no nonland/non-cardtype rendering, and the positive-type `when` below would misread the IsNonCardtype
    // "Land" clause as a positive Land filter — destroying lands instead of the nonland artifacts. Decline
    // (-> SCAFFOLD) rather than emit a confidently-wrong mass filter.
    if (filterNode.argWordsTagged("IsNonCardtype").isNotEmpty()) return null
    // "creatures that dealt damage to you this turn" (Retaliate): a DealtDamageToPlayerThisTurn relational
    // predicate this flat GroupFilter can't express. Silently dropping it would widen the destroy to a
    // one-sided board wipe, so decline rather than misrender.
    if ("DealtDamageToPlayerThisTurn" in blob) return null
    // "...with mana value X or less" (ManaValueIs against the cast-time ValueX, e.g. Vicious Rivalry's
    // "destroy all artifacts and creatures with mana value X or less"). `FilterPredicates.manaValueAtMost`
    // below returns null for a non-integer (X) bound, so without this guard the X cap would be SILENTLY
    // DROPPED — turning a "MV ≤ X" wipe into an unbounded board wipe. The SDK *can* express the cap
    // (CardPredicate.ManaValueAtMostX / .manaValueAtMostX()), but on a card whose X comes from a
    // pay-X-life additional cost — exactly the cast-time-X / extra-cost shape the module keeps at
    // SCAFFOLD — so decline the whole group rather than emit a constraint-dropping mass effect.
    if (filterNode.nodesTagged("ManaValueIs").any { findInteger(it["args"]) == null }) return null
    val types = targetTypes(filterNode)
    val subs = subtypes(filterNode)
    // Creature subtypes come from IsCreatureType (subtypes() only collects land/card subtypes).
    val creatureSubs = filterNode.argWordsTagged("IsCreatureType")
    // "Each land and Ally you control" — a creature subtype unioned with a non-creature cardtype (the
    // "land" half). The single-creature-subtype branch below would render only the Ally clause and
    // silently drop the other half, so decline the compound rather than emit half the filter
    // (Great Divide Guide).
    if (creatureSubs.isNotEmpty() && (types - "Creature").any { it in setOf("Land", "Artifact", "Enchantment", "Planeswalker") }) return null
    // Several cardtypes that aren't one of the renderable unions (e.g. Or[Creature, Planeswalker] —
    // Splatter Technique's "each creature and planeswalker") have no single GameObjectFilter; the
    // single-type branches below would keep only the first and silently drop the rest. Decline
    // (-> SCAFFOLD) rather than misrender a too-narrow mass filter.
    val renderableTypeUnions = setOf(
        setOf("Creature", "Land"), setOf("Creature", "Artifact"),
        setOf("Creature", "Enchantment"), setOf("Artifact", "Enchantment"),
    )
    if (types.size > 1 && types !in renderableTypeUnions) return null
    var node: Dsl = when {
        // "non-outlaw" (IsNonOutlaw): a creature with none of the five outlaw subtypes. Render the
        // non-outlaw creature group (matches Filters.NonOutlawCreature) — checked BEFORE the positive
        // cardtype arms so the restriction isn't silently dropped to a bare Creature group
        // (Caught in the Crossfire's "each non-outlaw creature").
        "IsNonOutlaw" in blob -> Lit("GameObjectFilter.Creature").dot("notAnyOfSubtypes", arg("Subtype.OUTLAW_TYPES"))
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
    val colors = (colorEnumNames(filterNode.wordsAtKey("_Color")) ?: return null).distinct()
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
    // "Blocking creatures get +7/+7" (Hold the Line): an IsBlocking predicate on the mass-effect group.
    // Without .blocking() the buff would silently widen to every creature, so compose it (the SDK has
    // GameObjectFilter.Creature.blocking(), the same helper TargetFilter.BlockingCreature uses).
    FilterPredicates.blocking(filterNode)?.let { node = node.dot(it) }
    FilterPredicates.nontoken(filterNode)?.let { node = node.dot(it) }
    // "creatures that saddled/crewed it this turn" (SaddledPermanentThisTurn / CrewedPermanentThisTurn,
    // bound to the trigger's permanent — Rambling Possum's "return any number of creatures that saddled
    // it this turn"). Source-relative; composes via .crewedOrSaddledSourceThisTurn() onto the creature
    // group. Dropping it would widen the group to every creature, so it's a real predicate here.
    if ("SaddledPermanentThisTurn" in blob || "CrewedPermanentThisTurn" in blob) {
        node = node.dot("crewedOrSaddledSourceThisTurn")
    }
    // "...that entered (the battlefield) this turn" (EnteredTheBattlefieldThisTurn) — Raucous
    // Entertainer's "each creature you control that entered this turn". Backed by the engine's
    // EnteredThisTurnComponent; composes via .enteredThisTurn(). Dropping it would widen the group to
    // every creature you control, so it's a real predicate here.
    if ("EnteredTheBattlefieldThisTurn" in blob) {
        node = node.dot("enteredThisTurn")
    }
    // The `.youControl()`/`.opponentControls()` suffix is a *controller* predicate — only a
    // `ControlledByAPlayer` clause carries it. Inspect that clause's player scope directly rather than
    // scanning the whole blob, so a bare `"You"` elsewhere (e.g. a graveyard count's
    // `InAPlayersGraveyard(You)` ownership clause) isn't misread as control of a battlefield permanent,
    // and `Other(You)` ("a player other than you" = opponents — Artistic Process's "each creature you
    // don't control") inverts to `opponentControls` instead of being mistaken for `youControl`.
    val controllerClause = filterNode.nodesTagged("ControlledByAPlayer").firstOrNull()
    if (controllerClause != null) {
        val players = controllerClause.field("args")
        val playersKind = players.strField("_Players")
        val playerRef = players.field("args").strField("_Player")
        node = when {
            playersKind == "Opponent" -> node.dot("opponentControls")
            playersKind == "Other" && playerRef == "You" -> node.dot("opponentControls")
            playersKind == "SinglePlayer" && playerRef == "You" -> node.dot("youControl")
            // "...target player controls" (Rustler Rampage / Requisition Raid): the group is keyed to
            // a player target the spell/mode declares. In every shape this surface renders (a single
            // chosen player), that target lives in the first slot, so bind the controller predicate to
            // ContextTarget(0). Any other player scope (Trigger_ThatPlayer, YourTeam, …) has no
            // rendering here; widening to every permanent would be confidently wrong, so decline.
            playersKind == "SinglePlayer" && playerRef == "Ref_TargetPlayer" ->
                node.dot("targetPlayerControls", arg("EffectTarget.ContextTarget(0)"))
            else -> return null
        }
    }
    // A supertype restriction (IsSupertype) — only "Legendary" renders here, via `.legendary()`
    // (e.g. Annie Joins Up's "legendary creature you control" trigger doubler). The basic-land
    // supertype is handled by its own land-subtype branch and never reaches this surface. Any other
    // supertype (Snow, World, …) has no GroupFilter rendering, so decline (-> SCAFFOLD) rather than
    // silently dropping the restriction and widening the group.
    val supertypes = filterNode.argWordsTagged("IsSupertype")
    if (supertypes.isNotEmpty()) {
        if (supertypes == listOf("Legendary")) node = node.dot("legendary")
        else return null
    }
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
    if (color != null) {
        val colorName = colorEnumNames(listOf(color))?.singleOrNull() ?: return null
        parts.add(Lit("GameObjectFilter.Any").dot("withColor", arg("Color.$colorName")))
    }
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
        "IsBasicLand" in blob || ("\"Basic\"" in blob && "\"Land\"" in blob) -> Lit("GameObjectFilter.BasicLand")
        "sorcery card" in oracle || "\"Sorcery\"" in blob -> Lit("GameObjectFilter.Sorcery")
        "instant card" in oracle || "\"Instant\"" in blob -> Lit("GameObjectFilter.Instant")
        // "an artifact card" (Fabricate) / "an enchantment card": a single positive cardtype with no
        // creature clause. Render the matching filter — checked after the more specific types above and
        // before the plain Land/Creature fallthrough; "Creature" present means an artifact-creature
        // compound the Creature arm handles, so guard against it.
        ("\"Artifact\"" in blob) && "\"Creature\"" !in blob -> Lit("GameObjectFilter.Artifact")
        ("\"Enchantment\"" in blob) && "\"Creature\"" !in blob -> Lit("GameObjectFilter.Enchantment")
        "\"Land\"" in blob -> Lit("GameObjectFilter.Land")
        "basic land" in oracle -> Lit("GameObjectFilter.BasicLand")
        "\"Creature\"" in blob || "creature" in oracle -> {
            var out: Dsl = Lit("GameObjectFilter.Creature")
            // "black and/or red creature" -> withAnyColor; a single colour -> withColor; fall back to the
            // oracle's "black creature" wording only when the filter node carries no structured colour.
            val colors = colorEnumNames(filterNode.colorsOf("IsColor")).orEmpty()
            when {
                colors.size > 1 -> out = out.dot("withAnyColor", *colors.map { arg("Color.$it") }.toTypedArray())
                colors.size == 1 -> out = out.dot("withColor", arg("Color.${colors[0]}"))
                "black creature" in oracle -> out = out.dot("withColor", arg("Color.BLACK"))
            }
            if ("tapped creature" in oracle) out = out.dot("tapped")
            if ("attacking" in oracle) out = out.dot("attacking")
            out
        }
        else -> Lit("GameObjectFilter.Any")
    }
}
