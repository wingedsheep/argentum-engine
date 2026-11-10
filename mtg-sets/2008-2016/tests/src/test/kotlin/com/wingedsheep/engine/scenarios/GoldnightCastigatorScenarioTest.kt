package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Goldnight Castigator (SOI #162) — {2}{R}{R} Creature — Angel 4/9, flying, haste.
 *
 *   If a source would deal damage to you, it deals double that damage to you instead.
 *   If a source would deal damage to this creature, it deals double that damage to this creature instead.
 *
 * The two replacements are scoped differently: "you" stacks across every Castigator you control
 * (two of them quadruple damage to you), while "this creature" only ever matches the Castigator
 * carrying it, so damage to one Castigator is doubled once no matter how many are in play.
 */
class GoldnightCastigatorScenarioTest : ScenarioTestBase() {

    init {
        context("Goldnight Castigator") {

            test("damage to its controller is doubled; damage to the opponent is not") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Goldnight Castigator")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
                withClue("Bolt's 3 damage to the Castigator's controller is doubled to 6") {
                    game.getLifeTotal(1) shouldBe 14
                }

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()
                withClue("damage to the opponent is untouched") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("damage to the Castigator itself is doubled") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Goldnight Castigator")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castigator = game.findPermanent("Goldnight Castigator")!!
                game.castSpell(2, "Lightning Bolt", castigator).error shouldBe null
                game.resolveStack()

                withClue("3 damage doubled to 6 marked on the 4/9") {
                    game.state.getEntity(castigator)?.get<DamageComponent>()?.amount shouldBe 6
                    game.isOnBattlefield("Goldnight Castigator") shouldBe true
                }
            }

            test("two Castigators quadruple damage to you but only double damage to one of them") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Goldnight Castigator")
                    .withCardOnBattlefield(1, "Goldnight Castigator")
                    .withCardInHand(2, "Lightning Bolt")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
                game.resolveStack()
                withClue("3 damage to you, doubled by each Castigator: 12") {
                    game.getLifeTotal(1) shouldBe 8
                }

                val (first, second) = game.findAllPermanents("Goldnight Castigator")
                game.castSpell(2, "Lightning Bolt", first).error shouldBe null
                game.resolveStack()
                withClue("only the damaged Castigator's own replacement applies: 6, not 12") {
                    game.state.getEntity(first)?.get<DamageComponent>()?.amount shouldBe 6
                    (game.state.getEntity(second)?.get<DamageComponent>()?.amount ?: 0) shouldBe 0
                    game.findAllPermanents("Goldnight Castigator").size shouldBe 2
                }
            }
        }
    }
}
