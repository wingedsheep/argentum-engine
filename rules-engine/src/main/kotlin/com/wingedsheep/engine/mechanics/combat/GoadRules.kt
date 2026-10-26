package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.GoadedComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Goaded
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.MustAttackDefender

/**
 * Who goads each creature (CR 701.15). Two sources feed the one set:
 *  - the [GoadedComponent] designation a resolved goad effect leaves, which expires on its own;
 *  - a [Goaded] static — printed on a battlefield permanent (Martial Impetus on the creature it
 *    enchants), whose goader is that permanent's projected controller, or granted by a resolving
 *    effect, whose goader is the grant's controller. A face-down or ability-stripped host
 *    contributes nothing.
 *
 * It also answers the wider question goad's second requirement belongs to — whom must this
 * creature attack someone *other than*, if able ([playersToAttackOtherThan]) — since
 * [MustAttackDefender.PlayerOtherThanYou] (Kardur, Doomscourge) asks it without goading.
 *
 * Read after projection, like [BlockStaticRules]: build once per declaration, ask per creature.
 */
class GoadRules(
    private val state: GameState,
    registry: CardRegistry,
    private val predicates: PredicateEvaluator,
) {
    private val entries = CombatStaticScan.collect(state, registry, predicates) { it as? Goaded }
    private val otherThanYouEntries = CombatStaticScan.collect(state, registry, predicates) { ability ->
        (ability as? MustAttack)?.takeIf { it.defender == MustAttackDefender.PlayerOtherThanYou }
    }

    /** Every player goading [creatureId]; empty when it isn't goaded. */
    fun goadersOf(creatureId: EntityId): Set<EntityId> {
        val designated = state.getEntity(creatureId)?.get<GoadedComponent>()?.goaderIds.orEmpty()
        if (entries.isEmpty()) return designated
        val fromStatics = entries.filter {
            CombatStaticScan.covers(state, predicates, it.holder, it.controller, it.ability.filter, creatureId)
        }.map { it.controller }
        return if (fromStatics.isEmpty()) designated else designated + fromStatics
    }

    fun isGoaded(creatureId: EntityId): Boolean = goadersOf(creatureId).isNotEmpty()

    /**
     * One entry per "attacks a player other than X if able" requirement on [creatureId], naming X:
     * each goader (CR 701.15b–c), the controller of each [MustAttackDefender.PlayerOtherThanYou]
     * static covering it, and each player a resolved
     * [com.wingedsheep.sdk.scripting.effects.MustAttackGroupEffect] named. A player named by two
     * requirements appears twice, so a declaration can be judged by how many it breaks.
     */
    fun playersToAttackOtherThan(creatureId: EntityId): List<EntityId> {
        val fromStatics = otherThanYouEntries.filter {
            CombatStaticScan.covers(state, predicates, it.holder, it.controller, it.ability.filter, creatureId)
        }.map { it.controller }
        return goadersOf(creatureId).toList() + fromStatics + state.projectedState.mustAttackOtherThan(creatureId)
    }
}
