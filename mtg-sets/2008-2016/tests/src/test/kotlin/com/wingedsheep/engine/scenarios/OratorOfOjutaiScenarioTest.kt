package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Orator of Ojutai — {1}{W} Creature — Bird Monk 0/4 (DTK #28).
 *
 * "As an additional cost to cast this spell, you may reveal a Dragon card from your hand. Defender,
 * flying. When this creature enters, if you revealed a Dragon card or controlled a Dragon as you cast
 * this spell, draw a card."
 *
 * Proves the "as you cast this spell" capture (CR 601.2i) reaches the permanent the spell becomes:
 * the controlled-Dragon answer is frozen at cast, so a Dragon destroyed while Orator is on the stack
 * still earns the card, while a blinked Orator and the token a copy of the spell becomes were never
 * cast and earn nothing.
 */
class OratorOfOjutaiScenarioTest : ScenarioTestBase() {

    private fun TestGame.castOratorRevealing(dragon: String): ExecutionResult {
        val orator = findCardsInHand(1, "Orator of Ojutai").single()
        val revealed = findCardsInHand(1, dragon).single()
        return execute(
            CastSpell(
                playerId = player1Id,
                cardId = orator,
                declaredCostSlot = ChoiceSlot.ADDITIONAL_COST_PAID,
                additionalCostPayment = AdditionalCostPayment(revealedCards = listOf(revealed)),
            )
        )
    }

    private fun board(
        dragonOnBattlefield: Boolean = false,
        dragonInHand: Boolean = false,
        casterExtras: List<String> = emptyList(),
    ): TestGame {
        var b = scenario()
            .withPlayers("Caster", "Opponent")
            .withCardInHand(1, "Orator of Ojutai")
            .withLandsOnBattlefield(1, "Plains", 4)
            .withLandsOnBattlefield(1, "Island", 1)
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withLandsOnBattlefield(1, "Forest", 1)
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(1, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (dragonOnBattlefield) b = b.withCardOnBattlefield(1, "Shivan Dragon")
        if (dragonInHand) b = b.withCardInHand(1, "Shivan Dragon")
        casterExtras.forEach { b = b.withCardInHand(1, it) }
        return b.build()
    }

    init {
        test("controlling a Dragon as you cast it draws a card") {
            val game = board(dragonOnBattlefield = true)
            val before = game.handSize(1)

            game.castSpell(1, "Orator of Ojutai").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Orator of Ojutai") shouldBe true
            withClue("Orator left the hand, then the trigger drew one") { game.handSize(1) shouldBe before }
            val orator = game.findPermanent("Orator of Ojutai").shouldNotBeNull()
            game.state.getEntity(orator)?.get<CastChoicesComponent>()?.castTimeValues?.get("controlledDragon") shouldBe 1
        }

        test("revealing a Dragon card from hand draws a card, and the revealed card stays in hand") {
            val game = board(dragonInHand = true)
            val before = game.handSize(1)

            game.castOratorRevealing("Shivan Dragon").error shouldBe null
            game.resolveStack()

            game.handSize(1) shouldBe before
            game.isInHand(1, "Shivan Dragon") shouldBe true
        }

        test("no Dragon revealed or controlled: no card") {
            val game = board(dragonInHand = true)
            val before = game.handSize(1)

            game.castSpell(1, "Orator of Ojutai").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Orator of Ojutai") shouldBe true
            game.handSize(1) shouldBe before - 1
        }

        test("the Dragon dying while Orator is on the stack still draws — control is judged as you cast") {
            val game = board(dragonOnBattlefield = true, casterExtras = listOf("Go for the Throat"))
            val shivan = game.findPermanent("Shivan Dragon").shouldNotBeNull()
            val before = game.handSize(1)

            game.castSpell(1, "Orator of Ojutai").error shouldBe null
            withClue("holding priority, the caster destroys their own Dragon in response") {
                game.castSpell(1, "Go for the Throat", shivan).error shouldBe null
            }
            game.resolveStack()

            game.isInGraveyard(1, "Shivan Dragon") shouldBe true
            game.isOnBattlefield("Orator of Ojutai") shouldBe true
            game.handSize(1) shouldBe before - 1
        }

        test("bounced in response to its trigger: still draws — how it was cast is fixed (CR 603.4)") {
            val game = board(dragonOnBattlefield = true, casterExtras = listOf("Unsummon"))
            val before = game.handSize(1)

            game.castSpell(1, "Orator of Ojutai").error shouldBe null
            game.passPriority()
            game.passPriority()
            val orator = game.findPermanent("Orator of Ojutai").shouldNotBeNull()
            withClue("the enters trigger is waiting on the stack") { game.state.stack.size shouldBe 1 }
            game.castSpell(1, "Unsummon", orator).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Orator of Ojutai") shouldBe true
            withClue("Orator and Unsummon left, Orator came back, the trigger drew one") {
                game.handSize(1) shouldBe before
            }
        }

        test("blinked later, it enters without having been cast: no card (CR 400.7)") {
            val game = board(dragonOnBattlefield = true, casterExtras = listOf("Go for the Throat", "Cloudshift"))
            val shivan = game.findPermanent("Shivan Dragon").shouldNotBeNull()

            game.castSpell(1, "Orator of Ojutai").error shouldBe null
            game.resolveStack()
            game.castSpell(1, "Go for the Throat", shivan).error shouldBe null
            game.resolveStack()
            val before = game.handSize(1)

            val orator = game.findPermanent("Orator of Ojutai").shouldNotBeNull()
            game.castSpell(1, "Cloudshift", orator).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Orator of Ojutai") shouldBe true
            withClue("only Cloudshift left the hand; the returned Orator has no cast-time capture") {
                game.handSize(1) shouldBe before - 1
            }
        }

        test("a copy of the spell wasn't cast: only the original earns the controlled-Dragon card") {
            val game = board(dragonOnBattlefield = true, casterExtras = listOf("Double Major"))
            val before = game.handSize(1)

            game.castSpell(1, "Orator of Ojutai").error shouldBe null
            game.castSpellTargetingStackSpell(1, "Double Major", "Orator of Ojutai").error shouldBe null
            game.resolveStack()

            game.findAllPermanents("Orator of Ojutai").size shouldBe 2
            withClue("Orator and Double Major left the hand; one card drawn, by the original") {
                game.handSize(1) shouldBe before - 1
            }
        }

        test("the reveal variant is offered") {
            val game = board(dragonInHand = true)
            game.getLegalActions(1).filter { it.actionType == "CastWithKicker" }.map { it.description } shouldContain
                "Cast Orator of Ojutai (Reveal a Dragon in your hand)"
        }
    }
}
