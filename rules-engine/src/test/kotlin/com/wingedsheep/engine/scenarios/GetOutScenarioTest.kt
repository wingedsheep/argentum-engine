package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.GetOut
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Get Out — {U}{U} Instant
 * Choose one —
 * • Counter target creature or enchantment spell.
 * • Return one or two target creatures and/or enchantments you own to your hand.
 */
class GetOutScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GetOut)
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Forest" to 20),
            startingLife = 20,
            skipMulligans = true
        )
        return driver
    }

    test("mode 1 counters a creature spell on the stack") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // The active player casts the (sorcery-speed) creature spell; their opponent counters it.
        val caster = driver.activePlayer!!
        val me = driver.getOpponent(caster)

        driver.giveMana(caster, Color.GREEN, 2)
        val bears = driver.putCardInHand(caster, "Grizzly Bears")
        driver.castSpell(caster, bears)
        val bearsSpell = driver.state.stack.first()
        driver.state.stack.size shouldBe 1
        driver.passPriority(caster)

        // I respond with Get Out (mode 1), countering it.
        driver.giveMana(me, Color.BLUE, 2)
        val getOut = driver.putCardInHand(me, "Get Out")
        val result = driver.submit(
            CastSpell(
                playerId = me,
                cardId = getOut,
                targets = listOf(ChosenTarget.Spell(bearsSpell)),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Spell(bearsSpell)))
            )
        )
        result.isSuccess shouldBe true

        driver.bothPass() // resolve Get Out (counters Grizzly Bears)
        driver.bothPass() // Grizzly Bears is countered, no permanent enters

        driver.state.stack.size shouldBe 0
        driver.findPermanent(caster, "Grizzly Bears") shouldBe null
        driver.getGraveyardCardNames(caster) shouldContain "Grizzly Bears"
    }

    test("mode 2 returns two of my own creatures/enchantments to hand") {
        val driver = createDriver()
        val me = driver.player1
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val enchantment = driver.putPermanentOnBattlefield(me, "Test Enchantment")

        driver.giveMana(me, Color.BLUE, 2)
        val getOut = driver.putCardInHand(me, "Get Out")
        val result = driver.submit(
            CastSpell(
                playerId = me,
                cardId = getOut,
                targets = listOf(
                    ChosenTarget.Permanent(bears),
                    ChosenTarget.Permanent(enchantment)
                ),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(
                    listOf(
                        ChosenTarget.Permanent(bears),
                        ChosenTarget.Permanent(enchantment)
                    )
                )
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        driver.findPermanent(me, "Grizzly Bears") shouldBe null
        driver.findPermanent(me, "Test Enchantment") shouldBe null
        // Both were returned to my hand (zone change mints a new entity id, so match by name).
        driver.findCardInHand(me, "Grizzly Bears") shouldNotBe null
        driver.findCardInHand(me, "Test Enchantment") shouldNotBe null
    }
})
