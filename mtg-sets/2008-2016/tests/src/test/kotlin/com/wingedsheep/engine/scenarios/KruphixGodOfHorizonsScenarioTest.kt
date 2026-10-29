package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.jou.cards.KruphixGodOfHorizons
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Kruphix, God of Horizons — "If you would lose unspent mana, that mana becomes colorless instead."
 *
 * The colorless form of ConvertEmptyingMana: the controller's would-be-lost mana is kept as that much
 * colorless mana at every step/phase boundary (and across turns), restricted mana keeps its
 * restriction (2014 ruling), and only Kruphix's controller is affected.
 */
class KruphixGodOfHorizonsScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KruphixGodOfHorizons))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.pool(playerId: EntityId): ManaPoolComponent =
        state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    test("would-be-lost mana becomes that much colorless mana; the opponent's pool still empties") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.putPermanentOnBattlefield(active, "Kruphix, God of Horizons")

        driver.giveMana(active, Color.GREEN, 2)
        driver.giveMana(active, Color.BLUE, 1)
        driver.giveColorlessMana(active, 1)
        driver.giveMana(opponent, Color.GREEN, 2)

        driver.passPriorityUntil(Step.BEGIN_COMBAT)

        driver.pool(active).colorless shouldBe 4
        driver.pool(active).total shouldBe 4
        driver.pool(opponent).total shouldBe 0
    }

    test("the kept mana survives into the next turn") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        driver.putPermanentOnBattlefield(active, "Kruphix, God of Horizons")
        driver.giveMana(active, Color.BLUE, 3)

        driver.passPriorityUntil(Step.UPKEEP)

        (driver.activePlayer == active) shouldBe false
        driver.pool(active).colorless shouldBe 3
        driver.pool(active).total shouldBe 3
    }

    test("restricted mana becomes colorless but keeps its restriction") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        driver.putPermanentOnBattlefield(active, "Kruphix, God of Horizons")
        driver.giveRestrictedMana(active, Color.GREEN, 1, ManaRestriction.CreatureSpellsOnly, ManaExpiry.END_OF_TURN)

        driver.passPriorityUntil(Step.BEGIN_COMBAT)

        val kept = driver.pool(active).restrictedMana.single()
        kept.color shouldBe null
        kept.restriction shouldBe ManaRestriction.CreatureSpellsOnly
        driver.pool(active).colorless shouldBe 0
    }

    test("firebending mana becomes colorless as combat ends instead of being lost") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        driver.putPermanentOnBattlefield(active, "Kruphix, God of Horizons")
        driver.giveRestrictedMana(active, Color.RED, 2, ManaRestriction.AnySpend, ManaExpiry.END_OF_COMBAT)

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        driver.pool(active).colorless shouldBe 2
        driver.pool(active).restrictedMana.size shouldBe 0
    }

    test("Kruphix isn't a creature while devotion to green and blue is below seven") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val kruphix = driver.putPermanentOnBattlefield(active, "Kruphix, God of Horizons")

        driver.state.projectedState.isCreature(kruphix) shouldBe false
    }
})
