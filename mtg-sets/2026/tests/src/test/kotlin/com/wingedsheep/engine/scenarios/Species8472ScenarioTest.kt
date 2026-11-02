package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Species 8472 — {4}{G}{G} 7/5.
 *  - Strange new worlds — ETB: gain life equal to the number of differently named lands you control.
 *  - Can't be blocked by artifact creatures.
 *  - Can't be the target of abilities from artifact sources your opponents control.
 *
 * The targeting clause is the opponents-only form of `CantBeTargetedBySourceTypeAbilities`: an
 * opponent's artifact ability can't target it, but its controller's own artifact ability and an
 * opponent's non-artifact ability can.
 */
class Species8472ScenarioTest : ScenarioTestBase() {

    private val rodAbilityId by lazy { cardRegistry.getCard("Rod of Ruin")!!.activatedAbilities[0].id }
    private val sorcererAbilityId by lazy { cardRegistry.getCard("Prodigal Sorcerer")!!.activatedAbilities[0].id }

    init {
        context("can't be the target of abilities from artifact sources your opponents control") {
            test("an opponent's artifact ability can't target it; an opponent's non-artifact ability can") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Species 8472", summoningSickness = false)
                    .withCardOnBattlefield(2, "Rod of Ruin")
                    .withCardOnBattlefield(2, "Prodigal Sorcerer", summoningSickness = false)
                    .withLandsOnBattlefield(2, "Mountain", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val species = game.findPermanent("Species 8472")!!
                val rodAttempt = game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = game.findPermanent("Rod of Ruin")!!,
                        abilityId = rodAbilityId,
                        targets = listOf(ChosenTarget.Permanent(species))
                    )
                )
                withClue("an opponent's Rod of Ruin (artifact source) must not target Species 8472") {
                    rodAttempt.error shouldNotBe null
                }

                val sorcererAttempt = game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = game.findPermanent("Prodigal Sorcerer")!!,
                        abilityId = sorcererAbilityId,
                        targets = listOf(ChosenTarget.Permanent(species))
                    )
                )
                withClue("an opponent's Prodigal Sorcerer (non-artifact) can target it: ${sorcererAttempt.error}") {
                    sorcererAttempt.error shouldBe null
                }
            }

            test("its controller's own artifact ability can target it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Species 8472", summoningSickness = false)
                    .withCardOnBattlefield(1, "Rod of Ruin")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val species = game.findPermanent("Species 8472")!!
                val attempt = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Rod of Ruin")!!,
                        abilityId = rodAbilityId,
                        targets = listOf(ChosenTarget.Permanent(species))
                    )
                )
                withClue("your own Rod of Ruin can target Species 8472: ${attempt.error}") {
                    attempt.error shouldBe null
                }
            }
        }

        context("Strange new worlds") {
            test("entering gains life equal to the number of differently named lands you control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Species 8472")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val before = game.getLifeTotal(1)
                val cast = game.castSpell(1, "Species 8472")
                withClue("Species 8472 should cast: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("Forest, Plains and Island are three differently named lands") {
                    game.getLifeTotal(1) shouldBe before + 3
                }
            }
        }
    }
}
