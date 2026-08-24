package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.Rarity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Sneaky Snacker (MH3 #205) — {U}{B} 2/1 Creature — Faerie Rogue.
 *
 * "Flying
 *  When you draw your third card in a turn, return this card from your graveyard to the battlefield
 *  tapped."
 *
 * The recursion trigger is graveyard-only: its effect moves the card out of the graveyard, so the
 * ability functions only there (CR 113.6m). A Snacker already on the battlefield must not see the
 * third draw — and the return itself carries the `fromZone = GRAVEYARD` guard, so a Snacker that
 * left the graveyard before resolution stays gone.
 */
class SneakySnackerScenarioTest : FunSpec({

    /** Local witness spell: one draw effect that crosses the third-card threshold on its own. */
    val DrawThreeForSnacker = card("Draw Three For Snacker") {
        manaCost = "{2}{U}"
        colorIdentity = "U"
        typeLine = "Sorcery"
        oracleText = "Draw three cards."

        spell {
            effect = Effects.DrawCards(3)
        }

        metadata {
            rarity = Rarity.COMMON
            collectorNumber = "T01"
        }
    }

    /** Local witness spell for the "only two cards drawn" case. */
    val DrawTwoForSnacker = card("Draw Two For Snacker") {
        manaCost = "{1}{U}"
        colorIdentity = "U"
        typeLine = "Sorcery"
        oracleText = "Draw two cards."

        spell {
            effect = Effects.DrawCards(2)
        }

        metadata {
            rarity = Rarity.COMMON
            collectorNumber = "T02"
        }
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(DrawThreeForSnacker, DrawTwoForSnacker))
        // Turn 1's active player skips their draw step (CR 103.7a), so the per-turn draw count
        // starts at 0 and a single draw-three spell lands exactly on the third card.
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun castDrawSpell(driver: GameTestDriver, player: com.wingedsheep.sdk.model.EntityId, name: String, generic: Int) {
        val spell = driver.putCardInHand(player, name)
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveColorlessMana(player, generic)
        driver.castSpell(player, spell)
        driver.bothPass()
    }

    test("drawing a third card returns the Snacker from the graveyard to the battlefield tapped") {
        val driver = newDriver()
        val player = driver.player1

        val snacker = driver.putCardInGraveyard(player, "Sneaky Snacker")

        castDrawSpell(driver, player, "Draw Three For Snacker", generic = 2)
        // The draw spell has resolved; the recursion trigger is now on the stack.
        driver.bothPass()
        driver.isPaused shouldBe false

        driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD)).contains(snacker) shouldBe true
        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)).contains(snacker) shouldBe false
        driver.isTapped(snacker) shouldBe true
    }

    test("drawing only two cards leaves the Snacker in the graveyard") {
        val driver = newDriver()
        val player = driver.player1

        val snacker = driver.putCardInGraveyard(player, "Sneaky Snacker")

        castDrawSpell(driver, player, "Draw Two For Snacker", generic = 1)
        driver.isPaused shouldBe false

        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)).contains(snacker) shouldBe true
        driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD)).contains(snacker) shouldBe false
    }

    test("a Snacker already on the battlefield does not trigger on the third draw") {
        // CR 113.6m: the ability moves the card out of the graveyard, so it functions only there.
        // Before the guard the trigger fired from the battlefield too and tapped the Snacker.
        val driver = newDriver()
        val player = driver.player1

        val snacker = driver.putCreatureOnBattlefield(player, "Sneaky Snacker")

        castDrawSpell(driver, player, "Draw Three For Snacker", generic = 2)
        driver.isPaused shouldBe false

        driver.assertStackSize(0)
        driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD)).contains(snacker) shouldBe true
        driver.isTapped(snacker) shouldBe false
    }

    test("an opponent's third draw does not return your Snacker") {
        val driver = newDriver()
        val player = driver.player1
        val opponent = driver.getOpponent(player)

        val snacker = driver.putCardInGraveyard(player, "Sneaky Snacker")

        // Hand the turn to the opponent, then have them draw three of their own cards. Their
        // turn-based draw already counted as card #1, so a draw-two crosses their third.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        castDrawSpell(driver, opponent, "Draw Two For Snacker", generic = 1)
        driver.isPaused shouldBe false

        driver.state.getZone(ZoneKey(player, Zone.GRAVEYARD)).contains(snacker) shouldBe true
        driver.state.getZone(ZoneKey(player, Zone.BATTLEFIELD)).contains(snacker) shouldBe false
    }
})
