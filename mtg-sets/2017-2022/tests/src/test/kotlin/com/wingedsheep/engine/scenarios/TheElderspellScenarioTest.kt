package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * The Elderspell (WAR #89, {B}{B}, Sorcery).
 *
 *   Destroy any number of target planeswalkers. Choose a planeswalker you control. Put two loyalty
 *   counters on it for each planeswalker destroyed this way.
 *
 * Pins the "destroyed this way" count — an indestructible target survives and isn't counted — and
 * the resolution-time choice of recipient, which may be that surviving target.
 */
class TheElderspellScenarioTest : ScenarioTestBase() {

    private fun loyalty(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

    /** Answer the "choose a planeswalker you control" prompt, whichever selection UI it uses. */
    private fun choose(game: TestGame, id: EntityId) {
        when (val pending = game.state.pendingDecision) {
            is SelectCardsDecision -> game.submitDecision(CardsSelectedResponse(pending.id, listOf(id)))
            is ChooseTargetsDecision -> game.submitDecision(TargetsResponse(pending.id, mapOf(0 to listOf(id))))
            else -> Unit
        }
    }

    init {
        context("The Elderspell") {

            test("destroys every target and puts two loyalty counters per kill on your planeswalker") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "The Elderspell")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Jace Beleren")
                    .withCardOnBattlefield(2, "Ajani Goldmane")
                    .withCardOnBattlefield(2, "Teferi, Hero of Dominaria")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jace = game.findPermanent("Jace Beleren")!!
                val startingLoyalty = loyalty(game, jace)
                val spell = game.findCardsInHand(1, "The Elderspell").single()
                val targets = listOf("Ajani Goldmane", "Teferi, Hero of Dominaria")
                    .map { ChosenTarget.Permanent(game.findPermanent(it)!!) }
                game.execute(CastSpell(game.player1Id, spell, targets)).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                choose(game, jace)

                game.isInGraveyard(2, "Ajani Goldmane") shouldBe true
                game.isInGraveyard(2, "Teferi, Hero of Dominaria") shouldBe true
                withClue("two planeswalkers destroyed -> four loyalty counters") {
                    loyalty(game, jace) shouldBe startingLoyalty + 4
                }
            }

            test("an indestructible target isn't counted, and the survivor can receive the counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "The Elderspell")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Avacyn, Angel of Hope")
                    .withCardOnBattlefield(1, "Teferi, Hero of Dominaria")
                    .withCardOnBattlefield(2, "Ajani Goldmane")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val teferi = game.findPermanent("Teferi, Hero of Dominaria")!!
                val startingLoyalty = loyalty(game, teferi)
                val spell = game.findCardsInHand(1, "The Elderspell").single()
                val targets = listOf(
                    ChosenTarget.Permanent(teferi),
                    ChosenTarget.Permanent(game.findPermanent("Ajani Goldmane")!!)
                )
                game.execute(CastSpell(game.player1Id, spell, targets)).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                choose(game, teferi)

                game.isInGraveyard(2, "Ajani Goldmane") shouldBe true
                withClue("Avacyn makes Teferi indestructible, so it survives") {
                    game.isOnBattlefield("Teferi, Hero of Dominaria") shouldBe true
                }
                withClue("only Ajani was destroyed -> two loyalty counters, not four") {
                    loyalty(game, teferi) shouldBe startingLoyalty + 2
                }
            }

            test("with no planeswalker of your own the targets are still destroyed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "The Elderspell")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(2, "Ajani Goldmane")
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spell = game.findCardsInHand(1, "The Elderspell").single()
                val ajani = ChosenTarget.Permanent(game.findPermanent("Ajani Goldmane")!!)
                game.execute(CastSpell(game.player1Id, spell, listOf(ajani))).error shouldBe null
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                game.isInGraveyard(2, "Ajani Goldmane") shouldBe true
                game.hasPendingDecision() shouldBe false
            }
        }
    }
}
