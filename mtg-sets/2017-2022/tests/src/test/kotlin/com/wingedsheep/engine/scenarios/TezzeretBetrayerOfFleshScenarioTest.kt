package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Tezzeret, Betrayer of Flesh (NEO #84, {2}{U}{U} planeswalker, loyalty 4).
 *
 *   The first activated ability of an artifact you activate each turn costs {2} less to activate.
 *   +1: Draw two cards. Then discard two cards unless you discard an artifact card.
 *   −2: Target artifact becomes an artifact creature. If it isn't a Vehicle, it has base power and
 *       toughness 4/4.
 *   −6: You get an emblem with "Whenever an artifact you control becomes tapped, draw a card."
 */
class TezzeretBetrayerOfFleshScenarioTest : ScenarioTestBase() {

    private val tezzeretName = "Tezzeret, Betrayer of Flesh"

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    private fun seedLoyalty(game: TestGame, id: EntityId, amount: Int) {
        game.state = game.state.updateEntity(id) { c ->
            c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
        }
    }

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.has<TappedComponent>() == true

    private fun loyaltyAbility(index: Int) =
        cardRegistry.getCard(tezzeretName)!!.script.activatedAbilities[index]

    private val millstoneAbility get() =
        cardRegistry.getCard("Millstone")!!.script.activatedAbilities.single()

    private val mindStoneManaAbility get() =
        cardRegistry.getCard("Mind Stone")!!.script.activatedAbilities[0]

    private fun activateMillstone(
        game: TestGame,
        playerId: EntityId,
        millstone: EntityId,
        target: EntityId,
        paymentStrategy: PaymentStrategy = PaymentStrategy.AutoPay
    ) = game.execute(
        ActivateAbility(
            playerId = playerId,
            sourceId = millstone,
            abilityId = millstoneAbility.id,
            targets = listOf(ChosenTarget.Player(target)),
            paymentStrategy = paymentStrategy
        )
    )

    init {
        test("the first artifact ability this turn costs {2} less; the second costs full price") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, tezzeretName)
                .withCardOnBattlefield(1, "Millstone")
                .withCardOnBattlefield(1, "Millstone")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val (first, second) = game.findAllPermanents("Millstone")
            val islands = game.findAllPermanents("Island")

            activateMillstone(game, game.player1Id, first, game.player2Id).error shouldBe null
            withClue("the first Millstone activation cost {0} — no Island was tapped") {
                islands.none { isTapped(game, it) } shouldBe true
            }
            game.resolveStack()
            game.librarySize(2) shouldBe 2

            activateMillstone(game, game.player1Id, second, game.player2Id).error shouldBe null
            withClue("the second activation this turn paid its full {2}") {
                islands.all { isTapped(game, it) } shouldBe true
            }
            game.resolveStack()
            game.librarySize(2) shouldBe 0
        }

        test("an artifact mana ability activated earlier this turn uses up the discount") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, tezzeretName)
                .withCardOnBattlefield(1, "Mind Stone")
                .withCardOnBattlefield(1, "Millstone")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val mindStone = game.findPermanent("Mind Stone")!!
            val millstone = game.findPermanent("Millstone")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = mindStone,
                    abilityId = mindStoneManaAbility.id
                )
            ).error shouldBe null
            isTapped(game, mindStone) shouldBe true

            withClue("Millstone is no longer the first artifact ability: {2} can't be paid from one {C}") {
                activateMillstone(
                    game, game.player1Id, millstone, game.player2Id, PaymentStrategy.FromPool
                ).error.shouldNotBeNull()
            }
            isTapped(game, millstone) shouldBe false
            game.librarySize(2) shouldBe 2
        }

        test("an opponent's artifact activations aren't reduced") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(2, tezzeretName)
                .withCardOnBattlefield(1, "Millstone")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val millstone = game.findPermanent("Millstone")!!
            val islands = game.findAllPermanents("Island")

            activateMillstone(game, game.player1Id, millstone, game.player2Id).error shouldBe null
            withClue("Player1 doesn't control Tezzeret, so Millstone cost its full {2}") {
                islands.all { isTapped(game, it) } shouldBe true
            }
        }

        test("+1 draws two, then one artifact card satisfies the discard") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, tezzeretName)
                .withCardInHand(1, "Millstone")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val tezz = game.findPermanent(tezzeretName)!!

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = tezz, abilityId = loyaltyAbility(0).id)
            ).error shouldBe null
            game.resolveStack()

            loyalty(game, tezz) shouldBe 5
            game.handSize(1) shouldBe 3
            val decision = game.getPendingDecision() as SelectCardsDecision
            withClue("one artifact card or two cards") {
                decision.minSelections shouldBe 1
                decision.maxSelections shouldBe 2
            }
            val islandInHand = game.findCardsInHand(1, "Island").first()
            withClue("a single non-artifact card doesn't satisfy the discard") {
                game.selectCards(listOf(islandInHand)).error.shouldNotBeNull()
            }
            val millstoneInHand = game.findCardsInHand(1, "Millstone").single()
            game.selectCards(listOf(millstoneInHand)).error shouldBe null

            game.findCardsInGraveyard(1, "Millstone").size shouldBe 1
            game.handSize(1) shouldBe 2
            game.findCardsInHand(1, "Island").size shouldBe 2
        }

        test("−2 permanently makes a non-Vehicle artifact a 4/4 artifact creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, tezzeretName)
                .withCardOnBattlefield(1, "Millstone")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val tezz = game.findPermanent(tezzeretName)!!
            val millstone = game.findPermanent("Millstone")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = tezz,
                    abilityId = loyaltyAbility(1).id,
                    targets = listOf(ChosenTarget.Permanent(millstone))
                )
            ).error shouldBe null
            game.resolveStack()

            loyalty(game, tezz) shouldBe 2
            fun assertAnimated() {
                val projected = game.state.projectedState
                projected.isCreature(millstone) shouldBe true
                projected.hasType(millstone, "ARTIFACT") shouldBe true
                projected.getPower(millstone) shouldBe 4
                projected.getToughness(millstone) shouldBe 4
            }
            assertAnimated()

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("the −2 has no duration — Millstone is still a 4/4 on the next turn") {
                game.state.activePlayerId shouldBe game.player2Id
                assertAnimated()
            }
        }

        test("−2 on a Vehicle makes it an artifact creature with its printed power and toughness") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, tezzeretName)
                .withCardOnBattlefield(1, "Renegade Freighter")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val tezz = game.findPermanent(tezzeretName)!!
            val freighter = game.findPermanent("Renegade Freighter")!!
            game.state.projectedState.isCreature(freighter) shouldBe false

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = tezz,
                    abilityId = loyaltyAbility(1).id,
                    targets = listOf(ChosenTarget.Permanent(freighter))
                )
            ).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.isCreature(freighter) shouldBe true
            projected.hasType(freighter, "ARTIFACT") shouldBe true
            withClue("a Vehicle keeps its printed 4/3 rather than becoming 4/4") {
                projected.getPower(freighter) shouldBe 4
                projected.getToughness(freighter) shouldBe 3
            }
        }

        test("−6 emblem draws a card whenever an artifact you control becomes tapped") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, tezzeretName)
                .withCardOnBattlefield(1, "Millstone")
                .withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val tezz = game.findPermanent(tezzeretName)!!
            val millstone = game.findPermanent("Millstone")!!
            seedLoyalty(game, tezz, 6)

            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = tezz, abilityId = loyaltyAbility(2).id)
            ).error shouldBe null
            game.resolveStack()
            withClue("Tezzeret went to 0 loyalty and died; the emblem stays") {
                game.findPermanent(tezzeretName) shouldBe null
            }
            val handBefore = game.handSize(1)

            withClue("with Tezzeret gone, Millstone costs its full {2}") {
                activateMillstone(game, game.player1Id, millstone, game.player2Id).error shouldBe null
            }
            game.resolveStack()

            withClue("Millstone becoming tapped draws exactly one card; the tapped Islands aren't artifacts") {
                game.handSize(1) shouldBe handBefore + 1
            }
            game.librarySize(2) shouldBe 0
        }
    }
}
