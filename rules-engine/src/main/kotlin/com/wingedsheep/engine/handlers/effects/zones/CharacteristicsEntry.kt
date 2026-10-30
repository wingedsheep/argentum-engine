package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.mechanics.layers.addFloatingEffects
import com.wingedsheep.engine.mechanics.layers.createFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.EntersWithCharacteristics

/**
 * A card entering the battlefield with characteristics an effect gives it as it enters —
 * "they enter as 5/5 creatures in addition to their other types" (Dance of the Manse, Relive the
 * Past; [EntersWithCharacteristics]). Carried on `ZoneEntryOptions.entersWith` and applied as the
 * object arrives, before the rest of its entry is settled, so the entry replacements, entry counters
 * and "whenever a creature enters" triggers already see the new types — the sibling of [NonAuraEntry].
 *
 * Every change is a [Duration.Permanent] floating effect on the new object: it lasts while the
 * permanent stays on the battlefield and is stripped with the object when it leaves (CR 400.7).
 * Types and subtypes are added in layer 4, colors in layer 5, and the base P/T is set in layer 7b,
 * so a later effect that sets P/T (or counters) still applies on top of it.
 */
object CharacteristicsEntry {

    fun apply(
        state: GameState,
        entityId: EntityId,
        controllerId: EntityId,
        characteristics: EntersWithCharacteristics,
    ): GameState {
        val context = EffectContext(sourceId = entityId, controllerId = controllerId)
        val target = setOf(entityId)
        val modifications = buildList {
            characteristics.addedCardTypes.forEach {
                add(Triple(Layer.TYPE, null, SerializableModification.AddType(it.name)))
            }
            characteristics.addedSubtypes.forEach {
                add(Triple(Layer.TYPE, null, SerializableModification.AddSubtype(it.value)))
            }
            if (characteristics.addedColors.isNotEmpty()) {
                add(Triple(Layer.COLOR, null, SerializableModification.AddColor(
                    characteristics.addedColors.mapTo(linkedSetOf()) { it.name })))
            }
            val power = characteristics.power
            val toughness = characteristics.toughness
            if (power != null && toughness != null) {
                add(Triple(Layer.POWER_TOUGHNESS, Sublayer.SET_VALUES,
                    SerializableModification.SetPowerToughness(power, toughness)))
            }
        }
        if (modifications.isEmpty()) return state
        var newState = state
        val effects = modifications.map { (layer, sublayer, modification) ->
            val (id, withId) = newState.newEntity()
            newState = withId
            newState.createFloatingEffect(
                layer, modification, target, Duration.Permanent, context, sublayer = sublayer, id = id
            )
        }
        return newState.addFloatingEffects(effects).tick()
    }
}
