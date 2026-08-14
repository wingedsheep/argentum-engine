package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.AxebaneFerox
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Axebane Ferox — "Ward—Collect evidence 4." (CR 702.21 + CR 701.59).
 *
 * The fourth collect-evidence context and the only one that is a *ward* cost. What these tests pin
 * is that it behaves like collect evidence everywhere else it appears, not like a counted sacrifice:
 *
 * - the constraint is a **mana-value sum**, so a graveyard of zero-cost lands is not payment however
 *   many cards it holds, and over-paying (two mana value 3 cards for a threshold of 4) is legal;
 * - **CR 701.59b fails closed** — an opponent who can't reach 4 *can't choose to collect evidence*,
 *   so the spell is countered with no prompt at all rather than a payment offered and refused;
 * - declining, which the prompt expresses as an empty selection, counters the spell.
 *
 * Centaur Courser ({2}{G}, mana value 3) is the fodder: one is never enough, two over-pay.
 */
class AxebaneFeroxScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(AxebaneFerox)
        return driver
    }

    test("collecting evidence 4 from the graveyard lets the targeting spell resolve") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ferox = driver.putCreatureOnBattlefield(opponent, "Axebane Ferox")

        val courserA = driver.putCardInGraveyard(activePlayer, "Centaur Courser")
        val courserB = driver.putCardInGraveyard(activePlayer, "Centaur Courser")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(ferox)))

        driver.bothPass()
        val decision = driver.pendingDecision
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        // The sum gate, not a count: any number of cards, total mana value >= 4.
        decision.minTotalManaValue shouldBe 4
        decision.minSelections shouldBe 0

        // Two mana value 3 cards total 6 — over-paying is the player's right (CR 701.59a).
        driver.submitCardSelection(activePlayer, listOf(courserA, courserB))

        repeat(3) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        // Paid → Bolt resolved (3 damage to a 4/4, so the Ferox lives) and the evidence is exiled.
        driver.getExileCardNames(activePlayer) shouldBe listOf("Centaur Courser", "Centaur Courser")
        driver.getGraveyardCardNames(activePlayer) shouldContain "Lightning Bolt"
        driver.findPermanent(opponent, "Axebane Ferox").shouldNotBeNull()
    }

    test("declining the collection counters the spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ferox = driver.putCreatureOnBattlefield(opponent, "Axebane Ferox")
        driver.putCardInGraveyard(activePlayer, "Centaur Courser")
        driver.putCardInGraveyard(activePlayer, "Centaur Courser")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(ferox)))

        driver.bothPass()
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(activePlayer, emptyList())

        repeat(2) { if (driver.state.priorityPlayerId != null) driver.bothPass() }

        // Countered — nothing exiled, the Bolt in the graveyard, the Ferox untouched.
        driver.getExileCardNames(activePlayer) shouldBe emptyList()
        driver.getGraveyardCardNames(activePlayer) shouldContain "Lightning Bolt"
        driver.findPermanent(opponent, "Axebane Ferox").shouldNotBeNull()
    }

    test("CR 701.59b: a graveyard that can't reach 4 is countered with no prompt at all") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ferox = driver.putCreatureOnBattlefield(opponent, "Axebane Ferox")
        // Mana value 3 — one short. The option must be absent, not offered and refused.
        driver.putCardInGraveyard(activePlayer, "Centaur Courser")

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(ferox)))

        driver.bothPass()
        driver.pendingDecision shouldBe null
        driver.getExileCardNames(activePlayer) shouldBe emptyList()
        driver.findPermanent(opponent, "Axebane Ferox").shouldNotBeNull()
    }

    test("the threshold is a mana-value sum: five lands in the graveyard are not evidence 4") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ferox = driver.putCreatureOnBattlefield(opponent, "Axebane Ferox")

        // Lands have no mana cost (CR 202.3b) — mana value 0, so five of them total 0.
        repeat(5) { driver.putCardInGraveyard(activePlayer, "Mountain") }

        driver.giveMana(activePlayer, Color.RED, 1)
        val bolt = driver.putCardInHand(activePlayer, "Lightning Bolt")
        driver.castSpellWithTargets(activePlayer, bolt, listOf(ChosenTarget.Permanent(ferox)))

        driver.bothPass()
        driver.pendingDecision shouldBe null
        driver.getExileCardNames(activePlayer) shouldBe emptyList()
        driver.findPermanent(opponent, "Axebane Ferox").shouldNotBeNull()
    }
})
