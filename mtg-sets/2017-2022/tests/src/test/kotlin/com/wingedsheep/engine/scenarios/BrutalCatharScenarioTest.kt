package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.daynight.DayNightService
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.DayNight
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Brutal Cathar // Moonrage Brute (MID #7).
 *
 * The exile lasts "until this creature leaves the battlefield" — transforming isn't leaving, so the
 * exiled creature stays gone through a day/night flip and comes back when the permanent leaves on
 * either face. Transforming back into Brutal Cathar triggers a fresh exile.
 */
class BrutalCatharScenarioTest : ScenarioTestBase() {

    init {
        test("the exile survives turning into Moonrage Brute and ends when the Brute leaves") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Brutal Cathar")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.state = game.state.copy(dayNight = DayNight.DAY)

            game.castSpell(1, "Brutal Cathar").error shouldBe null
            game.resolveStack()

            val giant = game.findPermanent("Hill Giant")!!
            game.selectTargets(listOf(giant)).error shouldBe null
            game.resolveStack()
            withClue("the ETB exiles the opponent's creature") { game.isInExile(2, "Hill Giant") shouldBe true }

            game.state = DayNightService.set(game.state, cardRegistry, DayNight.NIGHT, "test").first
            val brute = game.findPermanent("Moonrage Brute")
            withClue("night transforms it into Moonrage Brute") { brute shouldNotBe null }
            withClue("transforming isn't leaving the battlefield") { game.isInExile(2, "Hill Giant") shouldBe true }

            game.castSpell(1, "Lightning Bolt", brute).error shouldBe null
            game.resolveStack()

            withClue("the Brute died") { game.isOnBattlefield("Moonrage Brute") shouldBe false }
            withClue("leaving on its back face returns the exiled creature") {
                game.isOnBattlefield("Hill Giant") shouldBe true
                game.isInExile(2, "Hill Giant") shouldBe false
            }
        }

        test("transforming back into Brutal Cathar exiles another creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Brutal Cathar")
                .withCardInHand(1, "Lightning Bolt")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.state = DayNightService.set(
                game.state.copy(dayNight = DayNight.DAY), cardRegistry, DayNight.NIGHT, "test"
            ).first
            withClue("it starts the turn as Moonrage Brute") { game.findPermanent("Moonrage Brute") shouldNotBe null }

            // Two spells during its controller's turn at night make it day next turn (CR 731.2b).
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()
            game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
            game.resolveStack()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("day transforms it back into Brutal Cathar") {
                game.state.dayNight shouldBe DayNight.DAY
                game.findPermanent("Brutal Cathar") shouldNotBe null
            }

            if (game.state.pendingDecision != null) {
                val giant = game.findPermanent("Hill Giant")!!
                game.selectTargets(listOf(giant)).error shouldBe null
            }
            game.resolveStack()

            withClue("the transforms-into trigger exiled the opponent's creature") {
                game.isInExile(2, "Hill Giant") shouldBe true
            }
        }
    }
}
