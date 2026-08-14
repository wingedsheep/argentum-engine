package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.grn.cards.ThousandYearStorm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Thousand-Year Storm {4}{U}{R} — Enchantment (GRN #207).
 * "Whenever you cast an instant or sorcery spell, copy it for each other instant and sorcery
 *  spell you've cast before it this turn. You may choose new targets for the copies."
 *
 * The copy count is `DynamicAmount.SpellsCastThisTurn(beforeTriggeringSpell = true)`, which
 * truncates the cast history at the triggering spell's own record. These tests pin the two
 * halves that can silently go wrong: how many copies land on the stack (the "before it"
 * boundary and the instant/sorcery filter), and that each copy is retargeted independently
 * (CR 707.10c).
 *
 * Lightning Bolt is the probe — a single-target instant, so every copy pauses for a target and
 * the prompt count is a direct read on the copy count.
 */
class ThousandYearStormScenarioTest : FunSpec({

    fun setup(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ThousandYearStorm))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.putPermanentOnBattlefield(you, "Thousand-Year Storm")
        return Triple(driver, you, opponent)
    }

    /** Cast a Lightning Bolt at [target] from [you]'s hand, conjuring the mana for it. */
    fun bolt(driver: GameTestDriver, you: EntityId, target: EntityId) {
        driver.giveMana(you, Color.RED, 1)
        val card = driver.putCardInHand(you, "Lightning Bolt")
        driver.castSpellWithTargets(you, card, listOf(ChosenTarget.Player(target))).error shouldBe null
    }

    /**
     * Empty the stack, answering every copy-retargeting prompt with [retargetTo]. Returns how
     * many prompts appeared — one per copy created, since Bolt always has a target to re-choose.
     */
    fun drain(driver: GameTestDriver, chooser: EntityId, retargetTo: EntityId): Int {
        var prompts = 0
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard < 60) {
            if (driver.state.pendingDecision is ChooseTargetsDecision) {
                prompts++
                // Not `isSuccess`: answering copy N of M pauses again for copy N+1, and a paused
                // result is not "success". An error is the only real failure here.
                driver.submitTargetSelection(chooser, listOf(retargetTo)).error shouldBe null
            } else {
                driver.bothPass()
            }
            guard++
        }
        return prompts
    }

    fun copyTriggers(driver: GameTestDriver) = driver.state.stack.mapNotNull {
        driver.state.getEntity(it)?.get<TriggeredAbilityOnStackComponent>()
    }.count { it.effect is CopyTargetSpellEffect }

    test("the first instant of the turn triggers but makes no copies") {
        val (driver, you, opponent) = setup()

        bolt(driver, you, opponent)

        // The trigger always fires — "will copy any instant or sorcery spell", regardless of count.
        copyTriggers(driver) shouldBe 1

        // Nothing was cast before it, so it resolves into zero copies and never prompts.
        drain(driver, you, opponent) shouldBe 0
        driver.getLifeTotal(opponent) shouldBe 17
    }

    test("the second instant of the turn is copied once, and the copy is retargetable") {
        val (driver, you, opponent) = setup()

        bolt(driver, you, opponent)
        drain(driver, you, opponent) shouldBe 0

        bolt(driver, you, opponent)

        // Exactly one prompt, answered by pointing the copy at ourselves (CR 707.10c).
        var guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
            driver.bothPass(); guard++
        }
        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(you, listOf(you)).error shouldBe null

        val copyId = driver.state.stack.single { id ->
            val c = driver.state.getEntity(id)
            c?.get<SpellOnStackComponent>() != null && c.has<CopyOfComponent>()
        }
        driver.state.getEntity(copyId)!!.get<TargetsComponent>()?.targets shouldBe
            listOf(ChosenTarget.Player(you))

        // Copy resolves first (3 to us), then the original (3 more to the opponent).
        drain(driver, you, opponent) shouldBe 0
        driver.getLifeTotal(you) shouldBe 17
        driver.getLifeTotal(opponent) shouldBe 14
    }

    test("the third instant is copied twice, prompting once per copy") {
        val (driver, you, opponent) = setup()

        bolt(driver, you, opponent)
        drain(driver, you, opponent) shouldBe 0
        bolt(driver, you, opponent)
        drain(driver, you, opponent) shouldBe 1
        driver.getLifeTotal(opponent) shouldBe 11

        bolt(driver, you, opponent)
        drain(driver, you, opponent) shouldBe 2
        // Two copies + the original.
        driver.getLifeTotal(opponent) shouldBe 2
    }

    test("a creature spell cast in between does not add a copy") {
        val (driver, you, opponent) = setup()

        bolt(driver, you, opponent)
        drain(driver, you, opponent) shouldBe 0

        // Counted in the cast history, but the filter is instant-or-sorcery, so it must not
        // raise the copy count.
        driver.giveMana(you, Color.GREEN, 3)
        val courser = driver.putCardInHand(you, "Centaur Courser")
        driver.castSpell(you, courser).error shouldBe null
        drain(driver, you, opponent) shouldBe 0

        bolt(driver, you, opponent)
        drain(driver, you, opponent) shouldBe 1
        driver.getLifeTotal(opponent) shouldBe 11
    }

    test("a spell cast in response to the trigger does not retroactively add a copy") {
        val (driver, you, opponent) = setup()

        bolt(driver, you, opponent)
        drain(driver, you, opponent) shouldBe 0

        // Second Bolt: its trigger goes on the stack expecting exactly one copy.
        bolt(driver, you, opponent)
        copyTriggers(driver) shouldBe 1

        // Respond with a third Bolt while that trigger waits. "Before it this turn" excludes
        // anything cast after the second Bolt, so the waiting trigger still makes one copy —
        // while the responding Bolt's own trigger makes two.
        bolt(driver, you, opponent)
        copyTriggers(driver) shouldBe 2

        drain(driver, you, opponent) shouldBe 3

        // 3 Bolts + 3 copies = 18 damage.
        driver.getLifeTotal(opponent) shouldBe 2
    }
})
