package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Graverobber Spider (BNG #122) — "{3}{B}: This creature gets +X/+X until end of turn, where X is
 * the number of creature cards in your graveyard. Activate only once each turn."
 */
class GraverobberSpiderScenarioTest : ScenarioTestBase() {
    init {
        val pumpId = cardRegistry.getCard("Graverobber Spider")!!.script.activatedAbilities.single().id

        test("pumps by the creature cards in your graveyard, once per turn") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Graverobber Spider")
                .withLandsOnBattlefield(1, "Swamp", 8)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInGraveyard(2, "Savannah Lions")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val spider = game.findPermanent("Graverobber Spider")!!

            val result = game.execute(ActivateAbility(game.player1Id, spider, pumpId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            withClue("X = 2: only creature cards in your own graveyard count") {
                game.state.projectedState.getPower(spider) shouldBe 4
                game.state.projectedState.getToughness(spider) shouldBe 6
            }

            withClue("a second activation this turn is refused") {
                game.execute(ActivateAbility(game.player1Id, spider, pumpId)).error shouldNotBe null
            }
        }
    }
}
