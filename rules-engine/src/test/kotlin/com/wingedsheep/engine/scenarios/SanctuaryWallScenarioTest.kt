package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.SanctuaryWall
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sanctuary Wall (MKM) — {1}{W} 0/4 Artifact Creature — Wall with defender.
 *
 * "{2}{W}, {T}: Tap target creature. You may put a stun counter on it. If you do, put a stun counter
 *  on this creature."
 *
 * The optional half is the card: the counter is a real trade, taking the target off its next untap
 * step at the cost of the Wall skipping its own. These cover both answers plus the untap step that
 * makes the counters matter.
 */
class SanctuaryWallScenarioTest : FunSpec({

    val abilityId = SanctuaryWall.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SanctuaryWall))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun stunCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0

    fun activate(driver: GameTestDriver, player: EntityId, wall: EntityId, victim: EntityId) {
        driver.giveMana(player, Color.WHITE, 1)
        driver.giveColorlessMana(player, 2)
        driver.submit(
            ActivateAbility(
                playerId = player,
                sourceId = wall,
                abilityId = abilityId,
                targets = listOf(ChosenTarget.Permanent(victim))
            )
        ).error shouldBe null
        driver.bothPass()
    }

    test("accepting the counter stuns the target and the Wall alike") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val wall = driver.putCreatureOnBattlefield(me, "Sanctuary Wall")
        driver.removeSummoningSickness(wall)
        val victim = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        activate(driver, me, wall, victim)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)

        withClue("the target is tapped by the effect, the Wall by its own cost") {
            driver.isTapped(victim) shouldBe true
            driver.isTapped(wall) shouldBe true
        }
        withClue("\"if you do\" means the Wall takes one too") {
            stunCounters(driver, victim) shouldBe 1
            stunCounters(driver, wall) shouldBe 1
        }
    }

    test("declining leaves the target merely tapped and the Wall counter-free") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val wall = driver.putCreatureOnBattlefield(me, "Sanctuary Wall")
        driver.removeSummoningSickness(wall)
        val victim = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        activate(driver, me, wall, victim)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)

        withClue("the tap is not optional — only the counter is") {
            driver.isTapped(victim) shouldBe true
        }
        withClue("no counter on either permanent") {
            stunCounters(driver, victim) shouldBe 0
            stunCounters(driver, wall) shouldBe 0
        }
    }

    test("the stun counter is spent instead of untapping (CR 122.6d)") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val wall = driver.putCreatureOnBattlefield(me, "Sanctuary Wall")
        driver.removeSummoningSickness(wall)
        val victim = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        activate(driver, me, wall, victim)
        driver.submitYesNo(me, true)

        // Round to the opponent's untap step.
        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        withClue("the opponent's untap step removed the counter rather than untapping the Courser") {
            driver.activePlayer shouldBe opponent
            driver.isTapped(victim) shouldBe true
            stunCounters(driver, victim) shouldBe 0
        }
        withClue("the Wall keeps its own counter until its controller's untap step") {
            stunCounters(driver, wall) shouldBe 1
            driver.isTapped(wall) shouldBe true
        }
    }
})
