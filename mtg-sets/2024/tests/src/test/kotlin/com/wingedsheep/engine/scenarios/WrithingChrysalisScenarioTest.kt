package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Writhing Chrysalis (MH3 #208) — {2}{R}{G} Creature — Eldrazi Drone 2/3
 *
 *   Devoid
 *   When you cast this spell, create two 0/1 colorless Eldrazi Spawn creature tokens with
 *   "Sacrifice this token: Add {C}."
 *   Reach
 *   Whenever you sacrifice another Eldrazi, put a +1/+1 counter on this creature.
 */
class WrithingChrysalisScenarioTest : ScenarioTestBase() {

    /** Spawn-Gang Commander's "{1}{C}, Sacrifice an Eldrazi" is the sacrifice outlet. */
    private fun sacrificeBoard(fodder: String) = scenario()
        .withPlayers("Player1", "Opponent")
        .withCardOnBattlefield(1, "Writhing Chrysalis", summoningSickness = false)
        .withCardOnBattlefield(1, "Spawn-Gang Commander", summoningSickness = false)
        .withCardOnBattlefield(1, fodder, summoningSickness = false)
        .withLandsOnBattlefield(1, "Wastes", 1)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Writhing Chrysalis") {
            test("casting it creates two Eldrazi Spawn before it resolves; it has reach and is colorless") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Writhing Chrysalis")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Writhing Chrysalis").error shouldBe null
                game.state.stack shouldHaveSize 2

                game.passPriority()
                game.passPriority()
                game.findPermanents("Eldrazi Spawn") shouldHaveSize 2
                game.isOnBattlefield("Writhing Chrysalis") shouldBe false

                game.resolveStack()
                val chrysalis = game.findPermanent("Writhing Chrysalis")!!
                game.state.projectedState.hasKeyword(chrysalis, Keyword.REACH) shouldBe true
                game.state.projectedState.getColors(chrysalis).isEmpty() shouldBe true
            }

            test("sacrificing another Eldrazi puts a +1/+1 counter on it") {
                val game = sacrificeBoard("Nulldrifter")
                val chrysalis = game.findPermanent("Writhing Chrysalis")!!
                val abilityId = cardRegistry.getCard("Spawn-Gang Commander")!!.activatedAbilities[0].id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Spawn-Gang Commander")!!,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        costPayment = AdditionalCostPayment(
                            sacrificedPermanents = listOf(game.findPermanent("Nulldrifter")!!)
                        ),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("one counter: 3/4") {
                    game.state.projectedState.getPower(chrysalis) shouldBe 3
                    game.state.projectedState.getToughness(chrysalis) shouldBe 4
                }
                game.getLifeTotal(2) shouldBe 18
            }

            test("sacrificing the Chrysalis itself does not trigger it") {
                val game = sacrificeBoard("Grizzly Bears")
                val chrysalis = game.findPermanent("Writhing Chrysalis")!!
                val abilityId = cardRegistry.getCard("Spawn-Gang Commander")!!.activatedAbilities[0].id

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = game.findPermanent("Spawn-Gang Commander")!!,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Player(game.player2Id)),
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(chrysalis)),
                    )
                ).error shouldBe null
                withClue("only the Commander's ability is on the stack") {
                    game.state.stack shouldHaveSize 1
                }
                game.resolveStack()
                game.isInGraveyard(1, "Writhing Chrysalis") shouldBe true
            }
        }
    }
}
