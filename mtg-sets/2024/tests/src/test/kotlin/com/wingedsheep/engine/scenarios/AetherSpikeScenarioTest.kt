package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Aether Spike (MH3) — "Choose target spell. You get {E}{E}, then you may pay any amount of {E}.
 * Counter that spell unless its controller pays {1} for each {E} paid this way."
 */
class AetherSpikeScenarioTest : ScenarioTestBase() {

    private fun energy(game: TestGame, playerId: EntityId): Int =
        game.state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    init {
        context("Aether Spike") {

            fun board(theirForests: Int = 2) = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Aether Spike")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInHand(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", theirForests)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.castBearsThenSpike() {
                castSpell(2, "Grizzly Bears").error shouldBe null
                passPriority()
                castSpellTargetingStackSpell(1, "Aether Spike", "Grizzly Bears").error shouldBe null
                resolveStack()
            }

            test("paying both energy taxes {2}; declining counters the spell") {
                val game = board(theirForests = 4)
                game.castBearsThenSpike()

                val choose = game.getPendingDecision().shouldNotBeNull().shouldBeInstanceOf<ChooseNumberDecision>()
                choose.playerId shouldBe game.player1Id
                choose.minValue shouldBe 0
                choose.maxValue shouldBe 2
                game.chooseNumber(2).error shouldBe null

                val offer = game.getPendingDecision().shouldNotBeNull().shouldBeInstanceOf<YesNoDecision>()
                offer.playerId shouldBe game.player2Id
                offer.prompt shouldContain "{2}"
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("declined tax counters the Bears; all energy spent") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    energy(game, game.player1Id) shouldBe 0
                }
            }

            test("paying the tax saves the spell") {
                val game = board(theirForests = 3)
                game.castBearsThenSpike()
                game.chooseNumber(1).error shouldBe null

                val offer = game.getPendingDecision().shouldNotBeNull().shouldBeInstanceOf<YesNoDecision>()
                offer.prompt shouldContain "{1}"
                game.answerYesNo(true).error shouldBe null
                game.submitManaSourcesAutoPay().error shouldBe null
                game.resolveStack()

                withClue("Bears resolve; one energy remains") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    energy(game, game.player1Id) shouldBe 1
                }
            }

            test("can't pay the tax — the spell is countered") {
                val game = board(theirForests = 2)
                game.castBearsThenSpike()
                game.chooseNumber(1).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                energy(game, game.player1Id) shouldBe 1
            }

            test("paying zero energy keeps both counters and the spell resolves") {
                val game = board()
                game.castBearsThenSpike()
                game.chooseNumber(0).error shouldBe null

                withClue("a {0} tax makes no offer") {
                    game.getPendingDecision().shouldBeNull()
                }
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe true
                energy(game, game.player1Id) shouldBe 2
            }
        }
    }
}
