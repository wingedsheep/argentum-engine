package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.combat.MarkMustAttackExecutor
import com.wingedsheep.engine.state.components.combat.MustAttackNextCombatComponent
import com.wingedsheep.engine.state.components.player.SkippedTurnPartsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TurnPart
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.MarkMustAttackEffect
import com.wingedsheep.sdk.scripting.effects.MustAttackWindow
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * `MarkMustAttackEffect(window = MustAttackWindow.ControllersNextCombat)` — "that creature attacks
 * during its controller's next combat phase if able" (Sizzling Soloist). The requirement waits
 * across turns for its controller's combat, binds that declaration, and ends with that combat
 * phase whether or not it was obeyed.
 */
class MustAttackNextCombatTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.mark(creature: EntityId, by: EntityId) {
        val effect = MarkMustAttackEffect(EffectTarget.SpecificEntity(creature), MustAttackWindow.ControllersNextCombat)
        val result = MarkMustAttackExecutor().execute(state, effect, EffectContext(sourceId = null, controllerId = by))
        replaceState(result.state)
    }

    test("marked on its opponent's turn, it waits for its controller's combat and binds it") {
        val driver = newDriver()
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        driver.removeSummoningSickness(theirs)
        driver.mark(theirs, by = driver.player1)

        // Player 1's own combat this turn doesn't consume it.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.state.getEntity(theirs)?.get<MustAttackNextCombatComponent>() shouldNotBe null

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS, driver.player2)
        driver.declareAttackers(driver.player2, emptyMap()).error shouldNotBe null
        driver.declareAttackers(driver.player2, mapOf(theirs to driver.player1)).error shouldBe null
    }

    test("the requirement ends with its controller's next combat phase") {
        val driver = newDriver()
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        driver.removeSummoningSickness(theirs)
        driver.mark(theirs, by = driver.player1)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS, driver.player2)
        driver.declareAttackers(driver.player2, mapOf(theirs to driver.player1)).error shouldBe null
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN, driver.player2)
        driver.state.getEntity(theirs)?.get<MustAttackNextCombatComponent>() shouldBe null
    }

    test("a creature that can't attack in that combat loses the requirement anyway") {
        val driver = newDriver()
        // Tapped by its controller's combat, it can't attack — the requirement still ends.
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Centaur Courser")
        driver.mark(theirs, by = driver.player1)
        driver.passPriorityUntil(Step.BEGIN_COMBAT, driver.player2)
        driver.replaceState(driver.state.updateEntity(theirs) {
            it.with(com.wingedsheep.engine.state.components.battlefield.TappedComponent)
        })
        // Nothing can attack, so the empty declaration is made for them and combat moves on.
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN, driver.player2)
        driver.state.getEntity(theirs)?.get<MustAttackNextCombatComponent>() shouldBe null
    }

    test("marked after its controller's attackers are declared, it waits for their next combat") {
        val driver = newDriver()
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        driver.removeSummoningSickness(mine)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, emptyMap()).error shouldBe null
        driver.mark(mine, by = driver.player2)
        driver.state.getEntity(mine)?.get<MustAttackNextCombatComponent>()?.waitsOutCurrentCombat shouldBe true

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.state.getEntity(mine)?.get<MustAttackNextCombatComponent>()?.waitsOutCurrentCombat shouldBe false

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS, driver.player2)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS, driver.player1)
        driver.declareAttackers(driver.player1, emptyMap()).error shouldNotBe null
        driver.declareAttackers(driver.player1, mapOf(mine to driver.player2)).error shouldBe null
    }

    test("marked at its controller's beginning of combat, that combat is the next one") {
        val driver = newDriver()
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        driver.removeSummoningSickness(mine)
        driver.passPriorityUntil(Step.BEGIN_COMBAT)
        driver.mark(mine, by = driver.player2)
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, emptyMap()).error shouldNotBe null
    }

    test("a combat phase its controller skips doesn't use the requirement up") {
        val driver = newDriver()
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Centaur Courser")
        driver.removeSummoningSickness(mine)
        driver.mark(mine, by = driver.player2)
        driver.replaceState(driver.state.updateEntity(driver.player1) {
            it.with(SkippedTurnPartsComponent(setOf(TurnPart.COMBAT_PHASE)))
        })

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        driver.state.getEntity(mine)?.get<MustAttackNextCombatComponent>() shouldNotBe null
    }

    test("this-turn marks are unchanged by the window axis") {
        MarkMustAttackEffect(EffectTarget.Self).window shouldBe MustAttackWindow.ThisTurn
    }
})
