package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.clb.cards.JadeOrbOfDragonkind
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Jade Orb of Dragonkind (canonical CLB #236).
 *
 * {T}: Add {G}. When you spend this mana to cast a Dragon creature spell, it enters with an
 * additional +1/+1 counter on it and gains hexproof until your next turn.
 *
 * Pins the rulings: the trigger resolves above the Dragon and the creature enters with the counter
 * and hexproof already on it; two Orbs' mana means two counters; mana spent on a non-Dragon does
 * nothing; and hexproof ends as your next turn begins.
 */
class JadeOrbOfDragonkindScenarioTest : FunSpec({

    val orbAbilityId = JadeOrbOfDragonkind.activatedAbilities[0].id

    val dragon = CardDefinition.creature(
        name = "Test Jade Wyrm",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Dragon")),
        power = 2,
        toughness = 2,
    )
    val bigDragon = CardDefinition.creature(
        name = "Test Elder Jade Wyrm",
        manaCost = ManaCost.parse("{G}{G}"),
        subtypes = setOf(Subtype("Dragon")),
        power = 4,
        toughness = 4,
    )
    val nonDragon = CardDefinition.creature(
        name = "Test Jade Elf",
        manaCost = ManaCost.parse("{G}"),
        subtypes = setOf(Subtype("Elf")),
        power = 1,
        toughness = 1,
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(JadeOrbOfDragonkind, dragon, bigDragon, nonDragon))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.tapOrb(you: EntityId): EntityId {
        val orb = putPermanentOnBattlefield(you, JadeOrbOfDragonkind.name)
        submitSuccess(ActivateAbility(playerId = you, sourceId = orb, abilityId = orbAbilityId))
        return orb
    }

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.hexproof(id: EntityId): Boolean = state.projectedState.hasKeyword(id, Keyword.HEXPROOF)

    test("a Dragon paid with its mana enters with a +1/+1 counter and hexproof") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.tapOrb(you)
        val wyrm = driver.putCardInHand(you, dragon.name)

        driver.castSpell(you, wyrm).error shouldBe null
        driver.state.stack.size shouldBe 2

        driver.bothPass()
        // The trigger resolved above the Dragon; the Dragon is still a spell, owed its counter.
        driver.state.stack shouldContain wyrm
        driver.plusOnes(wyrm) shouldBe 0

        driver.bothPass()
        driver.findPermanent(you, dragon.name) shouldBe wyrm
        driver.plusOnes(wyrm) shouldBe 1
        driver.hexproof(wyrm) shouldBe true
    }

    test("hexproof lasts through the opponent's turn and ends as your next turn begins") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.tapOrb(you)
        val wyrm = driver.putCardInHand(you, dragon.name)
        driver.castSpell(you, wyrm).error shouldBe null
        driver.bothPass()
        driver.bothPass()

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, opponent)
        driver.hexproof(wyrm) shouldBe true

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, you)
        driver.hexproof(wyrm) shouldBe false
        driver.plusOnes(wyrm) shouldBe 1
    }

    test("two Orbs' mana spent on one Dragon gives two counters") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.tapOrb(you)
        driver.tapOrb(you)
        val wyrm = driver.putCardInHand(you, bigDragon.name)

        driver.castSpell(you, wyrm).error shouldBe null
        driver.state.stack.size shouldBe 3
        repeat(3) { driver.bothPass() }

        driver.plusOnes(wyrm) shouldBe 2
        driver.hexproof(wyrm) shouldBe true
    }

    test("mana spent on a non-Dragon creature spell does nothing") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        driver.tapOrb(you)
        val elf = driver.putCardInHand(you, nonDragon.name)

        driver.castSpell(you, elf).error shouldBe null
        driver.state.stack.size shouldBe 1
        driver.bothPass()

        driver.plusOnes(elf) shouldBe 0
        driver.hexproof(elf) shouldBe false
    }
})
