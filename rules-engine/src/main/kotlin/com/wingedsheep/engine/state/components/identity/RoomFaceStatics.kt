package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.StateTriggeredAbility
import com.wingedsheep.sdk.scripting.StaticAbility

/**
 * Resolves which static abilities are *currently functioning* on a battlefield permanent,
 * accounting for Room door state (CR 709.5).
 *
 * A Room (split card with a shared type line) is a single permanent whose two halves each carry
 * their own rules text in [com.wingedsheep.sdk.model.CardFace.script]. Per CR 709.5, a shared
 * type line represents two static abilities of the form "as long as this permanent doesn't have
 * the '<half> unlocked' designation, it doesn't have the … rules text of this object's <half>" —
 * so a *locked* face's abilities simply don't exist, and an *unlocked* face's abilities function
 * normally (the "unlocked" designations are CR 709.5c).
 *
 * The printed card's top-level [CardDefinition.script] for a Room is empty, so every historical
 * static-ability scan — which read only the top-level script — silently dropped *all* Room face
 * abilities. This helper folds in the static abilities of each unlocked face. It is the single
 * source of truth for "which static abilities does this battlefield permanent currently have":
 * continuous-effect projection ([com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler]),
 * granted activated abilities
 * ([com.wingedsheep.engine.legalactions.utils.CastPermissionUtils]), granted mana abilities
 * ([com.wingedsheep.engine.mechanics.mana.ManaSolver]), and no-maximum-hand-size
 * ([com.wingedsheep.engine.core.CleanupPhaseManager]) all route through it, so locked faces stay
 * inert and unlocking a door turns its statics on. The baked continuous-effect component is
 * refreshed on unlock by [com.wingedsheep.engine.handlers.actions.room.RoomDoorUnlocker].
 *
 * For a non-Room permanent (no [RoomComponent]) this returns exactly its top-level script's
 * effective static abilities — the same list instance, without copying, so the common case adds
 * no allocation on the projection / enumeration hot paths.
 */
object RoomFaceStatics {

    /**
     * Static abilities functioning on [container]'s permanent given its [cardDef]: its top-level
     * script's effective static abilities, plus the static abilities of every currently-unlocked
     * Room face.
     */
    fun activeStaticAbilities(
        container: ComponentContainer,
        cardDef: CardDefinition,
    ): List<StaticAbility> {
        val classLevel = container.get<ClassLevelComponent>()?.currentLevel
        val base = cardDef.script.effectiveStaticAbilities(classLevel)

        val room = container.get<RoomComponent>() ?: return base
        if (room.unlocked.isEmpty() || cardDef.cardFaces.isEmpty()) return base

        val faceStatics = cardDef.cardFaces
            .filter { RoomFaceId(it.name) in room.unlocked }
            .flatMap { it.script.effectiveStaticAbilities() }
        return if (faceStatics.isEmpty()) base else base + faceStatics
    }

    /**
     * State-triggered abilities (CR 603.8) functioning on [container]'s permanent given its
     * [cardDef]: its top-level script's state triggers, plus those of every currently-unlocked
     * Room face. Mirrors [activeStaticAbilities] so a locked door's state trigger stays inert and
     * unlocking it turns the trigger on (Promising Stairs' "you win the game if …").
     */
    fun activeStateTriggeredAbilities(
        container: ComponentContainer,
        cardDef: CardDefinition,
    ): List<StateTriggeredAbility> {
        val base = cardDef.script.stateTriggeredAbilities

        val room = container.get<RoomComponent>() ?: return base
        if (room.unlocked.isEmpty() || cardDef.cardFaces.isEmpty()) return base

        val faceAbilities = cardDef.cardFaces
            .filter { RoomFaceId(it.name) in room.unlocked }
            .flatMap { it.script.stateTriggeredAbilities }
        return if (faceAbilities.isEmpty()) base else base + faceAbilities
    }

    /**
     * Replacement effects printed on every currently-unlocked Room face of [container]'s permanent
     * (empty for a non-Room, or a Room with no door unlocked). Unlike [activeStaticAbilities] this
     * returns only the faces' share: the top-level script's replacement effects are collected by
     * the caller, which also filters them to the runtime-relevant ones. A locked door's replacement
     * effect doesn't exist (CR 709.5), so Torture Pit's "+2 noncombat damage" applies only once
     * that door is unlocked.
     */
    fun unlockedFaceReplacementEffects(
        container: ComponentContainer,
        cardDef: CardDefinition,
    ): List<ReplacementEffect> {
        val room = container.get<RoomComponent>() ?: return emptyList()
        if (room.unlocked.isEmpty() || cardDef.cardFaces.isEmpty()) return emptyList()
        return cardDef.cardFaces
            .filter { RoomFaceId(it.name) in room.unlocked }
            .flatMap { it.script.replacementEffects }
    }
}
