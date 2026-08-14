package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.soi.cards.MoonlightHunt
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Moonlight Hunt (SOI #219) — {1}{G} Instant
 *
 * "Choose target creature you don't control. Each creature you control that's a Wolf or a Werewolf
 *  deals damage equal to its power to that creature."
 *
 * The interesting wiring is per-iteration: each pack member is both the damage *source* and the
 * source of the *amount*. Differently-sized Wolves catch a loop that reads one creature's power for
 * every iteration, and a Bear plus an opposing Wolf catch a filter that's too wide.
 */
class MoonlightHuntScenarioTest : FunSpec({

    fun creature(name: String, subtype: String, power: Int, toughness: Int) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype(subtype)),
        power = power,
        toughness = toughness
    )

    val wolf = creature("Test Wolf", "Wolf", 2, 2)
    val werewolf = creature("Test Werewolf", "Werewolf", 3, 3)
    val bear = creature("Test Bear", "Bear", 4, 4)
    val victim = creature("Test Victim", "Ox", 1, 9)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MoonlightHunt, werewolf, wolf, bear, victim))
        return driver
    }

    fun damageOn(driver: GameTestDriver, entityId: EntityId): Int =
        driver.state.getEntity(entityId)?.get<DamageComponent>()?.amount ?: 0

    fun castHunt(driver: GameTestDriver, playerId: EntityId, target: EntityId) {
        val hunt = driver.putCardInHand(playerId, "Moonlight Hunt")
        driver.giveMana(playerId, Color.GREEN, 2)
        driver.castSpell(playerId, hunt, listOf(target))
        driver.bothPass()
    }

    test("every Wolf and Werewolf you control deals its own power to the target") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        driver.putCreatureOnBattlefield(you, "Test Wolf")          // 2 power  → counts
        driver.putCreatureOnBattlefield(you, "Test Werewolf")      // 3 power  → counts
        driver.putCreatureOnBattlefield(you, "Test Bear")          // 4 power, wrong type → 0
        driver.putCreatureOnBattlefield(opponent, "Test Wolf")     // a Wolf, but not yours → 0
        val target = driver.putCreatureOnBattlefield(opponent, "Test Victim")

        castHunt(driver, you, target)

        // Exactly 2 + 3. Anything that read a single creature's power, or swept in the Bear or the
        // opponent's Wolf, lands on a different number.
        damageOn(driver, target) shouldBe 5
        (target in driver.getPermanents(opponent)) shouldBe true
    }

    test("with no Wolves or Werewolves the spell resolves and deals nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        driver.putCreatureOnBattlefield(you, "Test Bear")
        val target = driver.putCreatureOnBattlefield(opponent, "Test Victim")

        castHunt(driver, you, target)

        driver.state.stack.size shouldBe 0
        damageOn(driver, target) shouldBe 0
        (target in driver.getPermanents(opponent)) shouldBe true
    }
})
