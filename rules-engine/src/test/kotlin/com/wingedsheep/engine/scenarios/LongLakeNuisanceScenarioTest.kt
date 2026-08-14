package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hob.cards.LongLakeNuisance
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Long Lake Nuisance (HOB #45) — {3}{U} Creature — Bird 3/1.
 *
 * "Flying" + "When this creature enters, recruit."
 *
 * The land-discard case is covered here rather than on the other recruit cards: discarding a land
 * must leave the battlefield token-free even though a card really was discarded.
 */
class LongLakeNuisanceScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(LongLakeNuisance)

        context("Long Lake Nuisance") {

            test("entering recruits; discarding a land mints no Soldier") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Long Lake Nuisance")
                    .withCardInHand(1, "Mountain")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Long Lake Nuisance").error shouldBe null
                game.resolveStack()

                withClue("the enters trigger should pause for recruit's discard choice") {
                    game.hasPendingDecision() shouldBe true
                }
                val mountain = game.findCardsInHand(1, "Mountain").single()
                game.selectCards(listOf(mountain))
                game.resolveStack()

                withClue("recruit drew the Bears before the discard") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
                withClue("the discarded land is in the graveyard") {
                    game.isInGraveyard(1, "Mountain") shouldBe true
                }
                withClue("a land discard fails 'if you discarded a nonland card' — no token") {
                    game.findAllPermanents("Human Soldier Token").size shouldBe 0
                }
            }

            test("has flying") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Long Lake Nuisance")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bird = game.findPermanent("Long Lake Nuisance")!!
                withClue("printed flying should be visible in projected state") {
                    game.state.projectedState.hasKeyword(bird, Keyword.FLYING) shouldBe true
                }
            }
        }
    }
}
