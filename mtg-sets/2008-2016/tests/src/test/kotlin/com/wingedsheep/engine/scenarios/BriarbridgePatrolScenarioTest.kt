package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Briarbridge Patrol (SOI #195) — {3}{G} Creature — Human Warrior 3/3.
 *
 *   Whenever this creature deals damage to one or more creatures, investigate.
 *   At the beginning of each end step, if you sacrificed three or more Clues this turn, you may put
 *   a creature card from your hand onto the battlefield.
 */
class BriarbridgePatrolScenarioTest : ScenarioTestBase() {

    private val clueAbilityId get() = cardRegistry.getCard("Clue")!!.activatedAbilities.single().id

    private fun TestGame.crackClues(count: Int) {
        val clues = findPermanents("Clue").take(count)
        clues shouldHaveSize count
        for (clue in clues) {
            execute(ActivateAbility(playerId = player1Id, sourceId = clue, abilityId = clueAbilityId)).error shouldBe null
            resolveStack()
        }
    }

    private fun clueBoard(clues: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Briarbridge Patrol")
        .apply { repeat(clues) { withCardOnBattlefield(1, "Clue", isToken = true) } }
        .withLandsOnBattlefield(1, "Forest", 2 * clues)
        .withCardInHand(1, "Grizzly Bears")
        .apply { repeat(clues + 2) { withCardInLibrary(1, "Forest") } }
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("blocked by two creatures, it investigates once") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Briarbridge Patrol", summoningSickness = false)
                .withCardOnBattlefield(2, "Llanowar Elves")
                .withCardOnBattlefield(2, "Llanowar Elves")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Briarbridge Patrol" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            val patrol = game.findPermanent("Briarbridge Patrol")!!
            val elves = game.findPermanents("Llanowar Elves")
            game.execute(DeclareBlockers(game.player2Id, elves.associateWith { listOf(patrol) })).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
            if (game.state.pendingDecision != null) game.submitDefaultCombatDamage()
            game.resolveStack()

            withClue("both Elves died, and the Patrol investigated once") {
                game.findPermanents("Llanowar Elves") shouldHaveSize 0
                game.findPermanents("Clue") shouldHaveSize 1
            }
        }

        test("unblocked, it deals damage only to a player and doesn't investigate") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Briarbridge Patrol", summoningSickness = false)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Briarbridge Patrol" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(2) shouldBe 17
            game.findPermanents("Clue") shouldHaveSize 0
        }

        test("after three Clues were sacrificed this turn, the end step puts a creature card onto the battlefield") {
            val game = clueBoard(clues = 3)
            game.crackClues(3)
            game.findPermanents("Clue") shouldHaveSize 0

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.state.stack.size shouldBe 1
            game.resolveStack()
            val bears = game.findCardsInHand(1, "Grizzly Bears").single()
            game.selectCards(listOf(bears))
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("the put is optional") {
            val game = clueBoard(clues = 3)
            game.crackClues(3)

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.selectCards(emptyList())
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.findCardsInHand(1, "Grizzly Bears") shouldHaveSize 1
        }

        test("it checks each end step: Clues cracked on the opponent's turn pay off in their end step") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Briarbridge Patrol")
                .apply { repeat(3) { withCardOnBattlefield(1, "Clue", isToken = true) } }
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardInHand(1, "Grizzly Bears")
                .apply { repeat(5) { withCardInLibrary(1, "Forest") } }
                .apply { repeat(3) { withCardInLibrary(2, "Forest") } }
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            for (clue in game.findPermanents("Clue")) {
                game.passPriority() // the active player passes; the Patrol's controller may act
                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = clue, abilityId = clueAbilityId)).error shouldBe null
                game.resolveStack()
            }
            game.findPermanents("Clue") shouldHaveSize 0

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.state.activePlayerId shouldBe game.player2Id
            game.state.stack.size shouldBe 1
            game.resolveStack()
            game.selectCards(game.findCardsInHand(1, "Grizzly Bears"))
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("two Clues aren't enough — the ability doesn't trigger") {
            val game = clueBoard(clues = 2)
            game.crackClues(2)

            game.passUntilPhase(Phase.ENDING, Step.END)

            game.state.stack.size shouldBe 0
            game.isOnBattlefield("Grizzly Bears") shouldBe false
        }
    }
}
