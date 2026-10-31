package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Labyrinth Raptor — "Whenever a creature you control with menace becomes blocked, defending player
 * sacrifices a creature of their choice that's blocking it." The defending player chooses, and only
 * among the creatures blocking *that* attacker — not blockers of another attacker.
 */
class LabyrinthRaptorScenarioTest : ScenarioTestBase() {

    init {
        test("defending player sacrifices one of the creatures blocking the menace attacker, of their choice") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Labyrinth Raptor")
                .withCardOnBattlefield(1, "Centaur Courser")    // unrelated attacker without menace
                .withCardOnBattlefield(2, "Hill Giant")         // blocks the Raptor
                .withCardOnBattlefield(2, "Craw Wurm")          // blocks the Raptor
                .withCardOnBattlefield(2, "Grizzly Bears")      // blocks the Courser
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Labyrinth Raptor" to 2, "Centaur Courser" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(
                mapOf(
                    "Hill Giant" to listOf("Labyrinth Raptor"),
                    "Craw Wurm" to listOf("Labyrinth Raptor"),
                    "Grizzly Bears" to listOf("Centaur Courser"),
                )
            )
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the defending player makes the choice") {
                decision.playerId shouldBe game.player2Id
            }
            withClue("only the Raptor's own blockers are offered") {
                decision.options shouldContainExactlyInAnyOrder listOf(
                    game.findPermanent("Hill Giant")!!,
                    game.findPermanent("Craw Wurm")!!,
                )
            }

            game.selectCards(listOf(game.findPermanent("Hill Giant")!!))

            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.isOnBattlefield("Craw Wurm") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Labyrinth Raptor") shouldBe true
        }

        test("a creature without menace becoming blocked does not trigger it") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Labyrinth Raptor")
                .withCardOnBattlefield(1, "Centaur Courser")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Centaur Courser" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Centaur Courser")))

            game.state.stack.isEmpty() shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
