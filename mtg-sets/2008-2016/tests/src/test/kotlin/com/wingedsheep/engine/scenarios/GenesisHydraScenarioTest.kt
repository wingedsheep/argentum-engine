package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Genesis Hydra (M15 #176) — "When you cast this spell, reveal the top X cards of your library.
 * You may put a nonland permanent card with mana value X or less from among them onto the
 * battlefield. Then shuffle the rest into your library. This creature enters with X +1/+1
 * counters on it."
 *
 * The cast trigger resolves before the Hydra and reads the X announced as it was cast, so it
 * still works — at that X — when the Hydra is countered in response.
 */
class GenesisHydraScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame, name: String): Int {
        val permanent = game.findPermanent(name)!!
        return game.state.getEntity(permanent)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    private fun stackNames(game: TestGame): List<String> =
        game.state.stack.mapNotNull { game.state.getEntity(it)?.get<CardComponent>()?.name }

    /** Exactly [library] cards in the library, so the top X are all of them when X = its size. */
    private fun board(vararg library: String, counterspellInHand: Boolean = false) = scenario()
        .withPlayers("Caster", "Opponent")
        .withCardInHand(1, "Genesis Hydra")
        .withLandsOnBattlefield(1, "Forest", 6)
        .apply { library.forEach { withCardInLibrary(1, it) } }
        .apply {
            if (counterspellInHand) {
                withCardInHand(2, "Counterspell")
                withLandsOnBattlefield(2, "Island", 2)
            }
        }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Genesis Hydra") {

            test("X=3: only a nonland permanent card with mana value 3 or less can be put onto the battlefield") {
                val game = board("Grizzly Bears", "Craw Wurm", "Giant Growth")
                game.castXSpell(1, "Genesis Hydra", xValue = 3).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
                withClue("Craw Wurm's mana value is too high and Giant Growth isn't a permanent card") {
                    decision.options shouldContainExactly listOf(bears)
                }

                game.selectCards(listOf(bears)).error shouldBe null
                withClue("the trigger resolves before the Hydra itself") {
                    game.findPermanent("Grizzly Bears") shouldNotBe null
                    stackNames(game) shouldContainExactly listOf("Genesis Hydra")
                }

                game.resolveStack()
                withClue("the Hydra enters with three +1/+1 counters") {
                    plusOneCounters(game, "Genesis Hydra") shouldBe 3
                }
                withClue("the rest were shuffled back into the library") {
                    game.librarySize(1) shouldBe 2
                    game.findCardsInLibrary(1, "Craw Wurm").size shouldBe 1
                    game.findCardsInLibrary(1, "Giant Growth").size shouldBe 1
                }
            }

            test("declining the 'may' leaves every revealed card in the library") {
                val game = board("Grizzly Bears", "Llanowar Elves")
                game.castXSpell(1, "Genesis Hydra", xValue = 2).error shouldBe null
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(emptyList()).error shouldBe null
                game.resolveStack()

                game.findPermanent("Grizzly Bears") shouldBe null
                game.findPermanent("Llanowar Elves") shouldBe null
                game.librarySize(1) shouldBe 2
                plusOneCounters(game, "Genesis Hydra") shouldBe 2
            }

            test("countered in response, the cast trigger still resolves with the announced X") {
                val game = board("Grizzly Bears", "Forest", counterspellInHand = true)
                game.castXSpell(1, "Genesis Hydra", xValue = 2).error shouldBe null
                game.passPriority()
                game.castSpellTargetingStackSpell(2, "Counterspell", "Genesis Hydra").error shouldBe null
                game.resolveStack()

                withClue("the Hydra is countered before its trigger resolves") {
                    game.isInGraveyard(1, "Genesis Hydra") shouldBe true
                }
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
                withClue("X is still 2: the Bears are eligible, the Forest is a land") {
                    decision.options shouldContainExactly listOf(bears)
                }

                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.findPermanent("Grizzly Bears") shouldNotBe null
                game.findPermanent("Genesis Hydra") shouldBe null
                game.librarySize(1) shouldBe 1
            }
        }
    }
}
