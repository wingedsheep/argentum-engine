package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Meren of Clan Nel Toth — experience counters from other creatures you control dying; at your
 * end step the target creature card returns to the battlefield if its mana value is at most your
 * experience count, otherwise to your hand. The comparison is made as the ability resolves.
 */
class MerenOfClanNelTothScenarioTest : ScenarioTestBase() {

    private fun experience(game: TestGame): Int =
        game.state.getEntity(game.player1Id)?.get<CountersComponent>()?.getCount(CounterType.EXPERIENCE) ?: 0

    private fun board(): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Meren of Clan Nel Toth")
            .withCardOnBattlefield(1, "Raging Goblin")
            .withCardOnBattlefield(2, "Elite Vanguard")
            .withCardInHand(1, "Shock")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withCardInGraveyard(1, "Savannah Lions") // MV 1
            .withCardInGraveyard(1, "Grizzly Bears") // MV 2
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(3) { builder = builder.withCardInLibrary(1, "Mountain") }
        repeat(3) { builder = builder.withCardInLibrary(2, "Plains") }
        return builder.build()
    }

    init {
        context("Meren of Clan Nel Toth") {
            test("only another creature you control dying gives an experience counter") {
                val game = board()
                game.castSpell(1, "Shock", game.findPermanent("Elite Vanguard")!!).error shouldBe null
                game.resolveStack()
                withClue("An opponent's creature dying gives nothing") { experience(game) shouldBe 0 }

                game.castSpell(1, "Lightning Bolt", game.findPermanent("Raging Goblin")!!).error shouldBe null
                game.resolveStack()
                withClue("Your own creature dying gives one experience counter") { experience(game) shouldBe 1 }
            }

            test("a card with mana value at most your experience returns to the battlefield") {
                val game = board()
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Raging Goblin")!!).error shouldBe null
                game.resolveStack()
                experience(game) shouldBe 1

                val lions = game.findCardsInGraveyard(1, "Savannah Lions").single()
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(lions)).error shouldBe null
                game.resolveStack()

                withClue("Savannah Lions (MV 1) with 1 experience returns to the battlefield") {
                    game.findPermanent("Savannah Lions") shouldBe lions
                    game.isInHand(1, "Savannah Lions") shouldBe false
                }
            }

            test("a card with mana value above your experience goes to your hand instead") {
                val game = board()
                game.castSpell(1, "Lightning Bolt", game.findPermanent("Raging Goblin")!!).error shouldBe null
                game.resolveStack()
                experience(game) shouldBe 1

                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears (MV 2) with 1 experience goes to hand") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
                game.findPermanent("Meren of Clan Nel Toth").shouldNotBeNull()
            }
        }
    }
}
