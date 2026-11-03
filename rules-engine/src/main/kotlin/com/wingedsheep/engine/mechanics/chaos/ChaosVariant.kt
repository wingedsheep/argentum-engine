package com.wingedsheep.engine.mechanics.chaos

import com.wingedsheep.engine.handlers.effects.player.EMBLEM_DEFINITION_PREFIX
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.EmblemSourceComponent
import com.wingedsheep.sdk.chaos.ChaosRules
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.EntityId

/**
 * Sets a game up for the **Chaos** house variant ([ChaosRules]): each player gets a Chaos emblem
 * whose ability — [ChaosRules.upkeepRoll], "At the beginning of your upkeep, roll a random chaos
 * rule" — rolls and plays a rule every upkeep.
 *
 * The emblem is built like the emblems
 * [com.wingedsheep.engine.handlers.effects.player.CreateGlobalTriggeredAbilityExecutor] mints for
 * "you get an emblem with '…'": a zone-less object the player owns and controls, with no types,
 * colors or mana cost (CR 114.3). Its one ability is a game-rule constant rather than state, the way
 * the monarch's and the initiative's inherent triggers are: [com.wingedsheep.engine.event.TriggerDetector]
 * puts it on the stack at each emblem controller's upkeep while [GameState.chaosRules] is set. A Chaos
 * game's saved state therefore carries a flag and two small entities, not two copies of every rule.
 * Everything after that is ordinary engine behaviour:
 *
 * - the rule is drawn from the game's seeded RNG as the trigger goes on the stack
 *   ([com.wingedsheep.sdk.scripting.effects.ModalEffect.chooseAtRandom]), so a game replays from
 *   its seed;
 * - each emblem is the object the "choose one that hasn't been chosen" memory is keyed to, so a
 *   player never rolls the same rule twice in a game;
 * - damage a rule deals comes from the emblem — a colorless source named Chaos.
 *
 * One emblem per player rather than one shared object keeps the rolled rule's "you" and every choice
 * it asks for with the player whose upkeep it is, which is how the table plays it.
 */
object ChaosVariant {

    /** The definition id stamped on every Chaos emblem — how the engine finds them. */
    const val EMBLEM_DEFINITION_ID: String = "$EMBLEM_DEFINITION_PREFIX${ChaosRules.SOURCE_NAME}"

    /**
     * Mark [state] as a Chaos game and give each of [players] a Chaos emblem. Draws no randomness and
     * moves no cards, so it can run at any point of setup.
     */
    fun install(state: GameState, players: List<EntityId>): GameState {
        var next = state.copy(chaosRules = true)
        for (playerId in players) {
            val (emblemId, withId) = next.newEntity()
            val emblem = ComponentContainer.EMPTY
                .with(ControllerComponent(playerId))
                .with(EmblemSourceComponent(sourceName = ChaosRules.SOURCE_NAME, description = ChaosRules.EMBLEM_TEXT))
                .with(
                    CardComponent(
                        cardDefinitionId = EMBLEM_DEFINITION_ID,
                        name = "${ChaosRules.SOURCE_NAME} emblem",
                        manaCost = ManaCost.ZERO,
                        typeLine = TypeLine(cardTypes = emptySet()),
                        oracleText = ChaosRules.EMBLEM_TEXT,
                        // The player who gets an emblem owns it (CR 114.2), so it leaves with them.
                        ownerId = playerId,
                    )
                )
            next = withId.withEntity(emblemId, emblem)
        }
        return next
    }

    /**
     * Each player's Chaos emblem, keyed by the player who controls it. Empty unless this is a Chaos
     * game. Read at upkeep only, so a scan of the entity map is cheap enough.
     */
    fun emblems(state: GameState): Map<EntityId, EntityId> {
        if (!state.chaosRules) return emptyMap()
        val result = LinkedHashMap<EntityId, EntityId>()
        for ((id, container) in state.entities) {
            if (container.get<CardComponent>()?.cardDefinitionId != EMBLEM_DEFINITION_ID) continue
            val controller = container.get<ControllerComponent>()?.playerId ?: continue
            result.putIfAbsent(controller, id)
        }
        return result
    }

    /** The Chaos emblem [playerId] has, if this is a Chaos game. */
    fun emblemOf(state: GameState, playerId: EntityId): EntityId? = emblems(state)[playerId]
}
