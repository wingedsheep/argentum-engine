package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Reef Worm (C14 #16) — dies into a 3/3 Fish, which dies into a 6/6 Whale, which dies into a
 * 9/9 Kraken. Each link's dies trigger lives on the token it was minted with, so the test walks
 * the whole chain and checks the Kraken is the end of it.
 */
class ReefWormScenarioTest : ScenarioTestBase() {

    init {
        context("Reef Worm — the Fish/Whale/Kraken chain") {

            test("each death creates the next, larger blue token, ending with a vanilla Kraken") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Reef Worm")
                    .withCardsInHand(1, "Doom Blade", 4)
                    .withLandsOnBattlefield(1, "Swamp", 8)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                fun killAndResolve(name: String) {
                    val victim = game.findPermanent(name)
                    withClue("$name is on the battlefield") { victim shouldNotBe null }
                    game.castSpell(1, "Doom Blade", victim!!).error shouldBe null
                    game.resolveStack()
                    withClue("$name died") { game.isOnBattlefield(name) shouldBe false }
                }

                fun checkToken(name: String, power: Int, toughness: Int) {
                    val id = game.findPermanent(name)
                    withClue("$name was created") { id shouldNotBe null }
                    val projected = game.state.projectedState
                    projected.getPower(id!!) shouldBe power
                    projected.getToughness(id) shouldBe toughness
                    projected.getColors(id).toList() shouldContainExactly listOf(Color.BLUE.name)
                    withClue("$name is controlled by the worm's controller") {
                        projected.getController(id) shouldBe game.player1Id
                    }
                }

                killAndResolve("Reef Worm")
                checkToken("Fish Token", 3, 3)

                killAndResolve("Fish Token")
                checkToken("Whale Token", 6, 6)

                killAndResolve("Whale Token")
                checkToken("Kraken Token", 9, 9)

                killAndResolve("Kraken Token")
                withClue("the Kraken has no dies trigger — the chain ends") {
                    game.state.getBattlefield().size shouldBe 8
                }
            }
        }
    }
}
