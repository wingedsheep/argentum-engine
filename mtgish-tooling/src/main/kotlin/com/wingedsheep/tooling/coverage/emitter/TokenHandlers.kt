package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.Call
import com.wingedsheep.tooling.coverage.Dsl
import com.wingedsheep.tooling.coverage.Lit
import com.wingedsheep.tooling.coverage.Eval
import com.wingedsheep.tooling.coverage.Stmt
import com.wingedsheep.tooling.coverage.arg
import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.asInt
import com.wingedsheep.tooling.coverage.asStr
import com.wingedsheep.tooling.coverage.call
import com.wingedsheep.tooling.coverage.compact
import com.wingedsheep.tooling.coverage.field
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.jsonContains
import com.wingedsheep.tooling.coverage.pascalToUpperSnake
import com.wingedsheep.tooling.coverage.strField
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/** Token creation: a `CreateTokens` action -> `Effects.CreateToken(...)` / a predefined-token facade.
 *  Plain creature tokens (P/T, colours, creature subtypes, optional keyword abilities) and the
 *  predefined Treasure token render exactly; anything else (other artifact/enchantment tokens, copy
 *  tokens, tokens with full abilities) scaffolds. */
internal val tokenHandlers: Map<String, ActionHandler> = actionHandlers {
    on("CreateTokens") { _, args, _ ->
        val spec = args.asArr?.firstOrNull() as? JsonObject ?: return@on null
        createTokenDsl(spec)
    }
    // `CreateTokensWithFlags` is a `CreateTokens` carrying enter-state flags (args[1]). The only flag we
    // render exactly is `EntersTapped` (a tapped token); any other flag (attacking, with a counter, …)
    // declines -> SCAFFOLD rather than silently drop it. Goldvein Hydra: "create a number of tapped
    // Treasure tokens equal to its power."
    on("CreateTokensWithFlags") { _, args, _ ->
        val a = args.asArr ?: return@on null
        val spec = (a.getOrNull(0) as? JsonArray)?.firstOrNull() as? JsonObject ?: return@on null
        val flags = (a.getOrNull(1) as? JsonArray)?.filterIsInstance<JsonObject>() ?: emptyList()
        val flagNames = flags.mapNotNull { it.strField("_TokenFlag") }
        if (flagNames.isEmpty()) return@on null
        // Only EntersTapped is renderable; bail on any other flag so we never drop it.
        if (flagNames.any { it != "EntersTapped" }) return@on null
        createTokenDsl(spec, tapped = true)
    }
}

private val TOKEN_COLOR = mapOf(
    "White" to "WHITE", "Blue" to "BLUE", "Black" to "BLACK", "Red" to "RED", "Green" to "GREEN",
)

/** A `_CreatableToken` spec -> `Effects.CreateToken(...)`, or null (-> SCAFFOLD) for shapes we can't
 *  render exactly. `NumberTokens` wraps a base spec with a count: a fixed [count], or a [dynamicCount]
 *  `DynamicAmount` DSL ("X" / "the number of creature cards in your graveyard"). [controller], when set,
 *  is the EffectTarget DSL for who receives the token (e.g. `EffectTarget.TargetController` for "its
 *  controller creates …"); null means the spell's controller (the facade default). */
