package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.nph.cards.Spellskite
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Spellskite (NPH #159) — {2} Artifact Creature — Phyrexian Horror 0/4
 *
 *   {U/P}: Change a target of target spell or ability to this creature.
 *
 *  An opponent's burn aimed at you is redirected onto Spellskite, paying 2 life for {U/P}.
 */
class SpellskiteScenarioTest : FunSpec({

    val redirectId = Spellskite.activatedAbilities.single().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Spellskite))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("redirects an opponent's Lightning Bolt from you onto itself, paying 2 life for {U/P}") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val skite = driver.putPermanentOnBattlefield(me, "Spellskite")
        driver.passPriority(me)

        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.submit(
            CastSpell(opponent, bolt, listOf(ChosenTarget.Player(me)), paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        val boltOnStack = driver.getTopOfStack()!!
        driver.passPriority(opponent)

        driver.submit(
            ActivateAbility(me, skite, redirectId, targets = listOf(ChosenTarget.Spell(boltOnStack)))
        ).error shouldBe null
        driver.getLifeTotal(me) shouldBe 18

        driver.bothPass()
        driver.state.getEntity(boltOnStack)!!.get<TargetsComponent>()!!.targets shouldBe
            listOf(ChosenTarget.Permanent(skite))
        driver.bothPass()

        driver.getLifeTotal(me) shouldBe 18
        driver.findPermanent(me, "Spellskite") shouldNotBe null
    }
})
