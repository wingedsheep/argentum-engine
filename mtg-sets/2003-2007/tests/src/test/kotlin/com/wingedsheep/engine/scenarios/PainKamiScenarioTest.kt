package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.PainKami
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Pain Kami (CHK #183) — "{X}{R}, Sacrifice this creature: It deals X damage to target creature."
 *
 * The Kami is gone by the time the ability resolves, so the damage has to come from its
 * last-known information rather than fizzling for want of a source.
 */
class PainKamiScenarioTest : ScenarioTestBase() {

    private val abilityId = PainKami.activatedAbilities.single().id

    init {
        context("Pain Kami") {

            test("sacrificed, it deals X damage to the target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pain Kami")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kami = game.findPermanent("Pain Kami")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = kami,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                        xValue = 2,
                    )
                )
                withClue("activation should succeed: ${activation.error}") { activation.error shouldBe null }

                withClue("the Kami is sacrificed as a cost") {
                    game.isInGraveyard(1, "Pain Kami") shouldBe true
                }
                game.resolveStack()

                withClue("X = 2 kills the 2/2") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("with X = 1 the damage is marked but not lethal") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Pain Kami")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kami = game.findPermanent("Pain Kami")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = kami,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                        xValue = 1,
                    )
                ).error shouldBe null
                game.resolveStack()

                game.findPermanent("Grizzly Bears") shouldNotBe null
            }
        }
    }
}
