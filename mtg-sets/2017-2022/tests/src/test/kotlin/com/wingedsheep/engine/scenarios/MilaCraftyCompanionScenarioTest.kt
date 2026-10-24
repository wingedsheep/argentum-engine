package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Mila, Crafty Companion // Lukka, Wayward Bonder (STX #153) — the first modal DFC whose back face
 * is a planeswalker.
 *
 * Pins down:
 *  1. Cast as its back face from hand, Lukka enters with its own printed loyalty (5) — the
 *     starting-loyalty entry counters must come off the back face, not the creature front.
 *  2. Mila's attack trigger fires only when a planeswalker you control is attacked, and puts a
 *     loyalty counter on *every* planeswalker you control (ruling), not just the attacked one.
 *  3. Mila's targeting trigger offers a draw when an opponent's spell targets your permanent.
 *  4. Lukka's +1 draws two when the discarded card was a creature card.
 */
class MilaCraftyCompanionScenarioTest : ScenarioTestBase() {

    private fun TestGame.loyaltyOf(id: EntityId): Int =
        state.getEntity(id)!!.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    init {
        test("cast as Lukka, Wayward Bonder, it enters with its printed loyalty of 5") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Mila, Crafty Companion")
                .withLandsOnBattlefield(1, "Mountain", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val cardId = game.findCardsInHand(1, "Mila, Crafty Companion").first()

            val result = game.execute(
                CastSpell(
                    game.player1Id,
                    cardId,
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.MODAL_BACK_FACE
                )
            )
            withClue("the back face is castable for {4}{R}{R}") { result.error shouldBe null }
            game.resolveStack()

            withClue("it resolved back face up") {
                game.isOnBattlefield("Mila, Crafty Companion") shouldBe false
            }
            val lukka = game.findPermanent("Lukka, Wayward Bonder").shouldNotBeNull()
            withClue("Lukka's starting loyalty comes from the back face") {
                game.loyaltyOf(lukka) shouldBe 5
            }
        }

        test("an opponent attacking one of your planeswalkers puts a loyalty counter on each of them") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Mila, Crafty Companion")
                .withCardOnBattlefield(1, "Karn, Living Legacy")
                .withCardOnBattlefield(1, "Kasmina, Enigma Sage")
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            val karn = game.findPermanent("Karn, Living Legacy")!!
            val kasmina = game.findPermanent("Kasmina, Enigma Sage")!!
            val karnBefore = game.loyaltyOf(karn)
            val kasminaBefore = game.loyaltyOf(kasmina)

            game.declareAttackersWithPermanentTargets(
                permanentAttackers = mapOf("Grizzly Bears" to "Karn, Living Legacy")
            ).error shouldBe null
            game.resolveStack()

            withClue("the attacked planeswalker gets a loyalty counter") {
                game.loyaltyOf(karn) shouldBe karnBefore + 1
            }
            withClue("so does the planeswalker that wasn't attacked (ruling)") {
                game.loyaltyOf(kasmina) shouldBe kasminaBefore + 1
            }
        }

        test("an attack aimed only at you, not a planeswalker, doesn't trigger Mila") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Mila, Crafty Companion")
                .withCardOnBattlefield(1, "Karn, Living Legacy")
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()
            val karn = game.findPermanent("Karn, Living Legacy")!!
            val karnBefore = game.loyaltyOf(karn)

            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.resolveStack()

            game.loyaltyOf(karn) shouldBe karnBefore
        }

        test("an opponent's spell targeting your permanent lets you draw a card") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Mila, Crafty Companion")
                .withCardInLibrary(1, "Forest")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val mila = game.findPermanent("Mila, Crafty Companion")!!
            val handBefore = game.handSize(1)

            game.castSpell(2, "Shock", mila).error shouldBe null
            game.resolveStack()

            withClue("the may-draw is offered before Shock resolves") {
                game.hasPendingDecision() shouldBe true
            }
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe handBefore + 1
            withClue("Shock still resolves; a 2/3 survives 2 damage") {
                game.isOnBattlefield("Mila, Crafty Companion") shouldBe true
            }
        }

        test("Lukka's +1 draws two cards when a creature card is discarded") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Lukka, Wayward Bonder")
                .withCardInHand(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val lukka = game.findPermanent("Lukka, Wayward Bonder")!!
            val plusOne = cardRegistry.getCard("Lukka, Wayward Bonder")!!.script.activatedAbilities[0].id
            val bears = game.findCardsInHand(1, "Grizzly Bears").first()

            game.execute(ActivateAbility(game.player1Id, lukka, plusOne)).error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("the creature card was discarded") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            }
            withClue("and two cards were drawn instead of one") {
                game.handSize(1) shouldBe 2
            }
        }
    }
}
