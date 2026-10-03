package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.NullElementalBlast
import com.wingedsheep.mtg.sets.definitions.rav.cards.LightningHelix
import com.wingedsheep.mtg.sets.definitions.rav.cards.Watchwolf
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Null Elemental Blast — {C} Instant
 * Choose one —
 * • Counter target multicolored spell.
 * • Destroy target multicolored permanent.
 *
 * Each mode is tested against a multicolored object (it works) and a monocolored one (it is not a
 * legal target), since a filter that dropped the multicolored predicate passes the positive case.
 */
class NullElementalBlastScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(NullElementalBlast, LightningHelix, Watchwolf))
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Forest" to 20),
            startingLife = 20,
            skipMulligans = true
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castBlast(player: EntityId, mode: Int, target: ChosenTarget) = run {
        giveColorlessMana(player, 1)
        val blast = putCardInHand(player, "Null Elemental Blast")
        submit(
            CastSpell(
                playerId = player,
                cardId = blast,
                targets = listOf(target),
                chosenModes = listOf(mode),
                modeTargetsOrdered = listOf(listOf(target)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
    }

    test("mode 1 counters a multicolored spell") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val me = driver.getOpponent(caster)

        driver.giveMana(caster, Color.RED, 1)
        driver.giveMana(caster, Color.WHITE, 1)
        val helix = driver.putCardInHand(caster, "Lightning Helix")
        driver.castSpell(caster, helix, listOf(me)).error shouldBe null
        val helixSpell = driver.state.stack.first()
        driver.passPriority(caster)

        driver.castBlast(me, 0, ChosenTarget.Spell(helixSpell)).error shouldBe null
        driver.bothPass() // resolve the Blast, countering the Helix

        driver.state.stack.size shouldBe 0
        driver.getLifeTotal(me) shouldBe 20
        driver.getLifeTotal(caster) shouldBe 20
        driver.getGraveyardCardNames(caster) shouldContain "Lightning Helix"
    }

    test("mode 1 can't target a monocolored spell") {
        val driver = createDriver()
        val caster = driver.activePlayer!!
        val me = driver.getOpponent(caster)

        driver.giveMana(caster, Color.RED, 1)
        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.castSpell(caster, bolt, listOf(me)).error shouldBe null
        val boltSpell = driver.state.stack.first()
        driver.passPriority(caster)

        driver.castBlast(me, 0, ChosenTarget.Spell(boltSpell)).error shouldNotBe null
    }

    test("mode 2 destroys a multicolored permanent") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val wolf = driver.putCreatureOnBattlefield(opponent, "Watchwolf")

        driver.castBlast(me, 1, ChosenTarget.Permanent(wolf)).error shouldBe null
        driver.bothPass()

        driver.findPermanent(opponent, "Watchwolf") shouldBe null
        driver.getGraveyardCardNames(opponent) shouldContain "Watchwolf"
    }

    test("mode 2 can't target a monocolored permanent") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.castBlast(me, 1, ChosenTarget.Permanent(bears)).error shouldNotBe null
        driver.findPermanent(opponent, "Grizzly Bears") shouldNotBe null
    }
})
