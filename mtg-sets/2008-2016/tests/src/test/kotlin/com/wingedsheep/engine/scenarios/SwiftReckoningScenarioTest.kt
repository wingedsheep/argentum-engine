package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Swift Reckoning (ORI #35).
 *
 * "Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, you may
 * cast this spell as though it had flash. Destroy target tapped creature."
 *
 * The spell-mastery count is read as the cast begins (the card's `conditionalFlash`): with two
 * instant/sorcery cards in its controller's graveyard it is offered on the opponent's turn; with only
 * one — or with non-instant/sorcery cards padding the graveyard — it keeps sorcery timing.
 */
class SwiftReckoningScenarioTest : ScenarioTestBase() {

    private fun game(graveyard: List<String>) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Swift Reckoning")
        .withLandsOnBattlefield(1, "Plains", 2)
        .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
        .apply { graveyard.forEach { withCardInGraveyard(1, it) } }
        .withActivePlayer(2)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.reckoningOffer() =
        getLegalActions(1).firstOrNull { it.actionType == "CastSpell" && it.description.contains("Swift Reckoning") }

    init {
        context("Swift Reckoning") {

            test("with spell mastery it is cast on the opponent's turn and destroys the tapped creature") {
                val game = game(listOf("Lightning Bolt", "Divination"))
                game.reckoningOffer().shouldNotBeNull()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                game.castSpell(1, "Swift Reckoning", bears).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }

            test("with only one instant or sorcery in the graveyard it keeps sorcery timing") {
                val game = game(listOf("Lightning Bolt"))
                game.reckoningOffer().shouldBeNull()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                game.castSpell(1, "Swift Reckoning", bears).error shouldNotBe null
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("non-instant, non-sorcery cards do not count toward spell mastery") {
                val game = game(listOf("Lightning Bolt", "Grizzly Bears", "Plains"))
                game.reckoningOffer().shouldBeNull()
            }
        }
    }
}
