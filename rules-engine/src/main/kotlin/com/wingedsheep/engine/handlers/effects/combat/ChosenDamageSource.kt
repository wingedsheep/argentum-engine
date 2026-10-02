package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.sdk.core.Zone
import kotlinx.serialization.Serializable

/** A source is a rules object, not the card's stable entity id across later zone visits. */
@Serializable
data class ChosenDamageSource(
    val reference: ObjectRef,
    val name: String,
    val permanentSpell: Boolean = false
)

/** Source choices follow CR 609.7a, including objects referred to by pending abilities or shields. */
fun damageSourceChoices(state: GameState, resolvingContext: EffectContext? = null): List<ChosenDamageSource> {
    val choices = linkedMapOf<ObjectRef, ChosenDamageSource>()
    fun add(ref: ObjectRef?, fallback: String = "Departed source", permanentSpell: Boolean = false) {
        if (ref == null) return
        val entity = state.getEntity(ref.entityId)
        val current = state.isCurrentObject(ref)
        val name = when {
            current && entity?.has<FaceDownComponent>() == true -> "Face-down source"
            current && state.logicalZone(ref.entityId)?.zoneType in setOf(Zone.HAND, Zone.LIBRARY) -> "Hidden source"
            current -> entity?.get<CardComponent>()?.name ?: fallback
            else -> fallback
        }
        if (permanentSpell || ref !in choices) choices[ref] = ChosenDamageSource(ref, name, permanentSpell)
    }
    fun environment(refs: ObjectReferenceEnvironment, name: String) {
        add(refs.origin, name)
        add(refs.source, name)
        add(refs.triggering)
        add(refs.iteration?.objectRef)
    }
    fun snapshots(values: List<EntitySnapshot>) {
        values.forEach { add(it.objectRef, if (it.wasFaceDown) "Face-down source" else it.name ?: "Departed source") }
    }
    fun ids(values: Iterable<EntityId>) { values.forEach { add(state.objectRef(it)) } }
    resolvingContext?.let { context ->
        environment(context.objectReferences, context.lastKnownSourceSnapshot?.let { if (it.wasFaceDown) "Face-down source" else it.name } ?: "Resolving ability source")
        if (!context.objectReferences.captured) add(context.sourceId?.let(state::objectRef))
        snapshots(context.sacrificedPermanents + context.exiledAsCostSnapshots + context.tappedEntitySnapshots + context.chosenEntitySnapshots)
        context.lastKnownSourceSnapshot?.let { snapshots(listOf(it)) }
        ids(context.discardedAsCostCards + context.exiledAsCostCards)
        context.pipeline?.storedCollections?.values?.forEach(::ids)
        context.granterId?.let { ids(listOf(it)) }
    }
    for (id in state.getBattlefield()) add(state.objectRef(id))
    for (id in state.stack) {
        val entity = state.getEntity(id) ?: continue
        val spell = entity.get<SpellOnStackComponent>()
        if (spell != null) add(state.objectRef(id), permanentSpell = entity.get<CardComponent>()?.typeLine?.isPermanent == true)
        entity.get<ActivatedAbilityOnStackComponent>()?.let { ability ->
            environment(ability.objectReferences, ability.sourceName)
            if (!ability.objectReferences.captured) add(state.objectRef(ability.sourceId), ability.sourceName)
        }
        entity.get<TriggeredAbilityOnStackComponent>()?.let { ability ->
            environment(ability.objectReferences, ability.sourceName)
            if (!ability.objectReferences.captured) add(state.objectRef(ability.sourceId), ability.sourceName)
            ids(ability.triggerContext?.capturedEntityIds.orEmpty())
            ability.carriedPipeline?.storedCollections?.values?.forEach(::ids)
            ability.granterId?.let { ids(listOf(it)) }
        }
        entity.get<ActivatedAbilityOnStackComponent>()?.let { ability ->
            snapshots(ability.sacrificedPermanents + ability.tappedEntitySnapshots)
            ability.lastKnownSourceSnapshot?.let { snapshots(listOf(it)) }
            ids(ability.exiledAsCostCards + ability.discardedAsCostCards)
            ability.granterId?.let { ids(listOf(it)) }
        }
        entity.get<SpellOnStackComponent>()?.let { spell ->
            snapshots(spell.sacrificedPermanents + spell.exiledAsCostSnapshots + spell.chosenEntitySnapshots)
            ids(spell.exiledAsCostCards + spell.discardedAsCostCards + spell.beheldCards)
        }
        entity.get<TargetsComponent>()?.targets?.forEach { target ->
            val targetId = when (target) {
                is ChosenTarget.Permanent -> target.entityId
                is ChosenTarget.Spell -> target.spellEntityId
                is ChosenTarget.Card -> target.cardId
                is ChosenTarget.Player -> null
            }
            targetId?.let { add(entity.get<TargetsComponent>()?.targetObjectRefs?.get(it) ?: state.objectRef(it)) }
        }
    }
    for (trigger in state.delayedTriggers) {
        environment(trigger.objectReferences, trigger.sourceName)
        if (!trigger.objectReferences.captured) add(state.objectRef(trigger.sourceId), trigger.sourceName)
        add(trigger.watchedEntityId?.let(state::objectRef))
        add(trigger.watchedRecipientId?.let(state::objectRef))
        trigger.carriedCollections.values.flatten().forEach { add(it.objectRef) }
    }
    for (floating in state.floatingEffects) {
        val mod = floating.effect.modification
        val referredSource = when (mod) {
            is SerializableModification.PreventNextDamage -> mod.onlyFromSource
            is SerializableModification.PreventAllDamageFromSource -> mod.damageSourceId
            is SerializableModification.PreventNextDamageInstanceFromSource -> mod.damageSourceId
            is SerializableModification.PreventNextDamageLeavingAmount -> mod.damageSourceId
            is SerializableModification.PreventNextDamageFromSourceShield -> mod.damageSourceId
            else -> null
        }
        val waiting = when (mod) {
            is SerializableModification.PreventNextDamage,
            is SerializableModification.PreventAllDamageFromSource,
            is SerializableModification.PreventNextDamageInstanceFromSource,
            is SerializableModification.PreventNextDamageLeavingAmount,
            is SerializableModification.PreventNextDamageFromSourceShield,
            is SerializableModification.PreventAllDamageDealtBy,
            is SerializableModification.PreventAllDamageTo,
            is SerializableModification.PreventCombatDamageToAndBy,
            is SerializableModification.RedirectNextDamage,
            is SerializableModification.ReplaceDrawWith -> true
            else -> false
        }
        if (waiting) floating.referencedObjects.forEach { add(it) }
        if (referredSource != null && floating.referencedObjects.none { it.entityId == referredSource }) {
            add(state.objectRef(referredSource))
        }
        if (mod is SerializableModification.RedirectNextDamage) {
            mod.chosenSource?.let { choices.putIfAbsent(it.reference, it) }
            add(mod.protectedRef)
            add(mod.redirectToRef)
        }
    }
    for ((id, entity) in state.entities) {
        if (state.logicalZone(id)?.zoneType == Zone.COMMAND && !entity.has<FaceDownComponent>()) add(state.objectRef(id))
    }
    return choices.values.toList()
}
