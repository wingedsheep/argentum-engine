package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ktk.cards.RuthlessRipper
import com.wingedsheep.mtg.sets.definitions.lea.cards.Clone
import com.wingedsheep.mtg.sets.definitions.ogw.cards.ReflectorMage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Reflector Mage (OGW #157).
 *
 * When this creature enters, return target creature an opponent controls to its owner's hand.
 * That creature's owner can't cast spells with the same name as that creature until your next turn.
 *
 * The ban is `CantCastSpells(SharesNameWith(target))`, which freezes the target's name as the
 * trigger resolves: it holds against every card of that name (not just the returned one), lets
 * other spells through, ends when the Mage's controller's next turn begins, and — per the card's
 * rulings — a face-down creature has no name, so bouncing one bans nothing.
 */
class ReflectorMageScenarioTest : FunSpec({

    val bear = CardDefinition.creature(
        name = "Test Reflected Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = emptySet(),
        power = 2,
        toughness = 2,
    )

    val otherBear = CardDefinition.creature(
        name = "Test Other Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = emptySet(),
        power = 2,
        toughness = 2,
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ReflectorMage, RuthlessRipper, Clone, bear, otherBear))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.canCast(player: EntityId, cardId: EntityId): Boolean =
        legalActions(player).any { (it.action as? CastSpell)?.cardId == cardId && it.affordable }

    /** A face-down 2/2 — no name (CR 708.2) — standing in for a morph cast face down. */
    fun GameTestDriver.putFaceDownCreature(playerId: EntityId, cardName: String): EntityId {
        val creatureId = putCreatureOnBattlefield(playerId, cardName)
        replaceState(state.updateEntity(creatureId) { it.with(FaceDownComponent) })
        return creatureId
    }

    /** You cast Reflector Mage and its trigger returns [victim]. */
    fun GameTestDriver.reflect(you: EntityId, victim: EntityId) {
        val mage = putCardInHand(you, "Reflector Mage")
        giveMana(you, Color.WHITE, 2)
        giveMana(you, Color.BLUE, 1)
        castSpell(you, mage).error shouldBe null
        bothPass()
        submitTargetSelection(you, listOf(victim)).error shouldBe null
        bothPass()
    }

    test("returns the creature and its owner can't cast spells with that name until your next turn") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val victim = driver.putCreatureOnBattlefield(opponent, "Test Reflected Bear")
        val secondCopy = driver.putCardInHand(opponent, "Test Reflected Bear")
        val unrelated = driver.putCardInHand(opponent, "Test Other Bear")

        driver.reflect(you, victim)

        driver.findPermanent(opponent, "Test Reflected Bear") shouldBe null
        driver.getHand(opponent).contains(victim) shouldBe true

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, opponent)
        driver.giveMana(opponent, Color.GREEN, 4)
        driver.canCast(opponent, victim) shouldBe false
        driver.canCast(opponent, secondCopy) shouldBe false
        driver.canCast(opponent, unrelated) shouldBe true

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, opponent)
        driver.giveMana(opponent, Color.GREEN, 2)
        driver.canCast(opponent, victim) shouldBe true
    }

    test("a Clone is banned under the name it copied, so the returned Clone itself stays castable") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.putCreatureOnBattlefield(opponent, "Test Reflected Bear")
        val bearInHand = driver.putCardInHand(opponent, "Test Reflected Bear")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, opponent)
        val clone = driver.putCardInHand(opponent, "Clone")
        driver.giveMana(opponent, Color.BLUE, 4)
        driver.castSpell(opponent, clone).error shouldBe null
        driver.bothPass()
        driver.submitCardSelection(opponent, listOf(driver.findPermanent(opponent, "Test Reflected Bear")!!)).error shouldBe null

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, you)
        driver.reflect(you, clone)
        driver.getHand(opponent).contains(clone) shouldBe true

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, opponent)
        driver.giveMana(opponent, Color.BLUE, 4)
        driver.giveMana(opponent, Color.GREEN, 2)
        driver.canCast(opponent, bearInHand) shouldBe false
        driver.canCast(opponent, clone) shouldBe true
    }

    test("a face-down creature has no name, so bouncing it bans nothing") {
        val driver = createDriver()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        val faceDown = driver.putFaceDownCreature(opponent, "Ruthless Ripper")

        driver.reflect(you, faceDown)

        driver.getHand(opponent).contains(faceDown) shouldBe true
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, opponent)
        driver.giveMana(opponent, Color.BLACK, 3)
        driver.canCast(opponent, faceDown) shouldBe true
        // Cast face down, a spell has no name either — the ban must not catch it.
        driver.legalActions(opponent).any {
            (it.action as? CastSpell)?.let { a -> a.cardId == faceDown && a.castFaceDown } == true && it.affordable
        } shouldBe true
    }
})
