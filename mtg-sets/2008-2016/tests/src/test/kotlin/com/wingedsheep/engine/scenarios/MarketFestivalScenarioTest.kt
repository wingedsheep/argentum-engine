package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Market Festival: "Whenever enchanted land is tapped for mana, its controller adds an additional
 * two mana in any combination of colors."
 */
class MarketFestivalScenarioTest : ScenarioTestBase() {

    init {
        test("tapping the enchanted Forest adds {G} plus two bonus mana of separately chosen colors") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Forest")
                .withCardOnBattlefield(1, "Market Festival")
                .withCardAttachedTo(1, "Market Festival", "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val forest = game.findPermanent("Forest")!!
            val ability = cardRegistry.requireCard("Forest").activatedAbilities.first { it.isManaAbility }
            game.execute(ActivateAbility(game.player1Id, forest, ability.id)).error shouldBe null

            for (color in listOf(Color.BLACK, Color.RED)) {
                val decision = game.getPendingDecision() as? ChooseColorDecision
                withClue("Market Festival asks for each bonus mana's color") { decision shouldNotBe null }
                game.submitDecision(ColorChosenResponse(decision!!.id, color)).error shouldBe null
            }

            val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()!!
            withClue("pool was $pool") {
                pool.green shouldBe 1
                pool.black shouldBe 1
                pool.red shouldBe 1
                pool.total shouldBe 3
            }
        }

        test("auto-pay reads one enchanted land as three mana: {G} plus any two colors") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Forest")
                .withCardOnBattlefield(1, "Market Festival")
                .withCardAttachedTo(1, "Market Festival", "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val solver = ManaSolver(cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
            solver.canPay(game.state, game.player1Id, ManaCost.parse("{W}{U}{G}")) shouldBe true
            solver.canPay(game.state, game.player1Id, ManaCost.parse("{W}{U}{B}")) shouldBe false
            solver.canPay(game.state, game.player1Id, ManaCost.parse("{3}{G}")) shouldBe false
        }
    }
}
