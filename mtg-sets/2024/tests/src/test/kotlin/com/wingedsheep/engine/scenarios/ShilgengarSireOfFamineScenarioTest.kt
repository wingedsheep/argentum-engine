package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Shilgengar, Sire of Famine — {3}{B}{B} Legendary Creature — Elder Demon 6/6
 *
 * Flying
 * Sacrifice another creature: Create a Blood token. If you sacrificed an Angel this way, create a
 * number of Blood tokens equal to its toughness instead.
 * {W/B}{W/B}{W/B}, Sacrifice six Blood tokens: Return each creature card from your graveyard to the
 * battlefield with a finality counter on it. Those creatures are Vampires in addition to their
 * other types.
 */
class ShilgengarSireOfFamineScenarioTest : ScenarioTestBase() {

    private val shilgengar get() = cardRegistry.getCard("Shilgengar, Sire of Famine")!!

    init {
        context("Shilgengar, Sire of Famine") {

            fun sacrificeFor(victimName: String): TestGame {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Shilgengar, Sire of Famine")
                    .withCardOnBattlefield(1, victimName)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Shilgengar, Sire of Famine")!!,
                        abilityId = shilgengar.activatedAbilities[0].id,
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(game.findPermanent(victimName)!!)
                        )
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()
                game.isInGraveyard(1, victimName) shouldBe true
                return game
            }

            test("sacrificing a non-Angel creates one Blood token") {
                val game = sacrificeFor("Grizzly Bears")
                game.findPermanents("Blood").size shouldBe 1
            }

            test("sacrificing an Angel creates Blood tokens equal to its toughness instead") {
                val game = sacrificeFor("Youthful Valkyrie") // 1/3: toughness, not power
                game.findPermanents("Blood").size shouldBe 3
            }

            test("sacrificing six Blood tokens returns each creature card as a Vampire with a finality counter") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Shilgengar, Sire of Famine")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Serra Angel")
                    .withCardInGraveyard(1, "Swamp")
                    .withCardInGraveyard(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(6) { builder.withCardOnBattlefield(1, "Blood", isToken = true) }
                val game = builder.build()

                val bloods = game.findPermanents("Blood")
                bloods.size shouldBe 6

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Shilgengar, Sire of Famine")!!,
                        abilityId = shilgengar.activatedAbilities[1].id,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = bloods)
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                game.findPermanents("Blood").size shouldBe 0

                val projected = game.state.projectedState
                for ((name, ownType) in listOf("Grizzly Bears" to "Bear", "Serra Angel" to "Angel")) {
                    val id = game.findPermanent(name)
                    withClue("$name returns to the battlefield") { (id != null) shouldBe true }
                    val counters = game.state.getEntity(id!!)?.get<CountersComponent>()?.counters ?: emptyMap()
                    withClue("$name has a finality counter") { counters[CounterType.FINALITY] shouldBe 1 }
                    withClue("$name is a Vampire") { projected.hasSubtype(id, "Vampire") shouldBe true }
                    withClue("$name keeps its other types") { projected.hasSubtype(id, ownType) shouldBe true }
                }

                withClue("noncreature cards stay in the graveyard") { game.isInGraveyard(1, "Swamp") shouldBe true }
                withClue("only your graveyard is returned") { game.isInGraveyard(2, "Centaur Courser") shouldBe true }
            }
        }
    }
}
