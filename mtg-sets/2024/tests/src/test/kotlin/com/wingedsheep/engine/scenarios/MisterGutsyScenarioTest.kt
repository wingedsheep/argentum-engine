package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.MisterGutsy
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Mister Gutsy (PIP #136).
 *
 * - "Whenever you cast an Aura or Equipment spell, put a +1/+1 counter on this creature."
 * - "When this creature dies, create X Junk tokens, where X is the number of +1/+1 counters on it."
 *
 * The dies trigger resolves after Mister Gutsy is in the graveyard, so X comes from the zone
 * change's last-known counters.
 */
class MisterGutsyScenarioTest : FunSpec({

    val testBlade = card("Test Blade") {
        manaCost = "{1}"
        typeLine = "Artifact — Equipment"
        oracleText = "Equip {3}"
        equipAbility("{3}")
    }

    val testCharm = card("Test Charm") {
        manaCost = "{G}"
        typeLine = "Enchantment — Aura"
        oracleText = "Enchant creature"
        auraTarget = TargetObject(filter = TargetFilter.Creature)
    }

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MisterGutsy, PredefinedTokens.Junk, testBlade, testCharm))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) passPriority(state.priorityPlayerId!!).error shouldBe null
        state.stack.size shouldBe 0
    }

    fun GameTestDriver.cast(player: EntityId, name: String, color: Color, mana: Int, targets: List<EntityId> = emptyList()) {
        val spell = putCardInHand(player, name)
        giveMana(player, color, mana)
        castSpell(player, spell, targets).error shouldBe null
        resolveStack()
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.junk(player: EntityId) = getPermanents(player).filter { getCardName(it) == "Junk" }

    test("casting an Aura or an Equipment spell each adds a +1/+1 counter; other spells don't") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val gutsy = driver.putCreatureOnBattlefield(me, "Mister Gutsy")

        driver.cast(me, "Test Charm", Color.GREEN, 1, listOf(gutsy))
        driver.plusOneCounters(gutsy) shouldBe 1

        driver.cast(me, "Test Blade", Color.GREEN, 1)
        driver.plusOneCounters(gutsy) shouldBe 2

        driver.state.projectedState.getPower(gutsy) shouldBe 3

        driver.cast(me, "Giant Growth", Color.GREEN, 1, listOf(gutsy))
        driver.plusOneCounters(gutsy) shouldBe 2
    }

    test("dying creates one Junk per +1/+1 counter it had") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val gutsy = driver.putCreatureOnBattlefield(me, "Mister Gutsy")
        driver.cast(me, "Test Charm", Color.GREEN, 1, listOf(gutsy))
        driver.cast(me, "Test Blade", Color.GREEN, 1)
        driver.plusOneCounters(gutsy) shouldBe 2

        driver.cast(me, "Doom Blade", Color.BLACK, 2, listOf(gutsy))
        driver.junk(me).size shouldBe 2
    }

    test("dying with no counters creates no Junk") {
        val driver = newGame()
        val me = driver.activePlayer!!
        val gutsy = driver.putCreatureOnBattlefield(me, "Mister Gutsy")

        driver.cast(me, "Doom Blade", Color.BLACK, 2, listOf(gutsy))
        driver.junk(me).size shouldBe 0
    }
})
