package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.Call
import com.wingedsheep.tooling.coverage.Dsl
import com.wingedsheep.tooling.coverage.arg
import com.wingedsheep.tooling.coverage.asArr
import com.wingedsheep.tooling.coverage.call
import com.wingedsheep.tooling.coverage.findInteger
import com.wingedsheep.tooling.coverage.strField
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Mana production: an `AddMana { _ManaProduce }` action -> the `Effects.Add*Mana` facade. Used both as
 * the leaf effect of a mana ability ({T}: Add {C}) and inside a composite ({T}: Add any color, this
 * deals 1 to you). The mana-ability flag itself is set by [EmitCtx.activatedBlock], which recognises a
 * targetless ability whose actions add mana.
 */
internal val manaHandlers: Map<String, ActionHandler> = actionHandlers {
    on("AddMana") { _, args, _ -> manaProduceDsl(args) }
    on("AddManaWithModifiers") { _, args, _ ->
        // "{T}: Add {U}. Spend this mana only to cast an instant or sorcery spell." (Hydro-Channeler,
        // Vodalian Arcanist): a produced mana plus a use-restriction modifier. args = [<ManaProduce>,
        // <ManaUseModifier>…]. Render only the one restriction we can express exactly —
        // CanOnlySpendOnSpells over an Or of {Instant, Sorcery} -> ManaRestriction.InstantOrSorceryOnly;
        // any other modifier scaffolds (return null) rather than drop the restriction silently.
        val arr = args.asArr ?: return@on null
        val produce = arr.getOrNull(0) as? JsonObject ?: return@on null
        val restriction = manaUseModifierRestriction(arr.drop(1)) ?: return@on null
        manaProduceWithRestrictionDsl(produce, restriction)
    }
    on("AddManaRepeatedWithModifiers") { _, args, _ ->
        // "{T}: Add two mana of any one color. Spend this mana only to cast Mount or Vehicle spells."
        // (Intrepid Stablemaster): a produced mana, a repeat count, and a use-restriction modifier.
        // args = [<ManaProduce>, <count>, <ManaUseModifier>…]. Only a fixed integer count over a produce
        // the restricted facades cover renders; any dynamic count or unrenderable restriction scaffolds.
        val arr = args.asArr ?: return@on null
        val produce = arr.getOrNull(0) as? JsonObject ?: return@on null
        val count = findInteger(arr.getOrNull(1)) as? Int ?: return@on null
        val restriction = manaUseModifierRestriction(arr.drop(2)) ?: return@on null
        manaProduceRepeatedWithRestrictionDsl(produce, count, restriction)
    }
    on("AddManaRepeated") { _, args, _ ->
        // "Add {R} for each Goblin on the battlefield" (Brightstone Ritual): a single mana symbol produced
        // a dynamic number of times -> Effects.AddMana(color, <count>). Only one colour/colourless symbol
        // with a recoverable dynamic count renders; anything else scaffolds.
        val arr = args.asArr ?: return@on null
        val amt = dynamicAmount(arr.getOrNull(1)) ?: return@on null
        when (val produce = (arr.getOrNull(0) as? JsonObject)?.strField("_ManaProduce")) {
            "ManaProduceC" -> call("Effects.AddColorlessMana", arg(amt))
            else -> MANA_PRODUCE_COLOR[produce]?.let { call("Effects.AddMana", arg(it), arg(amt)) }
        }
    }
}

private val MANA_PRODUCE_COLOR = mapOf(
    "ManaProduceW" to "Color.WHITE", "ManaProduceU" to "Color.BLUE",
    "ManaProduceB" to "Color.BLACK", "ManaProduceR" to "Color.RED", "ManaProduceG" to "Color.GREEN",
)

/** `{_ManaProduce}` -> the matching mana Effect, or null (-> SCAFFOLD) for shapes we don't render. */
internal fun manaProduceDsl(node: JsonElement?): Dsl? =
    when (val produce = (node as? JsonObject)?.strField("_ManaProduce")) {
        null -> null
        "ManaProduceC" -> call("Effects.AddColorlessMana", arg("1"))
        "AnyManaColor" -> call("Effects.AddManaOfChoice")
        // "{T}: Add one mana of the chosen color." — pairs with an `EntersWithChoice(ChoiceType.COLOR)`
        // replacement (Mirage Mesa, Uncharted Haven). The chosen color was fixed when the land entered.
        "ManaOfTheChosenColor" -> call("Effects.AddManaOfChosenColor")
        "And" -> manaAndDsl(node)  // {B}{B}{B} (Dark Ritual), {C}{C}{C} (Basalt Monolith), …
        else -> MANA_PRODUCE_COLOR[produce]?.let { call("Effects.AddMana", arg(it)) }
    }

/**
 * Recover the single use-restriction we can render exactly from the `_ManaUseModifier` list of an
 * `AddManaWithModifiers` action. Returns the Argentum `ManaRestriction.*` token, or null to scaffold
 * for any modifier we don't render (so a restriction is never silently dropped). Today only
 * `CanOnlySpendOnSpells` over an `Or` of `{Instant, Sorcery}` maps cleanly.
 */
