package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.LizardConnorssCurse
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Lizard, Connors's Curse (SPM) — {2}{G}{G} Legendary Creature — Lizard Villain 5/5, Trample.
 *
 *   Lizard Formula — When Lizard, Connors's Curse enters, up to one other target creature loses
 *   all abilities and becomes a green Lizard creature with base power and toughness 4/4.
 *
 * Exercises the permanent ETB transform (RemoveAllAbilities + BecomeCreature to a green 4/4
 * Lizard), the "up to one" decline, and the "other" self-exclusion.
 */
class LizardConnorssCurseScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + LizardConnorssCurse)
        return driver
    }

    /** Cast Lizard from [you]'s hand (mana pre-supplied) and resolve it so the ETB fires. */
    fun GameTestDriver.castLizard(you: EntityId) {
        val lizard = putCardInHand(you, "Lizard, Connors's Curse")
        giveMana(you, Color.GREEN, 2)
        giveColorlessMana(you, 2)
        castSpell(you, lizard)
        bothPass() // resolve the creature spell -> Lizard enters -> ETB trigger asks for a target
    }

    test("target creature loses all abilities and becomes a 4/4 green Lizard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Birds of Paradise: a 0/1 flying Bird with a mana ability — good proof that both the
        // keyword and activated ability are stripped, the P/T is reset, and the subtype changes.
        val birds = driver.putCreatureOnBattlefield(opponent, "Birds of Paradise")

        val before = projector.project(driver.state)
        before.getPower(birds) shouldBe 0
        before.getToughness(birds) shouldBe 1
        before.hasKeyword(birds, Keyword.FLYING) shouldBe true
        before.hasSubtype(birds, "Bird") shouldBe true

        driver.castLizard(you)

        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(you, listOf(birds))
        driver.bothPass() // resolve the ETB trigger

        val after = projector.project(driver.state)
        after.getPower(birds) shouldBe 4
        after.getToughness(birds) shouldBe 4
        after.isCreature(birds) shouldBe true
        after.hasSubtype(birds, "Lizard") shouldBe true
        after.hasSubtype(birds, "Bird") shouldBe false
        after.hasColor(birds, Color.GREEN) shouldBe true
        after.getColors(birds) shouldBe setOf(Color.GREEN.name)
        after.hasLostAllAbilities(birds) shouldBe true
        after.hasKeyword(birds, Keyword.FLYING) shouldBe false
    }

    test("'up to one' — the transform can be declined, leaving the creature untouched") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val birds = driver.putCreatureOnBattlefield(opponent, "Birds of Paradise")

        driver.castLizard(you)

        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(you, emptyList()) // decline the optional target
        driver.bothPass()

        val after = projector.project(driver.state)
        after.getPower(birds) shouldBe 0
        after.getToughness(birds) shouldBe 1
        after.hasSubtype(birds, "Bird") shouldBe true
        after.hasSubtype(birds, "Lizard") shouldBe false
        after.hasKeyword(birds, Keyword.FLYING) shouldBe true
        after.hasLostAllAbilities(birds) shouldBe false
    }

    test("'other' — Lizard itself is not a legal target") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val birds = driver.putCreatureOnBattlefield(opponent, "Birds of Paradise")

        driver.castLizard(you)

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val lizard = driver.findPermanent(you, "Lizard, Connors's Curse")!!
        val legal = decision.legalTargets[0] ?: emptyList()
        legal shouldContain birds
        legal shouldNotContain lizard
    }
})
