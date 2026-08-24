package com.wingedsheep.engine.event

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.SuppressesWardForGroupComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.filters.unified.Scope

/**
 * The battlefield-wide facts [TriggerAbilityResolver] needs for *every* entity whose triggered
 * abilities it resolves, gathered in one walk instead of one walk per entity.
 *
 * Resolving one entity's abilities used to open four separate `for (id in state.getBattlefield())`
 * loops — one for battlefield-scope [GrantWard] grants, one for ward suppressors (Nowhere to Run),
 * and two for Aura/Equipment grants attached to the entity. Ability resolution itself runs once per
 * battlefield permanent per trigger-detection pass, so those loops made detection O(battlefield²).
 *
 * Every one of them is looking for something the typical board does not contain, so [build] pays
 * one walk and the per-entity cost collapses to a lookup that usually finds nothing.
 *
 * [triggerGrantProviders] is the exception: it is not new work, it is `TriggerDetector`'s existing
 * grant-provider pass folded into the same walk, because it was scanning for a sibling of the same
 * `GrantX` shape over the same filtered entity set.
 */
class BattlefieldStaticsIndex private constructor(
    /**
     * Battlefield-scope [GrantTriggeredAbility] statics — the lord/sliver grants
     * [TriggerIndex.grantProviders] already existed to serve. Collected here because the loop that
     * finds them is character-for-character the one that finds the ward grants, and
     * `TriggerDetector` was walking the battlefield twice to run both.
     */
    val triggerGrantProviders: List<TriggerIndex.GrantProviderEntry>,
    /**
     * Battlefield-scope [GrantWard] statics, with the granter's projected controller (needed to
     * evaluate "you control" / "an opponent controls" predicates) and its id (needed for
     * `excludeSelf`).
     */
    val wardGrantProviders: List<WardGrantProvider>,
    /**
     * Permanents carrying [SuppressesWardForGroupComponent] (Nowhere to Run), with the projected
     * controller their group filter reads as "you".
     */
    val wardSuppressors: List<WardSuppressor>,
    /**
     * Every permanent with an [AttachedToComponent], keyed by what it is attached to. Deliberately
     * unfiltered — consumers apply their own face-down / card-definition checks, exactly as they
     * did while scanning the battlefield themselves.
     */
    val attachmentsByTarget: Map<EntityId, List<EntityId>>,
) {
    data class WardGrantProvider(
        val sourceId: EntityId,
        val grant: GrantWard,
        val sourceControllerId: EntityId,
    )

    data class WardSuppressor(
        val sourceId: EntityId,
        val component: SuppressesWardForGroupComponent,
        val sourceControllerId: EntityId,
    )

    fun attachmentsOn(entityId: EntityId): List<EntityId> =
        attachmentsByTarget[entityId] ?: emptyList()

    companion object {
        val EMPTY = BattlefieldStaticsIndex(emptyList(), emptyList(), emptyList(), emptyMap())

        fun build(state: GameState, cardRegistry: CardRegistry): BattlefieldStaticsIndex {
            // Reused across the whole walk; ConditionEvaluator is stateless.
            val conditionEvaluator = ConditionEvaluator()
            var triggerGrants: MutableList<TriggerIndex.GrantProviderEntry>? = null
            var wardGrants: MutableList<WardGrantProvider>? = null
            var suppressors: MutableList<WardSuppressor>? = null
            var attachments: MutableMap<EntityId, MutableList<EntityId>>? = null

            val projected = state.projectedState

            for (permanentId in state.getBattlefield()) {
                val container = state.getEntity(permanentId) ?: continue

                val attachedTo = container.get<AttachedToComponent>()?.targetId
                if (attachedTo != null) {
                    val map = attachments
                        ?: mutableMapOf<EntityId, MutableList<EntityId>>().also { attachments = it }
                    map.getOrPut(attachedTo) { mutableListOf() }.add(permanentId)
                }

                val suppressComponent = container.get<SuppressesWardForGroupComponent>()
                if (suppressComponent != null) {
                    val controller = projected.getController(permanentId)
                    if (controller != null) {
                        (suppressors ?: mutableListOf<WardSuppressor>().also { suppressors = it })
                            .add(WardSuppressor(permanentId, suppressComponent, controller))
                    }
                }

                // Face-down permanents have no abilities (CR 708.2).
                if (container.has<FaceDownComponent>()) continue
                val card = container.get<CardComponent>() ?: continue
                val sourceControllerId = projected.getController(permanentId) ?: continue
                val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                val classLevel = container.get<ClassLevelComponent>()?.currentLevel
                for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                    when {
                        // Battlefield scope is the lord/sliver grant. SoulbondPair is the same
                        // shape read through a different membership test — "each of those creatures
                        // has '<triggered ability>'" (Tandem Lookout) — and it has to be collected
                        // here too, or the layer system projects the pair while the trigger never
                        // fires. `getStaticGrantedFromProviders` branches on the scope to decide
                        // which test to apply.
                        ability is GrantTriggeredAbility &&
                            (ability.filter.scope is Scope.Battlefield || ability.filter.scope is Scope.SoulbondPair) ->
                            (triggerGrants
                                ?: mutableListOf<TriggerIndex.GrantProviderEntry>().also { triggerGrants = it })
                                .add(TriggerIndex.GrantProviderEntry(ability, sourceControllerId, permanentId))

                        ability is GrantWard && ability.filter.scope is Scope.Battlefield ->
                            (wardGrants ?: mutableListOf<WardGrantProvider>().also { wardGrants = it })
                                .add(WardGrantProvider(permanentId, ability, sourceControllerId))

                        // "As long as <condition>, <group> have ward <cost>" (Thorin Oakenshield's
                        // enduring-story ward). The layer system already projects the WARD keyword
                        // through the ConditionalStaticAbility wrapper, but the *trigger* is built
                        // here from the raw static list — so without this branch the badge appears
                        // and nothing ever triggers, which is the worse of the two failure modes.
                        // Mirrors the AttachedTo-scope unwrap in
                        // `TriggerAbilityResolver.getAttachedWardTriggeredAbilities`, gate and all.
                        ability is ConditionalStaticAbility -> {
                            val grant = ability.ability
                            if (grant is GrantWard && grant.filter.scope is Scope.Battlefield) {
                                val context = EffectContext(
                                    sourceId = permanentId,
                                    controllerId = sourceControllerId,
                                )
                                if (conditionEvaluator.evaluate(state, ability.condition, context)) {
                                    (wardGrants
                                        ?: mutableListOf<WardGrantProvider>().also { wardGrants = it })
                                        .add(WardGrantProvider(permanentId, grant, sourceControllerId))
                                }
                            }
                        }
                    }
                }
            }

            if (triggerGrants == null && wardGrants == null && suppressors == null && attachments == null) {
                return EMPTY
            }

            return BattlefieldStaticsIndex(
                triggerGrantProviders = triggerGrants ?: emptyList(),
                wardGrantProviders = wardGrants ?: emptyList(),
                wardSuppressors = suppressors ?: emptyList(),
                attachmentsByTarget = attachments ?: emptyMap(),
            )
        }
    }
}