private fun manaUseModifierRestriction(modifiers: List<JsonElement>): String? {
    val modifier = modifiers.singleOrNull() as? JsonObject ?: return null
    if (modifier.strField("_ManaUseModifier") != "CanOnlySpendOnSpells") return null
    val spells = modifier["args"] as? JsonObject ?: return null

    // "Spend this mana only to cast a Mount spell" (Bucolic Ranch): a BARE single subtype check, not
    // wrapped in an Or. Map the one subtype straight to SubtypeSpellsOnly(setOf("Mount")).
    subtypeOfSpellsCheck(spells)?.let { subtype ->
        return "ManaRestriction.SubtypeSpellsOnly(setOf(\"$subtype\"))"
    }

    if (spells.strField("_Spells") != "Or") return null
    val branches = spells["args"] as? JsonArray ?: return null
    val cardtypes = branches.mapNotNull { b ->
        (b as? JsonObject)?.takeIf { it.strField("_Spells") == "IsCardtype" }?.get("args")
    }.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }.toSet()
    if (cardtypes == setOf("Instant", "Sorcery")) return "ManaRestriction.InstantOrSorceryOnly"
    // "Spend this mana only to cast Mount or Vehicle spells" (Intrepid Stablemaster): an Or of subtype
    // checks — IsCreatureType / IsArtifactType — over named subtypes. Each branch must be a bare subtype
    // check carrying a string subtype; the engine's SubtypeSpellsOnly matches a spell whose type line
    // carries any of those subtypes. Decline if any branch is a non-subtype check rather than drop it.
    val subtypes = branches.map { b ->
        val obj = b as? JsonObject ?: return null
        subtypeOfSpellsCheck(obj) ?: return null
    }
    if (subtypes.isEmpty()) return null
    return "ManaRestriction.SubtypeSpellsOnly(setOf(${subtypes.joinToString(", ") { "\"$it\"" }}))"
}

/**
 * A single `IsCreatureType`/`IsArtifactType`/`IsEnchantmentType`/`IsLandType` spells check carrying a
 * bare string subtype -> that subtype name, else null. `SubtypeSpellsOnly` matches a spell whose type
 * line carries the subtype regardless of card type, so all four type-scoped checks map the same way.
 */
private fun subtypeOfSpellsCheck(node: JsonObject): String? =
    when (node.strField("_Spells")) {
        "IsCreatureType", "IsArtifactType", "IsEnchantmentType", "IsLandType" ->
            (node["args"] as? kotlinx.serialization.json.JsonPrimitive)?.content
        else -> null
    }

/**
 * `{_ManaProduce}` + a recovered restriction token -> the matching restricted mana Effect. Mirrors
 * [manaProduceDsl] but threads the `restriction = …` named argument; declines (null) for produce shapes
 * the restricted facades don't cover (And pools, etc.).
 */
private fun manaProduceWithRestrictionDsl(node: JsonObject, restriction: String): Dsl? =
    when (val produce = node.strField("_ManaProduce")) {
        null -> null
        "ManaProduceC" -> call("Effects.AddColorlessMana", arg("1"), arg("restriction", restriction))
        "AnyManaColor" -> call("Effects.AddManaOfChoice", arg("restriction", restriction))
        else -> MANA_PRODUCE_COLOR[produce]?.let {
            call("Effects.AddMana", arg(it), arg("1"), arg("restriction", restriction))
        }
    }

/**
 * `{_ManaProduce}` + a repeat [count] + a recovered restriction token -> the matching restricted mana
 * Effect producing [count] mana. "Two mana of any one color" maps to `Effects.AddAnyColorMana(amount = N,
 * restriction = …)` (the player picks one color and gets N of it). A single named color produces N of
 * that color; colorless produces N colorless. Declines (null) for produce shapes the restricted facades
 * don't cover (And pools), so a restriction is never silently dropped.
 */
private fun manaProduceRepeatedWithRestrictionDsl(node: JsonObject, count: Int, restriction: String): Dsl? =
    when (val produce = node.strField("_ManaProduce")) {
        null -> null
        "ManaProduceC" -> call("Effects.AddColorlessMana", arg("$count"), arg("restriction", restriction))
        "AnyManaColor" -> call("Effects.AddAnyColorMana", arg("amount", "$count"), arg("restriction", restriction))
        else -> MANA_PRODUCE_COLOR[produce]?.let {
            call("Effects.AddMana", arg(it), arg("$count"), arg("restriction", restriction))
        }
    }

/** `And[<produce>…]` -> one `Effects.Add*Mana(color, count)` per distinct mana, composited (inline) when
 *  the pool mixes colors. Null (-> SCAFFOLD) if any child is itself a non-leaf produce (nested And /
 *  choice), so we never emit a partial pool. */
private fun manaAndDsl(node: JsonObject): Dsl? {
    val children = node["args"] as? JsonArray ?: return null
    val produces = children.map { (it as? JsonObject)?.strField("_ManaProduce") ?: return null }
    if (produces.any { it != "ManaProduceC" && it !in MANA_PRODUCE_COLOR }) return null
    val counts = LinkedHashMap<String, Int>()
    produces.forEach { counts[it] = (counts[it] ?: 0) + 1 }
    val parts = counts.map { (p, n) ->
        if (p == "ManaProduceC") call("Effects.AddColorlessMana", arg("$n"))
        else call("Effects.AddMana", arg(MANA_PRODUCE_COLOR.getValue(p)), arg("$n"))
    }
    // The mana pool uses an INLINE Effects.Composite(...) (single line), distinct from the multi-line
    // Composite node the effect-list builder emits.
    return if (parts.size == 1) parts[0] else Call("Effects.Composite", parts.map { arg(it) })
}

/** True when this ability is a mana ability: no target, and at least one action adds mana. */
internal fun isManaAbility(tvar: String?, actions: List<JsonObject>): Boolean =
    tvar == null && actions.any {
        it.strField("_Action") in setOf(
            "AddMana", "AddManaRepeated", "AddManaWithModifiers", "AddManaRepeatedWithModifiers"
        )
    }
