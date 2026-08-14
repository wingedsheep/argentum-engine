package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Kishla Trawlers. */
class KishlaTrawlersScenarioTest : ScenarioTestBase() {

    private val rainveilManaAbilityId =
        cardRegistry.getCard("Rainveil Rejuvenator")!!.activatedAbilities.first().id
    private val unrootedAbilityId =
        cardRegistry.getCard("Unrooted Ancestor")!!.activatedAbilities.first().id

    init {
        context("Kishla Trawlers") {
            test("ETB exiles a creature card and returns an instant/sorcery to hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Kishla Trawlers")
                    .withCardInGraveyard(1, "Glory Seeker") // creature card to exile
                    .withCardInGraveyard(1, "Shock")        // instant to return
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Kishla Trawlers")
                withClue("Casting Kishla Trawlers should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                // Reflexive "you may" yes/no.
                withClue("ETB should present a 'you may exile' yes/no decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)
                game.resolveStack()

                // With a single creature card in the graveyard, it is auto-selected and
                // exiled; the reflexive trigger then prompts for the instant/sorcery target.
                withClue("Glory Seeker should be exiled from the graveyard") {
                    game.findCardsInGraveyard(1, "Glory Seeker").size shouldBe 0
                }
                withClue("Should now prompt to return an instant/sorcery to hand") {
                    game.hasPendingDecision() shouldBe true
                }
                val shock = game.findCardsInGraveyard(1, "Shock").first()
                game.selectTargets(listOf(shock))
                game.resolveStack()

                withClue("Glory Seeker should be exiled from the graveyard") {
                    game.findCardsInGraveyard(1, "Glory Seeker").size shouldBe 0
                }
                withClue("Shock should be returned to hand") {
                    game.findCardsInHand(1, "Shock").size shouldBe 1
                    game.findCardsInGraveyard(1, "Shock").size shouldBe 0
                }
            }

            test("ETB does nothing when there is no creature card to exile") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Kishla Trawlers")
                    .withCardInGraveyard(1, "Shock") // only an instant, no creature card
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Kishla Trawlers")
                game.resolveStack()

                withClue("With no creature card to exile, the may-decision is skipped entirely") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("Shock stays in the graveyard") {
                    game.findCardsInGraveyard(1, "Shock").size shouldBe 1
                }
            }
        }
    }
}
