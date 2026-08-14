package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mrd.cards.WeldingJar
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Welding Jar (MRD #274) — {0} Artifact, "Sacrifice this artifact: Regenerate target artifact."
 *
 * Regeneration on a permanent that isn't a creature is the part worth proving: the shield has to be
 * consulted at the destroy chokepoint for any permanent type, and the CR 701.15a replacement still
 * taps what it saves.
 */
class WeldingJarScenarioTest : ScenarioTestBase() {

    private val regenerateAbility = WeldingJar.activatedAbilities.single().id

    init {
        context("Welding Jar") {

            test("regenerates a targeted artifact out of a destroy effect, tapping it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Welding Jar")
                    .withCardOnBattlefield(1, "Frogmite") // 2/2 Artifact Creature
                    .withCardInHand(1, "Deconstruct")     // {2}{G}: Destroy target artifact
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val jar = game.findPermanent("Welding Jar")!!
                val frogmite = game.findPermanent("Frogmite")!!

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = jar,
                        abilityId = regenerateAbility,
                        targets = listOf(ChosenTarget.Permanent(frogmite))
                    )
                )
                withClue("activation should succeed: ${activation.error}") { activation.error shouldBe null }
                game.resolveStack()

                withClue("the Jar sacrificed itself to pay") { game.findPermanent("Welding Jar") shouldBe null }

                game.castSpell(1, "Deconstruct", frogmite).error shouldBe null
                game.resolveStack()

                withClue("the shield replaces the destruction — Frogmite survives, tapped") {
                    game.findPermanent("Frogmite") shouldNotBe null
                    game.state.getEntity(frogmite)?.has<TappedComponent>() shouldBe true
                }
            }

            test("without the shield the same destroy effect kills the artifact") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Frogmite")
                    .withCardInHand(1, "Deconstruct")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val frogmite = game.findPermanent("Frogmite")!!
                game.castSpell(1, "Deconstruct", frogmite).error shouldBe null
                game.resolveStack()

                withClue("control case — nothing shields it") { game.findPermanent("Frogmite") shouldBe null }
            }
        }
    }
}
