package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Kaalia of the Vast (CMD #206) — {1}{R}{W}{B} Legendary Human Cleric, 2/2, flying.
 *
 * "Whenever Kaalia of the Vast attacks an opponent, you may put an Angel, Demon, or Dragon
 *  creature card from your hand onto the battlefield tapped and attacking that opponent."
 *
 * This is the demonstration card for engine feature #1258: the trigger is gated on
 * `Triggers.AttacksAnOpponent` (SELF + `AttackPredicate.DefenderIsPlayer`), so per Kaalia's
 * 2024-06-07 ruling it fires only when she attacks a *player* — not a planeswalker (or battle).
 *
 * - attacks a player → the trigger fires; "you may put an Angel, Demon, or Dragon creature card"
 *   is the `Patterns.Hand.putFromHand(entersAttacking = true)` ChooseUpTo-1 selection over
 *   `Creature.withAnySubtype("Angel", "Demon", "Dragon")`; the chosen card enters tapped and
 *   attacking. A non-Angel/Demon/Dragon creature card is not a selectable option.
 * - attacks a planeswalker → the trigger must NOT fire, so no hand card is put in.
 */
class KaaliaOfTheVastScenarioTest : ScenarioTestBase() {

    init {
        // An eligible Dragon creature card to be cheated in when Kaalia attacks a player.
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Dragon",
                manaCost = ManaCost.parse("{4}{R}{R}"),
                subtypes = setOf(Subtype("Dragon")),
                power = 5,
                toughness = 5
            )
        )
        // An ineligible creature card (Bear — not Angel/Demon/Dragon) to prove the subtype filter.
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Bear",
                manaCost = ManaCost.parse("{1}{G}"),
                subtypes = setOf(Subtype("Bear")),
                power = 2,
                toughness = 2
            )
        )

        context("Kaalia of the Vast") {

            test("attacks a player: may put an Angel/Demon/Dragon from hand tapped and attacking") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kaalia of the Vast", summoningSickness = false)
                    .withCardInHand(1, "Test Dragon") // Dragon: eligible
                    .withCardInHand(1, "Test Bear")   // Bear: NOT eligible
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                // Attack the opponent player (player 2).
                game.declareAttackers(mapOf("Kaalia of the Vast" to 2)).error shouldBe null

                // The attack-a-player trigger goes on the stack; resolving it asks the controller
                // to choose up to one eligible creature card from hand.
                game.resolveStack()
                withClue("attack-a-player trigger paused for the hand-card selection") {
                    game.hasPendingDecision() shouldBe true
                }

                // Only the Dragon qualifies; the Bear must not be a selectable option. Select it.
                val dragon = game.findCardsInHand(1, "Test Dragon").single()
                game.selectCards(listOf(dragon)).error shouldBe null
                game.resolveStack()

                val put = game.findPermanents("Test Dragon").singleOrNull()
                    ?: error("Test Dragon should be on the battlefield once, found ${game.findPermanents("Test Dragon").size}")
                withClue("the put creature enters tapped") {
                    game.state.getEntity(put)?.has<TappedComponent>() shouldBe true
                }
                withClue("the put creature enters attacking") {
                    game.state.getEntity(put)?.has<AttackingComponent>() shouldBe true
                }
                withClue("Test Bear (not Angel/Demon/Dragon) stayed in hand — not eligible") {
                    game.isInHand(1, "Test Bear") shouldBe true
                }
            }

            test("attacks a planeswalker: the trigger does NOT fire (2024 ruling)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kaalia of the Vast", summoningSickness = false)
                    .withCardOnBattlefield(2, "Sorin, Solemn Visitor") // opponent planeswalker
                    .withCardInHand(1, "Test Dragon")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Seed the planeswalker's loyalty so an SBA doesn't remove it before combat.
                val sorin = game.findPermanent("Sorin, Solemn Visitor")!!
                game.state = game.state.updateEntity(sorin) { container ->
                    container.with(CountersComponent().withAdded(CounterType.LOYALTY, 4))
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                // Attack the planeswalker, not the player.
                game.declareAttackersWithPermanentTargets(
                    permanentAttackers = mapOf("Kaalia of the Vast" to "Sorin, Solemn Visitor")
                ).error shouldBe null
                game.resolveStack()

                withClue("attacking a planeswalker must NOT trigger Kaalia's ability") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("no creature was cheated onto the battlefield") {
                    game.findPermanents("Test Dragon").size shouldBe 0
                }
                withClue("Test Dragon remains in hand") {
                    game.isInHand(1, "Test Dragon") shouldBe true
                }
            }
        }
    }
}
