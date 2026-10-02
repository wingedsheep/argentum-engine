package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Goliath Hatchery (ONE #408) — {4}{G}{G} Enchantment.
 *
 * "When this enchantment enters, create two 3/3 green Phyrexian Beast creature tokens with toxic 1.
 *  Corrupted — At the beginning of your upkeep, if an opponent has three or more poison counters,
 *  choose a creature you control, then draw cards equal to its total toxic value."
 */
class GoliathHatcheryScenarioTest : ScenarioTestBase() {

    private fun TestGame.setPoison(playerId: EntityId, count: Int) {
        state = state.updateEntity(playerId) { it.with(CountersComponent(mapOf(CounterType.POISON to count))) }
    }

    /** Player 2's main phase, Player 1 controls the Hatchery, Slaughter Singer (toxic 2) and Grizzly Bears. */
    private fun upkeepBoard(opponentPoison: Int): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Goliath Hatchery")
            .withCardOnBattlefield(1, "Slaughter Singer")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(6) { builder.withCardInLibrary(1, "Forest") }
        repeat(6) { builder.withCardInLibrary(2, "Forest") }
        val game = builder.build()
        game.setPoison(game.player2Id, opponentPoison)
        return game
    }

    init {
        test("entering creates two 3/3 green Phyrexian Beasts with toxic 1") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Goliath Hatchery")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Goliath Hatchery").error shouldBe null
            game.resolveStack()

            val beasts = game.findPermanents("Phyrexian Beast Token").ifEmpty { game.findPermanents("Phyrexian Beast") }
            beasts.size shouldBe 2
            val projected = game.state.projectedState
            beasts.forEach { beast ->
                projected.getPower(beast) shouldBe 3
                projected.getToughness(beast) shouldBe 3
                projected.getColors(beast) shouldBe setOf(Color.GREEN.name)
                projected.hasSubtype(beast, "Phyrexian") shouldBe true
                projected.hasSubtype(beast, "Beast") shouldBe true
                projected.getKeywords(beast).contains("TOXIC_1") shouldBe true
                projected.getController(beast) shouldBe game.player1Id
            }
        }

        test("corrupted upkeep: choose a creature, draw cards equal to its total toxic value") {
            val game = upkeepBoard(opponentPoison = 3)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            val handBefore = game.handSize(1)

            game.resolveStack()
            withClue("the controller chooses among their creatures") { game.hasPendingDecision() shouldBe true }
            game.selectCards(listOf(game.findPermanent("Slaughter Singer")!!))
            game.resolveStack()

            game.handSize(1) shouldBe handBefore + 2
        }

        test("choosing a creature without toxic draws nothing") {
            val game = upkeepBoard(opponentPoison = 3)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            val handBefore = game.handSize(1)

            game.resolveStack()
            game.hasPendingDecision() shouldBe true
            game.selectCards(listOf(game.findPermanent("Grizzly Bears")!!))
            game.resolveStack()

            game.handSize(1) shouldBe handBefore
        }

        test("without an opponent at three poison the upkeep trigger does nothing") {
            val game = upkeepBoard(opponentPoison = 2)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            val handBefore = game.handSize(1)

            game.resolveStack()
            game.hasPendingDecision() shouldBe false
            game.handSize(1) shouldBe handBefore
            game.findPermanent("Goliath Hatchery") shouldNotBe null
            game.state.projectedState.hasKeyword(game.findPermanent("Slaughter Singer")!!, Keyword.TOXIC) shouldBe true
        }

        test("with no creature to choose, the corrupted trigger resolves without drawing") {
            val builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Goliath Hatchery")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            repeat(6) { builder.withCardInLibrary(1, "Forest") }
            repeat(6) { builder.withCardInLibrary(2, "Forest") }
            val game = builder.build()
            game.setPoison(game.player2Id, 3)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            val handBefore = game.handSize(1)

            game.resolveStack()
            game.hasPendingDecision() shouldBe false
            game.state.stack.isEmpty() shouldBe true
            game.handSize(1) shouldBe handBefore
        }
    }
}
