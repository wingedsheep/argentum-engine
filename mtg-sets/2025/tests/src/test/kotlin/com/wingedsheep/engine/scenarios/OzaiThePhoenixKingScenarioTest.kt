package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.OzaiThePhoenixKing
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ozai, the Phoenix King — {2}{B}{B}{R}{R} 7/7 Legendary Creature
 *  "Trample, firebending 4, haste
 *   If you would lose unspent mana, that mana becomes red instead.
 *   Ozai has flying and indestructible as long as you have six or more unspent mana."
 *
 * Two new pieces:
 *  - a mana-emptying COLOUR CONVERSION: at the single mana-empty point (end-of-turn cleanup) the
 *    controller's whole pool becomes that many *red* mana instead of emptying — only while Ozai
 *    (its ConvertEmptyingMana(RED) static) is on the battlefield;
 *  - a conditional static granting flying + indestructible while the controller has six or more
 *    unspent mana, read from the real mana-pool total via projected keywords.
 */
class OzaiThePhoenixKingScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(OzaiThePhoenixKing))
        return driver
    }

    fun GameTestDriver.pool(playerId: EntityId): ManaPoolComponent =
        state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    test("would-be-lost mana becomes that many red at each step/phase boundary while Ozai is out") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(active, "Ozai, the Phoenix King")

        // Active player has {W}{U}{1} floating (total 3, none red); opponent (no Ozai) has {G}{G}.
        driver.giveMana(active, Color.WHITE, 1)
        driver.giveMana(active, Color.BLUE, 1)
        driver.giveColorlessMana(active, 1)
        driver.giveMana(opponent, Color.GREEN, 2)

        // Cross a single step/phase boundary within the turn (CR 500.5: unspent mana empties as the
        // precombat main phase ends). No need to wait for end of turn.
        driver.passPriorityUntil(Step.BEGIN_COMBAT)

        // Ozai's controller: nothing lost — the 3 would-be-lost mana became {R}{R}{R}, kept in the pool.
        driver.pool(active).red shouldBe 3
        driver.pool(active).total shouldBe 3
        driver.pool(active).white shouldBe 0
        driver.pool(active).blue shouldBe 0
        driver.pool(active).colorless shouldBe 0

        // Opponent controls no Ozai — their pool emptied as the phase ended.
        driver.pool(opponent).total shouldBe 0
    }

    test("firebending mana becomes red when combat ends instead of being lost, while Ozai is out") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(active, "Ozai, the Phoenix King")

        // Both players hold 4 combat-duration (firebending) red mana — Ozai's controller and the
        // opponent, who controls no Ozai. This is the END_OF_COMBAT mana that CombatManager.endCombat
        // discards on entering the postcombat main phase.
        driver.giveRestrictedMana(
            active, Color.RED, 4,
            com.wingedsheep.sdk.scripting.effects.ManaRestriction.AnySpend,
            com.wingedsheep.sdk.scripting.effects.ManaExpiry.END_OF_COMBAT
        )
        driver.giveRestrictedMana(
            opponent, Color.RED, 4,
            com.wingedsheep.sdk.scripting.effects.ManaRestriction.AnySpend,
            com.wingedsheep.sdk.scripting.effects.ManaExpiry.END_OF_COMBAT
        )

        // Cross the end of combat (endCombat runs on entering POSTCOMBAT_MAIN).
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Ozai's controller: the firebending mana became four plain red mana instead of being lost,
        // so it survives combat as ordinary red mana (no longer combat-restricted).
        driver.pool(active).red shouldBe 4
        driver.pool(active).total shouldBe 4
        driver.pool(active).restrictedMana.size shouldBe 0

        // Opponent controls no Ozai — their combat-duration mana was discarded as normal.
        driver.pool(opponent).total shouldBe 0
    }

    test("Ozai has flying and indestructible only while you have six or more unspent mana") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ozai = driver.putCreatureOnBattlefield(active, "Ozai, the Phoenix King")

        // With an empty pool: no flying, no indestructible.
        driver.state.projectedState.hasKeyword(ozai, Keyword.FLYING) shouldBe false
        driver.state.projectedState.hasKeyword(ozai, Keyword.INDESTRUCTIBLE) shouldBe false

        // Five unspent mana — still below the threshold.
        driver.giveMana(active, Color.RED, 5)
        driver.state.projectedState.hasKeyword(ozai, Keyword.FLYING) shouldBe false
        driver.state.projectedState.hasKeyword(ozai, Keyword.INDESTRUCTIBLE) shouldBe false

        // Sixth mana crosses the threshold — both keywords switch on.
        driver.giveMana(active, Color.RED, 1)
        driver.pool(active).total shouldBe 6
        driver.state.projectedState.hasKeyword(ozai, Keyword.FLYING) shouldBe true
        driver.state.projectedState.hasKeyword(ozai, Keyword.INDESTRUCTIBLE) shouldBe true
    }
})
