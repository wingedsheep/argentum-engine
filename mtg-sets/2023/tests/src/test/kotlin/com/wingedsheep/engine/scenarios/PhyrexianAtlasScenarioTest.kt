package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.PhyrexianAtlas
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Phyrexian Atlas (ONE #237) — {3} Artifact.
 *
 * "{T}: Add one mana of any color.
 *  Corrupted — Whenever this artifact becomes tapped, each opponent who has three or more poison
 *  counters loses 1 life."
 *
 * Tapping for mana adds the mana immediately and puts the corrupted trigger on the stack; on
 * resolution only opponents at the poison threshold lose life.
 */
class PhyrexianAtlasScenarioTest : FunSpec({

    val abilityId = PhyrexianAtlas.activatedAbilities.first().id

    test("tapping for mana drains only the opponents with three or more poison") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(PhyrexianAtlas))
        val players = driver.initMultiplayer(
            decks = List(3) { Deck.of("Swamp" to 40) },
            skipMulligans = true,
            startingPlayer = 0,
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val (you, poisoned, clean) = players
        driver.addComponent(poisoned, CountersComponent(mapOf(CounterType.POISON to 3)))
        driver.addComponent(clean, CountersComponent(mapOf(CounterType.POISON to 2)))

        val atlas = driver.putPermanentOnBattlefield(you, "Phyrexian Atlas")
        driver.submit(ActivateAbility(you, atlas, abilityId, manaColorChoice = Color.GREEN)).error shouldBe null
        (driver.state.getEntity(you)?.get<ManaPoolComponent>()?.green ?: 0) shouldBe 1

        driver.state.stack.size shouldBe 1
        while (driver.state.stack.isNotEmpty()) driver.passPriority(driver.state.priorityPlayerId!!)

        driver.getLifeTotal(you) shouldBe 20
        driver.getLifeTotal(poisoned) shouldBe 19
        driver.getLifeTotal(clean) shouldBe 20
    }

    test("with no corrupted opponent the trigger resolves without effect") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(PhyrexianAtlas))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.addComponent(driver.player2, CountersComponent(mapOf(CounterType.POISON to 2)))

        val atlas = driver.putPermanentOnBattlefield(driver.player1, "Phyrexian Atlas")
        driver.submit(ActivateAbility(driver.player1, atlas, abilityId, manaColorChoice = Color.RED)).error shouldBe null
        driver.state.stack.size shouldBe 1
        driver.bothPass()

        driver.getLifeTotal(driver.player1) shouldBe 20
        driver.getLifeTotal(driver.player2) shouldBe 20
    }
})
