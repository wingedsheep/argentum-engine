package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Ogre Battlecaster (J22 #36, {2}{R} 3/3 first strike).
 *
 *   Whenever this creature attacks, you may cast target instant or sorcery card from your
 *   graveyard by paying {R}{R} in addition to its other costs. If that spell would be put into a
 *   graveyard, exile it instead. When you cast that spell, this creature gets +X/+0 until end of
 *   turn, where X is that spell's mana value.
 *
 * Pins the additional mana cost of a resolution-time cast: the spell costs its mana cost plus
 * {R}{R}, a caster who can't pay the total casts nothing, and the reflexive pump fires only when
 * the spell was actually cast.
 */
class OgreBattlecasterScenarioTest : ScenarioTestBase() {

    private fun battlecasterGame(mountains: Int) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Ogre Battlecaster", summoningSickness = false)
        .withCardInGraveyard(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Mountain", mountains)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.attackAndTargetBolt() {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Ogre Battlecaster" to 2)).error shouldBe null
        val bolt = findCardsInGraveyard(1, "Lightning Bolt").single()
        if (hasPendingDecision()) selectTargets(listOf(bolt))
        resolveStack()
    }

    private fun TestGame.battlecasterPower(): Int =
        state.projectedState.getPower(findPermanent("Ogre Battlecaster")!!)!!

    init {
        context("Ogre Battlecaster") {

            test("casting the graveyard spell costs its mana cost plus {R}{R}, exiles it, and pumps by its mana value") {
                val game = battlecasterGame(mountains = 3)
                game.attackAndTargetBolt()

                game.answerYesNo(true)
                // Lightning Bolt's own target, chosen as it's cast.
                game.selectTargets(listOf(game.player2Id))
                game.resolveStack()

                withClue("All three Mountains paid {R} + {R}{R}") {
                    game.findPermanents("Mountain").all { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
                }
                withClue("Bolt resolved against the defending player") {
                    game.getLifeTotal(2) shouldBe 17
                }
                withClue("The spell was exiled instead of going to the graveyard") {
                    game.isInExile(1, "Lightning Bolt") shouldBe true
                    game.findCardsInGraveyard(1, "Lightning Bolt").size shouldBe 0
                }
                withClue("Battlecaster gets +1/+0 for Bolt's mana value") {
                    game.battlecasterPower() shouldBe 4
                }
            }

            test("a caster who can't pay the additional {R}{R} casts nothing and gets no pump") {
                val game = battlecasterGame(mountains = 2)
                game.attackAndTargetBolt()

                game.answerYesNo(true)
                game.resolveStack()

                withClue("Bolt stays in the graveyard — {R} + {R}{R} needs three red") {
                    game.findCardsInGraveyard(1, "Lightning Bolt").size shouldBe 1
                    game.getLifeTotal(2) shouldBe 20
                }
                withClue("No lands were tapped for a cast that never happened") {
                    game.findPermanents("Mountain").none { game.state.getEntity(it)?.has<TappedComponent>() == true } shouldBe true
                }
                withClue("No cast, no reflexive pump") {
                    game.battlecasterPower() shouldBe 3
                }
            }

            test("declining the cast leaves the card in the graveyard and no pump") {
                val game = battlecasterGame(mountains = 3)
                game.attackAndTargetBolt()

                game.answerYesNo(false)
                game.resolveStack()

                game.findCardsInGraveyard(1, "Lightning Bolt").size shouldBe 1
                game.battlecasterPower() shouldBe 3
            }
        }
    }
}
