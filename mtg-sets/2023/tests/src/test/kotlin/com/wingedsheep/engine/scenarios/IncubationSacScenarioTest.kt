package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.IncubationSac
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Incubation Sac (ONE #171) — {G} Artifact.
 *
 * "This artifact enters with three oil counters on it.
 *  {4}, {T}, Remove an oil counter from this artifact: Create a 3/3 colorless Phyrexian Golem
 *  artifact creature token. Activate only as a sorcery."
 */
class IncubationSacScenarioTest : FunSpec({

    val abilityId = IncubationSac.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(IncubationSac))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun oil(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.OIL) ?: 0

    fun golems(driver: GameTestDriver, player: EntityId): List<EntityId> =
        driver.state.getBattlefield().filter { id ->
            val projected = driver.state.projectedState
            projected.getController(id) == player && projected.hasSubtype(id, "Golem")
        }

    fun castSac(driver: GameTestDriver): EntityId {
        val player = driver.player1
        val sac = driver.putCardInHand(player, "Incubation Sac")
        driver.giveMana(player, Color.GREEN, 1)
        driver.castSpell(player, sac).error shouldBe null
        driver.bothPass()
        return sac
    }

    test("enters with three oil counters") {
        val driver = newDriver()
        val sac = castSac(driver)
        oil(driver, sac) shouldBe 3
    }

    test("activating removes an oil counter and creates a 3/3 Phyrexian Golem artifact creature token") {
        val driver = newDriver()
        val p1 = driver.player1
        val sac = castSac(driver)

        driver.giveMana(p1, Color.GREEN, 4)
        driver.submitSuccess(ActivateAbility(playerId = p1, sourceId = sac, abilityId = abilityId))
        oil(driver, sac) shouldBe 2
        driver.state.getEntity(sac)?.get<TappedComponent>() shouldNotBe null

        driver.bothPass()
        val tokens = golems(driver, p1)
        tokens.size shouldBe 1
        val golem = tokens.single()
        val projected = driver.state.projectedState
        projected.getPower(golem) shouldBe 3
        projected.getToughness(golem) shouldBe 3
        projected.isCreature(golem) shouldBe true
        projected.hasType(golem, "ARTIFACT") shouldBe true
        projected.hasSubtype(golem, "Phyrexian") shouldBe true
        projected.getColors(golem).isEmpty() shouldBe true
    }

    test("can't be activated without four mana") {
        val driver = newDriver()
        val p1 = driver.player1
        val sac = castSac(driver)

        driver.giveMana(p1, Color.GREEN, 3)
        driver.submitExpectFailure(ActivateAbility(playerId = p1, sourceId = sac, abilityId = abilityId))
        oil(driver, sac) shouldBe 3
    }

    test("activate only as a sorcery — not with something on the stack, nor outside a main phase") {
        val driver = newDriver()
        val p1 = driver.player1
        val sac = castSac(driver)

        // A spell on the stack: not sorcery timing.
        val bears = driver.putCardInHand(p1, "Grizzly Bears")
        driver.giveMana(p1, Color.GREEN, 2)
        driver.castSpell(p1, bears).error shouldBe null
        driver.giveMana(p1, Color.GREEN, 4)
        driver.submitExpectFailure(ActivateAbility(playerId = p1, sourceId = sac, abilityId = abilityId))
        oil(driver, sac) shouldBe 3
        driver.bothPass()

        // Outside a main phase: not sorcery timing.
        driver.passPriorityUntil(Step.END)
        driver.giveMana(p1, Color.GREEN, 4)
        driver.submitExpectFailure(ActivateAbility(playerId = p1, sourceId = sac, abilityId = abilityId))
        oil(driver, sac) shouldBe 3
        golems(driver, p1).size shouldBe 0
    }
})
