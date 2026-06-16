package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class FuriousForebearScenarioTest : ScenarioTestBase() {
    init {
        context("Furious Forebear") {
            test("does not trigger from its own death") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Furious Forebear")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forebear = game.findPermanent("Furious Forebear")!!
                val cast = game.castSpell(1, "Shock", forebear)
                withClue("Shock should cast successfully: ${cast.error}") {
                    cast.error shouldBe null
                }

                game.resolveStack()

                withClue("Forebear's own death must not create its graveyard trigger") {
                    game.hasPendingDecision() shouldBe false
                    game.state.stack.size shouldBe 0
                }
                withClue("Forebear should remain in its owner's graveyard") {
                    game.findCardsInGraveyard(1, "Furious Forebear").size shouldBe 1
                    game.findCardsInHand(1, "Furious Forebear").size shouldBe 0
                }
            }

            test("returns from graveyard when another creature you control dies and you pay") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInGraveyard(1, "Furious Forebear")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val cast = game.castSpell(1, "Shock", bears)
                withClue("Shock should cast successfully: ${cast.error}") {
                    cast.error shouldBe null
                }

                game.resolveStack()

                withClue("Forebear should ask whether to pay {1}{W}") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }
                val chooseToPay = game.answerYesNo(true)
                withClue("Choosing to pay Forebear's trigger should succeed: ${chooseToPay.error}") {
                    chooseToPay.error shouldBe null
                }
                withClue("Forebear should ask which mana sources to use") {
                    (game.getPendingDecision() is SelectManaSourcesDecision) shouldBe true
                }
                val pay = game.submitManaSourcesAutoPay()
                withClue("Autopaying Forebear's trigger should succeed: ${pay.error}") {
                    pay.error shouldBe null
                }
                game.resolveStack()

                withClue("Forebear should return from graveyard to hand after payment") {
                    game.findCardsInHand(1, "Furious Forebear").size shouldBe 1
                    game.findCardsInGraveyard(1, "Furious Forebear").size shouldBe 0
                }
            }

            test("stays in graveyard when another creature you control dies and you decline") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInGraveyard(1, "Furious Forebear")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val cast = game.castSpell(1, "Shock", bears)
                withClue("Shock should cast successfully: ${cast.error}") {
                    cast.error shouldBe null
                }

                game.resolveStack()

                withClue("Forebear should ask whether to pay {1}{W}") {
                    (game.getPendingDecision() is YesNoDecision) shouldBe true
                }
                val decline = game.answerYesNo(false)
                withClue("Declining Forebear's trigger should succeed: ${decline.error}") {
                    decline.error shouldBe null
                }
                game.resolveStack()

                withClue("Forebear should stay in graveyard after declining") {
                    game.findCardsInGraveyard(1, "Furious Forebear").size shouldBe 1
                    game.findCardsInHand(1, "Furious Forebear").size shouldBe 0
                }
            }
        }
    }
}
