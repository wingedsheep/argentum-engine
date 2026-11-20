package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gift of Immortality (THS #14) — {2}{W} Enchantment — Aura.
 *
 * "When enchanted creature dies, return that card to the battlefield under its owner's control.
 *  Return this card to the battlefield attached to that creature at the beginning of the next end step."
 *
 * Pins the two-stage return: the creature comes back at once under its owner's control, the Aura
 * waits in the graveyard for the next end step (the following turn's when the creature died during
 * an end step) and returns attached to that creature — unless the creature is gone by then or was
 * a token (2013-09-15 rulings).
 */
class GiftOfImmortalityScenarioTest : ScenarioTestBase() {

    private fun killEnchantedBears(
        phase: Phase = Phase.PRECOMBAT_MAIN,
        step: Step = Step.PRECOMBAT_MAIN,
        bearsController: Int = 1,
        bearsIsToken: Boolean = false
    ): TestGame {
        val game = scenario()
            .withPlayers("Alice", "Bob")
            .withCardOnBattlefield(bearsController, "Grizzly Bears", isToken = bearsIsToken)
            .withCardAttachedTo(1, "Gift of Immortality", "Grizzly Bears")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .inPhase(phase, step)
            .build()

        val bears = game.findPermanent("Grizzly Bears")!!
        game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
        game.resolveStack()
        game.checkStateBasedActions()
        return game
    }

    private fun TestGame.giftHost(): Any? =
        findPermanent("Gift of Immortality")?.let { state.getEntity(it)?.get<AttachedToComponent>()?.targetId }

    init {
        context("Gift of Immortality") {

            test("the creature returns at once; the Aura returns attached to it at the next end step") {
                val game = killEnchantedBears(bearsController = 2)

                withClue("the opponent's creature card is back under its owner's control") {
                    val bears = game.findPermanent("Grizzly Bears")!!
                    game.state.projectedState.getController(bears) shouldBe game.player2Id
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                }
                withClue("the Aura waits in its owner's graveyard until the end step") {
                    game.isInGraveyard(1, "Gift of Immortality") shouldBe true
                    game.isOnBattlefield("Gift of Immortality") shouldBe false
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.checkStateBasedActions()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("the Aura returned attached to that creature, under Alice's control") {
                    game.giftHost() shouldBe bears
                    game.state.projectedState.getController(game.findPermanent("Gift of Immortality")!!) shouldBe
                        game.player1Id
                    game.isInGraveyard(1, "Gift of Immortality") shouldBe false
                }
            }

            test("a creature that dies during an end step brings the Aura back at the next turn's end step") {
                val game = killEnchantedBears(phase = Phase.ENDING, step = Step.END)
                val turnOfDeath = game.state.turnNumber

                withClue("the creature returns immediately") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("this turn's end step had already begun, so the Aura stayed in the graveyard") {
                    game.state.turnNumber shouldBe turnOfDeath + 1
                    game.isInGraveyard(1, "Gift of Immortality") shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.checkStateBasedActions()

                withClue("the following turn's end step returns the Aura attached to the creature") {
                    game.state.turnNumber shouldBe turnOfDeath + 1
                    game.giftHost() shouldBe game.findPermanent("Grizzly Bears")!!
                }
            }

            test("the Aura stays in the graveyard if the creature has left the battlefield by the end step") {
                val game = killEnchantedBears()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = zones.moveToZone(game.state, bears, Zone.GRAVEYARD).state

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.checkStateBasedActions()

                game.isInGraveyard(1, "Gift of Immortality") shouldBe true
                game.isOnBattlefield("Gift of Immortality") shouldBe false
            }

            test("a token creature doesn't return, and neither does the Aura") {
                val game = killEnchantedBears(bearsIsToken = true)

                game.isOnBattlefield("Grizzly Bears") shouldBe false

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.checkStateBasedActions()

                game.isInGraveyard(1, "Gift of Immortality") shouldBe true
                game.isOnBattlefield("Gift of Immortality") shouldBe false
            }
        }
    }
}
