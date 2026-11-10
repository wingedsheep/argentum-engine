package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Natural State — {G} Instant. "Destroy target artifact or enchantment with mana value 3 or less."
 */
class NaturalStateScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Natural State")
            .withLandsOnBattlefield(1, "Forest", 1)
            .withCardOnBattlefield(2, "Ornithopter")
            .withCardOnBattlefield(2, "Juggernaut")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("destroys an artifact with mana value 3 or less") {
            val game = board()
            val thopter = game.findPermanent("Ornithopter")!!

            val cast = game.castSpell(1, "Natural State", thopter)
            withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            game.isOnBattlefield("Ornithopter") shouldBe false
            game.isInGraveyard(2, "Ornithopter") shouldBe true
        }

        test("an artifact with mana value 4 isn't a legal target") {
            val game = board()
            val juggernaut = game.findPermanent("Juggernaut")!!

            game.castSpell(1, "Natural State", juggernaut).error shouldNotBe null
            game.isOnBattlefield("Juggernaut") shouldBe true
        }
    }
}
