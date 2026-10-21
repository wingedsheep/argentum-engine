package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Homestead Courage (MID #24) — "Put a +1/+1 counter on target creature you control. It gains
 * vigilance until end of turn. Flashback {W}"
 */
class HomesteadCourageScenarioTest : ScenarioTestBase() {

    init {
        context("Homestead Courage") {

            test("puts a +1/+1 counter on your creature and grants vigilance until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Homestead Courage")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Homestead Courage", bears).error shouldBe null
                game.resolveStack()

                game.state.getEntity(bears)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
                game.state.projectedState.getPower(bears) shouldBe 3
                withClue("vigilance is granted until end of turn") {
                    game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
                }
            }

            test("can't target an opponent's creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Homestead Courage")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Homestead Courage", game.findPermanent("Grizzly Bears")).error shouldNotBe null
            }

            test("flashback for {W} from the graveyard, then the card is exiled") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Homestead Courage")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpellFromGraveyard(1, "Homestead Courage", bears).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(bears) shouldBe 3
                game.isInExile(1, "Homestead Courage") shouldBe true
            }
        }
    }
}
