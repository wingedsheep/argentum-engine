package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.snc.cards.ObNixilisTheAdversary
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Ob Nixilis, the Adversary (SNC #206; {1}{B}{R}, Loyalty 3).
 *
 *   Casualty X. The copy isn't legendary and has starting loyalty X.
 *   +1: Each opponent loses 2 life unless they discard a card. If you control a Demon or Devil,
 *       you gain 2 life.
 *   −2: Create a 1/1 red Devil creature token with "When this creature dies, it deals 1 damage to
 *       any target."
 *   −7: Target player draws seven cards and loses 7 life.
 */
class ObNixilisTheAdversaryScenarioTest : ScenarioTestBase() {

    private val plusOne = ObNixilisTheAdversary.activatedAbilities[0].id
    private val minusTwo = ObNixilisTheAdversary.activatedAbilities[1].id
    private val minusSeven = ObNixilisTheAdversary.activatedAbilities[2].id

    init {
        context("Ob Nixilis, the Adversary") {

            test("casualty X: sacrificing a 2-power creature leaves a nonlegendary token copy with loyalty 2") {
                val game = castWithCasualty()

                val walkers = game.findAllPermanents("Ob Nixilis, the Adversary")
                withClue("the original and its copy are both on the battlefield") { walkers.size shouldBe 2 }
                val copy = walkers.single { game.state.getEntity(it)?.has<TokenComponent>() == true }
                val original = walkers.single { it != copy }
                game.state.getEntity(copy)!!.get<CardComponent>()!!.typeLine.isLegendary shouldBe false
                game.state.getEntity(original)!!.get<CardComponent>()!!.typeLine.isLegendary shouldBe true
                loyalty(game, copy) shouldBe 2
                loyalty(game, original) shouldBe 3
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }

            test("the copy's −2 makes a Devil, so the original's +1 drains 2 and gains 2") {
                val game = castWithCasualty()
                val walkers = game.findAllPermanents("Ob Nixilis, the Adversary")
                val copy = walkers.single { game.state.getEntity(it)?.has<TokenComponent>() == true }
                val original = walkers.single { it != copy }

                game.execute(ActivateAbility(game.player1Id, copy, minusTwo)).error shouldBe null
                game.resolveStack()
                game.findPermanent("Devil Token") shouldNotBe null
                withClue("the copy had 2 loyalty and paid 2 — it's put into the graveyard") {
                    game.findAllPermanents("Ob Nixilis, the Adversary").size shouldBe 1
                }

                game.execute(ActivateAbility(game.player1Id, original, plusOne)).error shouldBe null
                game.resolveStack()

                withClue("the opponent has no card to discard, so they lose 2 life") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("you control a Devil, so you gain 2 life") { game.getLifeTotal(1) shouldBe 22 }
                loyalty(game, original) shouldBe 4
            }

            test("−7: target player draws seven cards and loses 7 life") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Ob Nixilis, the Adversary")
                    .apply { repeat(8) { withCardInLibrary(2, "Swamp") } }
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val ob = game.findPermanent("Ob Nixilis, the Adversary")!!
                game.state = game.state.updateEntity(ob) { c ->
                    c.with(CountersComponent().withAdded(CounterType.LOYALTY, 7))
                }

                game.execute(
                    ActivateAbility(
                        game.player1Id, ob, minusSeven,
                        targets = listOf(ChosenTarget.Player(game.player2Id))
                    )
                ).error shouldBe null
                game.resolveStack()

                game.handSize(2) shouldBe 7
                game.getLifeTotal(2) shouldBe 13
            }
        }
    }

    /** Casts Ob Nixilis, sacrificing Grizzly Bears to casualty X (X = its power, 2), and resolves it. */
    private fun castWithCasualty(): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Ob Nixilis, the Adversary")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withLandsOnBattlefield(1, "Mountain", 1)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val ob = game.findCardsInHand(1, "Ob Nixilis, the Adversary").single()
        val bears = game.findPermanent("Grizzly Bears")!!

        game.execute(
            CastSpell(
                playerId = game.player1Id,
                cardId = ob,
                paymentStrategy = PaymentStrategy.AutoPay,
                casualtyCreature = bears
            )
        ).error shouldBe null
        game.resolveStack()
        return game
    }

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0
}
