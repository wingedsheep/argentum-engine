package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SearchLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The Hunger Tide Rises (MH3 #158) — {2}{G} Enchantment — Saga.
 *
 *  I, II, III — Create a 1/1 black and green Insect creature token.
 *  IV — Sacrifice any number of creatures. Search your library and/or graveyard for a creature card
 *       with mana value ≤ the number sacrificed and put it onto the battlefield. If you searched
 *       your library, shuffle.
 */
class TheHungerTideRisesScenarioTest : ScenarioTestBase() {

    private val saga = "The Hunger Tide Rises"

    private fun insects(game: TestGame): List<EntityId> =
        game.state.getBattlefield().filter {
            game.state.getEntity(it)?.has<TokenComponent>() == true &&
                game.state.projectedState.hasSubtype(it, "Insect")
        }

    /** Saga on the battlefield with three lore counters, about to reach the main phase (chapter IV). */
    private fun atChapterFour(extra: ScenarioBuilder.() -> Unit): TestGame {
        val game = scenario()
            .withPlayers("Alice", "Bob")
            .withCardOnBattlefield(1, saga)
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .withTurnNumber(3)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .apply(extra)
            .build()
        val sagaId = game.findPermanent(saga)!!
        game.state = game.state.updateEntity(sagaId) { c ->
            c.with(CountersComponent().withAdded(CounterType.LORE, 3))
                .with(SagaComponent(triggeredChapters = setOf(1, 2, 3)))
        }
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        game.resolveStack()
        return game
    }

    private fun chooseMode(game: TestGame, text: String) {
        val decision = game.getPendingDecision()
        withClue("chapter IV asks which zones to search, got $decision") {
            (decision is ChooseOptionDecision) shouldBe true
        }
        decision as ChooseOptionDecision
        game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf(text))).error shouldBe null
    }

    /** The search prompt's selectable cards, whichever decision shape it uses. */
    private fun searchOptions(game: TestGame): List<EntityId> = when (val d = game.getPendingDecision()) {
        is SelectCardsDecision -> d.options
        is SearchLibraryDecision -> d.options
        else -> error("expected a search decision, got $d")
    }

    private fun pickFromSearch(game: TestGame, card: EntityId) {
        val decision = game.getPendingDecision()!!
        game.submitDecision(CardsSelectedResponse(decision.id, listOf(card))).error shouldBe null
        game.resolveStack()
    }

    init {
        test("chapter I creates a 1/1 black and green Insect") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardInHand(1, saga)
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, saga).error shouldBe null
            game.resolveStack()
            game.resolveStack()

            val tokens = insects(game)
            tokens.size shouldBe 1
            val insect = tokens.single()
            game.state.projectedState.getPower(insect) shouldBe 1
            game.state.projectedState.getToughness(insect) shouldBe 1
            game.state.projectedState.getColors(insect) shouldBe setOf(Color.BLACK.name, Color.GREEN.name)
        }

        test("chapter IV: sacrifice two, fetch a mana value ≤ 2 creature from the library") {
            val game = atChapterFour {
                withCardOnBattlefield(1, "Grizzly Bears")
                withCardOnBattlefield(1, "Savannah Lions")
                withCardOnBattlefield(1, "Llanowar Elves")
                withCardInLibrary(1, "Centaur Courser")
                withCardInLibrary(1, "Grizzly Bears")
                withCardInLibrary(1, "Hill Giant")
            }
            val bears = game.findPermanent("Grizzly Bears")!!
            val lions = game.findPermanent("Savannah Lions")!!

            val sacrifice = game.getPendingDecision()
            withClue("chapter IV asks which creatures to sacrifice, got $sacrifice") {
                (sacrifice is SelectCardsDecision) shouldBe true
            }
            game.selectCards(listOf(bears, lions)).error shouldBe null
            chooseMode(game, "Search your library")

            val libraryBears = game.findCardsInLibrary(1, "Grizzly Bears").single()
            val options = searchOptions(game)
            options shouldContain libraryBears
            withClue("Centaur Courser (MV 3) and Hill Giant (MV 4) exceed two sacrificed") {
                options shouldNotContain game.findCardsInLibrary(1, "Centaur Courser").single()
                options shouldNotContain game.findCardsInLibrary(1, "Hill Giant").single()
            }
            pickFromSearch(game, libraryBears)

            withClue("the sacrificed creatures are in the graveyard") {
                game.isInGraveyard(1, "Savannah Lions") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
            withClue("the unsacrificed Elves stay") { game.findPermanent("Llanowar Elves") shouldNotBe null }
            withClue("the fetched Bears entered the battlefield") {
                game.state.getBattlefield() shouldContain libraryBears
            }
            withClue("the Saga is sacrificed after chapter IV") { game.findPermanent(saga) shouldBe null }
        }

        test("chapter IV can fetch from the graveyard, including a creature just sacrificed") {
            val game = atChapterFour {
                withCardOnBattlefield(1, "Savannah Lions")
                withCardInGraveyard(1, "Hill Giant")
            }
            val lions = game.findPermanent("Savannah Lions")!!

            game.selectCards(listOf(lions)).error shouldBe null
            chooseMode(game, "Search your graveyard")

            val lionsCard = game.findCardsInGraveyard(1, "Savannah Lions").single()
            val options = searchOptions(game)
            options shouldContain lionsCard
            options shouldNotContain game.findCardsInGraveyard(1, "Hill Giant").single()
            pickFromSearch(game, lionsCard)

            withClue("Savannah Lions came back from the graveyard") {
                game.findPermanent("Savannah Lions") shouldNotBe null
                game.isInGraveyard(1, "Savannah Lions") shouldBe false
            }
        }
    }
}
