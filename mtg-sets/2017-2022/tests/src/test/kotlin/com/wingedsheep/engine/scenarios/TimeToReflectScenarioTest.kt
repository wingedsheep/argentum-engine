package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Time to Reflect — "Exile target creature that blocked or was blocked by a Zombie this turn."
 * The Zombie partner is read as it stood when the block was declared, so it still counts after it
 * has died in that combat.
 */
class TimeToReflectScenarioTest : ScenarioTestBase() {

    init {
        test("exiles a creature that blocked a Zombie, after the Zombie has died") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Scathe Zombies") // 2/2 Zombie attacker
                .withCardOnBattlefield(2, "Hill Giant")     // 3/3 blocker survives
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInHand(1, "Time to Reflect")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Scathe Zombies" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hill Giant" to listOf("Scathe Zombies")))
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.isInGraveyard(1, "Scathe Zombies") shouldBe true
            game.castSpell(1, "Time to Reflect", game.findPermanent("Hill Giant")!!).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Hill Giant") shouldBe true
        }

        test("exiles a creature that was blocked by a Zombie") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Scathe Zombies")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInHand(1, "Time to Reflect")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Grizzly Bears" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Scathe Zombies" to listOf("Grizzly Bears")))
            game.passPriority()

            game.castSpell(1, "Time to Reflect", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Grizzly Bears") shouldBe true
        }

        test("can't target a creature that only fought a non-Zombie") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInHand(1, "Time to Reflect")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Grizzly Bears" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears")))
            game.passPriority()

            game.castSpell(1, "Time to Reflect", game.findPermanent("Hill Giant")!!).error shouldNotBe null
        }
    }
}
