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
}

private val TOKEN_COLOR = mapOf(
    "White" to "WHITE", "Blue" to "BLUE", "Black" to "BLACK", "Red" to "RED", "Green" to "GREEN",
)

/** A `_CreatableToken` spec -> `Effects.CreateToken(...)`, or null (-> SCAFFOLD) for shapes we can't
 *  render exactly. `NumberTokens` wraps a base spec with a count: a fixed [count], or a [dynamicCount]
 *  `DynamicAmount` DSL ("X" / "the number of creature cards in your graveyard"). [controller], when set,
 *  is the EffectTarget DSL for who receives the token (e.g. `EffectTarget.TargetController` for "its
 *  controller creates …"); null means the spell's controller (the facade default). */
internal fun EmitCtx.createTokenDsl(spec: JsonObject, count: Int = 1, dynamicCount: Dsl? = null, controller: String? = null): Dsl? {
    when (spec.strField("_CreatableToken")) {
        "NumberTokens" -> {
            val a = spec["args"].asArr ?: return null
            val inner = a.getOrNull(1) as? JsonObject ?: return null
            // A fixed integer count renders inline as `count = N`. A dynamic count — "X" tokens
            // (ValueX, Form a Posse) or "the number of creature cards in your graveyard" (Rise of
            // the Varmints) — renders as a `DynamicAmount` via [dynamicAmountExpr]; decline
            // (-> SCAFFOLD) on a count we can't render exactly rather than emit a wrong fixed number.
            // [controller] threads through unchanged so a counted token can still target its recipient.
            val n = findInteger(a.getOrNull(0)) as? Int
            if (n != null) return createTokenDsl(inner, n, controller = controller)
            val dynamic = dynamicAmountExpr(a.getOrNull(0)) ?: return null
            return createTokenDsl(inner, dynamicCount = dynamic, controller = controller)
        }
        // Predefined artifact token with fixed characteristics -> its dedicated facade
        // (serialises as CreatePredefinedToken, not the generic CreateToken). It has no controller
        // override, so a controller-directed Treasure declines rather than silently drop the recipient.
        "TreasureToken" -> return if (controller != null) null else call("Effects.CreateTreasure", arg("$count"))
        "TokenWithPT" -> {
            // args: [ {_PT [p,t]}, {_TokenColorList [names]}, [supertypes], [cardtypes],
            //         {_TokenSubtypes [subs]}, [abilities] ]
            val a = spec["args"].asArr ?: return null
            val cardtypes = (a.getOrNull(3) as? JsonArray)?.mapNotNull { it.asStr() } ?: emptyList()
            if (cardtypes != listOf("Creature")) return null  // only plain creature tokens
            val pt = (a.getOrNull(0) as? JsonObject)?.get("args").asArr ?: return null
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
            // `CreateTokenEffect.activatedAbilities`. Any other shape (a granted *triggered* ability, a
            // parameterized keyword, an activated ability we can't render whole) scaffolds rather than
            // silently drop it.
            val tokenKeywords = mutableListOf<String>()
            val tokenActivatedAbilities = mutableListOf<Dsl>()
            for (ability in ((a.getOrNull(5) as? JsonArray) ?: JsonArray(emptyList()))) {
                val abilityRule = ability as? JsonObject ?: return null
                val rname = abilityRule.strField("_Rule") ?: return null
                if (rname == "Activated" || rname == "ActivatedWithModifiers") {
                    tokenActivatedAbilities.add(grantedActivatedAbilityExpr(abilityRule) ?: return null)
                    continue
                }
                if (abilityRule["args"] != null) return null  // TriggerA / parameterized -> SCAFFOLD
                val kw = pascalToUpperSnake(rname)
                if (kw !in keywords) return null
                tokenKeywords.add(kw)
            }
            // The `Effects.CreateToken` facade can't carry abilities — only the raw `CreateTokenEffect`
            // constructor exposes `activatedAbilities`. Use it when an activated ability is present;
            // otherwise keep the tidier facade.
            val ctor = if (tokenActivatedAbilities.isEmpty()) "Effects.CreateToken" else "CreateTokenEffect"
            val parts = mutableListOf(arg("power", "$power"), arg("toughness", "$toughness"))
            if (colors.isNotEmpty()) parts.add(arg("colors", "setOf(${colors.joinToString(", ") { "Color.$it" }})"))
            parts.add(arg("creatureTypes", "setOf(${subs.joinToString(", ") { "\"$it\"" }})"))
            if (tokenKeywords.isNotEmpty()) parts.add(arg("keywords", "setOf(${tokenKeywords.joinToString(", ") { "Keyword.$it" }})"))
            if (tokenActivatedAbilities.isNotEmpty()) {
                parts.add(arg("activatedAbilities", Call("listOf", tokenActivatedAbilities.map { arg(it) })))
            }
            // The recipient ("its controller creates …") is a named arg on both ctors, independent of
            // the count, so add it once before resolving the count overload below.
            if (controller != null) parts.add(arg("controller", Lit(controller)))
            // Count rendering depends on the constructor we picked:
            //  - `Effects.CreateToken` facade: a fixed count is the trailing named `count: Int` overload;
            //    a dynamic count is the `count: DynamicAmount` first-positional overload.
            //  - raw `CreateTokenEffect` ctor (used when the token has activated abilities): the `count: Int`
            //    secondary ctor does NOT expose `activatedAbilities`, so the count MUST be a `DynamicAmount`
            //    on the primary ctor — a fixed count renders as `DynamicAmount.Fixed(N)`, not a bare Int
            //    (otherwise `CreateTokenEffect(count = 2, activatedAbilities = …)` fails to compile —
            //    Hellspur Posse Boss).
            val usesRawCtor = tokenActivatedAbilities.isNotEmpty()
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
