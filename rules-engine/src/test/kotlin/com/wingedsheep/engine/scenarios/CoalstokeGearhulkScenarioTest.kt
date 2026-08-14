package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Coalstoke Gearhulk — "When this creature enters, put target creature card with mana value 4 or
 * less from a graveyard onto the battlefield under your control with a finality counter on it.
 * That creature gains menace, deathtouch, and haste. At the beginning of your next end step, exile
 * that creature."
 *
 * Five chained effects behind one trigger, so the parts that can silently go wrong are:
 *
 * - the target reads **a** graveyard, not only yours;
 * - mana value 4 or less actually restricts the candidate set;
 * - the finality counter rides along on the zone change rather than being bolted on afterwards;
 * - the three keyword grants land on the *reanimated* creature, not on the Gearhulk;
 * - the delayed trigger fires on the controller's own end step and exiles that creature.
 */
class CoalstokeGearhulkScenarioTest : ScenarioTestBase() {

    init {
        test("reanimates an opponent's creature under your control, with a finality counter and the three keywords") {
            val game = gearhulkGame()
            val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()

            game.castSpell(1, "Coalstoke Gearhulk").error shouldBe null
            game.autoPayIfAsked()
            game.resolveStack()

            withClue("The enters trigger asks for its target") {
                game.hasPendingDecision() shouldBe true
            }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("It left the opponent's graveyard for the battlefield") {
                game.findPermanent("Grizzly Bears") shouldNotBe null
                game.isInGraveyard(2, "Grizzly Bears") shouldBe false
            }
            val reanimated = game.findPermanent("Grizzly Bears")!!
            withClue("\"under your control\" — the caster controls it, not its owner") {
                game.state.getEntity(reanimated)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
            }
            withClue("It entered with the finality counter") {
                game.state.getEntity(reanimated)?.get<CountersComponent>()
                    ?.getCount(CounterType.FINALITY) shouldBe 1
            }
            withClue("Menace, deathtouch and haste are granted to the reanimated creature") {
                val projected = game.state.projectedState
                projected.hasKeyword(reanimated, Keyword.MENACE) shouldBe true
                projected.hasKeyword(reanimated, Keyword.DEATHTOUCH) shouldBe true
                projected.hasKeyword(reanimated, Keyword.HASTE) shouldBe true
            }
        }

        test("the delayed trigger exiles the reanimated creature at your end step") {
            val game = gearhulkGame()
            val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()

            game.castSpell(1, "Coalstoke Gearhulk").error shouldBe null
            game.autoPayIfAsked()
            game.resolveStack()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("Still around during the turn it was reanimated") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("Exiled at the beginning of the controller's end step — not put into a graveyard") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInExile(2, "Grizzly Bears") shouldBe true
            }
        }

        test("a creature card with mana value 5 is not a legal target") {
            val game = gearhulkGame(graveyardCreature = "Serra Angel")

            game.castSpell(1, "Coalstoke Gearhulk").error shouldBe null
            game.autoPayIfAsked()
            game.resolveStack()

            withClue("Serra Angel costs {3}{W}{W} — mana value 5, above the trigger's ceiling, so " +
                "the trigger has no legal target and is removed rather than pausing for one") {
                game.hasPendingDecision() shouldBe false
                game.isInGraveyard(2, "Serra Angel") shouldBe true
            }
        }
    }

    /**
     * Coalstoke Gearhulk in hand with exactly {1}{B}{B}{R}{R} available, and one creature card
     * sitting in the *opponent's* graveyard.
     */
    private fun gearhulkGame(graveyardCreature: String = "Grizzly Bears"): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Coalstoke Gearhulk")
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withLandsOnBattlefield(1, "Mountain", 2)
            .withCardInGraveyard(2, graveyardCreature)
        repeat(12) {
            builder.withCardInLibrary(1, "Grizzly Bears")
            builder.withCardInLibrary(2, "Grizzly Bears")
        }
        val game = builder
            .withActivePlayer(1)
            .inPhase(Phase.BEGINNING, Step.UPKEEP)
            .build()
        game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        return game
    }

    /** Submit auto-pay if the engine paused for a mana-source decision. */
    private fun TestGame.autoPayIfAsked() {
        if (getPendingDecision() is com.wingedsheep.engine.core.SelectManaSourcesDecision) {
            submitManaSourcesAutoPay()
        }
    }
}
