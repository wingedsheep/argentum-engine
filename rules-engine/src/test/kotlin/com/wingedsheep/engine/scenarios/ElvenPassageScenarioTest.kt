package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hob.cards.ElvenPassage
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Elven Passage (HOB #181) — "{T}, Pay 1 life, Sacrifice this land: Search your library for a basic
 * land card, put it onto the battlefield tapped, then shuffle. You may behold an Elf. If you do,
 * untap that land."
 *
 * The behold is the resolution-time flavour, and its payoff has to reach the *specific* land the
 * search just moved — the pipeline stashes it with `storeMovedAs` so the untap can name it. Covered:
 * beholding untaps exactly that land, declining leaves it tapped, and the 1 life is paid either way.
 */
class ElvenPassageScenarioTest : ScenarioTestBase() {

    private val activateId = ElvenPassage.activatedAbilities.first().id

    init {
        context("Elven Passage — fetch tapped, behold an Elf to untap it") {

            test("beholding an Elf you control untaps the fetched land") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elven Passage")
                    .withCardOnBattlefield(1, "Llanowar Elves", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val passage = game.findPermanent("Elven Passage")!!
                game.execute(ActivateAbility(game.player1Id, passage, activateId)).error shouldBe null
                game.resolveStack()

                // Search: the only basic land in the library.
                withClue("the library search should be pending") { game.hasPendingDecision() shouldBe true }
                game.selectCards(game.findCardsInLibrary(1, "Forest"))
                game.resolveStack()

                // Behold: the Elf we control.
                withClue("the behold decision should be pending") { game.hasPendingDecision() shouldBe true }
                game.selectCards(listOf(game.findPermanent("Llanowar Elves")!!))
                game.resolveStack()

                val forest = game.findPermanent("Forest")!!
                withClue("the beheld Elf untapped the land the search put onto the battlefield") {
                    game.state.getEntity(forest)?.has<TappedComponent>() shouldBe false
                }
                withClue("the Passage paid 1 life and sacrificed itself") {
                    game.getLifeTotal(1) shouldBe 19
                    game.isInGraveyard(1, "Elven Passage") shouldBe true
                }
            }

            test("declining the behold leaves the fetched land tapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elven Passage")
                    .withCardOnBattlefield(1, "Llanowar Elves", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val passage = game.findPermanent("Elven Passage")!!
                game.execute(ActivateAbility(game.player1Id, passage, activateId)).error shouldBe null
                game.resolveStack()

                game.selectCards(game.findCardsInLibrary(1, "Forest"))
                game.resolveStack()

                game.skipSelection() // decline the behold
                game.resolveStack()

                val forest = game.findPermanent("Forest")!!
                withClue("no behold means no untap — the land stays as it entered") {
                    game.state.getEntity(forest)?.has<TappedComponent>() shouldBe true
                }
                withClue("the life is paid on activation, behold or not") {
                    game.getLifeTotal(1) shouldBe 19
                }
            }
        }
    }
}
