package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ogre Slumlord (GTC #74) — "Whenever another nontoken creature dies, you may create a 1/1 black
 * Rat creature token. Rats you control have deathtouch."
 */
class OgreSlumlordScenarioTest : ScenarioTestBase() {
    init {
        test("an opponent's nontoken creature dying offers a Rat, and Rats you control have deathtouch") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Ogre Slumlord")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val shock = game.findCardsInHand(1, "Shock").single()
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = shock,
                    targets = listOf(entityIdToChosenTarget(game.state, bears)),
                ),
            ).error shouldBe null
            game.resolveStack()

            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            val rats = game.findAllPermanents("Rat Token")
            rats.size shouldBe 1
            val rat = rats.single()
            withClue("the Rat is a 1/1 with deathtouch from the Slumlord") {
                game.state.projectedState.getPower(rat) shouldBe 1
                game.state.projectedState.getToughness(rat) shouldBe 1
                game.state.projectedState.hasKeyword(rat, Keyword.DEATHTOUCH) shouldBe true
            }
            val slumlord = game.findPermanent("Ogre Slumlord")!!
            withClue("the Ogre itself isn't a Rat") {
                game.state.projectedState.hasKeyword(slumlord, Keyword.DEATHTOUCH) shouldBe false
            }
        }

        test("a token creature dying does not trigger it") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Ogre Slumlord")
                .withCardOnBattlefield(2, "Grizzly Bears", isToken = true)
                .withCardInHand(1, "Shock")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val shock = game.findCardsInHand(1, "Shock").single()
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = shock,
                    targets = listOf(entityIdToChosenTarget(game.state, bears)),
                ),
            ).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.findAllPermanents("Rat Token").size shouldBe 0
        }
    }
}
