package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Wedding Invitation (VOW #260) — {2} Artifact.
 *
 *   When this artifact enters, draw a card.
 *   {T}, Sacrifice this artifact: Target creature can't be blocked this turn. If it's a Vampire,
 *   it also gains lifelink until end of turn.
 *
 * Exercises the ETB draw, the tap+sacrifice activated ability granting can't-be-blocked, and the
 * conditional bonus lifelink when the target is a Vampire.
 */
class WeddingInvitationScenarioTest : ScenarioTestBase() {

    private val abilityId = cardRegistry.getCard("Wedding Invitation")!!.activatedAbilities.first().id

    init {
        context("Wedding Invitation") {

            test("entering the battlefield draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Wedding Invitation")
                    .withCardInLibrary(1, "Plains")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Wedding Invitation").error shouldBe null
                game.resolveStack()

                withClue("Wedding Invitation resolved onto the battlefield") {
                    game.isOnBattlefield("Wedding Invitation") shouldBe true
                }
                withClue("a card was drawn from the library") {
                    game.isInHand(1, "Plains") shouldBe true
                }
            }

            test("tap, sacrifice: a non-Vampire creature can't be blocked, no lifelink") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wedding Invitation", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val invitation = game.findPermanent("Wedding Invitation")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = invitation,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                    )
                )
                withClue("Activating Wedding Invitation should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("Wedding Invitation is sacrificed") {
                    game.isOnBattlefield("Wedding Invitation") shouldBe false
                }
                withClue("Grizzly Bears can't be blocked this turn") {
                    game.state.projectedState.hasKeyword(bears, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
                }
                withClue("Grizzly Bears is not a Vampire, so it does not gain lifelink") {
                    game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe false
                }
            }

            test("tap, sacrifice: a Vampire creature can't be blocked and also gains lifelink") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Wedding Invitation", summoningSickness = false)
                    .withCardOnBattlefield(1, "Bloodcrazed Socialite") // {3}{B} Vampire, 3/3
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val invitation = game.findPermanent("Wedding Invitation")!!
                val vampire = game.findPermanent("Bloodcrazed Socialite")!!

                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = invitation,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(vampire)),
                    )
                )
                withClue("Activating Wedding Invitation should succeed: ${result.error}") {
                    result.error shouldBe null
                }
                game.resolveStack()

                withClue("the Vampire can't be blocked this turn") {
                    game.state.projectedState.hasKeyword(vampire, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
                }
                withClue("the Vampire also gains lifelink until end of turn") {
                    game.state.projectedState.hasKeyword(vampire, Keyword.LIFELINK) shouldBe true
                }
            }
        }
    }
}
