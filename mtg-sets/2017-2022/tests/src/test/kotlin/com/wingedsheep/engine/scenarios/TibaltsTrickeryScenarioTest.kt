package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe

/**
 * Tibalt's Trickery (KHM #153) — counter target spell; its controller mills 1, 2, or 3 cards chosen
 * at random, exiles from the top until a nonland card with a different name than the countered
 * spell, may cast it for free, and bottoms the exiled cards in a random order.
 */
class TibaltsTrickeryScenarioTest : ScenarioTestBase() {

    private val bear = card("Trickery Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    private val giant = card("Trickery Test Giant") {
        manaCost = "{5}{G}"
        typeLine = "Creature — Giant"
        power = 6
        toughness = 6
    }

    private val wurm = card("Trickery Test Wurm") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Wurm"
        power = 3
        toughness = 3
        cantBeCountered = true
    }

    init {
        listOf(bear, giant, wurm).forEach { cardRegistry.register(it) }

        // The opponent (player 2) casts the bear on their turn; player 1 answers with Trickery.
        // Opponent's library, top → bottom: three Forests (the mill), a same-named bear (skipped),
        // an Island, the giant (the hit), then two Forests that are never touched.
        fun board(seed: Long) = scenario()
            .withPlayers("Player", "Opponent")
            .withRngSeed(seed)
            .withCardInHand(1, "Tibalt's Trickery")
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withCardInHand(2, "Trickery Test Bear")
            .withLandsOnBattlefield(2, "Forest", 2)
            .withCardInLibrary(2, "Forest")
            .withCardInLibrary(2, "Forest")
            .withCardInLibrary(2, "Forest")
            .withCardInLibrary(2, "Trickery Test Bear")
            .withCardInLibrary(2, "Island")
            .withCardInLibrary(2, "Trickery Test Giant")
            .withCardInLibrary(2, "Plains")
            .withCardInLibrary(2, "Plains")
            .withCardInLibrary(1, "Mountain")
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.libraryNames(playerNumber: Int): List<String> =
            state.getLibrary(if (playerNumber == 1) player1Id else player2Id)
                .map { state.getEntity(it)!!.get<CardComponent>()!!.name }

        fun TestGame.counterTheBear() {
            castSpell(2, "Trickery Test Bear").error shouldBe null
            passPriority()
            castSpellTargetingStackSpell(1, "Tibalt's Trickery", "Trickery Test Bear").error shouldBe null
            resolveStack()
        }

        fun TestGame.getGraveyardCardNames(playerNumber: Int): List<String> =
            state.getGraveyard(if (playerNumber == 1) player1Id else player2Id)
                .map { state.getEntity(it)!!.get<CardComponent>()!!.name }

        /** Forests milled = Forests in the opponent's graveyard. */
        fun TestGame.milled(): Int = getGraveyardCardNames(2).count { it == "Forest" }

        test("counters the spell, mills 1-3 at random, skips the same name, and the controller casts the hit free") {
            val game = board(seed = 1L)
            game.counterTheBear()

            withClue("the opponent is offered the giant") {
                game.hasPendingDecision() shouldBe true
                game.state.pendingDecision!!.playerId shouldBe game.player2Id
            }
            game.answerYesNo(true)
            game.resolveStack()

            val milled = game.milled()
            withClue("the bear was countered and 1, 2 or 3 cards were milled") {
                game.getGraveyardCardNames(2).count { it == "Trickery Test Bear" } shouldBe 1
                milled shouldBeInRange 1..3
            }
            withClue("the giant was cast without paying its mana cost, under the opponent's control") {
                game.isOnBattlefield("Trickery Test Giant") shouldBe true
                game.state.projectedState.getController(game.findPermanent("Trickery Test Giant")!!) shouldBe game.player2Id
            }
            val library = game.libraryNames(2)
            withClue("the untouched cards stay on top; the rest of the exiled cards go to the bottom") {
                library.take(2) shouldBe listOf("Plains", "Plains")
                library.drop(2) shouldContainExactlyInAnyOrder
                    List(3 - milled) { "Forest" } + listOf("Trickery Test Bear", "Island")
            }
            game.state.getExile(game.player2Id).size shouldBe 0
        }

        test("the random count reaches 1, 2 and 3 across seeds, never prompting for it") {
            val counts = (0L until 40L).map { seed ->
                val game = board(seed)
                game.counterTheBear()
                // The only decision is the may-cast; no number is ever asked for.
                game.answerYesNo(false)
                game.resolveStack()
                game.milled()
            }.toSet()
            counts shouldBe setOf(1, 2, 3)
        }

        test("a declined hit goes to the bottom with the other exiled cards") {
            val game = board(seed = 3L)
            game.counterTheBear()
            game.answerYesNo(false)
            game.resolveStack()

            val milled = game.milled()
            game.isOnBattlefield("Trickery Test Giant") shouldBe false
            val library = game.libraryNames(2)
            library.take(2) shouldBe listOf("Plains", "Plains")
            library.drop(2) shouldContainExactlyInAnyOrder
                List(3 - milled) { "Forest" } + listOf("Trickery Test Bear", "Island", "Trickery Test Giant")
            withClue("Tibalt's Trickery's caster's library is untouched") {
                game.libraryNames(1) shouldBe listOf("Mountain")
            }
        }

        test("an uncounterable spell resolves, but its controller still mills, exiles and may cast") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withRngSeed(5L)
                .withCardInHand(1, "Tibalt's Trickery")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardInHand(2, "Trickery Test Wurm")
                .withLandsOnBattlefield(2, "Forest", 2)
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Trickery Test Giant")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(2, "Trickery Test Wurm").error shouldBe null
            game.passPriority()
            game.castSpellTargetingStackSpell(1, "Tibalt's Trickery", "Trickery Test Wurm").error shouldBe null
            game.resolveStack()
            game.answerYesNo(true)
            game.resolveStack()

            withClue("the wurm can't be countered and resolves") {
                game.isOnBattlefield("Trickery Test Wurm") shouldBe true
            }
            withClue("the rest of the effect still happened") {
                game.milled() shouldBeInRange 1..3
                game.isOnBattlefield("Trickery Test Giant") shouldBe true
            }
        }
    }
}
