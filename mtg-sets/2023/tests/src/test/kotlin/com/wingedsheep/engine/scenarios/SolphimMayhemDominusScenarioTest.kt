package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.SolphimMayhemDominus
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Solphim, Mayhem Dominus (ONE #150) — {2}{R}{R} 5/4 Legendary Creature — Phyrexian Horror.
 *
 *   If a source you control would deal noncombat damage to an opponent or a permanent an opponent
 *   controls, it deals double that damage to that player or permanent instead.
 *   {1}{R/P}{R/P}, Discard two cards: Put an indestructible counter on Solphim.
 */
class SolphimMayhemDominusScenarioTest : ScenarioTestBase() {

    private val abilityId = SolphimMayhemDominus.activatedAbilities.first().id

    private fun TestGame.damageOn(name: String): Int =
        state.getEntity(findPermanent(name)!!)?.get<DamageComponent>()?.amount ?: 0

    private fun baseScenario() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Solphim, Mayhem Dominus")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("noncombat damage to an opponent is doubled") {
            val game = baseScenario()
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .build()

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()

            withClue("Bolt's 3 doubled to 6") { game.getLifeTotal(2) shouldBe 14 }
        }

        test("noncombat damage to a permanent an opponent controls is doubled, not to your own") {
            val game = baseScenario()
                .withCardOnBattlefield(2, "Force of Nature")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .build()

            val theirs = game.findPermanent("Force of Nature")!!
            game.castSpell(1, "Lightning Bolt", theirs).error shouldBe null
            game.resolveStack()
            withClue("6 damage kills their 5/5") { game.isInGraveyard(2, "Force of Nature") shouldBe true }

            val solphim = game.findPermanent("Solphim, Mayhem Dominus")!!
            game.castSpell(1, "Lightning Bolt", solphim).error shouldBe null
            game.resolveStack()
            withClue("your own permanent takes the undoubled 3") {
                game.damageOn("Solphim, Mayhem Dominus") shouldBe 3
            }
        }

        test("noncombat damage to yourself is not doubled") {
            val game = baseScenario()
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .build()

            game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 17
        }

        test("an opponent's source is not doubled") {
            val game = baseScenario()
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .build()

            // Player 2 is Solphim's controller's opponent, so only the source clause keeps this at 3.
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
        }

        test("combat damage is not doubled") {
            val game = baseScenario().build()
            val solphim = game.findPermanent("Solphim, Mayhem Dominus")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.execute(DeclareAttackers(game.player1Id, mapOf(solphim to game.player2Id))).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareNoBlockers().error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("5 combat damage, undoubled") { game.getLifeTotal(2) shouldBe 15 }
        }

        test("discarding two cards puts an indestructible counter on Solphim") {
            val game = baseScenario()
                .withCardInHand(1, "Grizzly Bears")
                .withCardInHand(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .build()
            val solphim = game.findPermanent("Solphim, Mayhem Dominus")!!
            val discards = game.state.getHand(game.player1Id)

            val act = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = solphim,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(discardedCards = discards),
                )
            )
            withClue("activation: ${act.error}") { act.error shouldBe null }
            game.handSize(1) shouldBe 0
            game.resolveStack()

            game.state.getEntity(solphim)?.get<CountersComponent>()?.getCount(CounterType.INDESTRUCTIBLE) shouldBe 1
        }

        test("can't activate with fewer than two cards to discard") {
            val game = baseScenario()
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .build()
            val solphim = game.findPermanent("Solphim, Mayhem Dominus")!!

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = solphim,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(discardedCards = game.state.getHand(game.player1Id)),
                )
            ).error shouldNotBe null
        }
    }
}
