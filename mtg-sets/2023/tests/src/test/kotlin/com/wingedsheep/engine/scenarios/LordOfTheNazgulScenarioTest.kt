package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.RingBearerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Lord of the Nazgûl (LTC #60) — "Wraiths you control have protection from Ring-bearers." and
 * "Whenever you cast an instant or sorcery spell, create a 3/3 black Wraith creature token with
 * menace. Then if you control nine or more Wraiths, Wraiths you control have base power and
 * toughness 9/9 until end of turn."
 *
 * The protection is `GrantProtectionFromToGroup(ProtectionScope.RingBearers)`: a Ring-bearer can't
 * block a Wraith and deals it no damage, while the same creature without the designation can.
 */
class LordOfTheNazgulScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Wraith",
                manaCost = ManaCost.parse("{1}{B}"),
                subtypes = setOf(Subtype("Wraith")),
                power = 2,
                toughness = 2
            )
        )

        fun ringBearerBoard(bearer: Boolean): TestGame {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Lord of the Nazgûl")
                .withCardOnBattlefield(1, "Test Wraith")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            if (bearer) {
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(bears) { it.with(RingBearerComponent(game.player2Id)) }
            }
            return game
        }

        context("protection from Ring-bearers") {

            test("a Ring-bearer can't block a Wraith you control") {
                val game = ringBearerBoard(bearer = true)
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Test Wraith" to 2)).outcome shouldBe Outcome.Done
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("the Ring-bearer Bears can't block the Wraith") {
                    game.declareBlockers(mapOf("Grizzly Bears" to listOf("Test Wraith"))).outcome shouldNotBe Outcome.Done
                }
            }

            test("the same creature without the designation can block") {
                val game = ringBearerBoard(bearer = false)
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Test Wraith" to 2)).outcome shouldBe Outcome.Done
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Test Wraith"))).outcome shouldBe Outcome.Done
            }

            test("damage a Ring-bearer would deal to a Wraith is prevented") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lord of the Nazgûl")
                    .withCardOnBattlefield(1, "Test Wraith")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(bears) { it.with(RingBearerComponent(game.player2Id)) }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).outcome shouldBe Outcome.Done
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Test Wraith" to listOf("Grizzly Bears"))).outcome shouldBe Outcome.Done
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                withClue("the Wraith takes no damage from the Ring-bearer and survives") {
                    game.isOnBattlefield("Test Wraith") shouldBe true
                }
                withClue("the Ring-bearer still takes the Wraith's damage") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }
        }

        context("instant or sorcery trigger") {

            test("casting a sorcery creates a 3/3 menace Wraith, no 9/9 below nine Wraiths") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lord of the Nazgûl")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInHand(1, "Divination")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Divination").outcome shouldBe Outcome.Done
                game.resolveStack()

                val wraith = game.wraithTokens().single()
                game.state.projectedState.getPower(wraith) shouldBe 3
                game.state.projectedState.getToughness(wraith) shouldBe 3
                val lord = game.findPermanent("Lord of the Nazgûl")!!
                game.state.projectedState.getPower(lord) shouldBe 4
            }

            test("with nine Wraiths, every Wraith you control becomes a base 9/9") {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Lord of the Nazgûl")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInHand(1, "Divination")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                repeat(7) { builder = builder.withCardOnBattlefield(1, "Test Wraith") }
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Divination").outcome shouldBe Outcome.Done
                game.resolveStack()

                val wraiths = game.findPermanents("Test Wraith") + game.wraithTokens() +
                    game.findPermanent("Lord of the Nazgûl")!!
                wraiths.size shouldBe 9
                wraiths.forEach { id ->
                    withClue("Wraith $id is 9/9") {
                        game.state.projectedState.getPower(id) shouldBe 9
                        game.state.projectedState.getToughness(id) shouldBe 9
                    }
                }
                withClue("a non-Wraith is untouched") {
                    game.state.projectedState.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 2
                }
            }
        }
    }
}

/** The Wraith tokens on the battlefield — the triggered ability's 3/3s. */
private fun ScenarioTestBase.TestGame.wraithTokens(): List<EntityId> =
    state.getBattlefield().filter { id ->
        state.getEntity(id)?.has<TokenComponent>() == true &&
            state.projectedState.getSubtypes(id).contains("Wraith")
    }