internal fun EmitCtx.createTokenDsl(spec: JsonObject, count: Int = 1, dynamicCount: Dsl? = null, controller: String? = null, tapped: Boolean = false): Dsl? {
    when (spec.strField("_CreatableToken")) {
        "NumberTokens" -> {
            val a = spec["args"].asArr ?: return null
            val inner = a.getOrNull(1) as? JsonObject ?: return null
            // A fixed integer count renders inline as `count = N`. A dynamic count — "X" tokens
            // (ValueX, Form a Posse) or "the number of creature cards in your graveyard" (Rise of
            // the Varmints) — renders as a `DynamicAmount` via [dynamicAmountExpr]; decline
            // (-> SCAFFOLD) on a count we can't render exactly rather than emit a wrong fixed number.
            // [controller] / [tapped] thread through unchanged so a counted token keeps its recipient/state.
            val n = findInteger(a.getOrNull(0)) as? Int
            if (n != null) return createTokenDsl(inner, n, controller = controller, tapped = tapped)
            val dynamic = dynamicAmountExpr(a.getOrNull(0)) ?: return null
            return createTokenDsl(inner, dynamicCount = dynamic, controller = controller, tapped = tapped)
        }
        // Predefined artifact token with fixed characteristics -> its dedicated facade
        // (serialises as CreatePredefinedToken, not the generic CreateToken). It has no controller
        // override, so a controller-directed Treasure declines rather than silently drop the recipient.
        // A fixed count uses the `Int` overload; a dynamic count uses the `DynamicAmount` overload
        // (Goldvein Hydra's "tapped Treasure tokens equal to its power"). `tapped = true` renders the
        // tapped flag.
        // `NamedToken` is a predefined named token. We render ONLY the exact Everywhere token
        // (Overlord of the Hauntwoods): "a colorless land token named Everywhere that is every basic
        // land type." Its spec is `["Everywhere", {Colorless}, [], ["Land"], {AllBasicLandTypes}, []]` —
        // a colorless Land with `AllBasicLandTypes` subtypes and no abilities — and it maps to the
        // `Effects.CreateEverywhere(...)` facade (the predefined PredefinedTokens.Everywhere token, which
        // has all five basic land subtypes and taps for any color, per the Scryfall ruling). Any other
        // NamedToken (a different name, a coloured/non-Land token, a token with abilities) declines ->
        // SCAFFOLD rather than misrender an unknown predefined token.
        "NamedToken" -> {
            val a = spec["args"].asArr ?: return null
            val name = a.getOrNull(0).asStr() ?: return null
            if (name != "Everywhere") return null
            if (count != 1 || dynamicCount != null || controller != null) return null
            // Colorless.
            if ((a.getOrNull(1) as? JsonObject)?.strField("_TokenColorList") != "Colorless") return null
            // Land card type, no supertypes.
            if ((a.getOrNull(2) as? JsonArray)?.isNotEmpty() == true) return null
            val cardtypes = (a.getOrNull(3) as? JsonArray)?.mapNotNull { it.asStr() } ?: emptyList()
            if (cardtypes != listOf("Land")) return null
            // Every basic land type.
            if ((a.getOrNull(4) as? JsonObject)?.strField("_TokenSubtypes") != "AllBasicLandTypes") return null
            // No granted abilities (the predefined token carries the mana ability itself).
            if ((a.getOrNull(5) as? JsonArray)?.isNotEmpty() == true) return null
            val parts = mutableListOf<com.wingedsheep.tooling.coverage.Arg>()
            if (tapped) parts.add(arg("tapped", "true"))
            return Call("Effects.CreateEverywhere", parts)
        }
        "TreasureToken" -> {
            if (controller != null) return null
            val parts = mutableListOf<com.wingedsheep.tooling.coverage.Arg>()
            when {
                dynamicCount != null -> parts.add(arg("count", dynamicCount))
                count != 1 -> parts.add(arg("count", "$count"))
            }
            if (tapped) parts.add(arg("tapped", "true"))
            return Call("Effects.CreateTreasure", parts)
        }
        "TokenWithPT" -> {
            // args: [ {_PT [p,t]}, {_TokenColorList [names]}, [supertypes], [cardtypes],
            //         {_TokenSubtypes [subs]}, [abilities] ]
            val a = spec["args"].asArr ?: return null
            val cardtypes = (a.getOrNull(3) as? JsonArray)?.mapNotNull { it.asStr() } ?: emptyList()
            // Plain creature tokens render directly; an *artifact* or *enchantment* creature token
            // (e.g. Duskmourn's "1/1 white Glimmer enchantment creature token") sets the matching
            // `artifactToken` / `enchantmentToken` flag. Any other card-type combination (a noncreature
            // token, a tripartite type line, …) declines -> SCAFFOLD rather than drop a type.
            val artifactToken = "Artifact" in cardtypes
            val enchantmentToken = "Enchantment" in cardtypes
            val extraTypes = cardtypes.toSet() - setOf("Creature", "Artifact", "Enchantment")
            if ("Creature" !in cardtypes || extraTypes.isNotEmpty()) return null
            val ptSpec = a.getOrNull(0) as? JsonObject ?: return null
            // A `PTX` spec is an X/X token whose X is a game number ("Create an X/X … where X is the
            // greatest power among creatures you control" — Tumbleweed Rising). Render it via
            // `Effects.CreateDynamicToken(dynamicPower = …, dynamicToughness = …)`; the dynamic-token path
            // can't carry abilities or a per-token count, so decline (-> SCAFFOLD) if either is present.
            if (ptSpec.strField("_PT") == "PTX") {
                if (count != 1 || dynamicCount != null) return null
                return dynamicPtTokenDsl(ptSpec, a, controller)
            }
            val pt = ptSpec["args"].asArr ?: return null
            val power = pt.getOrNull(0).asInt() ?: return null
            val toughness = pt.getOrNull(1).asInt() ?: return null
            val colors = ((a.getOrNull(1) as? JsonObject)?.get("args").asArr ?: JsonArray(emptyList()))
                .mapNotNull { it.asStr()?.let(TOKEN_COLOR::get) }
            val subs = ((a.getOrNull(4) as? JsonObject)?.get("args").asArr ?: JsonArray(emptyList()))
                .mapNotNull { it.asStr() }
            if (subs.isEmpty()) return null
            // The token's ability list (a[5]). A bare keyword is granted via `keywords = …`; an
            // `Activated` / `ActivatedWithModifiers` rule (e.g. Mourner's Surprise's Mercenary token
            // with "{T}: Target creature you control gets +1/+0 … Activate only as a sorcery.") renders
            // as an inline `ActivatedAbility(…)` via [grantedActivatedAbilityExpr] and is passed through
            // `CreateTokenEffect.activatedAbilities`. A granted *triggered* ability (Send in the Pest's
            // Pest with "Whenever this token attacks, you gain 1 life.") renders as an inline
            // `TriggeredAbility.create(…)` via [grantedTriggeredAbilityExpr] and is passed through
            // `CreateTokenEffect.triggeredAbilities`. Any other shape (a parameterized keyword, an
            // ability we can't render whole) scaffolds rather than silently drop it.
            val tokenKeywords = mutableListOf<String>()
            val tokenActivatedAbilities = mutableListOf<Dsl>()
            val tokenTriggeredAbilities = mutableListOf<Dsl>()
            for (ability in ((a.getOrNull(5) as? JsonArray) ?: JsonArray(emptyList()))) {
                val abilityRule = ability as? JsonObject ?: return null
                val rname = abilityRule.strField("_Rule") ?: return null
                if (rname == "Activated" || rname == "ActivatedWithModifiers") {
                    tokenActivatedAbilities.add(grantedActivatedAbilityExpr(abilityRule) ?: return null)
                    continue
                }
                if (rname == "TriggerA") {
                    tokenTriggeredAbilities.add(grantedTriggeredAbilityExpr(abilityRule) ?: return null)
                    continue
                }
                if (abilityRule["args"] != null) return null  // other parameterized rule -> SCAFFOLD
                val kw = pascalToUpperSnake(rname)
                if (kw !in keywords) return null
                tokenKeywords.add(kw)
                // Training on a token (Torens, Fist of the Angels' "1/1 … token with training") is a
                // keyword ability whose attack-counter trigger the engine derives from the DSL helper,
                // not the bare keyword tag — so pair the display keyword with `trainingTriggeredAbility()`
                // (the standalone helper built for token-borne Training), exactly like the card-level
                // training() render above. A bare keywords(Keyword.TRAINING) alone would make the token's
                // +1/+1 attack trigger a no-op, diverging from the hand-authored golden. The paired trigger
                // flips the token onto the raw CreateTokenEffect ctor (which exposes triggeredAbilities).
                if (kw == "TRAINING") tokenTriggeredAbilities.add(call("trainingTriggeredAbility"))
            }
            // The `Effects.CreateToken` facade can't carry abilities OR the tapped flag — only the raw
            // `CreateTokenEffect` constructor exposes `activatedAbilities` / `triggeredAbilities` / `tapped`.
            // Use it when a granted ability is present or the token enters tapped; otherwise keep the tidier
            // facade. (Army of the Damned: "thirteen … tokens tapped"; Rise from the Tides: dynamic-count
            // Zombies tapped.)
            val hasGrantedAbilities = tokenActivatedAbilities.isNotEmpty() || tokenTriggeredAbilities.isNotEmpty()
            val usesRawCtor = hasGrantedAbilities || tapped
            val ctor = if (!usesRawCtor) "Effects.CreateToken" else "CreateTokenEffect"
            val parts = mutableListOf(arg("power", "$power"), arg("toughness", "$toughness"))
            if (colors.isNotEmpty()) parts.add(arg("colors", "setOf(${colors.joinToString(", ") { "Color.$it" }})"))
            parts.add(arg("creatureTypes", "setOf(${subs.joinToString(", ") { "\"$it\"" }})"))
            if (tokenKeywords.isNotEmpty()) parts.add(arg("keywords", "setOf(${tokenKeywords.joinToString(", ") { "Keyword.$it" }})"))
            // Artifact / enchantment creature tokens carry the matching flag (named arg on both ctors).
            if (artifactToken) parts.add(arg("artifactToken", "true"))
            if (enchantmentToken) parts.add(arg("enchantmentToken", "true"))
            if (tokenActivatedAbilities.isNotEmpty()) {
                parts.add(arg("activatedAbilities", Call("listOf", tokenActivatedAbilities.map { arg(it) })))
            }
            if (tokenTriggeredAbilities.isNotEmpty()) {
                parts.add(arg("triggeredAbilities", Call("listOf", tokenTriggeredAbilities.map { arg(it) })))
            }
            if (tapped) parts.add(arg("tapped", "true"))
            // The recipient ("its controller creates …") is a named arg on both ctors, independent of
            // the count, so add it once before resolving the count overload below.
            if (controller != null) parts.add(arg("controller", Lit(controller)))
            // Count rendering depends on the constructor we picked:
            //  - `Effects.CreateToken` facade: a fixed count is the trailing named `count: Int` overload;
            //    a dynamic count is the `count: DynamicAmount` first-positional overload.
            //  - raw `CreateTokenEffect` ctor (used for granted abilities or the tapped flag): the `count: Int`
            //    secondary ctor does NOT expose the extra fields, so the count MUST be a `DynamicAmount` on the
            //    primary ctor — a fixed count renders as `DynamicAmount.Fixed(N)`, not a bare Int (otherwise
            //    `CreateTokenEffect(count = 2, tapped = true)` fails to compile — Hellspur Posse Boss).
            when {
                dynamicCount != null -> return Call(ctor, listOf(arg("count", dynamicCount)) + parts)
                count == 1 -> return Call(ctor, parts)
                usesRawCtor -> return Call(ctor, listOf(arg("count", Lit("DynamicAmount.Fixed($count)"))) + parts)
                else -> { parts.add(arg("count", "$count")); return Call(ctor, parts) }
            }
        }
    }
    return null
}

