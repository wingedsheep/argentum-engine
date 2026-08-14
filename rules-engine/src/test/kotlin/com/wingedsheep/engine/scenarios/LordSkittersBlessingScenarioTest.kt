package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/** Scenario tests for Lord Skitter's Blessing. */
class LordSkittersBlessingScenarioTest : ScenarioTestBase() {

    private fun power(game: TestGame, id: EntityId): Int? = game.state.projectedState.getPower(id)
    private fun toughness(game: TestGame, id: EntityId): Int? = game.state.projectedState.getToughness(id)

    private fun auraOn(game: TestGame, auraName: String, host: EntityId): EntityId? =
        game.findPermanents(auraName).firstOrNull { aura ->
            game.state.getEntity(aura)?.get<AttachedToComponent>()?.targetId == host
        }

    init {
        context("Lord Skitter's Blessing") {
            test("the enters trigger attaches a Wicked Role to the targeted creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Lord Skitter's Blessing")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()

                game.castSpell(1, "Lord Skitter's Blessing").error shouldBe null
                game.resolveStack()

                // The Role's target belongs to the *enters* trigger, so it is chosen when that
                // trigger goes on the stack — after the enchantment itself has resolved.
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("a Wicked Role token is attached to the Bears") {
                    auraOn(game, "Wicked Role", bears).shouldNotBeNull()
                }
                withClue("Wicked Role grants +1/+1 — the 2/2 Bears projects as 3/3") {
                    power(game, bears) shouldBe 3
                    toughness(game, bears) shouldBe 3
                }
            }

            test("the draw step fires when you control an enchanted creature: -1 life, +1 card") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lord Skitter's Blessing")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Pacifism", "Grizzly Bears")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    // Not turn 1 — the starting player skips their first draw step (CR 103.7a),
                    // which would hide the turn-based draw this test counts against.
                    .withTurnNumber(3)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                repeat(10) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(10) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val handBefore = game.handSize(1)

                game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
                game.resolveStack()

                withClue("you lose 1 life") { game.getLifeTotal(1) shouldBe 19 }
                withClue("the turn-based draw plus the additional card — two cards this draw step") {
                    game.handSize(1) shouldBe handBefore + 2
                }
            }

            test("with nothing enchanted the intervening 'if' suppresses the whole ability") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lord Skitter's Blessing")
                    // A creature, but no Aura on it.
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .withTurnNumber(3)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                repeat(10) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(10) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val handBefore = game.handSize(1)

                game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
                game.resolveStack()

                withClue("no life lost — the ability never went on the stack") {
                    game.getLifeTotal(1) shouldBe 20
                }
                withClue("only the turn-based draw") { game.handSize(1) shouldBe handBefore + 1 }
            }
        }
    }
}
