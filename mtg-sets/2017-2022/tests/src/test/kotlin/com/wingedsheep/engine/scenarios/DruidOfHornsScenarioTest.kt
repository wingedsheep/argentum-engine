package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Druid of Horns (M19 #176) — "Whenever you cast an Aura spell that targets this creature,
 * create a 3/3 green Beast creature token."
 *
 * Proves an Aura's enchant target reaches the `TargetsSource` cast predicate, and that the
 * Aura-spell filter and the targets-this requirement both gate the trigger.
 */
class DruidOfHornsScenarioTest : ScenarioTestBase() {

    init {
        test("casting an Aura on Druid of Horns creates a 3/3 Beast token") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Rancor")
                .withCardOnBattlefield(1, "Druid of Horns")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val druid = game.findPermanent("Druid of Horns")!!

            game.castSpell(1, "Rancor", druid).error shouldBe null
            game.resolveStack()

            val beast = game.findPermanents("Beast Token").single()
            game.state.projectedState.getPower(beast) shouldBe 3
            game.state.projectedState.getToughness(beast) shouldBe 3
            game.isOnBattlefield("Rancor") shouldBe true
        }

        test("an Aura cast on another creature does not trigger") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Rancor")
                .withCardOnBattlefield(1, "Druid of Horns")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Rancor", bears).error shouldBe null
            game.resolveStack()

            game.findPermanents("Beast Token").shouldBeEmpty()
        }

        test("a non-Aura spell targeting Druid of Horns does not trigger") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Giant Growth")
                .withCardOnBattlefield(1, "Druid of Horns")
                .withLandsOnBattlefield(1, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val druid = game.findPermanent("Druid of Horns")!!

            game.castSpell(1, "Giant Growth", druid).error shouldBe null
            game.resolveStack()

            game.findPermanents("Beast Token").shouldBeEmpty()
            game.findPermanents("Druid of Horns") shouldHaveSize 1
        }
    }
}
