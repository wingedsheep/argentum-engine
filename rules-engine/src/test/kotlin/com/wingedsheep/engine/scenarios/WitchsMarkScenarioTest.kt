package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Witch's Mark. */
class WitchsMarkScenarioTest : ScenarioTestBase() {

    private val outriderAbilityId by lazy {
        cardRegistry.requireCard("Verdant Outrider").activatedAbilities[0].id
    }

    init {
        context("Witch's Mark — loot half and Role half are independent") {
            test("accepting the discard draws two and still creates the Wicked Role") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Witch's Mark")
                    .withCardInHand(1, "Mountain")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Grizzly Bears")!!
                val spareMountain = game.findCardsInHand(1, "Mountain").first()

                // Hand after casting: the spare Mountain only (Witch's Mark goes to the stack).
                game.castSpell(1, "Witch's Mark", bear)
                game.resolveStack()

                // "You may discard a card." — say yes; with a single card in hand the engine
                // auto-selects it, so only prompt-driven cases need an explicit selection.
                game.getPendingDecision() shouldNotBe null
                game.answerYesNo(true)
                if (game.isInHand(1, "Mountain")) game.selectCards(listOf(spareMountain))
                game.resolveStack()

                withClue("discarded 1, drew 2 -> net +1 card in hand") {
                    game.handSize(1) shouldBe 2
                }
                withClue("Mountain went to the graveyard") {
                    game.isInGraveyard(1, "Mountain") shouldBe true
                }
                val role = game.findPermanent("Wicked Role")
                withClue("the Wicked Role token was created") { role shouldNotBe null }
                withClue("the Role is attached to the targeted Bear") {
                    game.state.getEntity(role!!)?.get<AttachedToComponent>()?.targetId shouldBe bear
                }
                withClue("2/2 Bear + Wicked Role's +1/+1 = 3/3") {
                    game.state.projectedState.getPower(bear) shouldBe 3
                    game.state.projectedState.getToughness(bear) shouldBe 3
                }
            }

            test("declining the discard skips the draw but still creates the Wicked Role") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Witch's Mark")
                    .withCardInHand(1, "Mountain")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Witch's Mark", bear)
                game.resolveStack()

                game.answerYesNo(false)
                game.resolveStack()

                withClue("no discard and no draw — the spare Mountain is untouched") {
                    game.handSize(1) shouldBe 1
                    game.isInHand(1, "Mountain") shouldBe true
                }
                withClue("the Role half is independent of the loot half") {
                    game.findPermanent("Wicked Role") shouldNotBe null
                    game.state.projectedState.getPower(bear) shouldBe 3
                }
            }
        }
    }
}
