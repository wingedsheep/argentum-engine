package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Distinguished Conjurer (J22 #4, reprinted MH3 #264) — {1}{W} Creature — Human Wizard, 1/2.
 *
 *   Whenever another creature you control enters, you gain 1 life.
 *   {4}{W}, {T}: Exile another target creature you control, then return it to the
 *   battlefield under its owner's control.
 *
 * The blinked creature re-entering triggers the Conjurer's own lifegain (per ruling).
 */
class DistinguishedConjurerScenarioTest : ScenarioTestBase() {

    init {
        context("Distinguished Conjurer") {

            test("blinking another creature returns it and the re-entry gains 1 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Distinguished Conjurer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val conjurer = game.findPermanent("Distinguished Conjurer")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val abilityId = cardRegistry.getCard("Distinguished Conjurer")!!
                    .activatedAbilities.first().id

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = conjurer,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                )
                withClue("Activation should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                val returned = game.findPermanent("Grizzly Bears")
                withClue("Grizzly Bears is back on the battlefield") { returned shouldNotBe null }
                withClue("Conjurer is tapped by its {T} cost") {
                    game.state.getEntity(conjurer)?.has<TappedComponent>() shouldBe true
                }
                withClue("Re-entry of another creature gains 1 life") {
                    game.getLifeTotal(1) shouldBe 21
                }
            }

            test("Conjurer cannot target itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Distinguished Conjurer", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val conjurer = game.findPermanent("Distinguished Conjurer")!!
                val abilityId = cardRegistry.getCard("Distinguished Conjurer")!!
                    .activatedAbilities.first().id

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = conjurer,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(conjurer))
                    )
                )
                withClue("Targeting itself must be rejected") {
                    activation.error shouldNotBe null
                }
            }

            test("an opponent's creature entering does not gain life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Distinguished Conjurer")
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears")
                game.resolveStack()

                withClue("Bears entered for Player2") {
                    game.findPermanent("Grizzly Bears") shouldNotBe null
                }
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
