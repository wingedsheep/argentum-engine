package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Wall of Shadows (LEG #128) — {1}{B}{B} Creature — Wall 0/1.
 *
 * "Defender. Prevent all damage that would be dealt to this creature by creatures it's blocking.
 *  This creature can't be the target of spells that can target only Walls or of abilities that
 *  can target only Walls."
 *
 * The prevention reads live combat state (`StatePredicate.IsBlockedBySource`): the attacker it
 * blocks can't hurt it, but any other creature — even mid-combat — still can.
 */
class WallOfShadowsScenarioTest : ScenarioTestBase() {

    init {
        context("Wall of Shadows") {

            fun builder() = scenario()
                .withPlayers("Wall Player", "Attacker")
                .withCardOnBattlefield(1, "Wall of Shadows")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Prodigal Sorcerer")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

            test("damage from the creature it's blocking is prevented") {
                val game = builder().build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Wall of Shadows" to listOf("Hill Giant"))).outcome shouldBe Outcome.Done
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                val wall = game.findPermanent("Wall of Shadows")
                withClue("Hill Giant's 3 combat damage to the 0/1 Wall was prevented") {
                    wall.shouldNotBeNull()
                    (game.state.getEntity(wall)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
                }
            }

            test("a creature it isn't blocking still damages it, even during that combat") {
                val game = builder().build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Wall of Shadows" to listOf("Hill Giant"))).outcome shouldBe Outcome.Done

                val wall = game.findPermanent("Wall of Shadows")!!
                val sorcerer = game.findPermanent("Prodigal Sorcerer")!!
                val abilityId = cardRegistry.getCard("Prodigal Sorcerer")!!.script.activatedAbilities[0].id
                game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = sorcerer,
                        abilityId = abilityId,
                        targets = listOf(entityIdToChosenTarget(game.state, wall))
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("Prodigal Sorcerer isn't blocked by the Wall, so its 1 damage kills the 0/1") {
                    game.isOnBattlefield("Wall of Shadows") shouldBe false
                }
            }
        }
    }
}
