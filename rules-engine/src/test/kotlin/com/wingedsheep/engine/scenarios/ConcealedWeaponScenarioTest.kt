package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.ConcealedWeapon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Concealed Weapon (MKM #117) — {1}{R} Artifact — Equipment with Disguise {2}{R}.
 *
 * "Equipped creature gets +3/+0.
 *  Disguise {2}{R}
 *  When this Equipment is turned face up, attach it to target creature you control.
 *  Equip {1}{R}"
 *
 * MKM's one **noncreature** disguise card, and that is the first thing worth proving: face-down-ness
 * is a characteristic-defining effect, not a property of the card, so CR 708.2 gives the Equipment a
 * 2/2 creature body while it is face down and takes it away again on the flip. Neither the
 * face-down cast path nor the turn-up procedure may gate on the card being a creature.
 *
 * The flip's attachment is a genuine **triggered** ability, not a `disguiseFaceUpEffect`
 * replacement — the oracle text says "When". That distinction is observable and tested: it uses the
 * stack and it targets. Per the official ruling it is *not* an equip activation, so it costs no mana
 * and skips equip's sorcery-speed restriction.
 *
 * The two ruling-driven edge cases both come down to the Equipment surviving unattached: flipping is
 * legal with no creatures at all (the targetless trigger is simply removed from the stack), and a
 * target that leaves before resolution fizzles the trigger rather than the flip.
 */
class ConcealedWeaponScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ConcealedWeapon))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun attachedTo(driver: GameTestDriver, equipment: EntityId): EntityId? =
        driver.state.getEntity(equipment)?.get<AttachedToComponent>()?.targetId

    /** Cast the Equipment face down for {3} and return the resulting face-down permanent. */
    fun castFaceDown(driver: GameTestDriver, player: EntityId): EntityId {
        val card = driver.putCardInHand(player, "Concealed Weapon")
        driver.giveColorlessMana(player, 3)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = card,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        driver.bothPass()
        return driver.getPermanents(player).single {
            driver.state.getEntity(it)?.has<FaceDownComponent>() == true
        }
    }

    /** Turn it face up for its disguise cost {2}{R}. Does not resolve the resulting trigger. */
    fun flipFaceUp(driver: GameTestDriver, player: EntityId, weapon: EntityId) {
        driver.giveColorlessMana(player, 2)
        driver.giveMana(player, Color.RED, 1)
        driver.submit(
            TurnFaceUp(
                playerId = player,
                sourceId = weapon,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
    }

    /** Pass until the stack is empty, tolerating the trigger's pauses. */
    fun settle(driver: GameTestDriver) {
        repeat(4) { if (!driver.isPaused && driver.stackSize > 0) driver.bothPass() }
    }

    test("a noncreature card can be cast face down, arriving as a 2/2 creature") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val weapon = castFaceDown(driver, player)

        withClue("CR 708.2 gives any face-down permanent a 2/2 creature body") {
            driver.state.projectedState.isCreature(weapon) shouldBe true
            driver.state.projectedState.getPower(weapon) shouldBe 2
            driver.state.projectedState.getToughness(weapon) shouldBe 2
        }
        attachedTo(driver, weapon) shouldBe null
    }

    test("flipping it attaches to the targeted creature and grants +3/+0") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val weapon = castFaceDown(driver, player)

        flipFaceUp(driver, player, weapon)

        withClue("the attachment is a trigger — it uses the stack and it targets") {
            val decision = driver.pendingDecision
            (decision is ChooseTargetsDecision) shouldBe true
        }
        driver.submitTargetSelection(player, listOf(bears)).error shouldBe null
        settle(driver)

        driver.state.getEntity(weapon)?.get<FaceDownComponent>() shouldBe null
        attachedTo(driver, weapon) shouldBe bears
        withClue("Grizzly Bears is a 2/2; equipped it is a 5/2") {
            driver.state.projectedState.getPower(bears) shouldBe 5
            driver.state.projectedState.getToughness(bears) shouldBe 2
        }
        withClue("face up it is an Equipment again, not a creature") {
            driver.state.projectedState.isCreature(weapon) shouldBe false
        }
    }

    test("it may be turned face up with no creatures at all, and stays unattached") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val weapon = castFaceDown(driver, player)
        // The Equipment's own face-down body is the only creature, and it stops being one on the
        // flip — so the trigger has no legal target.
        flipFaceUp(driver, player, weapon)
        settle(driver)

        withClue("the special action isn't gated on the trigger having a target") {
            driver.state.getEntity(weapon)?.get<FaceDownComponent>() shouldBe null
        }
        driver.findPermanent(player, "Concealed Weapon").shouldNotBeNull()
        attachedTo(driver, weapon) shouldBe null
    }

    test("an opponent's creature is not a legal target") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val theirBears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val myBears = driver.putCreatureOnBattlefield(player, "Centaur Courser")
        val weapon = castFaceDown(driver, player)

        flipFaceUp(driver, player, weapon)

        val decision = driver.pendingDecision as? ChooseTargetsDecision
        decision.shouldNotBeNull()
        withClue("'target creature you control' — the opponent's creature is not offered") {
            decision.legalTargets.values.flatten().contains(theirBears) shouldBe false
            decision.legalTargets.values.flatten().contains(myBears) shouldBe true
        }
    }

    test("cast face up for its mana cost it is a plain Equipment with no trigger") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val card = driver.putCardInHand(player, "Concealed Weapon")
        driver.giveColorlessMana(player, 1)
        driver.giveMana(player, Color.RED, 1)
        driver.submit(
            CastSpell(playerId = player, cardId = card, paymentStrategy = PaymentStrategy.FromPool)
        ).error shouldBe null
        driver.bothPass()

        val weapon = driver.findPermanent(player, "Concealed Weapon").shouldNotBeNull()
        withClue("nothing was turned face up, so nothing attached itself") {
            driver.pendingDecision shouldBe null
            attachedTo(driver, weapon) shouldBe null
            driver.state.projectedState.getPower(bears) shouldBe 2
        }
    }
})
