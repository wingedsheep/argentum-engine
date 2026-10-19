package com.wingedsheep.engine.view.projection

import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.mechanics.citysblessing.CitysBlessingService
import com.wingedsheep.engine.mechanics.enduringstory.EnduringStoryService
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.targeting.ControllerHexproof
import com.wingedsheep.engine.mechanics.targeting.ControllerShroud
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.faceDownDisplayName
import com.wingedsheep.engine.view.Visibility
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.combat.MustAttackPlayerComponent
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.player.*
import com.wingedsheep.engine.view.ClientEffectProgress
import com.wingedsheep.engine.view.ClientPlayerEffect
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive

/**
 * Projects the badges shown on a player: damage shields and doublers, skipped steps and turns,
 * player-level shroud/hexproof and casting grants, designations (city's blessing, the Ring),
 * pending next-spell riders, and emblems / delayed triggers they control.
 *
 * Each family is its own function; [project] concatenates them in a fixed order, which is the order
 * the client lists the badges in.
 */
internal class PlayerActiveEffectsProjector(
    private val predicateEvaluator: PredicateEvaluator,
    private val visibility: Visibility,
) {

    /**
     * Every badge on [playerId], in display order, as [viewingPlayerId] sees them: a badge that
     * names a face-down object names it only to a viewer who may look under it.
     */
    fun project(
        state: GameState,
        playerId: EntityId,
        container: ComponentContainer?,
        viewingPlayerId: EntityId,
        isSpectator: Boolean = false,
    ): List<ClientPlayerEffect> {
        if (container == null) return emptyList()
        val audience = Audience(viewingPlayerId, isSpectator)
        return damagePreventionShields(state, playerId, audience) +
            damageDoublers(state, playerId) +
            turnSkips(container) +
            targetingAndCastingGrants(state, playerId, container) +
            designations(state, playerId, container, audience) +
            attackRequirements(container) +
            pendingSpellRiders(state, playerId) +
            emblemsAndDelayedTriggers(state, playerId, container, audience)
    }

    /**
     * Damage prevention shields protecting [playerId]: one badge per group shield that includes its
     * controller, then one per kind of shield totalled across every floating effect.
     */
    private fun damagePreventionShields(state: GameState, playerId: EntityId, audience: Audience): List<ClientPlayerEffect> =
        controllerGroupShieldBadges(state, playerId) + tallyShields(state, playerId, audience).badges(state, audience)

    /** "Prevent all damage to creatures you control and you" shields, one badge per shield. */
    private fun controllerGroupShieldBadges(state: GameState, playerId: EntityId): List<ClientPlayerEffect> =
        state.floatingEffects.mapNotNull { floatingEffect ->
            val modification = floatingEffect.effect.modification
            if (
                modification !is SerializableModification.PreventAllDamageToGroup ||
                !modification.includesController ||
                floatingEffect.controllerId != playerId
            ) return@mapNotNull null
            val sourceDescription = modification.sourceFilter?.description
            val scopeDescription = if (modification.combatOnly) "combat damage" else "damage"
            val sourceSuffix = sourceDescription?.let { " from $it" } ?: ""
            ClientPlayerEffect(
                effectId = "prevent_damage_to_controller_${floatingEffect.id.value}",
                name = "Damage Shield",
                description = "All $scopeDescription that would be dealt to you$sourceSuffix is prevented",
                icon = "prevent-damage"
            )
        }

    /** The prevention shields on one player, totalled per kind across every floating effect. */
    private class ShieldTally {
        /** Each "prevent the next N" shield's ending and amount. */
        val preventNext = mutableListOf<Pair<String?, Int>>()
        var preventsAllDamage = false
        var preventsAllCombatDamage = false
        val preventedNextFromMatching = mutableSetOf<String>()
        val preventedCombatDamageSources = mutableSetOf<String>()
        val preventedAllDamageSources = mutableSetOf<String>()
        val preventedFromSources = mutableSetOf<EntityId>()
        // Single-instance chosen-source shields, kept separate from the all-damage ones above
        // because they read differently: "the next time" rather than "all damage", and Dark Sphere
        // halves rather than prevents. Pair = (source, halved).
        val preventedNextInstanceFromSources = mutableListOf<Pair<EntityId, Boolean>>()
        val leavingAmountShields = mutableListOf<ClientPlayerEffect>()
    }

    /** Check floating effects for damage prevention shields on this player. */
    private fun tallyShields(state: GameState, playerId: EntityId, audience: Audience): ShieldTally {
        val tally = ShieldTally()
        for (floatingEffect in state.floatingEffects) {
            val modification = floatingEffect.effect.modification
            // Board-wide combat-damage prevention (Fog, Holy Day, Spore Flower's ability). These
            // shields name neither a permanent nor a player, so they carry no affected entity to
            // hang a card badge on — and without a player badge the only trace that one resolved is
            // a line in the log. They apply to every creature's combat damage, so both players
            // carry the badge.
            when (modification) {
                is SerializableModification.PreventAllCombatDamage -> tally.preventsAllCombatDamage = true
                is SerializableModification.PreventCombatDamageFromGroup ->
                    tally.preventedCombatDamageSources.add(modification.filter.description)
                is SerializableModification.PreventAllDamageFromGroup ->
                    if (modification.combatOnly) tally.preventedCombatDamageSources.add(modification.filter.description)
                    else tally.preventedAllDamageSources.add(modification.filter.description)
                else -> {}
            }
            if (playerId !in floatingEffect.effect.affectedEntities) continue
            when (modification) {
                is SerializableModification.PreventAllDamageTo -> {
                    // A Silhouette-style shield covers only targeted damage, not "all damage".
                    if (!modification.onlyCausedByObjectTargetingIt) tally.preventsAllDamage = true
                }
                is SerializableModification.PreventNextDamage -> {
                    tally.preventNext += durationPhrase(state, floatingEffect) to modification.remainingAmount
                }
                is SerializableModification.PreventNextDamageFromMatching -> {
                    tally.preventedNextFromMatching.add(modification.filter.description)
                }
                is SerializableModification.PreventAllDamageFromSource -> {
                    tally.preventedFromSources.add(modification.damageSourceId)
                }
                is SerializableModification.PreventNextDamageLeavingAmount -> {
                    val sourceRef = floatingEffect.referencedObjects.firstOrNull { it.entityId == modification.damageSourceId }
                    val sourceName = if (sourceRef != null && state.isCurrentObject(sourceRef) &&
                        state.logicalZone(modification.damageSourceId)?.zoneType !in setOf(Zone.HAND, Zone.LIBRARY)) {
                        audience.name(state, modification.damageSourceId,
                            state.getEntity(modification.damageSourceId)?.get<CardComponent>()?.name ?: modification.sourceName)
                    } else modification.sourceName
                    val kind = if (modification.combatOnly) "combat damage" else "damage"
                    val ending = durationPhrase(state, floatingEffect)?.let { " $it" } ?: ""
                    tally.leavingAmountShields.add(ClientPlayerEffect(
                        effectId = "prevent_next_damage_leaving_amount_${floatingEffect.id.value}",
                        name = "Leave ${modification.amountToLeave} from $sourceName",
                        description = "The next time $sourceName would deal $kind to you$ending, " +
                            "prevent all but ${modification.amountToLeave} of that damage",
                        icon = "prevent-damage"
                    ))
                }
                is SerializableModification.PreventNextDamageInstanceFromSource -> {
                    tally.preventedNextInstanceFromSources.add(
                        modification.damageSourceId to modification.halveRoundedDown
                    )
                }
                else -> {}
            }
        }
        return tally
    }

    private fun ShieldTally.badges(state: GameState, audience: Audience): List<ClientPlayerEffect> {
        val effects = leavingAmountShields.toMutableList()
        if (preventsAllDamage) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "prevent_all_damage",
                    name = "Prevent All Damage",
                    description = "All damage that would be dealt to you is prevented",
                    icon = "prevent-damage"
                )
            )
        }
        if (preventsAllCombatDamage) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "prevent_all_combat_damage",
                    name = "No Combat Damage",
                    description = "All combat damage that would be dealt this turn is prevented",
                    icon = "prevent-damage"
                )
            )
        }
        for (sourceDescription in preventedCombatDamageSources) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "prevent_combat_damage_from_${sourceDescription.lowercase().replace(' ', '_')}",
                    name = "No Combat Damage",
                    description = "Combat damage that would be dealt by $sourceDescription this turn is prevented",
                    icon = "prevent-damage"
                )
            )
        }
        for (sourceDescription in preventedAllDamageSources) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "prevent_all_damage_from_${sourceDescription.lowercase().replace(' ', '_')}",
                    name = "No Damage",
                    description = "All damage that would be dealt by $sourceDescription this turn is prevented",
                    icon = "prevent-damage"
                )
            )
        }
        val preventDamageTotal = preventNext.sumOf { it.second }
        if (preventDamageTotal > 0) {
            val ending = combinedEnding(preventNext.map { it.first }, preventNext.map { it.second })
            effects.add(
                ClientPlayerEffect(
                    effectId = "prevent_damage",
                    name = "Prevent $preventDamageTotal",
                    description = "The next $preventDamageTotal damage that would be dealt to you is prevented" +
                        (ending?.let { " ($it)" } ?: ""),
                    icon = "prevent-damage",
                    duration = ending
                )
            )
        }
        for (sourceDescription in preventedNextFromMatching) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "prevent_next_damage_from_${sourceDescription.lowercase().replace(' ', '_')}",
                    name = "Prevent Next",
                    description = "The next time a $sourceDescription would deal damage to you this turn, prevent that damage",
                    icon = "prevent-damage"
                )
            )
        }
        for (sourceId in preventedFromSources) {
            val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name
                ?.let { audience.name(state, sourceId, it) } ?: "a chosen source"
            effects.add(
                ClientPlayerEffect(
                    effectId = "prevent_damage_from_source_${sourceId.value}",
                    name = "Prevent from $sourceName",
                    description = "All damage that would be dealt to you by $sourceName this turn is prevented",
                    icon = "prevent-damage"
                )
            )
        }

        // The Circle of Protection family and Dark Sphere: one instance from one chosen source.
        // Listed per shield rather than deduplicated by source — two Circles pointed at the same
        // source are two separate shields, each spent by its own damage instance.
        for ((sourceId, halved) in preventedNextInstanceFromSources) {
            val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name
                ?.let { audience.name(state, sourceId, it) } ?: "a chosen source"
            effects.add(
                ClientPlayerEffect(
                    effectId = "prevent_next_damage_instance_from_source_${sourceId.value}" +
                        if (halved) "_halved" else "",
                    name = if (halved) "Halve from $sourceName" else "Prevent from $sourceName",
                    description = if (halved) {
                        "The next time $sourceName would deal damage to you this turn, " +
                            "prevent half that damage, rounded down"
                    } else {
                        "The next time $sourceName would deal damage to you this turn, prevent that damage"
                    },
                    icon = "prevent-damage"
                )
            )
        }
        return effects
    }

    /**
     * Damage-doubling warnings — the mirror image of the prevention shields above, so a player
     * about to take double damage can see it before they attack into it.
     */
    private fun damageDoublers(state: GameState, playerId: EntityId): List<ClientPlayerEffect> {
        val effects = mutableListOf<ClientPlayerEffect>()
        // Twinflame Tyrant, Gratuitous Violence. One badge per replacement: each applies once
        // (CR 616.1), so two Tyrants show two badges and quadruple the damage.
        for (doubler in DamageUtils.damageDoublersAffectingPlayer(state, playerId, predicateEvaluator = predicateEvaluator)) {
            val scope = when (doubler.damageType) {
                is DamageType.Combat -> "Combat damage"
                is DamageType.NonCombat -> "Noncombat damage"
                is DamageType.Any -> "Damage"
            }
            effects.add(
                ClientPlayerEffect(
                    effectId = "damage_doubled_${doubler.sourceId.value}",
                    name = "Damage ${doubler.multiplierVerb.replaceFirstChar { it.uppercase() }}",
                    description = "$scope dealt to you is ${doubler.multiplierVerb} by ${doubler.sourceName}",
                    icon = "double-damage"
                )
            )
        }

        // Player-scoped damage doubling from a floating effect (Lightning, Army of One's "Stagger").
        // Same badge, but it outlives its source, so it's named for the effect rather than a card.
        for (floatingEffect in state.floatingEffects) {
            val modification = floatingEffect.effect.modification
            if (modification !is SerializableModification.DoubleDamageToPlayer) continue
            if (modification.playerId != playerId) continue
            val attribution = floatingEffect.sourceName?.let { " ($it)" } ?: ""
            effects.add(
                ClientPlayerEffect(
                    effectId = "damage_doubled_player_${floatingEffect.id.value}",
                    name = "Damage Doubled",
                    description = "Damage dealt to you and to permanents you control is doubled$attribution",
                    icon = "double-damage"
                )
            )
        }
        return effects
    }

    /** Parts of the player's turn (or the whole turn) that will be skipped, and Last Chance's loss. */
    private fun turnSkips(container: ComponentContainer): List<ClientPlayerEffect> {
        val effects = mutableListOf<ClientPlayerEffect>()

        // Check for SkipCombatPhasesComponent (False Peace effect)
        if (container.has<SkipCombatPhasesComponent>()) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "skip_combat",
                    name = "Skip Combat",
                    description = "Combat phases will be skipped on your next turn",
                    icon = "shield-off"
                )
            )
        }

        // Turn-scoped step/phase skips (Fatespinner). One badge per chosen part, so a player who
        // has been hit by two of them sees both.
        container.get<SkippedTurnPartsComponent>()?.parts?.forEach { part ->
            effects.add(
                ClientPlayerEffect(
                    effectId = "skip_turn_part_${part.name.lowercase()}",
                    name = "Skip ${part.displayName.replaceFirstChar { it.uppercase() }}",
                    description = "You skip each ${part.displayName} this turn",
                    icon = "shield-off"
                )
            )
        }

        // Skipped untap steps (Shisato, Whispering Hunter)
        container.get<SkipNextUntapStepComponent>()?.let { skip ->
            effects.add(
                ClientPlayerEffect(
                    effectId = "skip_untap_step",
                    name = "Skip Untap Step",
                    description = if (skip.steps == 1) "You skip your next untap step"
                        else "You skip your next ${skip.steps} untap steps",
                    icon = "shield-off"
                )
            )
        }

        // Check for SkipUntapComponent (Exhaustion effect)
        val skipUntap = container.get<SkipUntapComponent>()
        if (skipUntap != null) {
            val affected = when {
                skipUntap.affectsCreatures && skipUntap.affectsLands -> "Creatures and lands"
                skipUntap.affectsCreatures -> "Creatures"
                skipUntap.affectsLands -> "Lands"
                else -> "Permanents"
            }
            effects.add(
                ClientPlayerEffect(
                    effectId = "skip_untap",
                    name = "Skip Untap",
                    description = "$affected won't untap during your next untap step",
                    icon = "shield"
                )
            )
        }

        // Check for LoseAtEndStepComponent (Last Chance effect)
        val loseAtEndStep = container.get<LoseAtEndStepComponent>()
        if (loseAtEndStep != null) {
            val description = if (loseAtEndStep.turnsUntilLoss <= 0) {
                "You will lose the game at the beginning of this turn's end step"
            } else {
                "You will lose the game at the beginning of your next end step"
            }
            effects.add(
                ClientPlayerEffect(
                    effectId = "lose_at_end_step",
                    name = "Last Chance",
                    description = description,
                    icon = "skull"
                )
            )
        }

        val turnSkips = container.get<SkipNextTurnComponent>()
        if (turnSkips != null && turnSkips.turns > turnSkips.extraTurnBypasses) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "skip_next_turn",
                    name = "Skip Turn",
                    description = "Your next turn will be skipped",
                    icon = "skip"
                )
            )
        }
        return effects
    }

    /** Player shroud / hexproof, and this turn's casting grants (uncounterable, flash). */
    private fun targetingAndCastingGrants(
        state: GameState,
        playerId: EntityId,
        container: ComponentContainer
    ): List<ClientPlayerEffect> {
        val effects = mutableListOf<ClientPlayerEffect>()

        // Shroud, from a resolution-time effect (Gilded Light) or from a permanent that grants it
        // (True Believer). [ControllerShroud] unions the two and re-evaluates any "as long as …"
        // gate the grant sits behind, so the badge tracks the gate instead of freezing on entry.
        if (ControllerShroud.appliesTo(state, playerId, predicateEvaluator = predicateEvaluator)) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "player_shroud",
                    name = "Shroud",
                    description = "You have shroud (you can't be the target of spells or abilities)",
                    icon = "shield"
                )
            )
        }

        // Check for SpellsCantBeCounteredComponent (e.g., Domri, Anarch of Bolas +1)
        container.get<SpellsCantBeCounteredComponent>()?.let { component ->
            val filterDescription = component.filters
                .joinToString(", ") { it.description }
                .ifBlank { "Spells" }
            effects.add(
                ClientPlayerEffect(
                    effectId = "spells_cant_be_countered",
                    name = "Uncounterable",
                    description = "${filterDescription.replaceFirstChar { it.uppercase() }} " +
                        "spells you cast this turn can't be countered",
                    icon = "no-counter"
                )
            )
        }

        // Check for FlashGrantsComponent (Borne Upon a Wind, Teferi, Time Raveler's +1), one
        // badge per lifetime. GameObjectFilter.Any describes itself as "card" — render it as a bare
        // "spells" so the Borne-Upon-a-Wind case reads naturally, and join multiple filter branches
        // with "or" to keep stacked grants grammatical.
        container.get<FlashGrantsComponent>()?.let { component ->
            for ((removeOn, grants) in component.grants.groupBy { it.removeOn }) {
                val rendered = grants.map { it.filter }
                    .map { if (it == GameObjectFilter.Any) "" else it.description.lowercase() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                val spellPhrase = if (rendered.isEmpty()) "spells" else "${rendered.joinToString(" or ")} spells"
                val (idSuffix, window) = when (removeOn) {
                    PlayerEffectRemoval.EndOfTurn -> "this_turn" to " this turn"
                    PlayerEffectRemoval.UntilYourNextTurn -> "until_next_turn" to " until your next turn"
                    PlayerEffectRemoval.EndOfYourNextTurn -> "end_of_next_turn" to " until the end of your next turn"
                    PlayerEffectRemoval.Permanent -> "permanent" to ""
                }
                effects.add(
                    ClientPlayerEffect(
                        effectId = "flash_grants_$idSuffix",
                        name = "Flash",
                        description = "You may cast $spellPhrase$window as though they had flash",
                        icon = "lightning"
                    )
                )
            }
        }

        // Loyalty-activation permissions (Jace's Machinations, Urza Assembles the Titans).
        container.get<com.wingedsheep.engine.state.components.player.LoyaltyActivationPermissionsComponent>()?.let { component ->
            val instant = component.grants.filter { it.instantSpeed }
            if (instant.isNotEmpty()) {
                val rendered = instant.joinToString(" or ") { "${it.filter.description}s" }
                effects.add(
                    ClientPlayerEffect(
                        effectId = "instant_speed_loyalty",
                        name = "Instant-speed loyalty",
                        description = "You may activate loyalty abilities of $rendered any time you could cast an instant this turn",
                        icon = "lightning"
                    )
                )
            }
            val extra = component.grants.filter { it.times >= 2 }
            if (extra.isNotEmpty()) {
                val rendered = extra.joinToString(" or ") {
                    "${it.filter.description}s " + (if (it.times == 2) "twice" else "${it.times} times")
                }
                effects.add(
                    ClientPlayerEffect(
                        effectId = "extra_loyalty_activations",
                        name = "Extra loyalty activations",
                        description = "You may activate loyalty abilities of $rendered this turn rather than only once",
                        icon = "lightning"
                    )
                )
            }
        }

        // Tapping other players' permanents for mana (Piracy).
        container.get<com.wingedsheep.engine.state.components.player.TapForManaGrantsComponent>()?.let { component ->
            val rendered = component.grants.joinToString(" or ") { "${it.filter.description}s" }
            val restrictions = component.grants.mapNotNull { it.restriction?.description }.distinct()
            val suffix = if (restrictions.isEmpty()) "" else ". " + restrictions.joinToString(". ")
            effects.add(
                ClientPlayerEffect(
                    effectId = "tap_for_mana_you_dont_control",
                    name = "Borrowed mana",
                    description = "You may tap $rendered you don't control for mana this turn$suffix",
                    icon = "lightning"
                )
            )
        }

        // Hexproof, from a resolution-time effect (Dawn's Truce) or from a permanent that grants it
        // (Shalai, Voice of Plenty). Same union-and-re-evaluate as shroud above; this used to be
        // two blocks, the second scanning the battlefield on the *base* controller so a stolen
        // Shalai badged the wrong player.
        if (ControllerHexproof.appliesTo(state, playerId, predicateEvaluator = predicateEvaluator)) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "player_hexproof",
                    name = "Hexproof",
                    description = "You have hexproof (you can't be the target of spells or abilities your opponents control)",
                    icon = "shield"
                )
            )
        }
        return effects
    }

    /** Lasting player designations: the city's blessing, an enduring story, the Ring emblem. */
    private fun designations(
        state: GameState,
        playerId: EntityId,
        container: ComponentContainer,
        audience: Audience,
    ): List<ClientPlayerEffect> {
        val effects = mutableListOf<ClientPlayerEffect>()

        // Ascend / city's blessing (CR 702.131). Read through CitysBlessingService rather than the
        // component so the badge tracks the rule: ascend on a permanent is continuous, and a player
        // who has just crossed ten permanents already has the blessing even though the state-based
        // action that writes the marker hasn't been polled yet. Surface the actual Scryfall "City's
        // Blessing" marker card (tblc #40) as the badge image so it matches the physical-game
        // marker players know.
        if (CitysBlessingService.has(state, playerId)) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "citys_blessing",
                    name = "City's Blessing",
                    description = "You have the city's blessing for the rest of the game",
                    icon = "shield",
                    imageUri = "https://cards.scryfall.io/normal/front/3/0/30758c2e-fc01-4037-838c-bdabe8a4e5a3.jpg?1721428739"
                )
            )
        }

        // The monarch (CR 725). Surface the Scryfall "The Monarch" marker card (tcn2 #1) as the badge
        // image, like the City's Blessing above.
        if (state.monarch == playerId) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "monarch",
                    name = "The Monarch",
                    description = "Draw a card at the beginning of your end step. A creature that " +
                        "deals combat damage to you makes its controller the monarch.",
                    icon = "monarch",
                    imageUri = "https://cards.scryfall.io/normal/front/4/0/40b79918-22a7-4fff-82a6-8ebfe6e87185.jpg?1783937275"
                )
            )
        }

        // The initiative (CR 726). Independent of the monarch — one player can hold both. The badge
        // image is the back face of the Undercity // The Initiative card (tclb #20).
        if (state.initiative == playerId) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "initiative",
                    name = "The Initiative",
                    description = "Venture into Undercity when you take the initiative and at the " +
                        "beginning of your upkeep. Creatures that deal combat damage to you take the " +
                        "initiative for their controller.",
                    icon = "initiative",
                    imageUri = "https://cards.scryfall.io/normal/back/2/c/2c65185b-6cf0-451d-985e-56aa45d9a57d.jpg?1788840929"
                )
            )
        }

        // Storied / enduring story (CR 702.195). Read through EnduringStoryService rather than the
        // component for the same reason as the city's blessing above: storied on a permanent is
        // continuous, so a player who has just crossed three qualifying permanents already has the
        // designation even though the state-based action that writes the marker hasn't been polled
        // yet. The badge image is the actual Scryfall "Enduring Story" marker card (thob #14), so it
        // matches the physical-game marker players know.
        if (EnduringStoryService.has(state, playerId)) {
            effects.add(
                ClientPlayerEffect(
                    effectId = "enduring_story",
                    name = "Enduring Story",
                    description = "You have an enduring story for the rest of the game",
                    icon = "shield",
                    imageUri = "https://cards.scryfall.io/normal/front/5/3/53bfaac7-07cf-4637-8f64-aba93ec7fd1a.jpg?1785534385"
                )
            )
        }

        // The Ring emblem (CR 701.54). Surface the tempt count so the player can see which of the
        // emblem's four cumulative abilities are active and which creature is their Ring-bearer.
        container.get<TheRingComponent>()?.let { ring ->
            val bearerName = state.getBattlefield()
                .firstOrNull { state.getEntity(it)?.get<RingBearerComponent>()?.ownerId == playerId }
                ?.let { id -> state.getEntity(id)?.get<CardComponent>()?.name?.let { audience.name(state, id, it) } }
            val bearerLine = bearerName?.let { "Your Ring-bearer is $it." } ?: "You have no Ring-bearer."
            effects.add(
                ClientPlayerEffect(
                    effectId = "the_ring",
                    name = "The Ring",
                    description = "The Ring has tempted you ${ring.temptCount} time(s). $bearerLine",
                    icon = "the-ring",
                    // current = raw tempt count (may exceed 4); total = the four cumulative abilities.
                    progress = ClientEffectProgress(current = ring.temptCount, total = 4)
                )
            )
        }
        return effects
    }

    /** Check for MustAttackPlayerComponent (Taunt effect). */
    private fun attackRequirements(container: ComponentContainer): List<ClientPlayerEffect> {
        val mustAttack = container.get<MustAttackPlayerComponent>() ?: return emptyList()
        val description = if (mustAttack.activeThisTurn) {
            "Your creatures must attack this turn"
        } else {
            "Your creatures must attack on your next turn"
        }
        return listOf(
            ClientPlayerEffect(
                effectId = "must_attack",
                name = "Taunted",
                description = description,
                icon = "taunt"
            )
        )
    }

    /** "Your next spell …" riders still waiting for that spell: copy it, uncounterable, free. */
    private fun pendingSpellRiders(state: GameState, playerId: EntityId): List<ClientPlayerEffect> {
        val effects = mutableListOf<ClientPlayerEffect>()

        // Check for pending spell copies (e.g., Howl of the Horde)
        val pendingCopies = state.pendingSpellCopies.filter { it.controllerId == playerId }
        if (pendingCopies.isNotEmpty()) {
            val totalCopies = pendingCopies.sumOf { it.copies }
            val sourceName = pendingCopies.joinToString(", ") { it.sourceName }
            val filterDesc = pendingCopies.map { it.spellFilter.description }.distinct().joinToString("/")
            effects.add(
                ClientPlayerEffect(
                    effectId = "pending_spell_copy",
                    name = "Copy Spell",
                    description = "Your next $filterDesc spell will be copied $totalCopies time(s) ($sourceName)",
                    icon = "copy-spell"
                )
            )
        }

        // Check for pending "next spell can't be countered" riders (e.g., Mistrise Village)
        val pendingUncounterable = state.pendingUncounterableSpells.filter { it.controllerId == playerId }
        if (pendingUncounterable.isNotEmpty()) {
            val sourceName = pendingUncounterable.map { it.sourceName }.distinct().joinToString(", ")
            val filterDesc = pendingUncounterable.map { it.spellFilter.description }.distinct().joinToString("/")
            effects.add(
                ClientPlayerEffect(
                    effectId = "pending_uncounterable_spell",
                    name = "Uncounterable",
                    description = "Your next $filterDesc spell can't be countered ($sourceName)",
                    icon = "no-counter"
                )
            )
        }

        // Check for pending "next spell has improvise / convoke / delve" riders (Archway of
        // Innovation) — one badge per keyword, so the player sees the grant is still waiting.
        state.pendingNextSpellKeywords
            .filter { it.controllerId == playerId }
            .groupBy { it.keyword }
            .forEach { (keyword, riders) ->
                val sourceName = riders.map { it.sourceName }.distinct().joinToString(", ")
                val filterDesc = if (riders.any { it.spellFilter == GameObjectFilter.Any }) {
                    ""
                } else {
                    riders.map { it.spellFilter.description }.distinct().joinToString("/") + " "
                }
                effects.add(
                    ClientPlayerEffect(
                        effectId = "pending_next_spell_keyword_${keyword.name.lowercase()}",
                        name = keyword.displayName,
                        description = "Your next ${filterDesc}spell has ${keyword.displayName.lowercase()} ($sourceName)",
                        icon = "granted-ability"
                    )
                )
            }

        // Check for pending "next spell can be cast without paying its mana cost" riders
        // (e.g. World War Hulk I) — the player needs to see the free cast is still available.
        val pendingFreeCasts = state.pendingFreeCastSpells.filter { it.controllerId == playerId }
        if (pendingFreeCasts.isNotEmpty()) {
            val sourceName = pendingFreeCasts.map { it.sourceName }.distinct().joinToString(", ")
            // With several riders the broadest one sets the wording: an unfiltered rider frees the
            // next spell whatever it is, so naming only a filtered sibling's types would understate
            // it — and joining a blank filter into the list rendered as "red or green creature /".
            val filterDesc = if (pendingFreeCasts.any { it.spellFilter == GameObjectFilter.Any }) {
                ""
            } else {
                pendingFreeCasts
                    .map { it.spellFilter.description }
                    .distinct()
                    .joinToString("/") + " "
            }
            effects.add(
                ClientPlayerEffect(
                    effectId = "pending_free_cast_spell",
                    name = "Free Cast",
                    description = "Your next ${filterDesc}spell can be cast without paying its mana cost ($sourceName)",
                    icon = "free-cast"
                )
            )
        }

        // Check for pending "for one spell this turn, mana of any type" riders (North Star) — the
        // player needs to see the rider is still waiting for the spell that needs it.
        val pendingAnyType = state.pendingAnyManaTypeSpells.filter { it.controllerId == playerId }
        if (pendingAnyType.isNotEmpty()) {
            val sourceName = pendingAnyType.map { it.sourceName }.distinct().joinToString(", ")
            val filterDesc = if (pendingAnyType.any { it.spellFilter == GameObjectFilter.Any }) {
                ""
            } else {
                pendingAnyType.map { it.spellFilter.description }.distinct().joinToString("/") + " "
            }
            effects.add(
                ClientPlayerEffect(
                    effectId = "pending_one_spell_any_mana_type",
                    name = "Any Mana Type",
                    description = "For one ${filterDesc}spell this turn, you may spend mana as though it were mana of any type to pay its mana cost ($sourceName)",
                    icon = "granted-ability"
                )
            )
        }
        return effects
    }

    /**
     * Abilities the player holds that live on no permanent: global granted triggers (emblems and
     * "this turn" abilities), event-based delayed triggers, static emblems, and granted spell
     * keywords.
     */
    private fun emblemsAndDelayedTriggers(
        state: GameState,
        playerId: EntityId,
        container: ComponentContainer,
        audience: Audience,
    ): List<ClientPlayerEffect> {
        val effects = mutableListOf<ClientPlayerEffect>()

        // Check for global triggered abilities controlled by this player
        // Group by source name so abilities from the same source show as one badge
        val globalAbilitiesBySource = state.globalGrantedTriggeredAbilities
            .filter { it.controllerId == playerId }
            .groupBy { it.sourceName to it.duration }

        for ((key, abilities) in globalAbilitiesBySource) {
            val (sourceName, duration) = key
            val description = abilities.joinToString(" ") {
                it.descriptionOverride ?: it.ability.description
            }
            val isPermanent = duration == Duration.Permanent
            val ending = durationPhrase(state, duration, playerId)
            val durationSuffix = ending?.let { " ($it)" } ?: ""
            effects.add(
                ClientPlayerEffect(
                    effectId = "emblem_${sourceName.lowercase().replace(" ", "_").replace(",", "")}${if (!isPermanent) "_temp" else ""}",
                    name = if (isPermanent) "$sourceName Emblem" else sourceName,
                    description = description + durationSuffix,
                    icon = if (isPermanent) "emblem" else "triggered-ability",
                    duration = ending
                )
            )
        }

        // Check for event-based delayed triggers controlled by this player
        // (e.g., Flitterwing Nuisance's "whenever a creature you control deals combat
        //  damage to a player this turn, draw a card" floating ability).
        // Step-based delayed triggers are scheduled actions and get their own badges below.
        val delayedBySource = state.delayedTriggers
            .filter { it.controllerId == playerId && it.trigger != null }
            // A face-down source (a morph granted the ability) shows its face-down name, not its face.
            .groupBy {
                DelayedBadgeKey(
                    audience.name(state, it.sourceId, it.sourceName), it.expiry, it.fireOnce,
                    it.expiryPlayerId ?: it.watchedEntityId?.takeIf { id -> id in state.turnOrder }
                )
            }

        for ((key, triggers) in delayedBySource) {
            val (sourceName, expiry, fireOnce) = key
            val first = triggers.first()
            val event = first.trigger?.event
            // A cast watcher names the one player it watches ("Whenever Bob casts a spell").
            val watchedCaster = first.watchedEntityId
                ?.takeIf { event is com.wingedsheep.sdk.scripting.EventPattern.SpellCastEvent }
                ?.let { state.getEntity(it)?.get<PlayerComponent>()?.name }
            val triggerDesc = when {
                event is com.wingedsheep.sdk.scripting.EventPattern.SpellCastEvent && watchedCaster != null &&
                    event.player == com.wingedsheep.sdk.scripting.references.Player.Each ->
                    watchedCaster + event.description.removePrefix(event.player.description)
                else -> event?.description ?: "the triggered event"
            }
            val effectDesc = first.effect.description.replaceFirstChar { it.lowercase() }
            val countSuffix = if (triggers.size > 1) " (×${triggers.size})" else ""
            val lead = if (fireOnce) "The next time" else "Whenever"
            val ends = expiryPhrase(state, first, playerId)
            effects.add(
                ClientPlayerEffect(
                    effectId = "delayed_trigger_${sourceName.lowercase().replace(" ", "_").replace(",", "")}" +
                        "_${expiry?.let { it::class.simpleName } ?: "Never"}${if (fireOnce) "_once" else ""}" +
                        (key.scopePlayerId?.let { "_${it.value}" } ?: ""),
                    name = "$sourceName$countSuffix",
                    description = "$lead $triggerDesc, $effectDesc." + (ends?.let { " (${it.replaceFirstChar { c -> c.uppercase() }})" } ?: ""),
                    icon = "triggered-ability",
                    duration = ends
                )
            )
        }

        // Step-based delayed triggers: something scheduled for a coming step ("return it at the
        // beginning of the next end step"). The spell or ability that scheduled one resolved in
        // the open, but nothing on the board shows it until it fires.
        for (scheduled in state.delayedTriggers) {
            if (scheduled.controllerId != playerId || scheduled.trigger != null) continue
            val step = scheduled.fireAtStep ?: continue
            effects.add(
                ClientPlayerEffect(
                    effectId = "scheduled_trigger_${scheduled.id}",
                    // Both players see this badge, so a face-down source keeps its face-down name.
                    name = audience.name(state, scheduled.sourceId, scheduled.sourceName),
                    description = "${scheduleText(state, scheduled, step)}: " +
                        "${scheduledEffectText(state, scheduled.effect, audience).replaceFirstChar { it.uppercase() }}.",
                    icon = "triggered-ability"
                )
            )
        }

        // Check for static-ability emblems controlled by this player. These are synthetic
        // "emblem source" entities created by CreatePermanentEmblemExecutor; they live in
        // GameState.entities but never enter a zone.
        for ((emblemId, emblemContainer) in state.entities) {
            val emblem = emblemContainer.get<EmblemSourceComponent>() ?: continue
            val emblemController = emblemContainer.get<ControllerComponent>()?.playerId
            if (emblemController != playerId) continue
            effects.add(
                ClientPlayerEffect(
                    effectId = "emblem_static_${emblemId.value}",
                    name = "${emblem.sourceName} Emblem",
                    description = emblem.description,
                    icon = "emblem",
                    sourceId = emblemId.takeIf {
                        emblemContainer.has<EmblemOwnedActivatedAbilityComponent>()
                    }
                )
            )
        }

        // Check for granted spell keywords (emblems like "spells you cast have storm")
        val grantedKeywords = container.get<GrantedSpellKeywordsComponent>()
        if (grantedKeywords != null) {
            for (grant in grantedKeywords.grants) {
                val filterDesc = grant.spellFilter.description
                val spellTypeDesc = if (filterDesc == "card" || filterDesc.isBlank()) "Spells"
                    else "${filterDesc.replaceFirstChar { it.uppercase() }} spells"
                effects.add(
                    ClientPlayerEffect(
                        effectId = "emblem_spell_keyword_${grant.keyword.name.lowercase()}",
                        name = "Emblem",
                        description = "$spellTypeDesc you cast have ${grant.keyword.name.lowercase()}.",
                        icon = "emblem"
                    )
                )
            }
        }
        return effects
    }

    /** When an event-based delayed trigger stops watching, or null when only firing ends it. */
    private fun expiryPhrase(state: GameState, delayed: DelayedTriggeredAbility, controllerId: EntityId): String? =
        when (delayed.expiry) {
            DelayedTriggerExpiry.EndOfTurn -> "until end of turn"
            DelayedTriggerExpiry.EndOfCombat -> "until end of combat"
            DelayedTriggerExpiry.UntilControllersNextTurn -> durationPhrase(state, Duration.UntilYourNextTurn, controllerId)
            is DelayedTriggerExpiry.EndOfPlayersNextTurn -> delayed.expiryPlayerId?.let { player ->
                durationPhrase(state, Duration.EndOfYourNextTurn, player, delayed.expiresAfterTurn)
            }
            DelayedTriggerExpiry.Never, null -> null
        }

    /**
     * Delayed triggers that share a badge: same source, ending and firing shape, and the same
     * player they watch or wait on (two Nuka-Nuke riders on different opponents are two badges).
     */
    private data class DelayedBadgeKey(
        val sourceName: String,
        val expiry: DelayedTriggerExpiry?,
        val fireOnce: Boolean,
        val scopePlayerId: EntityId?
    )

    /**
     * When a step-based delayed trigger fires: "At the beginning of the next end step", "At the
     * beginning of Bob's next upkeep", "At the beginning of each combat this turn".
     */
    private fun scheduleText(state: GameState, scheduled: DelayedTriggeredAbility, step: Step): String {
        val stepName = when (step) {
            Step.UPKEEP -> "upkeep"
            Step.BEGIN_COMBAT -> "combat"
            else -> step.displayName.lowercase()
        }
        // Both seats read this badge, so the player is named rather than called "your".
        val onTurnOf = scheduled.fireOnPlayerId?.let { player ->
            "${state.getEntity(player)?.get<PlayerComponent>()?.name ?: "a player"}'s"
        }
        // A trigger that expires at cleanup ("at the beginning of the next combat this turn") says
        // so: cast after the last combat, it lapses without firing.
        val thisTurn = if (scheduled.expiry == DelayedTriggerExpiry.EndOfTurn) " this turn" else ""
        val occasion = if (scheduled.repeatAtEachMatchingStep) {
            "each $stepName" + (onTurnOf?.let { " on $it turn" } ?: "") + thisTurn
        } else {
            "${onTurnOf ?: "the"} next $stepName$thisTurn"
        }
        val notBefore = scheduled.notBeforeTurn?.takeIf { it > state.turnNumber }?.let { " (not before turn $it)" } ?: ""
        return "At the beginning of $occasion$notBefore"
    }

    /**
     * The scheduled effect's text with each object it captured named. Scheduling bakes the
     * effect's targets into [EffectTarget.SpecificEntity] references, whose own text is only
     * "specific entity"; it is replaced by the name every player may read. The text and the
     * serialized effect need not mention the references in the same order, so this happens only
     * when every reference reads the same; otherwise the placeholder stays rather than risk
     * naming the wrong card.
     */
    private fun scheduledEffectText(state: GameState, effect: Effect, audience: Audience): String {
        val text = effect.description
        if (SPECIFIC_ENTITY !in text) return text
        val captured = mutableListOf<EntityId>()
        fun collect(element: JsonElement) {
            when (element) {
                is JsonObject -> {
                    if (element["type"]?.jsonPrimitive?.content == "SpecificEntity") {
                        element["entityId"]?.jsonPrimitive?.content?.let { captured += EntityId(it) }
                    }
                    element.values.forEach(::collect)
                }
                is JsonArray -> element.forEach(::collect)
                else -> Unit
            }
        }
        collect(effectJson.encodeToJsonElement<Effect>(effect))
        val names = captured.map { publicName(state, it, audience) }.distinct()
        return if (names.size == 1) text.replace(SPECIFIC_ENTITY, names.single()) else text
    }

    /** What [audience] may call [entityId]: a player's name, a public card's, or a placeholder. */
    private fun publicName(state: GameState, entityId: EntityId, audience: Audience): String {
        state.getEntity(entityId)?.get<PlayerComponent>()?.let { return it.name }
        val name = state.getEntity(entityId)?.get<CardComponent>()?.name ?: return "a card"
        val hidden = state.zones.any { (key, ids) ->
            key.zoneType in setOf(Zone.HAND, Zone.LIBRARY, Zone.SIDEBOARD) && entityId in ids
        }
        return if (hidden) "a card" else audience.name(state, entityId, name)
    }

    /**
     * The player the badges are rendered for. A face-down object reads as its face-down label
     * unless [Visibility] lets this viewer look under it; anything else reads as [fallback], the
     * name the caller already chose (often one recorded when the effect was created).
     */
    private inner class Audience(private val viewerId: EntityId, private val isSpectator: Boolean) {
        fun name(state: GameState, entityId: EntityId, fallback: String): String =
            if (visibility.isCardIdentityHiddenFrom(state, entityId, viewerId, isSpectator)) {
                faceDownDisplayName(state, entityId) ?: fallback
            } else {
                fallback
            }
    }

    private companion object {
        const val SPECIFIC_ENTITY = "specific entity"
        val effectJson = Json { serializersModule = engineSerializersModule }
    }
}
