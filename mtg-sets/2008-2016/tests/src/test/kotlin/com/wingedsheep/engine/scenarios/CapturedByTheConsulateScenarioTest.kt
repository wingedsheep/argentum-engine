package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.kld.cards.CapturedByTheConsulate
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Captured by the Consulate (KLD #8) — {3}{W} Enchantment — Aura
 *
 *   Enchant creature you don't control
 *   Enchanted creature can't attack.
 *   Whenever an opponent casts a spell, if it has a single target, change the target to enchanted
 *   creature if able.
 */
class CapturedByTheConsulateScenarioTest : FunSpec({

    class Board(val driver: GameTestDriver, val me: EntityId, val opponent: EntityId, val host: EntityId)

    /**
     * I control the Aura, enchanting the opponent's [hostName], in the main phase of whoever is
     * active — me, or the opponent when [opponentActive] (so they can cast sorceries).
     */
    fun setup(hostName: String = "Grizzly Bears", opponentActive: Boolean = false): Board {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(CapturedByTheConsulate))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true,
            startingPlayer = if (opponentActive) 1 else 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!.let { if (opponentActive) driver.getOpponent(it) else it }
        val opponent = driver.getOpponent(me)
        val host = driver.putCreatureOnBattlefield(opponent, hostName)
        val aura = driver.putPermanentOnBattlefield(me, "Captured by the Consulate")
        var state = driver.state.updateEntity(aura) { it.with(AttachedToComponent(host)) }
        state = state.updateEntity(host) { it.with(AttachmentsComponent(listOf(aura))) }
        driver.replaceState(state)
        return Board(driver, me, opponent, host)
    }

    fun GameTestDriver.targetsOf(spell: EntityId) = state.getEntity(spell)!!.get<TargetsComponent>()!!.targets

    test("an opponent's Lightning Bolt at me is redirected onto the enchanted creature") {
        val b = setup()
        val driver = b.driver
        driver.passPriority(b.me)

        val bolt = driver.putCardInHand(b.opponent, "Lightning Bolt")
        driver.giveMana(b.opponent, Color.RED, 1)
        driver.submit(
            CastSpell(b.opponent, bolt, listOf(ChosenTarget.Player(b.me)), paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.state.stack.size shouldBe 2 // the bolt, and the Aura's trigger above it

        driver.bothPass() // the trigger resolves
        driver.targetsOf(bolt) shouldBe listOf(ChosenTarget.Permanent(b.host))

        driver.bothPass() // the bolt resolves
        driver.getLifeTotal(b.me) shouldBe 20
        driver.findPermanent(b.opponent, "Grizzly Bears") shouldBe null
    }

    test("if the spell can't target the enchanted creature, its target is unchanged") {
        val b = setup(hostName = "Black Creature", opponentActive = true)
        val driver = b.driver
        val myBears = driver.putCreatureOnBattlefield(b.me, "Grizzly Bears")

        val doomBlade = driver.putCardInHand(b.opponent, "Doom Blade")
        driver.giveMana(b.opponent, Color.BLACK, 2)
        driver.submit(
            CastSpell(b.opponent, doomBlade, listOf(ChosenTarget.Permanent(myBears)), paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.state.stack.size shouldBe 2

        driver.bothPass()
        driver.targetsOf(doomBlade) shouldBe listOf(ChosenTarget.Permanent(myBears))
        driver.bothPass()
        driver.findPermanent(b.me, "Grizzly Bears") shouldBe null
        driver.findPermanent(b.opponent, "Black Creature") shouldNotBe null
    }

    test("an opponent's spell with no target doesn't trigger it") {
        val b = setup(opponentActive = true)
        val driver = b.driver

        val study = driver.putCardInHand(b.opponent, "Careful Study")
        driver.giveMana(b.opponent, Color.BLACK, 1)
        driver.submit(
            CastSpell(b.opponent, study, emptyList(), paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.state.stack.size shouldBe 1
    }

    test("my own spells don't trigger it") {
        val b = setup()
        val driver = b.driver

        val bolt = driver.putCardInHand(b.me, "Lightning Bolt")
        driver.giveMana(b.me, Color.RED, 1)
        driver.submit(
            CastSpell(b.me, bolt, listOf(ChosenTarget.Player(b.opponent)), paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.state.stack.size shouldBe 1
        driver.bothPass()
        driver.getLifeTotal(b.opponent) shouldBe 17
    }
    test("a spell countered before the trigger resolves is simply gone — the trigger does nothing") {
        val b = setup()
        val driver = b.driver
        driver.passPriority(b.me)

        val bolt = driver.putCardInHand(b.opponent, "Lightning Bolt")
        driver.giveMana(b.opponent, Color.RED, 1)
        driver.submit(
            CastSpell(b.opponent, bolt, listOf(ChosenTarget.Player(b.me)), paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.passPriority(b.opponent)

        val counter = driver.putCardInHand(b.me, "Counterspell")
        driver.giveMana(b.me, Color.BLUE, 2)
        driver.submit(
            CastSpell(b.me, counter, listOf(ChosenTarget.Spell(bolt)), paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.bothPass() // Counterspell resolves
        driver.bothPass() // the Aura's trigger resolves with nothing to redirect
        driver.state.stack.size shouldBe 0
        driver.getLifeTotal(b.me) shouldBe 20
        driver.findPermanent(b.opponent, "Grizzly Bears") shouldNotBe null
    }
})
