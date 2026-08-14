package com.wingedsheep.ai.insight

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId

/**
 * Names for the things an [AiActionOption] talks about.
 *
 * Read off the state the decision was made from, at capture time: an entity id means nothing to a
 * reader, and by the time anyone opens the panel the permanent it referred to may be gone.
 */
object AiInsightLabels {

    /** A card, permanent or player name for [id], falling back to the raw id. */
    fun nameOf(state: GameState, id: EntityId): String {
        val entity = state.getEntity(id) ?: return id.value
        entity.get<CardComponent>()?.name?.let { return it }
        entity.get<PlayerComponent>()?.name?.let { return it }
        return id.value
    }

    /** Names of the targets [action] commits to, in submission order. */
    fun targetNames(state: GameState, action: GameAction): List<String> {
        val targets = when (action) {
            is CastSpell -> action.targets
            is ActivateAbility -> action.targets
            else -> return emptyList()
        }
        return targets.map { target ->
            when (target) {
                is ChosenTarget.Player -> nameOf(state, target.playerId)
                is ChosenTarget.Permanent -> nameOf(state, target.entityId)
                is ChosenTarget.Card -> nameOf(state, target.cardId)
                is ChosenTarget.Spell -> nameOf(state, target.spellEntityId)
            }
        }
    }

    /** The card [action] is cast from / activated from / played, if any. */
    fun cardName(state: GameState, action: GameAction): String? {
        val id = when (action) {
            is CastSpell -> action.cardId
            is ActivateAbility -> action.sourceId
            is PlayLand -> action.cardId
            else -> return null
        }
        return state.getEntity(id)?.get<CardComponent>()?.name
    }

    /**
     * The enumerator's description, plus the chosen targets and X.
     *
     * The description alone is what the *player* would see on a button ("Cast Lightning Bolt"), and
     * two candidates for the same card differ only in what the AI aimed it at — which is exactly the
     * thing worth reading here.
     */
    fun describe(state: GameState, legalAction: LegalAction, action: GameAction): String {
        val x = when (action) {
            is CastSpell -> action.xValue
            is ActivateAbility -> action.xValue
            else -> null
        }
        val base = if (x != null && legalAction.hasXCost) "${legalAction.description} (X=$x)" else legalAction.description
        val targets = targetNames(state, action)
        return if (targets.isEmpty()) base else "$base → ${targets.joinToString(", ")}"
    }

    /** `Grizzly Bears, Llanowar Elves → Opponent`, or "No attacks" for an empty plan. */
    fun describeAttackPlan(state: GameState, attackers: Map<EntityId, EntityId>): String {
        if (attackers.isEmpty()) return "No attacks"
        return attackers.entries
            .groupBy({ it.value }, { it.key })
            .entries
            .joinToString("; ") { (defender, ids) ->
                "${ids.joinToString(", ") { nameOf(state, it) }} → ${nameOf(state, defender)}"
            }
    }

    /** `Grizzly Bears blocks Shivan Dragon`, or "No blocks" for an empty plan. */
    fun describeBlockPlan(state: GameState, blockers: Map<EntityId, List<EntityId>>): String {
        val real = blockers.filterValues { it.isNotEmpty() }
        if (real.isEmpty()) return "No blocks"
        return real.entries.joinToString("; ") { (blocker, attackers) ->
            "${nameOf(state, blocker)} blocks ${attackers.joinToString(", ") { nameOf(state, it) }}"
        }
    }
}
