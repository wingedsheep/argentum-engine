package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.BronzeCudgels
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bronze Cudgels (NEO #240) — {1} Artifact — Equipment.
 *
 * "{2}: Until end of turn, equipped creature gets +X/+0, where X is the number of times this
 *  ability has resolved this turn.
 *  Equip {1}"
 */
class BronzeCudgelsScenarioTest : FunSpec({

    val pumpId = BronzeCudgels.activatedAbilities.single { !it.isEquipAbility }.id
    val equipId = BronzeCudgels.activatedAbilities.single { it.isEquipAbility }.id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BronzeCudgels))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    fun GameTestDriver.activate(player: EntityId, cudgels: EntityId, abilityId: AbilityId,
                                targets: List<ChosenTarget> = emptyList()) {
        giveColorlessMana(player, 2)
        submit(ActivateAbility(playerId = player, sourceId = cudgels, abilityId = abilityId, targets = targets))
            .outcome shouldBe Outcome.Done
        resolveAll()
    }

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val cudgels = driver.putPermanentOnBattlefield(me, "Bronze Cudgels")
        driver.activate(me, cudgels, equipId, listOf(ChosenTarget.Permanent(bears)))
        return Triple(driver, bears, cudgels)
    }

    test("each resolution adds one more than the last: +1, then +2, then +3") {
        val (driver, bears, cudgels) = setup()
        val me = driver.activePlayer!!
        driver.state.projectedState.getPower(bears) shouldBe 2

        driver.activate(me, cudgels, pumpId)
        driver.state.projectedState.getPower(bears) shouldBe 3
        driver.state.projectedState.getToughness(bears) shouldBe 2

        driver.activate(me, cudgels, pumpId)
        driver.state.projectedState.getPower(bears) shouldBe 5

        driver.activate(me, cudgels, pumpId)
        driver.state.projectedState.getPower(bears) shouldBe 8
        driver.state.projectedState.getToughness(bears) shouldBe 2
    }

    test("a resolution with no equipped creature still counts") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val cudgels = driver.putPermanentOnBattlefield(me, "Bronze Cudgels")

        driver.activate(me, cudgels, pumpId)
        driver.state.projectedState.getPower(bears) shouldBe 2

        driver.activate(me, cudgels, equipId, listOf(ChosenTarget.Permanent(bears)))
        driver.activate(me, cudgels, pumpId)
        driver.state.projectedState.getPower(bears) shouldBe 4
    }

    test("the count starts over the next turn") {
        val (driver, bears, cudgels) = setup()
        val me = driver.activePlayer!!
        driver.activate(me, cudgels, pumpId)
        driver.activate(me, cudgels, pumpId)
        driver.state.projectedState.getPower(bears) shouldBe 5

        driver.passPriorityUntil(Step.UPKEEP, me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
        driver.state.projectedState.getPower(bears) shouldBe 2
        driver.activate(me, cudgels, pumpId)
        driver.state.projectedState.getPower(bears) shouldBe 3
    }
})
