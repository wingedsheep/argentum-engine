package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Rite of Oblivion (MID #237) — {W}{B} Sorcery.
 *
 *   As an additional cost to cast this spell, sacrifice a nonland permanent.
 *   Exile target nonland permanent.
 *   Flashback {2}{W}{B}
 */
class RiteOfOblivionScenarioTest : ScenarioTestBase() {

    init {
        context("Rite of Oblivion") {

            test("sacrificing a nonland permanent exiles the target nonland permanent") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Rite of Oblivion")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val card = game.findCardsInHand(1, "Rite of Oblivion").first()

                game.execute(
                    CastSpell(
                        game.player1Id,
                        card,
                        listOf(ChosenTarget.Permanent(giant)),
                        additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)),
                    )
                ).error shouldBe null

                // The sacrifice is paid on cast, before resolution.
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true

                game.resolveStack()

                game.isInExile(2, "Hill Giant") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isInGraveyard(1, "Rite of Oblivion") shouldBe true
            }

            test("cannot be cast without a nonland permanent to sacrifice") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Rite of Oblivion")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                val plains = game.findPermanent("Plains")!!
                val card = game.findCardsInHand(1, "Rite of Oblivion").first()

                game.execute(
                    CastSpell(
                        game.player1Id,
                        card,
                        listOf(ChosenTarget.Permanent(giant)),
                        additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(plains)),
                    )
                ).error shouldNotBe null
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }
    }
}
