package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Duskmantle Guildmage (GTC #158) — the first ability creates a this-turn global trigger: each card
 * put into an opponent's graveyard makes *that* player lose 1 life, once per card, and once per
 * activation (second ruling). Cards going to the activator's own graveyard don't count.
 */
class DuskmantleGuildmageScenarioTest : ScenarioTestBase() {
    init {
        val abilities = cardRegistry.getCard("Duskmantle Guildmage")!!.script.activatedAbilities
        val drainId = abilities[0].id
        val millId = abilities[1].id

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Duskmantle Guildmage")
            .withLandsOnBattlefield(1, "Island", 7)
            .withLandsOnBattlefield(1, "Swamp", 7)
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(1, "Hill Giant")
            .withCardInLibrary(1, "Elvish Warrior")
            .withCardInLibrary(2, "Grizzly Bears")
            .withCardInLibrary(2, "Hill Giant")
            .withCardInLibrary(2, "Elvish Warrior")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("each card milled into an opponent's graveyard costs that player 1 life; own mills don't") {
            val game = board()
            val mage = game.findPermanent("Duskmantle Guildmage")!!

            val drain = game.execute(ActivateAbility(game.player1Id, mage, drainId))
            withClue("drain activation: ${drain.error}") { drain.error shouldBe null }
            game.resolveStack()

            val millSelf = game.execute(
                ActivateAbility(game.player1Id, mage, millId, targets = listOf(ChosenTarget.Player(game.player1Id)))
            )
            withClue("self mill activation: ${millSelf.error}") { millSelf.error shouldBe null }
            game.resolveStack()
            game.graveyardSize(1) shouldBe 2
            withClue("cards into your own graveyard don't trigger it") { game.getLifeTotal(1) shouldBe 20 }
            game.getLifeTotal(2) shouldBe 20

            val millOpp = game.execute(
                ActivateAbility(game.player1Id, mage, millId, targets = listOf(ChosenTarget.Player(game.player2Id)))
            )
            withClue("opponent mill activation: ${millOpp.error}") { millOpp.error shouldBe null }
            game.resolveStack()
            game.graveyardSize(2) shouldBe 2
            withClue("one life per card milled") { game.getLifeTotal(2) shouldBe 18 }
        }

        test("two activations stack: each card costs the opponent 2 life") {
            val game = board()
            val mage = game.findPermanent("Duskmantle Guildmage")!!

            repeat(2) {
                val drain = game.execute(ActivateAbility(game.player1Id, mage, drainId))
                withClue("drain activation: ${drain.error}") { drain.error shouldBe null }
                game.resolveStack()
            }
            val millOpp = game.execute(
                ActivateAbility(game.player1Id, mage, millId, targets = listOf(ChosenTarget.Player(game.player2Id)))
            )
            withClue("opponent mill activation: ${millOpp.error}") { millOpp.error shouldBe null }
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 16
        }
    }
}
