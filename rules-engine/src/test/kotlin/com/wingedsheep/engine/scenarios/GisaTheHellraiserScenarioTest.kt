package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.GisaTheHellraiser
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Gisa, the Hellraiser — composite ward "Ward—{2}, Pay 2 life." (CR 702.21a).
 *
 * An opponent targeting Gisa must pay BOTH components — {2} mana, then 2 life — in order, or
 * their spell is countered. The two components are charged one at a time: a mana-source decision
 * first, then a yes/no life decision; only paying both lets the spell resolve.
 */
class GisaTheHellraiserScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GisaTheHellraiser)
        return driver
    }

    test("paying both ward components (mana then life) lets the targeting spell resolve") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val gisa = driver.putCreatureOnBattlefield(opponent, "Gisa, the Hellraiser")

        // Three Mountains: {R} for Bolt, {2} for the mana ward component.
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(gisa)))

        // Resolve ward trigger — first component is the {2} mana cost.
        driver.bothPass()
        val manaDecision = driver.pendingDecision
        manaDecision.shouldNotBeNull()
        manaDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        manaDecision.requiredCost shouldBe "{2}"

        val lifeBefore = driver.getLifeTotal(activePlayer)
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = true)

        // Second component is the 2-life yes/no decision.
        val lifeDecision = driver.pendingDecision
        lifeDecision.shouldNotBeNull()
        lifeDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(activePlayer, true)

        // Both paid → Bolt resolves (3 damage to a 4/4 — Gisa survives but the spell was not countered).
        repeat(3) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        driver.getLifeTotal(activePlayer) shouldBe (lifeBefore - 2)
        driver.getGraveyardCardNames(activePlayer).contains("Lightning Bolt") shouldBe true
        driver.findPermanent(opponent, "Gisa, the Hellraiser").shouldNotBeNull()
    }

    test("declining the second component (life) after paying mana still counters the spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val gisa = driver.putCreatureOnBattlefield(opponent, "Gisa, the Hellraiser")

        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(gisa)))

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = true)

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        // Decline the life component — even though mana was paid, the spell is countered.
        driver.submitYesNo(activePlayer, false)

        repeat(2) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        driver.findPermanent(opponent, "Gisa, the Hellraiser") shouldNotBe null
        driver.getGraveyardCardNames(activePlayer).contains("Lightning Bolt") shouldBe true
    }

    test("declining the first component (mana) counters immediately without a life prompt") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val gisa = driver.putCreatureOnBattlefield(opponent, "Gisa, the Hellraiser")

        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(gisa)))

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        val lifeBefore = driver.getLifeTotal(activePlayer)
        driver.submitManaAutoPayOrDecline(activePlayer, autoPay = false)

        // No life prompt — the spell is already countered.
        driver.pendingDecision shouldBe null
        repeat(2) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        driver.findPermanent(opponent, "Gisa, the Hellraiser") shouldNotBe null
        driver.getLifeTotal(activePlayer) shouldBe lifeBefore
    }

    test("ward counters immediately when the caster cannot pay the mana component") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val gisa = driver.putCreatureOnBattlefield(opponent, "Gisa, the Hellraiser")

        // Only {R} for Bolt — nothing left to pay the {2} ward component.
        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(gisa)))

        driver.bothPass()
        driver.pendingDecision shouldBe null
        driver.findPermanent(opponent, "Gisa, the Hellraiser") shouldNotBe null
    }
})
