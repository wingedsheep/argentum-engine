package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Cleric Class {W} — Enchantment — Class (AFR #6).
 *
 * Pins the three class abilities: level 1's +1 life-gain replacement, level 2's life-gain trigger
 * (absent at level 1), and level 3's reanimation whose life gain reads the returned creature's
 * toughness off the battlefield — after the zone change, not before — and itself feeds levels 1
 * and 2.
 */
class ClericClassScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        test("level 1: you gain that much life plus 1; no counter trigger yet") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Cleric Class", classLevel = 1)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardInHand(1, "Angel's Mercy")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Angel's Mercy").error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 28
            withClue("The level 2 trigger isn't active at level 1") {
                game.hasPendingDecision() shouldBe false
                game.plusOneCounters(game.findPermanent("Grizzly Bears").shouldNotBeNull()) shouldBe 0
            }
        }

        test("becoming level 3 returns a creature card and gains its toughness, boosted by levels 1 and 2") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Cleric Class", classLevel = 2)
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val classId = game.findPermanent("Cleric Class").shouldNotBeNull()
            val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            game.execute(
                ActivateAbility(game.player1Id, classId, AbilityId.classLevelUp(3))
            ).error shouldBe null
            game.resolveStack()

            if (game.hasPendingDecision()) {
                game.selectTargets(listOf(bears)).error shouldBe null
            }
            game.resolveStack()

            val returned = game.findPermanent("Grizzly Bears").shouldNotBeNull()
            withClue("Toughness 2 read on the battlefield, plus 1 from level 1") {
                game.getLifeTotal(1) shouldBe 23
            }

            // Level 2: the life gain puts a +1/+1 counter on target creature you control.
            if (game.hasPendingDecision()) {
                game.selectTargets(listOf(returned)).error shouldBe null
            }
            game.resolveStack()
            game.plusOneCounters(returned) shouldBe 1
        }
    }
}
