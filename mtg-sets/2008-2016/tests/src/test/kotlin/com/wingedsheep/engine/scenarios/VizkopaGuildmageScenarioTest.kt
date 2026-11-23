package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Vizkopa Guildmage (GTC #206) — the second ability creates a this-turn global trigger: whenever the
 * activator gains life, each opponent loses that much. It stacks per activation (second ruling), reads
 * lifelink gains from the first ability, and is gone on a later turn.
 */
class VizkopaGuildmageScenarioTest : ScenarioTestBase() {
    init {
        val abilities = cardRegistry.getCard("Vizkopa Guildmage")!!.script.activatedAbilities
        val lifelinkId = abilities[0].id
        val drainId = abilities[1].id

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Vizkopa Guildmage")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Plains", 7)
            .withLandsOnBattlefield(1, "Swamp", 7)
            .withCardInHand(1, "Sacred Nectar")
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(1, "Hill Giant")
            .withCardInLibrary(2, "Grizzly Bears")
            .withCardInLibrary(2, "Hill Giant")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("after activating, gaining life makes each opponent lose that much") {
            val game = board()
            val mage = game.findPermanent("Vizkopa Guildmage")!!

            val drain = game.execute(ActivateAbility(game.player1Id, mage, drainId))
            withClue("drain activation: ${drain.error}") { drain.error shouldBe null }
            game.resolveStack()

            val nectar = game.castSpell(1, "Sacred Nectar")
            withClue("Sacred Nectar: ${nectar.error}") { nectar.error shouldBe null }
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 24
            withClue("opponent loses the 4 life gained") { game.getLifeTotal(2) shouldBe 16 }
        }

        test("two activations stack: each gain drains twice") {
            val game = board()
            val mage = game.findPermanent("Vizkopa Guildmage")!!

            repeat(2) {
                val drain = game.execute(ActivateAbility(game.player1Id, mage, drainId))
                withClue("drain activation: ${drain.error}") { drain.error shouldBe null }
                game.resolveStack()
            }
            game.castSpell(1, "Sacred Nectar").error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 24
            game.getLifeTotal(2) shouldBe 12
        }

        test("lifelink from the first ability feeds the drain") {
            val game = board()
            val mage = game.findPermanent("Vizkopa Guildmage")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            val grant = game.execute(
                ActivateAbility(game.player1Id, mage, lifelinkId, targets = listOf(ChosenTarget.Permanent(bears)))
            )
            withClue("lifelink activation: ${grant.error}") { grant.error shouldBe null }
            game.resolveStack()
            val drain = game.execute(ActivateAbility(game.player1Id, mage, drainId))
            withClue("drain activation: ${drain.error}") { drain.error shouldBe null }
            game.resolveStack()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.resolveStack()

            withClue("2 lifelink life gained") { game.getLifeTotal(1) shouldBe 22 }
            withClue("2 combat damage + 2 drained") { game.getLifeTotal(2) shouldBe 16 }
        }

        test("the drain ends with the turn") {
            val game = board()
            val mage = game.findPermanent("Vizkopa Guildmage")!!

            val drain = game.execute(ActivateAbility(game.player1Id, mage, drainId))
            withClue("drain activation: ${drain.error}") { drain.error shouldBe null }
            game.resolveStack()

            // Through the opponent's turn and back to our next main phase.
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id
            game.state.globalGrantedTriggeredAbilities.size shouldBe 0

            game.castSpell(1, "Sacred Nectar").error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 24
            withClue("no drain on a later turn") { game.getLifeTotal(2) shouldBe 20 }
        }
    }
}
