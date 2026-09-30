package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Elesh Norn, Mother of Machines (ONE #10) — {4}{W} 4/7 Legendary Creature — Phyrexian Praetor.
 *
 * "Vigilance / If a permanent entering causes a triggered ability of a permanent you control to
 *  trigger, that ability triggers an additional time. / Permanents entering don't cause abilities of
 *  permanents your opponents control to trigger."
 *
 * Soul Warden on both sides tells the halves apart: the same entry doubles one Warden and silences
 * the other, and an opponent's creature's own enters trigger is silenced too.
 */
class EleshNornMotherOfMachinesScenarioTest : ScenarioTestBase() {

    init {
        test("your creature entering: your watcher triggers twice, the opponent's not at all") {
            val game = scenario()
                .withPlayers("Norn", "Opponent")
                .withCardOnBattlefield(1, "Elesh Norn, Mother of Machines")
                .withCardOnBattlefield(1, "Soul Warden")
                .withCardOnBattlefield(2, "Soul Warden")
                .withCardInHand(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            withClue("your Soul Warden triggered an additional time") { game.getLifeTotal(1) shouldBe 22 }
            withClue("the opponent's Soul Warden never triggered") { game.getLifeTotal(2) shouldBe 20 }
        }

        test("an opponent's creature entering: its own enters trigger is silenced, your watcher doubles") {
            val game = scenario()
                .withPlayers("Norn", "Opponent")
                .withCardOnBattlefield(1, "Elesh Norn, Mother of Machines")
                .withCardOnBattlefield(1, "Soul Warden")
                .withCardInHand(2, "Nightdrinker Moroii")
                .withLandsOnBattlefield(2, "Swamp", 4)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Nightdrinker Moroii").error shouldBe null
            game.resolveStack()

            withClue("the Moroii's \"you lose 3 life\" never triggered") { game.getLifeTotal(2) shouldBe 20 }
            withClue("your Soul Warden still triggered, twice") { game.getLifeTotal(1) shouldBe 22 }
        }

        test("without Norn, both watchers and the enters trigger fire once") {
            val game = scenario()
                .withPlayers("Norn", "Opponent")
                .withCardOnBattlefield(1, "Soul Warden")
                .withCardInHand(2, "Nightdrinker Moroii")
                .withLandsOnBattlefield(2, "Swamp", 4)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Nightdrinker Moroii").error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            game.getLifeTotal(1) shouldBe 21
        }
    }
}