/**
 * A `PTX` token spec — "Create an X/X [color] [subtype] creature token, where X is <game number>"
 * (Tumbleweed Rising) -> `Effects.CreateDynamicToken(dynamicPower = …, dynamicToughness = …, colors = …,
 * creatureTypes = …)`. `PTX`'s args are `[{_PTXValue}, {_PTXValue}, <countNode>]`: both the power and
 * toughness X resolve to the SAME [countNode] game number, rendered via [dynamicAmountExpr] (decline ->
 * SCAFFOLD when it doesn't render exactly). Only the plain-creature, subtyped, no-ability shape renders;
 * a token carrying granted abilities declines (the dynamic-token facade can't carry them). [outer] is the
 * `TokenWithPT` arg list (colours at [1], subtypes at [4], abilities at [5]).
 */
private fun EmitCtx.dynamicPtTokenDsl(ptSpec: JsonObject, outer: JsonArray, controller: String?): Dsl? {
    val ptArgs = ptSpec["args"].asArr ?: return null
    if (ptArgs.size != 3) return null
    val countNode = ptArgs.getOrNull(2) ?: return null
    val dynamic = dynamicAmountExpr(countNode) ?: return null
    val colors = ((outer.getOrNull(1) as? JsonObject)?.get("args").asArr ?: JsonArray(emptyList()))
        .mapNotNull { it.asStr()?.let(TOKEN_COLOR::get) }
    val subs = ((outer.getOrNull(4) as? JsonObject)?.get("args").asArr ?: JsonArray(emptyList()))
        .mapNotNull { it.asStr() }
    if (subs.isEmpty()) return null
    // The dynamic-token facade has no ability list; an abilitied X/X token declines rather than drop them.
    if ((outer.getOrNull(5) as? JsonArray)?.isNotEmpty() == true) return null
    val parts = mutableListOf(arg("dynamicPower", dynamic), arg("dynamicToughness", dynamic))
    if (colors.isNotEmpty()) parts.add(arg("colors", "setOf(${colors.joinToString(", ") { "Color.$it" }})"))
    parts.add(arg("creatureTypes", "setOf(${subs.joinToString(", ") { "\"$it\"" }})"))
    if (controller != null) parts.add(arg("controller", Lit(controller)))
    return Call("Effects.CreateDynamicToken", parts)
}

