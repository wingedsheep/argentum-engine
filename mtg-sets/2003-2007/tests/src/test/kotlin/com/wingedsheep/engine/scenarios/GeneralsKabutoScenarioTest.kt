package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * General's Kabuto (CHK #251)
 * {4} Artifact — Equipment
 * Equipped creature has shroud.
 * Prevent all combat damage that would be dealt to equipped creature.
 * Equip {2}
 */
class GeneralsKabutoScenarioTest : ScenarioTestBase() {

    private fun markedDamage(game: TestGame, name: String): Int =
        game.state.getEntity(game.findPermanent(name)!!)?.get<DamageComponent>()?.amount ?: 0

    init {
        test("combat damage dealt to the equipped creature is prevented") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardAttachedTo(1, "General's Kabuto", "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("Hill Giant's 3 combat damage to the equipped Bears must be prevented") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                markedDamage(game, "Grizzly Bears") shouldBe 0
            }
            withClue("the equipped creature's own combat damage is not prevented") {
                markedDamage(game, "Hill Giant") shouldBe 2
            }
        }

        test("the equipped creature has shroud and can't be targeted") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "General's Kabuto", "Grizzly Bears")
                .withCardInHand(2, "Shock")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val result = game.castSpell(2, "Shock", game.findPermanent("Grizzly Bears"))
            result.error shouldNotBe null
        }

        test("noncombat damage to the equipped creature is not prevented") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "General's Kabuto", "Grizzly Bears")
                .withCardInHand(2, "Pyroclasm")
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Pyroclasm").error shouldBe null
            game.resolveStack()

            withClue("Pyroclasm's 2 noncombat damage kills the equipped 2/2") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }
        }
    }
}
