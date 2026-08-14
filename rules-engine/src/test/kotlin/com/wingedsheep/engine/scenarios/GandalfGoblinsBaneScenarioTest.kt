package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Gandalf, Goblins' Bane // Flameshape (HOB #96) — {2}{R} Legendary Creature — Avatar Wizard 2/3.
 *
 *   Whenever you cast a noncreature spell, Gandalf gets +1/+1 until end of turn and deals 1 damage
 *   to each opponent.
 *
 *   Adventure — Flameshape {1}{R}, Sorcery:
 *   Look at the top two cards of your library and exile them face down. For as long as they remain
 *   exiled, you may play them if you control a Wizard.
 *
 * The interesting claim is Flameshape's gate. "You may play them **if** you control a Wizard" is a
 * standing condition on a never-expiring permission, not a one-shot check at resolution — so the
 * permission has to be re-evaluated whenever the cards are queried. The cast trigger's own claim is
 * that both halves fire off a single *noncreature* cast, and neither fires off a creature spell.
 */
class GandalfGoblinsBaneScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun resolveStack(driver: GameTestDriver) {
        var safety = 0
        while (driver.stackSize > 0 && safety < 20) {
            driver.bothPass()
            safety++
        }
    }

    /** Can [player] currently cast [card] — from anywhere the enumerator will offer it? */
    fun castableNow(driver: GameTestDriver, player: EntityId, card: EntityId): Boolean =
        driver.legalActions(player).any { (it.action as? CastSpell)?.cardId == card }

    test("casting a noncreature spell pumps Gandalf and pings each opponent; a creature spell does neither") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.player2

        val gandalf = driver.putPermanentOnBattlefield(me, "Gandalf, Goblins' Bane")
        val victim = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        // Control: a creature spell must not trigger the ability.
        val creature = driver.putCardInHand(me, "Goblin Guide")
        driver.giveMana(me, Color.RED, 1)
        driver.submitSuccess(
            CastSpell(playerId = me, cardId = creature, paymentStrategy = PaymentStrategy.FromPool)
        )
        resolveStack(driver)
        withClue("a creature spell is not a noncreature spell") {
            driver.getLifeTotal(opponent) shouldBe 20
            driver.state.projectedState.getPower(gandalf) shouldBe 2
        }

        // The real case: an instant.
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.submitSuccess(
            CastSpell(
                playerId = me,
                cardId = bolt,
                targets = listOf(ChosenTarget.Permanent(victim)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        resolveStack(driver)

        withClue("the cast trigger pumps Gandalf to 3/4 until end of turn") {
            driver.state.projectedState.getPower(gandalf) shouldBe 3
            driver.state.projectedState.getToughness(gandalf) shouldBe 4
        }
        withClue("...and deals 1 damage to each opponent (the Bolt itself hit the creature, not the player)") {
            driver.getLifeTotal(opponent) shouldBe 19
            driver.getLifeTotal(me) shouldBe 20
        }
    }

    test("Flameshape exiles the top two face down and gates the play permission on controlling a Wizard") {
        val driver = createDriver()
        val me = driver.player1

        val second = driver.putCardOnTopOfLibrary(me, "Goblin Guide")
        val first = driver.putCardOnTopOfLibrary(me, "Lightning Bolt")

        val card = driver.putCardInHand(me, "Gandalf, Goblins' Bane")
        driver.giveMana(me, Color.RED, 2)

        // faceIndex 0 is the Adventure face (Flameshape), not the creature.
        driver.submitSuccess(
            CastSpell(
                playerId = me,
                cardId = card,
                faceIndex = 0,
                paymentStrategy = PaymentStrategy.FromPool,
            )
        )
        resolveStack(driver)

        withClue("both cards are exiled, face down") {
            driver.getExile(me) shouldContain first
            driver.getExile(me) shouldContain second
            driver.state.getEntity(first)?.get<FaceDownComponent>().shouldNotBeNull()
            driver.state.getEntity(second)?.get<FaceDownComponent>().shouldNotBeNull()
        }

        val permission = driver.state.mayPlayPermissions.single { first in it.cardIds }
        withClue("'for as long as they remain exiled' is a permanent grant to the caster, covering both cards") {
            permission.permanent shouldBe true
            permission.controllerId shouldBe me
            permission.cardIds shouldContain second
        }
        withClue("the Wizard requirement rides the permission as a re-checked condition") {
            (permission.condition != null) shouldBe true
        }

        // Plenty of mana in both branches, so the only variable is the Wizard.
        driver.giveMana(me, Color.RED, 6)
        withClue("no Wizard on the battlefield → the permission is suspended") {
            castableNow(driver, me, first) shouldBe false
            castableNow(driver, me, second) shouldBe false
        }

        // Gandalf is himself an Avatar Wizard, so resolving the creature face turns the gate on.
        driver.putPermanentOnBattlefield(me, "Gandalf, Goblins' Bane")
        withClue("with a Wizard on the battlefield the exiled cards become playable") {
            castableNow(driver, me, first) shouldBe true
            castableNow(driver, me, second) shouldBe true
        }
    }
})
