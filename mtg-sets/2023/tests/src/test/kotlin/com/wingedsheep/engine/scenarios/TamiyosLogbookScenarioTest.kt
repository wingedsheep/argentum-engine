package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tamiyo's Logbook (ONE #70) — Artifact — Book.
 *
 *   {5}{U}, {T}: Draw a card. This ability costs {1} less to activate for each other artifact
 *   you control.
 *
 * The points worth pinning are that the reduction counts only *other* artifacts (the Logbook
 * itself never discounts its own ability) and only ones *you* control.
 */
class TamiyosLogbookScenarioTest : ScenarioTestBase() {

    private fun abilityId() = cardRegistry.getCard("Tamiyo's Logbook")!!.activatedAbilities.first().id

    init {
        context("Tamiyo's Logbook") {

            test("two other artifacts cut the cost to {3}{U}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tamiyo's Logbook")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val logbook = game.findPermanent("Tamiyo's Logbook")!!
                val handBefore = game.handSize(1)

                val result = game.execute(ActivateAbility(game.player1Id, logbook, abilityId()))
                withClue("{3}{U} from four Islands: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Drew one card") {
                    game.handSize(1) shouldBe handBefore + 1
                }
            }

            test("the Logbook does not count itself, and opponents' artifacts don't count") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Tamiyo's Logbook")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardOnBattlefield(2, "Ornithopter")
                    // {3}{U} is due; three Islands would only suffice if the Logbook (or the
                    // opponent's Ornithopter) were wrongly counted.
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val logbook = game.findPermanents("Tamiyo's Logbook").first()

                val result = game.execute(ActivateAbility(game.player1Id, logbook, abilityId()))
                withClue("three Islands can't pay {3}{U}") {
                    result.error shouldNotBe null
                }
            }
        }
    }
}
