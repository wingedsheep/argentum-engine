package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Carnivorous Canopy (ONE #162) — {2}{G} Sorcery.
 *
 * "Destroy target artifact, enchantment, or creature with flying. If that permanent's mana value
 * was 3 or less, proliferate."
 */
class CarnivorousCanopyScenarioTest : ScenarioTestBase() {

    private fun seed(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.PLUS_ONE_PLUS_ONE, amount))
        }
    }

    private fun counters(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun board(flyer: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Carnivorous Canopy")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, flyer)
        .withLandsOnBattlefield(1, "Forest", 3)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun resolveAll(game: TestGame, pick: List<EntityId>): Int {
        var prompts = 0
        var guard = 0
        while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 20) {
            if (game.hasPendingDecision()) { game.selectCards(pick); prompts++ } else game.resolveStack()
        }
        return prompts
    }

    init {
        test("destroying a flyer with mana value 3 or less proliferates") {
            val game = board("Wind Drake")
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, 1)
            val drake = game.findPermanent("Wind Drake")!!

            game.castSpell(1, "Carnivorous Canopy", targetId = drake).error shouldBe null
            val prompts = resolveAll(game, listOf(bears))

            withClue("Wind Drake is destroyed") { game.isInGraveyard(2, "Wind Drake") shouldBe true }
            withClue("one proliferate prompt") { prompts shouldBe 1 }
            withClue("Grizzly Bears got another +1/+1 counter") { counters(game, bears) shouldBe 2 }
        }

        test("destroying a flyer with mana value 4 or more does not proliferate") {
            val game = board("Serra Angel")
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, 1)
            val angel = game.findPermanent("Serra Angel")!!

            game.castSpell(1, "Carnivorous Canopy", targetId = angel).error shouldBe null
            val prompts = resolveAll(game, listOf(bears))

            withClue("Serra Angel is destroyed") { game.isInGraveyard(2, "Serra Angel") shouldBe true }
            withClue("no proliferate prompt") { prompts shouldBe 0 }
            withClue("counters unchanged") { counters(game, bears) shouldBe 1 }
        }

        test("a creature without flying is not a legal target") {
            val game = board("Hill Giant")
            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Carnivorous Canopy", targetId = giant).error shouldNotBe null
        }
    }
}
