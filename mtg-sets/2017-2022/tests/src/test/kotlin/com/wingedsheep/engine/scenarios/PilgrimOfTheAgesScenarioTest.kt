package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Pilgrim of the Ages (STX #22, reprinted J22 #225) — {2}{W} Creature — Spirit, 2/1.
 *
 *   When this creature enters, you may search your library for a basic Plains card, reveal it,
 *   put it into your hand, then shuffle.
 *   {6}: Return this card from your graveyard to your hand.
 */
class PilgrimOfTheAgesScenarioTest : ScenarioTestBase() {

    init {
        context("Pilgrim of the Ages") {

            test("entering searches for a basic Plains and puts it into hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Pilgrim of the Ages")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Scrubland") // a Plains, but not a basic one
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Pilgrim of the Ages").error shouldBe null
                game.resolveStack()

                game.answerYesNo(true).error shouldBe null
                val search = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected a library search; got ${game.getPendingDecision()}")
                withClue("only the basic Plains is offered (not Scrubland), and at most one") {
                    search.options.size shouldBe 1
                    search.maxSelections shouldBe 1
                }
                game.selectCards(search.options).error shouldBe null
                game.resolveStack()

                withClue("the Plains went to hand, the other cards stayed in the library") {
                    game.isInHand(1, "Plains") shouldBe true
                    game.findCardsInLibrary(1, "Plains").size shouldBe 0
                    game.librarySize(1) shouldBe 3
                }
                game.isOnBattlefield("Pilgrim of the Ages") shouldBe true
            }

            test("declining the search leaves the library untouched") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Pilgrim of the Ages")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Pilgrim of the Ages").error shouldBe null
                game.resolveStack()

                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("no Plains was fetched") {
                    game.isInHand(1, "Plains") shouldBe false
                    game.librarySize(1) shouldBe 1
                }
            }

            test("{6}: returns it from the graveyard to hand") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Pilgrim of the Ages")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pilgrim = game.findCardsInGraveyard(1, "Pilgrim of the Ages").single()
                val abilityId = cardRegistry.getCard("Pilgrim of the Ages")!!.activatedAbilities.first().id

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = pilgrim,
                        abilityId = abilityId,
                    )
                )
                withClue("activation from the graveyard should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("Pilgrim of the Ages moved from the graveyard to hand") {
                    game.isInGraveyard(1, "Pilgrim of the Ages") shouldBe false
                    game.isInHand(1, "Pilgrim of the Ages") shouldBe true
                }
            }

            test("cannot activate the return with only five mana") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInGraveyard(1, "Pilgrim of the Ages")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val pilgrim = game.findCardsInGraveyard(1, "Pilgrim of the Ages").single()
                val abilityId = cardRegistry.getCard("Pilgrim of the Ages")!!.activatedAbilities.first().id

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = pilgrim,
                        abilityId = abilityId,
                    )
                )
                withClue("activation must fail without {6} available") {
                    (activation.error != null) shouldBe true
                }
                game.isInGraveyard(1, "Pilgrim of the Ages") shouldBe true
            }
        }
    }
}
