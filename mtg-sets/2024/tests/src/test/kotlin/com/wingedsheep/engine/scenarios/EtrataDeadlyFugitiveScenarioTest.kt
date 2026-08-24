package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.EtrataDeadlyFugitive
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Etrata, Deadly Fugitive (MKM) — {1}{U}{B} 1/4 Legendary Creature — Vampire Assassin.
 *
 * The card under test for [com.wingedsheep.sdk.scripting.effects.SuccessCriterion.TurnedFaceUp]:
 * the ability Etrata grants reads "Turn this creature face up. **If you can't**, exile it, then you
 * may cast the exiled card without paying its mana cost", and the only honest way to know whether
 * you could is to try. Both branches get a test, plus the cloak trigger that manufactures the
 * face-down creatures in the first place.
 */
class EtrataDeadlyFugitiveScenarioTest : FunSpec({

    // A creature card: cloaked, it *can* be turned face up (CR 701.58b).
    val bear = card("Cloakable Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    // An Assassin, so its combat damage fires Etrata's trigger without Etrata herself attacking.
    val assassin = card("Test Assassin") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Human Assassin"
        power = 2
        toughness = 2
    }

    val grantedAbilityId = EtrataDeadlyFugitive.staticAbilities
        .filterIsInstance<GrantActivatedAbility>()
        .single()
        .ability
        .id

    val allCards = TestCards.all + listOf(bear, assassin, EtrataDeadlyFugitive)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    /** Cloak [cardName] onto [playerId]'s battlefield, deriving turn-up data the way a real entry does. */
    fun GameTestDriver.cloak(playerId: EntityId, cardName: String): EntityId {
        val id = putPermanentOnBattlefield(playerId, cardName)
        val cardDef = cardRegistry.requireCard(cardName)
        replaceState(
            state.updateEntity(id) { container ->
                var c = container.with(FaceDownComponent)
                    .with(FaceDownModeComponent(FaceDownMode.CLOAK))
                FaceDownTurnUp.dataFor(cardDef, cardName, FaceDownMode.CLOAK)?.let { c = c.with(it) }
                c
            }
        )
        removeSummoningSickness(id)
        return id
    }

    context("the granted ability — \"turn this face up. If you can't, …\"") {

        test("a cloaked creature card turns face up and is not exiled") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Island" to 40))
            val me = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            driver.putCreatureOnBattlefield(me, "Etrata, Deadly Fugitive")
            val hidden = driver.cloak(me, "Cloakable Bear")

            driver.giveMana(me, Color.BLUE, 2)
            driver.giveMana(me, Color.BLACK, 2)
            driver.submitSuccess(
                ActivateAbility(playerId = me, sourceId = hidden, abilityId = grantedAbilityId)
            )
            var guard = 0
            while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

            driver.state.getEntity(hidden)?.get<FaceDownComponent>() shouldBe null
            driver.state.getEntity(hidden)?.get<CardComponent>()?.name shouldBe "Cloakable Bear"
            driver.state.getBattlefield().contains(hidden) shouldBe true
        }

        test("a cloaked instant card can't be turned face up, so it is exiled instead") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Island" to 40))
            val me = driver.activePlayer!!
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            driver.putCreatureOnBattlefield(me, "Etrata, Deadly Fugitive")
            // CR 701.58g — a cloaked instant that would turn face up is revealed and stays face
            // down, so no TurnFaceUpEvent is emitted and the gate takes the failure branch.
            val hidden = driver.cloak(me, "Lightning Bolt")

            driver.giveMana(me, Color.BLUE, 2)
            driver.giveMana(me, Color.BLACK, 2)
            driver.submitSuccess(
                ActivateAbility(playerId = me, sourceId = hidden, abilityId = grantedAbilityId)
            )

            var guard = 0
            while (guard++ < 30) {
                if (driver.state.pendingDecision != null) {
                    driver.autoResolveDecision()
                } else if (driver.state.stack.isNotEmpty()) {
                    driver.bothPass()
                } else {
                    break
                }
            }

            driver.state.getBattlefield().contains(hidden) shouldBe false
            driver.getExile(me).contains(hidden) shouldBe true
        }

        test("an opponent's face-down creature does not gain the ability") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Island" to 40))
            val me = driver.activePlayer!!
            val opp = driver.getOpponent(me)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            driver.putCreatureOnBattlefield(me, "Etrata, Deadly Fugitive")
            val theirs = driver.cloak(opp, "Cloakable Bear")

            // "Face-down creatures **you control**" — the grant's filter is controller-scoped.
            driver.legalActions(opp)
                .none { (it.action as? ActivateAbility)?.sourceId == theirs } shouldBe true
        }
    }

    context("the cloak trigger") {

        test("an Assassin's combat damage cloaks the top card of the damaged player's library") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Island" to 40))
            val me = driver.activePlayer!!
            val opp = driver.getOpponent(me)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            driver.putCreatureOnBattlefield(me, "Etrata, Deadly Fugitive")
            val killer = driver.putCreatureOnBattlefield(me, "Test Assassin")
            driver.removeSummoningSickness(killer)

            val topOfTheirLibrary = driver.putCardOnTopOfLibrary(opp, "Cloakable Bear")

            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            driver.declareAttackers(me, listOf(killer), opp)
            driver.declareNoBlockers(opp)

            var guard = 0
            while (guard++ < 40) {
                if (driver.state.pendingDecision != null) {
                    driver.autoResolveDecision()
                } else if (driver.state.stack.isNotEmpty()) {
                    driver.bothPass()
                } else if (driver.state.getBattlefield().contains(topOfTheirLibrary)) {
                    break
                } else {
                    driver.bothPass()
                }
            }

            // It came off the opponent's library but is a face-down 2/2 under *my* control.
            driver.state.getBattlefield().contains(topOfTheirLibrary) shouldBe true
            driver.state.getEntity(topOfTheirLibrary)?.get<FaceDownComponent>().shouldNotBeNull()
            driver.state.projectedState.getController(topOfTheirLibrary) shouldBe me
            driver.state.getEntity(topOfTheirLibrary)
                ?.get<FaceDownModeComponent>()?.mode shouldBe FaceDownMode.CLOAK
        }

        test("a non-Assassin's combat damage cloaks nothing") {
            val driver = createDriver()
            driver.initMirrorMatch(deck = Deck.of("Island" to 40))
            val me = driver.activePlayer!!
            val opp = driver.getOpponent(me)
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            driver.putCreatureOnBattlefield(me, "Etrata, Deadly Fugitive")
            val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
            driver.removeSummoningSickness(bears)

            val topOfTheirLibrary = driver.putCardOnTopOfLibrary(opp, "Cloakable Bear")

            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            driver.declareAttackers(me, listOf(bears), opp)
            driver.declareNoBlockers(opp)

            var guard = 0
            while (guard++ < 20) {
                if (driver.state.pendingDecision != null) driver.autoResolveDecision()
                else if (driver.state.stack.isNotEmpty()) driver.bothPass()
                else break
            }

            driver.state.getBattlefield().contains(topOfTheirLibrary) shouldBe false
        }
    }
})
