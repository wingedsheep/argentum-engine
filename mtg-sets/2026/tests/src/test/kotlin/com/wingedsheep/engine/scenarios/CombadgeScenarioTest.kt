package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Combadge (TRK #260) — {3} Artifact — Communicator Equipment.
 *
 * Equipped creature has "Whenever this creature attacks while you control another creature with a
 * Communicator attached to it, this creature can't be blocked this turn."
 *
 * The "another creature" clause is what a reader can't confirm from the script: the restriction
 * looks for a Communicator attached to a creature you control *other than the attacker*, so a
 * second Combadge on the attacker itself doesn't switch the evasion on.
 */
class CombadgeScenarioTest : ScenarioTestBase() {

    private fun TestGame.attach(equipmentId: EntityId, hostId: EntityId) {
        state = state.updateEntity(equipmentId) { it.with(AttachedToComponent(hostId)) }
        state = state.updateEntity(hostId) { container ->
            val existing = container.get<AttachmentsComponent>()?.attachedIds ?: emptyList()
            container.with(AttachmentsComponent(existing + equipmentId))
        }
    }

    /**
     * Hill Giant and Centaur Courser for player 1, a Grizzly Bears to block for player 2, and two
     * Combadges placed by [placeBadges] (attacker host, other host). Hill Giant attacks.
     */
    private fun attackWithGiant(placeBadges: TestGame.(giant: EntityId, courser: EntityId, badges: List<EntityId>) -> Unit): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
            .withCardOnBattlefield(1, "Centaur Courser", summoningSickness = false)
            .withCardOnBattlefield(1, "Combadge")
            .withCardOnBattlefield(1, "Combadge")
            .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        val giant = game.findPermanent("Hill Giant")!!
        val courser = game.findPermanent("Centaur Courser")!!
        val badges = game.findAllPermanents("Combadge")
        badges.size shouldBe 2
        game.placeBadges(giant, courser, badges)

        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        return game
    }

    init {
        context("Combadge's granted attack trigger") {

            test("with another creature wearing a Communicator, the attacker can't be blocked") {
                val game = attackWithGiant { giant, courser, badges ->
                    attach(badges[0], giant)
                    attach(badges[1], courser)
                }

                withClue("Grizzly Bears must not be able to block the Combadge-equipped attacker") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldNotBe null
                }
            }

            test("with no other Communicator-equipped creature, the attacker can be blocked") {
                val game = attackWithGiant { giant, _, badges ->
                    attach(badges[0], giant)
                }

                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
            }

            test("a second Combadge on the attacker itself is not 'another creature'") {
                val game = attackWithGiant { giant, _, badges ->
                    attach(badges[0], giant)
                    attach(badges[1], giant)
                }

                withClue("both Communicators sit on the attacker, so no other creature has one") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
                }
            }
        }
    }
}
