package com.wingedsheep.sdk.scripting.values

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement

/**
 * Serial names of the [DynamicAmount] / [EntityReference] /
 * [com.wingedsheep.sdk.scripting.references.Player] shapes whose value lives in the resolution-time
 * `EffectContext` — the chosen targets, the announced X, the triggering object, the things
 * sacrificed or tapped to pay a cost, the resolution pipeline's stored collections.
 *
 * The engine's layer projector rebuilds a bare `EffectContext(sourceId, controllerId,
 * affectedEntityId)` on every pass, so none of these can resolve there; an amount containing one
 * would read as absent (0, or an empty player/entity list) forever. They are therefore rejected by
 * any effect that carries a `DynamicAmount` into the projection — today
 * [com.wingedsheep.sdk.scripting.effects.SetBaseStatsEffect]'s `reevaluateContinuously` — rather
 * than silently mis-evaluated. Everything not listed here is computable from the source, the
 * affected entity, or global game state, all of which the projector does carry.
 *
 * Kept as a deny list rather than an allow list because the projector-safe set is open-ended (every
 * `Count`/`AggregateBattlefield`/`EntityProperty(Source | AffectedEntity)` shape works); the
 * *traversal* is what has to be exhaustive, and encoding to JSON makes it so — no nesting site can
 * be missed the way a hand-written `when` over the composite amounts could.
 *
 * Two entries that look like they belong on the safe side but do not:
 *  - `ControllerOf` / `OwnerOf` take a `targetDescription` string and resolve through
 *    `context.targets`, so they are chosen-target reads despite naming no target type.
 *  - `VariableReference` reads the resolution pipeline's stored numbers, which the projector has no
 *    copy of.
 * And two that look unsafe but are not, so they are deliberately absent: `DistinctColorsManaSpent`
 * and `ManaSpentFromSubtype` read components stamped on the source entity, not the context, as do
 * the `CraftedMaterials*` amounts.
 */
private val CONTEXT_SCOPED_SERIAL_NAMES: Set<String> = setOf(
    // DynamicAmount
    "XValue", "CastX", "CastChoice", "ContextProperty", "VariableReference", "StoredCardManaValue",
    "DistinctEntitiesInCollections", "DistinctCardTypesInCollections", "ManaValueSumOfCollection",
    "TotalManaSpent", "ManaSpentOnX", "PermanentsSacrificedThisWay",
    "TotalPowerSacrificedThisWay", "StationCharge",
    "LastKnownSourceCounters", "LastKnownDamageDealtToSource",
    // EntityReference
    "Target", "Triggering", "Sacrificed", "TappedAsCost", "FromCostStorage", "AmassedArmy",
    "IterationEntity",
    // Player
    "TargetPlayer", "TargetOpponent", "ContextPlayer", "TriggeringPlayer", "ControllerOf", "OwnerOf",
)

private val CONTEXT_SCAN_JSON = Json

/**
 * The serial name of the first context-scoped reference anywhere inside [amount], or null when the
 * whole tree is projector-safe. See [CONTEXT_SCOPED_SERIAL_NAMES].
 *
 * Lives in the SDK, and is called from `init` blocks, so that "this amount can't be re-evaluated by
 * the projector" is a **card-load** failure — thrown as the `cardDef { }` is built and caught by
 * `CardDiscovery` and the corpus tests — rather than an exception raised mid-game the first time
 * the effect happens to resolve.
 */
fun contextScopedReferenceIn(amount: DynamicAmount): String? =
    findContextScoped(CONTEXT_SCAN_JSON.encodeToJsonElement<DynamicAmount>(amount))

private fun findContextScoped(element: JsonElement): String? = when (element) {
    is JsonObject -> element.entries.firstNotNullOfOrNull { (key, value) ->
        val discriminator = (value as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (key == "type" && discriminator in CONTEXT_SCOPED_SERIAL_NAMES) discriminator
        else findContextScoped(value)
    }
    is JsonArray -> element.firstNotNullOfOrNull { findContextScoped(it) }
    else -> null
}
