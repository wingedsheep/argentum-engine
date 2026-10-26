package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Nether Traitor — "Whenever another creature is put into your graveyard from the battlefield,
 * you may pay {B}. If you do, return this card from your graveyard to the battlefield."
 */
class NetherTraitorScenarioTest : ScenarioTestBase() {
    init {
        context("Nether Traitor") {
            test("returns from the graveyard when another creature you own dies and you pay {B}") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInGraveyard(1, "Nether Traitor")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!)
                withClue("Shock should cast: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null
                game.getPendingDecision().shouldBeInstanceOf<SelectManaSourcesDecision>()
                game.submitManaSourcesAutoPay().error shouldBe null
                game.resolveStack()

                withClue("Nether Traitor should be back on the battlefield") {
                    game.findPermanent("Nether Traitor") shouldNotBe null
                    game.findCardsInGraveyard(1, "Nether Traitor").size shouldBe 0
                }
            }

            test("stays in the graveyard when you decline to pay") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInGraveyard(1, "Nether Traitor")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!)
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.findPermanent("Nether Traitor") shouldBe null
                game.findCardsInGraveyard(1, "Nether Traitor").size shouldBe 1
            }

            test("does not trigger when a creature goes to an opponent's graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInGraveyard(1, "Nether Traitor")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Shock", game.findPermanent("Grizzly Bears")!!)
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.state.stack.size shouldBe 0
                game.findCardsInGraveyard(1, "Nether Traitor").size shouldBe 1
            }

            test("does not trigger for a creature that dies at the same time as it") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Wrath of God")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(1, "Nether Traitor")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Wrath of God")
                withClue("Wrath of God should cast: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Nether Traitor was not in the graveyard when Grizzly Bears died (ruling 2021-03-19)") {
                    game.hasPendingDecision() shouldBe false
                    game.state.stack.size shouldBe 0
                    game.findCardsInGraveyard(1, "Nether Traitor").size shouldBe 1
                }
            }
        }
    }
}
