package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CompelledAction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.OpponentsCantCauseYouTo
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * [OpponentsCantCauseYouTo] — "spells and abilities your opponents control can't cause you to
 * discard cards or sacrifice permanents" (Tamiyo, Collector of Tales), against the rulings:
 *
 * - a forced discard from an opponent's spell just doesn't happen, and the protected player isn't
 *   asked to choose cards for it (targeted, each-opponent and random shapes);
 * - an opponent's "unless you discard / sacrifice" can't be paid that way, so the consequence
 *   happens; the same for a ward—discard cost;
 * - the controller's own spells still make them discard.
 */
class CompelledActionImmunityTest : FunSpec({

    val warden = card("Discard Warden") {
        manaCost = "{G}{U}"
        typeLine = "Creature — Human Advisor"
        power = 1
        toughness = 1
        staticAbility {
            ability = OpponentsCantCauseYouTo(setOf(CompelledAction.DISCARD, CompelledAction.SACRIFICE))
        }
    }
    val mindRot = card("Test Mind Rot") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell {
            val player = target(Targets.Player)
            effect = Effects.Discard(2, player)
        }
    }
    val raidersWake = card("Test Each Opponent Discards") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell { effect = Patterns.Hand.eachOpponentDiscards(1) }
    }
    val hymn = card("Test Random Discard") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell {
            val player = target(Targets.Player)
            effect = Patterns.Hand.discardRandom(1, player)
        }
    }
    val quandary = card("Test Discard Quandary") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell {
            val player = target(Targets.Player)
            effect = Effects.PayOrSuffer(
                cost = Costs.pay.Discard(),
                suffer = Effects.LoseLife(5, player),
                player = player,
            )
        }
    }
    val tribute = card("Test Sacrifice Tribute") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell {
            val player = target(Targets.Player)
            effect = Effects.PayOrSuffer(
                cost = Costs.pay.Sacrifice(GameObjectFilter.Creature),
                suffer = Effects.LoseLife(3, player),
                player = player,
            )
        }
    }
    val selfDiscard = card("Test Self Discard") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        spell { effect = Effects.Discard(1, EffectTarget.Controller) }
    }
    val wardedSage = card("Ward Discard Sage") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Human Wizard"
        power = 2
        toughness = 2
        keywordAbility(KeywordAbility.Ward(WardCost.Discard()))
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(warden, mindRot, raidersWake, hymn, quandary, tribute, selfDiscard, wardedSage)
        )
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castAt(caster: EntityId, name: String, victim: EntityId) {
        val spell = putCardInHand(caster, name)
        giveMana(caster, Color.BLACK, 1)
        castSpellWithTargets(caster, spell, listOf(ChosenTarget.Player(victim))).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("an opponent's targeted discard does nothing and asks the protected player nothing") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val defender = driver.getOpponent(caster)
        driver.putCreatureOnBattlefield(defender, "Discard Warden")
        val handBefore = driver.getHandSize(defender)

        driver.castAt(caster, "Test Mind Rot", defender)

        withClue("no card selection is offered for a discard that can't happen") {
            driver.pendingDecision shouldBe null
        }
        driver.getHandSize(defender) shouldBe handBefore
        driver.getGraveyardCardNames(defender) shouldBe emptyList()
    }

    test("the same discard still works on a player without the grant") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val defender = driver.getOpponent(caster)
        val handBefore = driver.getHandSize(defender)

        driver.castAt(caster, "Test Mind Rot", defender)
        var guard = 0
        while (driver.isPaused && guard++ < 5) driver.autoResolveDecision()

        driver.getHandSize(defender) shouldBe handBefore - 2
    }

    test("each-opponent and random discards skip the protected player") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val defender = driver.getOpponent(caster)
        driver.putCreatureOnBattlefield(defender, "Discard Warden")
        val handBefore = driver.getHandSize(defender)

        val wake = driver.putCardInHand(caster, "Test Each Opponent Discards")
        driver.giveMana(caster, Color.BLACK, 1)
        driver.castSpell(caster, wake).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.pendingDecision shouldBe null

        driver.castAt(caster, "Test Random Discard", defender)
        driver.pendingDecision shouldBe null

        driver.getHandSize(defender) shouldBe handBefore
    }

    test("an opponent's 'unless you discard' can't be paid by discarding, so the life is lost") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val defender = driver.getOpponent(caster)
        driver.putCreatureOnBattlefield(defender, "Discard Warden")
        val handBefore = driver.getHandSize(defender)

        driver.castAt(caster, "Test Discard Quandary", defender)

        driver.pendingDecision shouldBe null
        driver.getLifeTotal(defender) shouldBe 15
        driver.getHandSize(defender) shouldBe handBefore
    }

    test("an opponent's 'unless you sacrifice' can't be paid by sacrificing, so the life is lost") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val defender = driver.getOpponent(caster)
        driver.putCreatureOnBattlefield(defender, "Discard Warden")
        driver.putCreatureOnBattlefield(defender, "Centaur Courser")

        driver.castAt(caster, "Test Sacrifice Tribute", defender)

        driver.pendingDecision shouldBe null
        driver.getLifeTotal(defender) shouldBe 17
        driver.findPermanent(defender, "Centaur Courser") shouldNotBe null
    }

    test("a ward—discard cost can't be paid, so the protected player's spell is countered unasked") {
        val driver = newDriver()
        val caster = driver.activePlayer!!
        val defender = driver.getOpponent(caster)
        // The protected player is the one casting into the ward this time.
        driver.putCreatureOnBattlefield(caster, "Discard Warden")
        val sage = driver.putCreatureOnBattlefield(defender, "Ward Discard Sage")
        val handBefore = driver.getHandSize(caster)

        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)
        driver.castSpellWithTargets(caster, bolt, listOf(ChosenTarget.Permanent(sage))).outcome shouldBe Outcome.Done
        driver.bothPass() // ward trigger resolves
        var guard = 0
        while (driver.state.stack.isNotEmpty() && !driver.isPaused && guard++ < 5) driver.bothPass()

        withClue("no 'discard a card?' question for a cost the payer can't pay") {
            driver.pendingDecision shouldBe null
        }
        driver.findPermanent(defender, "Ward Discard Sage") shouldNotBe null
        driver.getGraveyardCardNames(caster) shouldBe listOf("Lightning Bolt")
        driver.getHandSize(caster) shouldBe handBefore
    }

    test("the protected player's own spell still makes them discard") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        driver.putCreatureOnBattlefield(player, "Discard Warden")

        val spell = driver.putCardInHand(player, "Test Self Discard")
        driver.giveMana(player, Color.BLACK, 1)
        driver.castSpell(player, spell).outcome shouldBe Outcome.Done
        val handBefore = driver.getHandSize(player)
        driver.bothPass()

        driver.pendingDecision shouldNotBe null
        var guard = 0
        while (driver.isPaused && guard++ < 5) driver.autoResolveDecision()
        driver.getHandSize(player) shouldBe handBefore - 1
    }
})
