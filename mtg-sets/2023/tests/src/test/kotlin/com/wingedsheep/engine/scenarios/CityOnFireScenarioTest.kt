package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * City on Fire (MOM #135) — "Convoke. If a source you control would deal damage to a permanent or
 * player, it deals triple that damage instead."
 */
class CityOnFireScenarioTest : ScenarioTestBase() {

    private fun board(block: ScenarioBuilder.() -> ScenarioBuilder = { this }) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "City on Fire")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .block()
        .build()

    init {
        test("a spell you control deals triple damage to a player") {
            val game = board {
                withCardInHand(1, "Lightning Bolt").withLandsOnBattlefield(1, "Mountain", 1)
            }
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 11
        }

        test("combat damage from a creature you control triples") {
            val game = board { withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) }
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(2) shouldBe 14
        }

        test("damage to a permanent triples too, so Shock kills a 5-toughness creature") {
            val game = board {
                withCardInHand(1, "Shock").withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Craw Wurm")
            }
            val wurm = game.findPermanent("Craw Wurm")!!
            game.castSpell(1, "Shock", targetId = wurm).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Craw Wurm") shouldBe false
        }

        test("an opponent's source is not tripled") {
            val game = board {
                withCardInHand(2, "Lightning Bolt").withLandsOnBattlefield(2, "Mountain", 1)
            }
            game.passPriority()
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 17
        }

        test("stacks multiplicatively with a doubler — Twinflame Tyrant makes it six times") {
            val game = board {
                withCardOnBattlefield(1, "Twinflame Tyrant")
                    .withCardInHand(1, "Shock").withLandsOnBattlefield(1, "Mountain", 1)
            }
            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.resolveStack()

            withClue("2 damage × 3 × 2") { game.getLifeTotal(2) shouldBe 8 }
        }
    }
}
