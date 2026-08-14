package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.CreaturesDiedThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Dreaded Bat-Cloud — {4}{B} Creature — Bat, 4/2, Flying, Deathtouch
 *   "This spell costs {3} less to cast if a creature died this turn."
 *
 * The discount is `ModifySpellCost(SelfCast, ReduceGenericBy(FixedIfCreatureDiedThisTurn(3)))`,
 * which reads the same table-wide `CreaturesDiedThisTurnComponent` tallies that back
 * `Conditions.CreatureDiedThisTurn`. Each test hands the caster *exactly* the mana the expected
 * cost needs, so a wrong reduction shows up as a failed cast rather than a silently overpaid one:
 * five black for the full {4}{B}, two black for the discounted {1}{B}.
 */
class DreadedBatCloudScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
    }

    test("no creature died this turn — the Bat-Cloud costs the full {4}{B}") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = d.putCardInHand(you, "Dreaded Bat-Cloud")
        d.giveMana(you, Color.BLACK, 5)
        d.castSpell(you, card).error shouldBe null
        d.bothPass()

        d.findPermanent(you, "Dreaded Bat-Cloud").shouldNotBeNull()
    }

    test("no creature died this turn — two black mana is not enough to cast it") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = d.putCardInHand(you, "Dreaded Bat-Cloud")
        d.giveMana(you, Color.BLACK, 2)
        // {1}{B} of mana against a {4}{B} spell — the cast must be rejected, proving the discount
        // isn't applied unconditionally.
        (d.castSpell(you, card).error != null) shouldBe true
    }

    test("a creature died this turn — {1}{B} is enough") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.addComponent(you, CreaturesDiedThisTurnComponent(1))

        val card = d.putCardInHand(you, "Dreaded Bat-Cloud")
        d.giveMana(you, Color.BLACK, 2)
        d.castSpell(you, card).error shouldBe null
        d.bothPass()

        d.findPermanent(you, "Dreaded Bat-Cloud").shouldNotBeNull()
    }

    test("an opponent's creature dying also turns on the discount") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // The oracle says "a creature", not "a creature you controlled" — the tally is summed
        // across the table, so the opponent's loss pays for your Bat-Cloud.
        d.addComponent(opponent, CreaturesDiedThisTurnComponent(1))

        val card = d.putCardInHand(you, "Dreaded Bat-Cloud")
        d.giveMana(you, Color.BLACK, 2)
        d.castSpell(you, card).error shouldBe null
        d.bothPass()

        d.findPermanent(you, "Dreaded Bat-Cloud").shouldNotBeNull()
    }

    test("a real death — not a hand-stamped tally — enables the discount") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Kill a creature for real, so ZoneTransitionService credits the tally rather than the test.
        val victim = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val bolt = d.putCardInHand(you, "Lightning Bolt")
        d.giveMana(you, Color.RED, 1)
        d.castSpell(you, bolt, targets = listOf(victim)).error shouldBe null
        d.bothPass()
        d.findPermanent(you, "Grizzly Bears").shouldBeNull()

        val card = d.putCardInHand(you, "Dreaded Bat-Cloud")
        d.giveMana(you, Color.BLACK, 2)
        d.castSpell(you, card).error shouldBe null
        d.bothPass()

        d.findPermanent(you, "Dreaded Bat-Cloud").shouldNotBeNull()
    }
})
