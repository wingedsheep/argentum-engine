package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.rna.cards.PriestOfForgottenGods
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Priest of Forgotten Gods {1}{B} — Creature — Human Cleric 1/2:
 * "{T}, Sacrifice two other creatures: Any number of target players each lose 2 life and sacrifice
 * a creature of their choice. You add {B}{B} and draw a card."
 */
class PriestOfForgottenGodsScenarioTest : ScenarioTestBase() {

    private fun setUp() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Priest of Forgotten Gods")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Savannah Lions")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardInLibrary(1, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.activate(targets: List<ChosenTarget>) =
        execute(
            ActivateAbility(
                player1Id,
                findPermanent("Priest of Forgotten Gods")!!,
                PriestOfForgottenGods.activatedAbilities.single().id,
                targets = targets,
                costPayment = AdditionalCostPayment(
                    sacrificedPermanents = listOf(findPermanent("Grizzly Bears")!!, findPermanent("Savannah Lions")!!),
                ),
            )
        )

    private fun TestGame.blackInPool() =
        state.getEntity(player1Id)?.get<ManaPoolComponent>()?.getAmount(Color.BLACK)

    init {
        test("a targeted opponent loses 2 life and sacrifices a creature; you add {B}{B} and draw") {
            val game = setUp()
            val handBefore = game.handSize(1)

            game.activate(listOf(ChosenTarget.Player(game.player2Id))).error shouldBe null
            withClue("both other creatures were sacrificed as the cost") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Savannah Lions") shouldBe true
            }
            game.resolveStack()
            if (game.getPendingDecision() is SelectCardsDecision) {
                game.selectCards(listOf(game.findPermanent("Hill Giant")!!)).error shouldBe null
                game.resolveStack()
            }

            game.getLifeTotal(2) shouldBe 18
            game.isInGraveyard(2, "Hill Giant") shouldBe true
            game.getLifeTotal(1) shouldBe 20
            game.blackInPool() shouldBe 2
            game.handSize(1) shouldBe handBefore + 1
        }

        test("with no targets chosen it still adds {B}{B} and draws a card (ruling)") {
            val game = setUp()
            val handBefore = game.handSize(1)

            game.activate(emptyList()).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 20
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.blackInPool() shouldBe 2
            game.handSize(1) shouldBe handBefore + 1
        }

        test("it can't be activated without two other creatures to sacrifice") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Priest of Forgotten Gods")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val priest = game.findPermanent("Priest of Forgotten Gods")!!
            withClue("the Priest itself isn't an \"other\" creature") {
                game.execute(
                    ActivateAbility(
                        game.player1Id,
                        priest,
                        PriestOfForgottenGods.activatedAbilities.single().id,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(game.findPermanent("Grizzly Bears")!!, priest),
                        ),
                    )
                ).error shouldNotBe null
            }
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
