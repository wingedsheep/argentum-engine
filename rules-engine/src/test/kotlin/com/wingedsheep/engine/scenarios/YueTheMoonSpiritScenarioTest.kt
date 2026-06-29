package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.YueTheMoonSpirit
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Yue, the Moon Spirit.
 *
 * Yue, the Moon Spirit ({3}{U}): Legendary Creature — Spirit Ally, 3/3, Flash, Flying, vigilance.
 * "Waterbend {5}, {T}: You may cast a noncreature spell from your hand without paying its mana cost."
 *
 * Exercises the activated-ability waterbend carrier (`hasWaterbend = true`) plus the
 * gather → choose-up-to-one → cast-without-paying pipeline filtered to noncreature, nonland cards.
 */
class YueTheMoonSpiritScenarioTest : FunSpec({

    // A {2}{U} noncreature permanent with no targeting — a clean, observable free-cast payoff
    // (it simply enters the battlefield on resolution).
    val freeSpell = card("Yue Test Free Spell") {
        manaCost = "{2}{U}"
        typeLine = "Enchantment"
        oracleText = "Test enchantment."
    }

    val abilityId = YueTheMoonSpirit.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(YueTheMoonSpirit))
        driver.registerCard(freeSpell)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("Waterbend ability casts a noncreature spell from hand for free") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val yue = driver.putCreatureOnBattlefield(player, "Yue, the Moon Spirit")
        driver.removeSummoningSickness(yue)
        val spell = driver.putCardInHand(player, "Yue Test Free Spell")
        // Exactly enough mana to pay the {5} activation — and nothing left over, so the {2}{U}
        // enchantment can only enter if it was genuinely cast without paying.
        driver.giveMana(player, Color.BLUE, 5)

        driver.submit(
            ActivateAbility(playerId = player, sourceId = yue, abilityId = abilityId),
        ).isSuccess shouldBe true

        // The ability is on the stack — both players pass so it resolves, then it pauses to
        // choose the noncreature spell to free-cast.
        driver.passPriority(player)
        driver.passPriority(driver.getOpponent(player))
        driver.submitCardSelection(player, listOf(spell))
        driver.bothPass()

        // The enchantment was cast for free and is now on the battlefield, out of hand.
        (spell in driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD))) shouldBe true
        (spell in driver.state.getZone(ZoneKey(player, Zone.HAND))) shouldBe false
        // The {T} cost tapped Yue.
        driver.isTapped(yue) shouldBe true
    }

    test("declining the optional cast leaves the spell in hand") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val yue = driver.putCreatureOnBattlefield(player, "Yue, the Moon Spirit")
        driver.removeSummoningSickness(yue)
        val spell = driver.putCardInHand(player, "Yue Test Free Spell")
        driver.giveMana(player, Color.BLUE, 5)

        driver.submit(
            ActivateAbility(playerId = player, sourceId = yue, abilityId = abilityId),
        ).isSuccess shouldBe true

        // Resolve the ability off the stack, then decline the optional cast (choose nothing).
        driver.passPriority(player)
        driver.passPriority(driver.getOpponent(player))
        driver.submitCardSelection(player, emptyList())
        driver.bothPass()

        // The spell never left the player's hand.
        (spell in driver.state.getZone(ZoneKey(player, Zone.HAND))) shouldBe true
        (spell in driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD))) shouldBe false
    }
})
