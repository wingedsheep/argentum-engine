package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.SaddledComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Kolodin, Triumph Caster (DFT #210).
 *
 * Kolodin, Triumph Caster {R}{W} — Legendary Creature — Human Pilot 2/3
 * Mounts and Vehicles you control have haste.
 * Whenever a Mount you control enters, it becomes saddled until end of turn.
 * Whenever a Vehicle you control enters, it becomes an artifact creature until end of turn.
 *
 * The two triggers key on the *entering* permanent ("it"), so what's being pinned is that each one
 * fires for the right card type and applies to the entrant rather than to Kolodin. The haste lord
 * covers both types under one static.
 */
class KolodinTriumphCasterScenarioTest : ScenarioTestBase() {

    init {
        context("Kolodin, Triumph Caster") {

            test("a Mount entering becomes saddled, and the marker clears at end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kolodin, Triumph Caster")
                    .withCardInHand(1, "Brightfield Glider") // {W} Creature — Possum Mount
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Brightfield Glider").error shouldBe null
                game.resolveStack()

                val glider = game.findPermanent("Brightfield Glider")!!
                withClue("the entering Mount is saddled by Kolodin's trigger") {
                    game.state.getEntity(glider)!!.get<SaddledComponent>().shouldNotBeNull()
                }
                withClue("BecomeSaddled is a marker only — the Mount keeps its printed 1/1") {
                    game.state.projectedState.getPower(glider) shouldBe 1
                    game.state.projectedState.getToughness(glider) shouldBe 1
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("\"until end of turn\" — the saddled marker is cleared in cleanup") {
                    game.state.getEntity(glider)!!.get<SaddledComponent>() shouldBe null
                }
            }

            test("a Vehicle entering becomes an artifact creature for the turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kolodin, Triumph Caster")
                    .withCardInHand(1, "Air Response Unit") // {2}{W} Artifact — Vehicle 3/3
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Air Response Unit").error shouldBe null
                game.resolveStack()

                val unit = game.findPermanent("Air Response Unit")!!
                val projected = game.state.projectedState
                withClue("the Vehicle is animated at its printed P/T and stays an artifact") {
                    projected.isCreature(unit) shouldBe true
                    projected.hasType(unit, "ARTIFACT") shouldBe true
                    projected.getPower(unit) shouldBe 3
                    projected.getToughness(unit) shouldBe 3
                }
                withClue("no saddled marker — a Vehicle isn't a Mount, so only one trigger fired") {
                    game.state.getEntity(unit)!!.get<SaddledComponent>() shouldBe null
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("the animation is until end of turn, not permanent") {
                    game.state.projectedState.isCreature(unit) shouldBe false
                }
            }

            test("the haste lord covers Mounts and Vehicles you control but not other creatures") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kolodin, Triumph Caster")
                    .withCardOnBattlefield(1, "Brightfield Glider") // Mount
                    .withCardOnBattlefield(1, "Air Response Unit") // Vehicle
                    .withCardOnBattlefield(1, "Grizzly Bears") // neither
                    .withCardOnBattlefield(2, "Brightfield Glider") // an opponent's Mount
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val projected = game.state.projectedState
                val yourGlider = game.findAllPermanents("Brightfield Glider")
                    .single { projected.getController(it) == game.player1Id }
                val theirGlider = game.findAllPermanents("Brightfield Glider")
                    .single { projected.getController(it) == game.player2Id }

                withClue("your Mount and Vehicle have haste") {
                    projected.hasKeyword(yourGlider, Keyword.HASTE) shouldBe true
                    projected.hasKeyword(game.findPermanent("Air Response Unit")!!, Keyword.HASTE) shouldBe true
                }
                withClue("a plain creature you control is not a Mount or Vehicle") {
                    projected.hasKeyword(game.findPermanent("Grizzly Bears")!!, Keyword.HASTE) shouldBe false
                }
                withClue("the lord is scoped to permanents YOU control") {
                    projected.hasKeyword(theirGlider, Keyword.HASTE) shouldBe false
                }
                withClue("Kolodin is a Human Pilot — it grants haste but doesn't have it") {
                    projected.hasKeyword(
                        game.findPermanent("Kolodin, Triumph Caster")!!,
                        Keyword.HASTE
                    ) shouldBe false
                }
            }
        }
    }
}
