package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sweettooth Witch — {2}{B} Creature — Human Warlock 3/2 (WOE).
 *
 * When this creature enters, create a Food token.
 * {2}, Sacrifice a Food: Target player loses 2 life.
 *
 * Covers the enters-trigger Food, the drain ability paid with that Food, and the fact that
 * the cost needs a Food (not just any artifact) to be payable.
 */
class SweettoothWitchScenarioTest : ScenarioTestBase() {

    private val drainAbility by lazy {
        cardRegistry.requireCard("Sweettooth Witch").activatedAbilities[0]
    }

    init {
        test("entering the battlefield creates a Food token") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Sweettooth Witch")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Sweettooth Witch").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Sweettooth Witch") shouldBe true
            game.findPermanents("Food").size shouldBe 1
        }

        test("{2}, Sacrifice a Food: target player loses 2 life") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Sweettooth Witch")
                .withCardOnBattlefield(1, "Food", isToken = true)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val witch = game.findPermanent("Sweettooth Witch")!!
            val food = game.findPermanent("Food")!!
            val lifeBefore = game.getLifeTotal(2)

            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = witch,
                    abilityId = drainAbility.id,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(food))
                )
            )
            withClue("Activating the drain should succeed: ${result.error}") {
                result.error shouldBe null
            }
            game.resolveStack()

            withClue("The Food paid for the ability and is gone") {
                game.isOnBattlefield("Food") shouldBe false
            }
            game.getLifeTotal(2) shouldBe lifeBefore - 2
        }

        test("the ability can target its controller") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Sweettooth Witch")
                .withCardOnBattlefield(1, "Food", isToken = true)
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val witch = game.findPermanent("Sweettooth Witch")!!
            val food = game.findPermanent("Food")!!
            val lifeBefore = game.getLifeTotal(1)

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = witch,
                    abilityId = drainAbility.id,
                    targets = listOf(ChosenTarget.Player(game.player1Id)),
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(food))
                )
            ).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe lifeBefore - 2
        }

        test("without a Food the ability cannot be activated") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Sweettooth Witch")
                // A non-Food artifact is not acceptable fodder.
                .withCardOnBattlefield(1, "Prophetic Prism")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val witch = game.findPermanent("Sweettooth Witch")!!
            val prism = game.findPermanent("Prophetic Prism")!!
            val lifeBefore = game.getLifeTotal(2)

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = witch,
                    abilityId = drainAbility.id,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(prism))
                )
            ).error shouldNotBe null

            game.isOnBattlefield("Prophetic Prism") shouldBe true
            game.getLifeTotal(2) shouldBe lifeBefore
        }
    }
}
