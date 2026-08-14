package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.RuneBrandJuggler
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Rune-Brand Juggler (MKM #229).
 *
 * "When this creature enters, suspect up to one other target creature you control.
 *  {3}{B}{R}, Sacrifice a suspected creature: Target creature gets -5/-5 until end of turn."
 *
 * The claim under test is that "Sacrifice a **suspected** creature" is a real cost restriction and
 * not a fail-open filter — the historically expensive bug shape in this engine's cost enumerators.
 * A cost that quietly accepts any creature would turn a two-mana uncommon into unconditional
 * repeatable removal.
 *
 * Covers:
 *  - The ETB suspects the chosen other creature, and that creature then pays the sacrifice cost.
 *  - An un-suspected creature is rejected as payment (the fail-open case).
 *  - The Juggler can't suspect itself ("other"), so its own body never becomes the fuel.
 */
class RuneBrandJugglerScenarioTest : FunSpec({

    val abilityId = RuneBrandJuggler.activatedAbilities.single().id
    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        return driver
    }

    /**
     * Put a Juggler onto the battlefield by casting it, answering the ETB's optional target with
     * [suspectTarget] (or declining when null). Returns the Juggler's entity id.
     */
    fun castJuggler(d: GameTestDriver, caster: EntityId, suspectTarget: EntityId?): EntityId {
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.giveMana(caster, Color.BLACK, 1)
        d.giveMana(caster, Color.RED, 1)

        val card = d.putCardInHand(caster, "Rune-Brand Juggler")
        val cast = d.castSpell(caster, card)
        withClue("casting Rune-Brand Juggler: ${cast.error}") { cast.error shouldBe null }

        // Juggler resolves and enters; the ETB trigger then asks for its up-to-one target.
        d.bothPass()
        if (suspectTarget != null) {
            withClue("expected the ETB trigger's target selection") {
                d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            }
            d.submitTargetSelection(caster, listOf(suspectTarget)).error shouldBe null
        }
        d.bothPass()

        return d.findPermanent(caster, "Rune-Brand Juggler")!!
    }

    test("the suspected creature can pay the sacrifice cost and the target gets -5/-5") {
        val d = createDriver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)

        val fodder = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val victim = d.putCreatureOnBattlefield(p2, "Grizzly Bears")

        castJuggler(d, p1, suspectTarget = fodder)

        withClue("the ETB suspected the chosen other creature") {
            projector.project(d.state).isSuspected(fodder) shouldBe true
        }

        d.giveMana(p1, Color.BLACK, 1)
        d.giveMana(p1, Color.RED, 1)
        d.giveMana(p1, Color.GREEN, 3)

        val juggler = d.findPermanent(p1, "Rune-Brand Juggler")!!
        val result = d.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = juggler,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(d.state, victim)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder))
            )
        )
        withClue("activating with a suspected creature as payment") { result.isSuccess shouldBe true }

        // The cost is paid on activation, so the fodder is already gone before resolution.
        d.findPermanent(p1, "Grizzly Bears") shouldBe null

        d.bothPass()

        // A 2/2 given -5/-5 is a -3/-3 and dies to state-based actions.
        withClue("the -5/-5 killed the targeted 2/2") {
            d.findPermanent(p2, "Grizzly Bears") shouldBe null
            d.state.getGraveyard(p2) shouldContain victim
        }
    }

    test("an un-suspected creature is rejected as payment — the cost filter is not fail-open") {
        val d = createDriver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)

        val innocent = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val victim = d.putCreatureOnBattlefield(p2, "Grizzly Bears")

        // Decline the ETB target, so nothing on the board is suspected.
        castJuggler(d, p1, suspectTarget = null)
        projector.project(d.state).isSuspected(innocent) shouldBe false

        d.giveMana(p1, Color.BLACK, 1)
        d.giveMana(p1, Color.RED, 1)
        d.giveMana(p1, Color.GREEN, 3)

        val juggler = d.findPermanent(p1, "Rune-Brand Juggler")!!
        val result = d.submit(
            ActivateAbility(
                playerId = p1,
                sourceId = juggler,
                abilityId = abilityId,
                targets = listOf(entityIdToChosenTarget(d.state, victim)),
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(innocent))
            )
        )
        withClue("a creature that isn't suspected must not satisfy the cost") {
            result.isSuccess shouldBe false
        }

        // Nothing was sacrificed and the would-be victim is untouched.
        d.findPermanent(p1, "Grizzly Bears") shouldBe innocent
        projector.project(d.state).getToughness(victim) shouldBe 2
    }

    test("the Juggler cannot suspect itself — the ETB target must be another creature") {
        val d = createDriver()
        val p1 = d.activePlayer!!

        // No other creature on the board, so the up-to-one target has no legal choice at all.
        val juggler = castJuggler(d, p1, suspectTarget = null)

        withClue("'other target creature you control' excludes the Juggler itself") {
            projector.project(d.state).isSuspected(juggler) shouldBe false
        }
    }
})
