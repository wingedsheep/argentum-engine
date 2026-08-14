package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.soi.cards.JaceUnravelerOfSecrets
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Jace, Unraveler of Secrets (SOI #69; {3}{U}{U}, Loyalty 5).
 *
 *   +1: Scry 1, then draw a card.
 *   −2: Return target creature to its owner's hand.
 *   −8: You get an emblem with "Whenever an opponent casts their first spell each turn, counter
 *       that spell."
 *
 * The −8 is what needed proving: the emblem is a permanent global triggered ability whose trigger
 * counts spells *per caster per turn*, so an opponent's first spell is countered and their second
 * one the same turn resolves — which is also why a first spell that can't be countered still eats
 * the turn's trigger.
 */
class JaceUnravelerOfSecretsScenarioTest : ScenarioTestBase() {

    private val minusTwo = JaceUnravelerOfSecrets.activatedAbilities[1].id
    private val minusEight = JaceUnravelerOfSecrets.activatedAbilities[2].id

    private val jolt = card("Test Jolt") {
        manaCost = "{U}"
        colorIdentity = "U"
        typeLine = "Instant"
        spell { effect = Effects.GainLife(3) }
    }

    init {
        cardRegistry.register(jolt)

        context("Jace, Unraveler of Secrets") {

            test("−2 returns target creature to its owner's hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jace, Unraveler of Secrets")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jace = game.findPermanent("Jace, Unraveler of Secrets")!!
                seedLoyalty(game, jace, 5)
                val bears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = jace,
                        abilityId = minusTwo,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.findPermanent("Grizzly Bears") shouldBe null
                game.handSize(2) shouldBe 1
                loyalty(game, jace) shouldBe 3
            }

            test("the −8 emblem counters an opponent's first spell each turn but not their second") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jace, Unraveler of Secrets")
                    .withCardsInHand(2, "Test Jolt", 2)
                    .withLandsOnBattlefield(2, "Island", 4)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jace = game.findPermanent("Jace, Unraveler of Secrets")!!
                seedLoyalty(game, jace, 8)

                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = jace, abilityId = minusEight)
                ).error shouldBe null
                game.resolveStack()
                withClue("the ultimate produced the emblem") {
                    game.state.globalGrantedTriggeredAbilities.size shouldBe 1
                }

                // Opponent's first spell this turn: the emblem trigger counters it.
                game.passPriority()
                game.castSpell(2, "Test Jolt").error shouldBe null
                game.resolveStack()
                withClue("countered — no life gained") { game.state.lifeTotal(game.player2Id) shouldBe 20 }

                // Their second spell the same turn: no trigger, so it resolves.
                game.passPriority()
                game.castSpell(2, "Test Jolt").error shouldBe null
                game.resolveStack()
                withClue("second spell of the turn resolves") { game.state.lifeTotal(game.player2Id) shouldBe 23 }
            }

            test("the emblem leaves the controller's own spells alone") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Jace, Unraveler of Secrets")
                    .withCardInHand(1, "Test Jolt")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jace = game.findPermanent("Jace, Unraveler of Secrets")!!
                seedLoyalty(game, jace, 8)
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = jace, abilityId = minusEight)
                ).error shouldBe null
                game.resolveStack()

                game.castSpell(1, "Test Jolt").error shouldBe null
                game.resolveStack()
                game.state.lifeTotal(game.player1Id) shouldBe 23
            }
        }
    }
}

/**
 * The scenario builder drops permanents straight onto the battlefield without running the
 * planeswalker's enters-with-loyalty rider, so loyalty is seeded explicitly.
 */
private fun seedLoyalty(game: ScenarioTestBase.TestGame, id: EntityId, amount: Int) {
    game.state = game.state.updateEntity(id) { c ->
        c.with(CountersComponent().withAdded(CounterType.LOYALTY, amount))
    }
}

private fun loyalty(game: ScenarioTestBase.TestGame, id: EntityId): Int =
    game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0