/**
 * `ReplaceAPlayerWouldCreateTokens` -> `replacementEffect(ReplaceTokenCreationWithAttachedCopy(...))`.
 *
 * We render exactly the printed shape this primitive models: "the first time you would create one or
 * more tokens each turn, you may instead create that many tokens that are copies of [attached]
 * permanent" (Mirrormind Crown, Moonlit Meditation). Any other token-creation replacement (mandatory,
 * not-first-time-each-turn, doubling, additional tokens, or copying something other than the source's
 * host permanent) declines to a scaffold rather than misrender, since the SDK type's optional /
 * oncePerTurn defaults would no longer be faithful. The display-only `attachmentVerb` follows the
 * source's attachment subtype (Aura -> "enchanted", Equipment -> "equipped", Fortification ->
 * "fortified"); a host with none of those declines.
 */
internal fun EmitCtx.replaceTokenCreationBlock(card: JsonObject, rule: JsonObject): List<Stmt>? {
    val blob = compact(rule)
    val faithful = jsonContains(rule, "_ReplacableEventAPlayerWouldCreateTokens",
        "APlayerWouldCreateTokensForTheFirstTimeEachTurn") &&          // once per turn
        jsonContains(rule, "_Player", "You") &&                        // "you would create"
        jsonContains(rule, "_ReplacementActionAPlayerWouldCreateTokens", "ChooseAnAction") && // "you may"
        "TokenCopyOfPermanent" in blob &&                              // copy a permanent
        jsonContains(rule, "_Permanent", "HostPermanent") &&           // the attached permanent
        "NoTokenCopyEffects" in blob                                   // plain copy, no riders
    if (!faithful) { reasons.add("ReplaceAPlayerWouldCreateTokens"); return null }
    val verb = attachmentVerb(card) ?: run { reasons.add("ReplaceAPlayerWouldCreateTokens"); return null }
    val effect = call("ReplaceTokenCreationWithAttachedCopy", arg("attachmentVerb", "\"$verb\""))
    return listOf(Eval(call("replacementEffect", arg(effect))))
}

/** The display verb for a token-copy replacement's attached permanent, from the source's subtype. */
private fun attachmentVerb(card: JsonObject): String? {
    val subs = card.field("Typeline").field("Subtypes").asArr?.mapNotNull { it.asStr() } ?: emptyList()
    return when {
        "Aura" in subs -> "enchanted"
        "Equipment" in subs -> "equipped"
        "Fortification" in subs -> "fortified"
        else -> null
    }
}
