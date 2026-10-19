package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.leg.cards.PsychicPurge
import com.wingedsheep.mtg.sets.definitions.tmp.cards.PatchworkGnomes
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Psychic Purge (LEG #68) — {U} Sorcery. "Psychic Purge deals 1 damage to any target. When a spell
 * or ability an opponent controls causes you to discard this card, that player loses 5 life."
 *
 * Pins the opponent-caused self-discard trigger: an opponent's Mind Rot fires it against the
 * caster; your own Mind Rot and a discard paid as a cost don't.
 */
class PsychicPurgeScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PsychicPurge)
        driver.registerCard(PatchworkGnomes)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("deals 1 damage to any target") {
        val driver = newDriver()
        val me = driver.player1
        val foe = driver.player2

        val purge = driver.putCardInHand(me, "Psychic Purge")
        driver.giveMana(me, Color.BLUE, 1)
        driver.castSpell(me, purge, listOf(foe)).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getLifeTotal(foe) shouldBe 19
    }

    test("an opponent's discard spell makes that opponent lose 5 life") {
        val driver = newDriver()
        val me = driver.player1
        val foe = driver.player2

        val purge = driver.putCardInHand(foe, "Psychic Purge")
        val bears = driver.putCardInHand(foe, "Grizzly Bears")
        val mindRot = driver.putCardInHand(me, "Mind Rot")
        driver.giveMana(me, Color.BLACK, 3)
        driver.castSpell(me, mindRot, listOf(foe)).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.submitCardSelection(foe, listOf(purge, bears))

        // The trigger goes on the stack from Psychic Purge in the graveyard; Mind Rot has finished
        // resolving, so "that player" comes from the discard event, not a live stack object.
        driver.state.getZone(ZoneKey(foe, Zone.GRAVEYARD)).contains(purge) shouldBe true
        driver.bothPass()

        driver.getLifeTotal(me) shouldBe 15
        driver.getLifeTotal(foe) shouldBe 20
    }

    test("your own discard spell doesn't trigger it") {
        val driver = newDriver()
        val me = driver.player1

        val purge = driver.putCardInHand(me, "Psychic Purge")
        val bears = driver.putCardInHand(me, "Grizzly Bears")
        val mindRot = driver.putCardInHand(me, "Mind Rot")
        driver.giveMana(me, Color.BLACK, 3)
        driver.castSpell(me, mindRot, listOf(me)).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.submitCardSelection(me, listOf(purge, bears))

        driver.state.getZone(ZoneKey(me, Zone.GRAVEYARD)).contains(purge) shouldBe true
        driver.state.stack.isEmpty() shouldBe true
        driver.getLifeTotal(me) shouldBe 20
        driver.getLifeTotal(driver.player2) shouldBe 20
    }

    test("discarding it to pay a cost doesn't trigger it") {
        val driver = newDriver()
        val me = driver.player1

        val purge = driver.putCardInHand(me, "Psychic Purge")
        val gnomes = driver.putCreatureOnBattlefield(me, "Patchwork Gnomes")
        driver.submit(
            ActivateAbility(
                playerId = me,
                sourceId = gnomes,
                abilityId = PatchworkGnomes.activatedAbilities.first().id,
                costPayment = AdditionalCostPayment(discardedCards = listOf(purge))
            )
        ).outcome shouldBe Outcome.Done
        // Only the regeneration ability is on the stack — no discard trigger joined it.
        driver.state.stack.size shouldBe 1
        driver.bothPass()

        driver.state.getZone(ZoneKey(me, Zone.GRAVEYARD)).contains(purge) shouldBe true
        driver.state.stack.isEmpty() shouldBe true
        driver.getLifeTotal(me) shouldBe 20
        driver.getLifeTotal(driver.player2) shouldBe 20
    }
})
