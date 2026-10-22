package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sludge Monster (MID #76) — {3}{U}{U} Creature — Horror, 5/5
 *
 *   Whenever this creature enters or attacks, put a slime counter on up to one other target creature.
 *   Non-Horror creatures with slime counters on them lose all abilities and have base power and
 *   toughness 2/2.
 *
 * Proves the counter-keyed group static: a slimed non-Horror loses its keywords and becomes base 2/2
 * (with counters still applying on top, layer 7b before 7c), a slimed Horror is untouched, and the
 * attack trigger may choose no target.
 */
class SludgeMonsterScenarioTest : ScenarioTestBase() {

    init {
        context("Sludge Monster") {

            test("entering slimes the target: it loses its abilities and becomes 2/2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sludge Monster")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val angel = game.findPermanent("Serra Angel")!!

                game.castSpell(1, "Sludge Monster").error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(angel)).error shouldBe null
                game.resolveStack()

                game.state.getEntity(angel)?.get<CountersComponent>()?.getCount(CounterType.SLIME) shouldBe 1
                val projected = game.state.projectedState
                projected.getPower(angel) shouldBe 2
                projected.getToughness(angel) shouldBe 2
                projected.hasKeyword(angel, Keyword.FLYING) shouldBe false
                projected.hasKeyword(angel, Keyword.VIGILANCE) shouldBe false
            }

            test("counters apply on top of the base 2/2, and a slimed Horror is unaffected") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sludge Monster")
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withCardOnBattlefield(2, "Abyssal Horror")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val angel = game.findPermanent("Serra Angel")!!
                val horror = game.findPermanent("Abyssal Horror")!!
                game.state = game.state.updateEntity(angel) {
                    it.with(CountersComponent(mapOf(CounterType.SLIME to 1, CounterType.PLUS_ONE_PLUS_ONE to 1)))
                }
                game.state = game.state.updateEntity(horror) {
                    it.with(CountersComponent(mapOf(CounterType.SLIME to 1)))
                }

                val projected = game.state.projectedState
                withClue("the Angel is base 2/2 plus its +1/+1 counter, with no abilities") {
                    projected.getPower(angel) shouldBe 3
                    projected.getToughness(angel) shouldBe 3
                    projected.hasKeyword(angel, Keyword.FLYING) shouldBe false
                }
                withClue("the Horror keeps its printed stats and flying") {
                    projected.getPower(horror) shouldBe 2
                    projected.getToughness(horror) shouldBe 2
                    projected.hasKeyword(horror, Keyword.FLYING) shouldBe true
                }
            }

            test("attacking may put the counter on nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sludge Monster", summoningSickness = false)
                    .withCardOnBattlefield(2, "Serra Angel")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val angel = game.findPermanent("Serra Angel")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Sludge Monster" to 2)).error shouldBe null
                game.selectTargets(emptyList()).error shouldBe null
                game.resolveStack()

                (game.state.getEntity(angel)?.get<CountersComponent>()?.getCount(CounterType.SLIME) ?: 0) shouldBe 0
                game.state.projectedState.hasKeyword(angel, Keyword.FLYING) shouldBe true
            }
        }
    }
}
