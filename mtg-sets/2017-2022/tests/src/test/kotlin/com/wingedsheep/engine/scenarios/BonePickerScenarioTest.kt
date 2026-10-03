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
 * Bone Picker — {3}{B} Creature — Bird, 3/2, Flying, deathtouch
 *   "This spell costs {3} less to cast if a creature died this turn."
 *
 * Each test hands the caster exactly the mana the expected cost needs, so a wrong reduction shows
 * up as a failed cast: four black for the full {3}{B}, one black for the discounted {B}.
 */
class BonePickerScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
    }

    test("no creature died this turn — a single {B} is not enough") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = d.putCardInHand(you, "Bone Picker")
        d.giveMana(you, Color.BLACK, 1)
        (d.castSpell(you, card).error != null) shouldBe true
    }

    test("no creature died this turn — it costs the full {3}{B}") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = d.putCardInHand(you, "Bone Picker")
        d.giveMana(you, Color.BLACK, 4)
        d.castSpell(you, card).error shouldBe null
        d.bothPass()

        d.findPermanent(you, "Bone Picker").shouldNotBeNull()
    }

    test("an opponent's creature dying this turn makes it cost {B}") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // "a creature" — the died-this-turn tally is table-wide.
        d.addComponent(opponent, CreaturesDiedThisTurnComponent(1))

        val card = d.putCardInHand(you, "Bone Picker")
        d.giveMana(you, Color.BLACK, 1)
        d.castSpell(you, card).error shouldBe null
        d.bothPass()

        d.findPermanent(you, "Bone Picker").shouldNotBeNull()
    }

    test("a real death enables the discount") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val victim = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val bolt = d.putCardInHand(you, "Lightning Bolt")
        d.giveMana(you, Color.RED, 1)
        d.castSpell(you, bolt, targets = listOf(victim)).error shouldBe null
        d.bothPass()
        d.findPermanent(you, "Grizzly Bears").shouldBeNull()

        val card = d.putCardInHand(you, "Bone Picker")
        d.giveMana(you, Color.BLACK, 1)
        d.castSpell(you, card).error shouldBe null
        d.bothPass()

        d.findPermanent(you, "Bone Picker").shouldNotBeNull()
    }
})
