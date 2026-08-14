package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MomentaryBlinkScenarioTest : ScenarioTestBase() {
    init {
        test("exiles then returns a creature you control") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardInHand(1, "Momentary Blink")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.getEntity(bears)!!.has<SummoningSicknessComponent>() shouldBe false

            game.castSpell(1, "Momentary Blink", bears).error shouldBe null
            game.resolveStack()

            val blinked = game.findPermanent("Grizzly Bears")
            blinked shouldNotBe null
            withClue("blink re-enters with summoning sickness") {
                game.state.getEntity(blinked!!)!!.has<SummoningSicknessComponent>() shouldBe true
            }
        }

        test("blinking re-triggers the creature's enter-the-battlefield ability") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Cathedral Sanctifier", summoningSickness = false)
                .withCardInHand(1, "Momentary Blink")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val sanctifier = game.findPermanent("Cathedral Sanctifier")!!
            val lifeBefore = game.getLifeTotal(1)

            game.castSpell(1, "Momentary Blink", sanctifier).error shouldBe null
            game.resolveStack()
            if (game.state.stack.isNotEmpty()) game.resolveStack()

            withClue("the returned creature is a new object, so its ETB gains 3 more life") {
                game.getLifeTotal(1) shouldBe lifeBefore + 3
            }
        }

        test("flashback {3}{U} casts it again from the graveyard") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardInGraveyard(1, "Momentary Blink")
                .withLandsOnBattlefield(1, "Island", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val blink = game.findCardsInGraveyard(1, "Momentary Blink").single()
            // Flashback is an alternative cost, so the cast must announce it (CR 601.2b) — the
            // printed {1}{W} is unpayable here, which is what proves {3}{U} was used.
            val cast = game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = blink,
                    targets = listOf(ChosenTarget.Permanent(bears)),
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.FLASHBACK,
                ),
            )
            withClue("flashback {3}{U}: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("the creature blinks back") {
                game.findPermanent("Grizzly Bears") shouldNotBe null
            }
            withClue("a flashed-back card is exiled, not returned to the graveyard") {
                game.isInGraveyard(1, "Momentary Blink") shouldBe false
                game.isInExile(1, "Momentary Blink") shouldBe true
            }
        }
    }
}
