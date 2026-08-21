package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.IcyManipulator
import com.wingedsheep.mtg.sets.definitions.mrd.cards.FracturedLoyalty
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Fractured Loyalty (MRD #93) — "Whenever enchanted creature becomes the target of a spell or
 * ability, that spell or ability's controller gains control of that creature."
 *
 * The card exists to prove one reference: `Player.ControllerOfTargetingSource`. A becomes-target
 * trigger binds the *targeted object* as its triggering entity, so every player reference already
 * in the SDK points at the wrong end — `TriggeringPlayer` is null for an object target, and
 * `ControllerOfTriggeringEntity` names the victim's controller. Each test below therefore puts the
 * Aura's controller and the targeting player on *different* sides, so a hardcoded "you" or "its
 * controller" fails rather than coincidentally passing.
 */
class FracturedLoyaltyScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + FracturedLoyalty)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Cast Fractured Loyalty from [enchanter] onto [creature] and let it resolve. */
    fun GameTestDriver.enchant(enchanter: EntityId, creature: EntityId) {
        val aura = putCardInHand(enchanter, "Fractured Loyalty")
        giveMana(enchanter, Color.RED, 2)
        castSpell(enchanter, aura, listOf(creature)).isSuccess shouldBe true
        bothPass()
    }

    /**
     * The *projected* controller. A control change is a Layer 2 floating effect and never touches
     * the base `ControllerComponent`, so `GameTestDriver.getController` — which reads that
     * component directly — would report the original controller no matter what happened.
     */
    fun GameTestDriver.controllerOf(id: EntityId): EntityId? =
        state.projectedState.getController(id)

    fun resolveStack(d: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && d.state.stack.isNotEmpty() && !d.isPaused) d.bothPass()
    }

    test("a spell's controller takes the creature — the enchanter pointing at an opponent's creature") {
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.enchant(d.player1, bears)
        d.controllerOf(bears) shouldBe d.player2

        val growth = d.putCardInHand(d.player1, "Giant Growth")
        d.giveMana(d.player1, Color.GREEN, 1)
        d.castSpell(d.player1, growth, listOf(bears)).isSuccess shouldBe true
        resolveStack(d)

        withClue("the creature ends up with the player who pointed a spell at it") {
            d.controllerOf(bears) shouldBe d.player1
        }
    }

    test("the targeting player takes it even when that player is not the enchanter") {
        // The mirror of the first test: the Aura sits on the enchanter's OWN creature, and the
        // opponent's spell hands it to the opponent. A `Player.You` shortcut passes test one and
        // fails here.
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.enchant(d.player1, bears)
        d.controllerOf(bears) shouldBe d.player1

        val growth = d.putCardInHand(d.player2, "Giant Growth")
        d.giveMana(d.player2, Color.GREEN, 1)
        d.passPriority(d.player1)
        d.castSpell(d.player2, growth, listOf(bears)).isSuccess shouldBe true
        resolveStack(d)

        withClue("'that spell's controller' is the caster, not the Aura's controller") {
            d.controllerOf(bears) shouldBe d.player2
        }
    }

    test("an activated ability counts too, not only spells") {
        // The printed text is "a spell or ability". A trigger narrowed to spellsOnly would pass
        // both tests above and silently drop this half.
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.enchant(d.player1, bears)

        val manipulator = d.putPermanentOnBattlefield(d.player1, "Icy Manipulator")
        d.giveColorlessMana(d.player1, 1)

        d.submit(
            ActivateAbility(
                d.player1,
                manipulator,
                IcyManipulator.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Permanent(bears))
            )
        ).isSuccess shouldBe true
        resolveStack(d)

        withClue("the ability's controller gains control") {
            d.controllerOf(bears) shouldBe d.player1
        }
    }

    test("destroying the Aura in response does not stop the control change") {
        // The trigger goes on the stack above the spell that caused it, so there is a window to
        // kill the Aura before it resolves — and the ability, already independent of its source,
        // still moves "that creature". This is why the effect names the triggering entity rather
        // than the enchanted creature: by resolution there is no enchanted creature left to name.
        val d = driver()
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        d.enchant(d.player1, bears)
        val aura = d.findPermanent(d.player1, "Fractured Loyalty")!!

        val growth = d.putCardInHand(d.player2, "Giant Growth")
        d.giveMana(d.player2, Color.GREEN, 1)
        d.passPriority(d.player1)
        d.castSpell(d.player2, growth, listOf(bears)).isSuccess shouldBe true

        val disenchant = d.putCardInHand(d.player1, "Disenchant")
        d.giveMana(d.player1, Color.WHITE, 2)
        d.passPriority(d.player2)
        d.castSpell(d.player1, disenchant, listOf(aura)).isSuccess shouldBe true
        resolveStack(d)

        withClue("the Aura is gone") {
            d.findPermanent(d.player1, "Fractured Loyalty") shouldBe null
        }
        withClue("the already-triggered ability still hands over that creature") {
            d.controllerOf(bears) shouldBe d.player2
        }
    }
})
