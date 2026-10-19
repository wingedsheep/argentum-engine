package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for the one-shot "for one spell this turn, you may spend mana as though it were mana of any
 * type to pay that spell's mana cost" rider
 * ([com.wingedsheep.sdk.scripting.effects.GrantOneSpellAnyManaTypeEffect]), the primitive behind
 * North Star.
 *
 * What sets it apart from the "next spell" riders is that the player picks the spell: it is spent
 * only by a cast whose mana cost can't be paid without it, and it relaxes only the spell's own mana
 * cost — "(Additional costs are still paid normally.)". The card itself is covered by
 * `NorthStarScenarioTest`.
 */
class GrantOneSpellAnyManaTypeTest : FunSpec({

    // A free sorcery, so installing the rider costs no mana that could muddy the payments below.
    val anyTypeRitual = card("Any Type Ritual") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.SpendAnyManaTypeForOneSpell()
        }
    }

    // A {G} spell with a {R} kicker: the kicker is an additional cost, so its pip keeps its type.
    val kickedGrowth = card("Test Kicked Growth") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        keywordAbility(KeywordAbility.kicker("{R}"))
        spell {
            effect = Effects.GainLife(1)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(anyTypeRitual, kickedGrowth))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.installRider(player: EntityId) {
        val ritual = putCardInHand(player, "Any Type Ritual")
        castSpell(player, ritual).outcome shouldBe Outcome.Done
        bothPass()
        state.pendingAnyManaTypeSpells shouldHaveSize 1
    }

    test("without the rider red mana can't pay a green pip") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.giveMana(player, Color.RED, 2)
        val bears = driver.putCardInHand(player, "Grizzly Bears")
        driver.castSpell(player, bears).outcome shouldNotBe Outcome.Done
    }

    test("an off-type payment spends the rider, and it serves only that one spell") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.installRider(player)

        driver.giveMana(player, Color.RED, 2)
        val bears = driver.putCardInHand(player, "Grizzly Bears")
        driver.castSpell(player, bears).outcome shouldBe Outcome.Done
        driver.state.pendingAnyManaTypeSpells.shouldBeEmpty()
        driver.bothPass()
        driver.findPermanent(player, "Grizzly Bears") shouldNotBe null

        withClue("the rider is gone, so a second off-type payment fails") {
            driver.giveMana(player, Color.RED, 2)
            val second = driver.putCardInHand(player, "Grizzly Bears")
            driver.castSpell(player, second).outcome shouldNotBe Outcome.Done
        }
    }

    test("a spell paid with the right types doesn't use the rider up") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.installRider(player)

        driver.giveMana(player, Color.GREEN, 1)
        driver.giveMana(player, Color.RED, 1)
        val first = driver.putCardInHand(player, "Grizzly Bears")
        driver.castSpell(player, first).outcome shouldBe Outcome.Done
        withClue("{1}{G} was payable with G + R, so the rider waits for a spell that needs it") {
            driver.state.pendingAnyManaTypeSpells shouldHaveSize 1
        }
        driver.bothPass()

        driver.giveMana(player, Color.RED, 2)
        val second = driver.putCardInHand(player, "Grizzly Bears")
        driver.castSpell(player, second).outcome shouldBe Outcome.Done
        driver.state.pendingAnyManaTypeSpells.shouldBeEmpty()
    }

    test("a kicker's pips are still paid normally") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.installRider(player)
        val growth = driver.putCardInHand(player, "Test Kicked Growth")

        driver.giveMana(player, Color.BLUE, 2)
        withClue("blue may pay the {G} mana cost but not the {R} kicker") {
            driver.submit(CastSpell(player, growth, declaredCostSlot = ChoiceSlot.KICKED)).outcome shouldNotBe Outcome.Done
            driver.state.pendingAnyManaTypeSpells shouldHaveSize 1
        }

        driver.giveMana(player, Color.RED, 1)
        driver.submit(CastSpell(player, growth, declaredCostSlot = ChoiceSlot.KICKED)).outcome shouldBe Outcome.Done
        driver.state.pendingAnyManaTypeSpells.shouldBeEmpty()
    }

    test("the cast enumerator offers a spell only the rider makes affordable") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.putPermanentOnBattlefield(player, "Mountain")
        driver.putPermanentOnBattlefield(player, "Mountain")
        val bears = driver.putCardInHand(player, "Grizzly Bears")
        fun bearsAffordable() = driver.legalActions(player)
            .filter { (it.action as? CastSpell)?.cardId == bears }
            .any { it.affordable }

        bearsAffordable() shouldBe false
        driver.installRider(player)
        bearsAffordable() shouldBe true

        driver.castSpell(player, bears).outcome shouldBe Outcome.Done
        driver.state.pendingAnyManaTypeSpells.shouldBeEmpty()
    }

    test("an unused rider ends with the turn") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.installRider(player)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.pendingAnyManaTypeSpells.shouldBeEmpty()
    }
})
