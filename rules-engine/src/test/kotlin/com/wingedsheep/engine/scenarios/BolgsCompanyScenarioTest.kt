package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bolg's Company (HOB) — {B}{R} Creature — Goblin Soldier 2/2
 *
 * This creature has haste as long as you control another Goblin.
 * {T}, Sacrifice another Goblin: Add {B}{R}.
 *
 * Pins the two halves that could silently misbehave: the conditional keyword must key off a
 * *different* Goblin (not itself), and the mana ability must be a real mana ability producing one
 * black and one red — not two of one color.
 */
class BolgsCompanyScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Bolg's Company") {

            test("no other Goblin: the haste clause is off") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bolg's Company")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bolg = game.findPermanent("Bolg's Company")!!
                withClue("A non-Goblin on the battlefield must not switch haste on") {
                    projector.project(game.state).hasKeyword(bolg, Keyword.HASTE) shouldBe false
                }
            }

            test("another Goblin you control switches haste on") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bolg's Company")
                    .withCardOnBattlefield(1, "Goblin Guide")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bolg = game.findPermanent("Bolg's Company")!!
                withClue("Another Goblin you control grants haste") {
                    projector.project(game.state).hasKeyword(bolg, Keyword.HASTE) shouldBe true
                }
            }

            test("an opponent's Goblin does not switch haste on") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bolg's Company")
                    .withCardOnBattlefield(2, "Goblin Guide")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bolg = game.findPermanent("Bolg's Company")!!
                withClue("The clause reads \"you control\", so an opposing Goblin is irrelevant") {
                    projector.project(game.state).hasKeyword(bolg, Keyword.HASTE) shouldBe false
                }
            }

            test("{T}, Sacrifice another Goblin: adds one black and one red") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bolg's Company")
                    .withCardOnBattlefield(1, "Goblin Guide")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bolg = game.findPermanent("Bolg's Company")!!
                val fodder = game.findPermanent("Goblin Guide")!!
                val ability = cardRegistry.getCard("Bolg's Company")!!.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = bolg,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder))
                    )
                )
                withClue("Activating the mana ability should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                withClue("The sacrificed Goblin has left the battlefield") {
                    game.isOnBattlefield("Goblin Guide") shouldBe false
                }

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>()
                withClue("Exactly {B}{R} lands in the pool") {
                    (pool?.black ?: 0) shouldBe 1
                    (pool?.red ?: 0) shouldBe 1
                }
            }

            test("the sacrifice must be another Goblin, not Bolg's Company itself") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Bolg's Company")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bolg = game.findPermanent("Bolg's Company")!!
                val ability = cardRegistry.getCard("Bolg's Company")!!.script.activatedAbilities[0]

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = bolg,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bolg))
                    )
                )
                withClue("\"another Goblin\" excludes the source") {
                    (result.error != null) shouldBe true
                }
                withClue("Bolg's Company survives a rejected activation") {
                    game.isOnBattlefield("Bolg's Company") shouldBe true
                }
            }
        }
    }
}
