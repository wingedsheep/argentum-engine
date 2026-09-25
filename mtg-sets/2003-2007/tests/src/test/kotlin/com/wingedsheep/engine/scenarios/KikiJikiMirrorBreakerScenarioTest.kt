package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.chk.cards.KikiJikiMirrorBreaker
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kiki-Jiki, Mirror Breaker (CHK #175) — "{T}: Create a token that's a copy of target nonlegendary
 * creature you control, except it has haste. Sacrifice it at the beginning of the next end step."
 */
class KikiJikiMirrorBreakerScenarioTest : ScenarioTestBase() {

    private val abilityId = KikiJikiMirrorBreaker.activatedAbilities.single().id

    init {
        context("Kiki-Jiki, Mirror Breaker") {

            test("copies a nonlegendary creature with haste, then sacrifices the token at the end step") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kiki-Jiki, Mirror Breaker", summoningSickness = true)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kiki = game.findPermanent("Kiki-Jiki, Mirror Breaker")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("Kiki-Jiki's own haste lets it tap the turn it arrives") {
                    game.execute(
                        ActivateAbility(
                            playerId = game.player1Id,
                            sourceId = kiki,
                            abilityId = abilityId,
                            targets = listOf(ChosenTarget.Permanent(bears)),
                        )
                    ).error shouldBe null
                }
                game.resolveStack()

                val token = game.findPermanents("Grizzly Bears").single { it != bears }
                withClue("the copy is a token with haste; the original has none") {
                    game.state.getEntity(token)?.has<TokenComponent>() shouldBe true
                    game.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe true
                    game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe false
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the token is sacrificed at the beginning of the end step") {
                    game.findPermanents("Grizzly Bears") shouldBe listOf(bears)
                }
            }

            test("a legendary creature is not a legal target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kiki-Jiki, Mirror Breaker")
                    .withCardOnBattlefield(1, "Keiga, the Tide Star")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val kiki = game.findPermanent("Kiki-Jiki, Mirror Breaker")!!
                val keiga = game.findPermanent("Keiga, the Tide Star")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = kiki,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(keiga)),
                    )
                ).error shouldNotBe null
            }
        }
    }
}
