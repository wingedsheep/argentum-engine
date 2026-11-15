package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Dragon Hunter — "Protection from Dragons. This creature can block Dragons as though it had
 * reach." It walls a flying Dragon (and protection prevents the Dragon's damage to it), but a
 * flyer that isn't a Dragon still evades it.
 */
class DragonHunterScenarioTest : ScenarioTestBase() {

    init {
        test("it blocks a flying Dragon and survives it thanks to protection") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Shivan Dragon")
                .withCardOnBattlefield(2, "Dragon Hunter")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Shivan Dragon" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Dragon Hunter" to listOf("Shivan Dragon"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("the blocked Dragon dealt no damage to the defending player") {
                game.getLifeTotal(2) shouldBe 20
            }
            withClue("protection from Dragons prevented the Dragon's combat damage to the Hunter") {
                game.findPermanent("Dragon Hunter") shouldNotBe null
            }
            val dragon = game.findPermanent("Shivan Dragon")!!
            withClue("the Hunter dealt its 2 damage to the Dragon") {
                game.state.getEntity(dragon)?.get<DamageComponent>()?.amount shouldBe 2
            }
        }

        test("it can't block a flyer that isn't a Dragon") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Wind Drake")
                .withCardOnBattlefield(2, "Dragon Hunter")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Wind Drake" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Dragon Hunter" to listOf("Wind Drake"))).error shouldNotBe null
        }
    }
}
