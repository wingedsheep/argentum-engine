package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.emn.cards.MausoleumWanderer
import com.wingedsheep.mtg.sets.definitions.soi.cards.SpectralShepherd
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mausoleum Wanderer (EMN) — {U} Creature — Spirit 1/1
 *
 * "Flying
 *  Whenever another Spirit you control enters, this creature gets +1/+1 until end of turn.
 *  Sacrifice this creature: Counter target instant or sorcery spell unless its controller pays {X},
 *  where X is this creature's power."
 *
 * The load-bearing detail is that X is read *after* the Wanderer has already been sacrificed to pay
 * the cost. `EntityReference.Source` resolves with last-known information (CR 112.7a / 608.2h), so
 * the pre-sacrifice snapshot must supply the pumped power. These tests pin X to 1 unpumped and 2
 * after one Spirit has entered, by giving the opposing spell's controller exactly one mana: at X=1
 * they get a real pay-or-be-countered choice, at X=2 they simply can't pay.
 */
class MausoleumWandererScenarioTest : FunSpec({

    val sacAbilityId = MausoleumWanderer.activatedAbilities.single().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(MausoleumWanderer)
        driver.registerCard(SpectralShepherd)
        return driver
    }

    test("another Spirit entering pumps the Wanderer to 2/2 until end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val wanderer = driver.putCreatureOnBattlefield(me, "Mausoleum Wanderer")
        driver.state.projectedState.getPower(wanderer) shouldBe 1

        val shepherd = driver.putCardInHand(me, "Spectral Shepherd")
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveColorlessMana(me, 2)
        driver.castSpell(me, shepherd).isSuccess shouldBe true
        driver.bothPass() // Shepherd enters
        driver.bothPass() // the +1/+1 trigger resolves

        driver.state.projectedState.getPower(wanderer) shouldBe 2
        driver.state.projectedState.getToughness(wanderer) shouldBe 2
    }

    test("unpumped: X is 1, so one floating mana is enough to pay through the counter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val wanderer = driver.putCreatureOnBattlefield(me, "Mausoleum Wanderer")

        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(me)
        driver.castSpell(opponent, bolt, listOf(me))
        val boltOnStack = driver.getTopOfStack()!!
        driver.passPriority(opponent)

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = wanderer,
                abilityId = sacAbilityId,
                targets = listOf(ChosenTarget.Spell(boltOnStack))
            )
        )
        driver.getGraveyardCardNames(me) shouldContain "Mausoleum Wanderer"

        // Exactly {1} available: enough for X = 1, so a real choice is offered.
        driver.giveColorlessMana(opponent, 1)
        driver.bothPass()
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        (driver.pendingDecision as YesNoDecision).playerId shouldBe opponent

        driver.submitYesNo(opponent, true) // pay {1}
        driver.bothPass() // Lightning Bolt resolves
        driver.getLifeTotal(me) shouldBe 17
    }

    test("pumped: X is 2 by last-known information, so one floating mana can't save the spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Mountain" to 20), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val wanderer = driver.putCreatureOnBattlefield(me, "Mausoleum Wanderer")

        // Pump it once: 1/1 → 2/2 for the turn.
        val shepherd = driver.putCardInHand(me, "Spectral Shepherd")
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveColorlessMana(me, 2)
        driver.castSpell(me, shepherd).isSuccess shouldBe true
        driver.bothPass()
        driver.bothPass()
        driver.state.projectedState.getPower(wanderer) shouldBe 2

        val bolt = driver.putCardInHand(opponent, "Lightning Bolt")
        driver.giveMana(opponent, Color.RED, 1)
        driver.passPriority(me)
        driver.castSpell(opponent, bolt, listOf(me))
        val boltOnStack = driver.getTopOfStack()!!
        driver.passPriority(opponent)

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = wanderer,
                abilityId = sacAbilityId,
                targets = listOf(ChosenTarget.Spell(boltOnStack))
            )
        )

        // Only {1} available but X is 2 — the tax can't be paid, so the Bolt is countered outright.
        driver.giveColorlessMana(opponent, 1)
        driver.bothPass()
        driver.isPaused shouldBe false
        driver.getGraveyardCardNames(opponent) shouldContain "Lightning Bolt"
        driver.getLifeTotal(me) shouldBe 20
    }
})
