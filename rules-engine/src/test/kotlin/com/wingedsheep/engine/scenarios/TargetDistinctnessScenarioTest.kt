package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * CR 601.2c: "The same target can't be chosen multiple times for any one instance of the word
 * 'target' on the spell." A single targeting requirement with `count > 1` ("two target creatures")
 * therefore needs two *different* objects. This exercises the cast-path guard in
 * [com.wingedsheep.engine.mechanics.targeting.TargetValidator] using a directly-cast spell whose
 * sole requirement is `count = 2`. (Cross-requirement distinctness — a different "target" instance —
 * is opt-in via TargetOther and is covered by the Friendly Rivalry scenario test.)
 */
class TargetDistinctnessScenarioTest : FunSpec({

    // A minimal instant: "deals 1 damage to each of two target creatures" — one count=2 requirement.
    val twinPing = card("Test Twin Ping") {
        manaCost = "{R}"
        colorIdentity = "R"
        typeLine = "Instant"
        oracleText = "Test Twin Ping deals 1 damage to each of two target creatures."
        spell {
            target("two target creatures", TargetCreature(count = 2))
            effect = Effects.DealDamage(1, EffectTarget.ContextTarget(0))
                .then(Effects.DealDamage(1, EffectTarget.ContextTarget(1)))
        }
        metadata {
            rarity = Rarity.COMMON
            collectorNumber = "0"
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + twinPing)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("the same creature cannot be chosen twice for a single 'two target creatures' requirement") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val creature = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        driver.putCreatureOnBattlefield(opp, "Centaur Courser") // a second legal target exists

        driver.giveMana(me, Color.RED, 1)
        val spell = driver.putCardInHand(me, "Test Twin Ping")

        val result = driver.castSpellWithTargets(
            me,
            spell,
            targets = listOf(ChosenTarget.Permanent(creature), ChosenTarget.Permanent(creature))
        )
        result.error shouldNotBe null
    }

    test("two different creatures satisfy the same 'two target creatures' requirement") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val first = driver.putCreatureOnBattlefield(opp, "Centaur Courser")
        val second = driver.putCreatureOnBattlefield(opp, "Centaur Courser")

        driver.giveMana(me, Color.RED, 1)
        val spell = driver.putCardInHand(me, "Test Twin Ping")

        driver.castSpellWithTargets(
            me,
            spell,
            targets = listOf(ChosenTarget.Permanent(first), ChosenTarget.Permanent(second))
        ).error shouldBe null
        driver.bothPass()

        // Both 3/3s took only 1 damage → both survive; the cast was legal and resolved.
        driver.getPermanents(opp).contains(first) shouldBe true
        driver.getPermanents(opp).contains(second) shouldBe true
    }
})
