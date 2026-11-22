package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dgm.cards.PossibilityStorm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Possibility Storm {3}{R}{R} — Enchantment (DGM #34).
 * "Whenever a player casts a spell from their hand, that player exiles it, then exiles cards from
 *  the top of their library until they exile a card that shares a card type with it. That player
 *  may cast that card without paying its mana cost. Then they put all cards exiled with this
 *  enchantment on the bottom of their library in a random order."
 *
 * Pins: the original spell never resolves; the walk stops at the first card sharing a card type
 * (an artifact creature shares "creature" with a plain creature); the free cast is offered to the
 * *caster* — an opponent casting gets the choice and walks their own library; the free cast (from
 * exile) doesn't retrigger; and everything left in exile, the original included, ends on the
 * bottom of the caster's library.
 */
class PossibilityStormScenarioTest : FunSpec({

    fun setup(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + PossibilityStorm)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(d.player1, "Possibility Storm")
        return d
    }

    fun GameTestDriver.library(player: EntityId) = state.getZone(ZoneKey(player, Zone.LIBRARY))
    fun GameTestDriver.names(ids: List<EntityId>) = ids.map { getCardName(it) }

    /** Empty the stack; [caster] answers the may-cast with [cast] and aims every Bolt at [boltTarget]. */
    fun GameTestDriver.drain(caster: EntityId, cast: Boolean, boltTarget: EntityId? = null): Int {
        var mayPrompts = 0
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard++ < 60) {
            val decision = state.pendingDecision
            when {
                decision is YesNoDecision -> {
                    withClue("the caster, not Possibility Storm's controller, decides the free cast") {
                        decision.playerId shouldBe caster
                    }
                    mayPrompts++
                    submitYesNo(caster, cast).error shouldBe null
                }
                decision is ChooseTargetsDecision && boltTarget != null -> {
                    decision.playerId shouldBe caster
                    submitTargetSelection(caster, listOf(boltTarget)).error shouldBe null
                }
                decision != null -> autoResolveDecision()
                else -> bothPass()
            }
        }
        return mayPrompts
    }

    test("a creature from hand is exiled and the first creature-sharing card is cast for free") {
        val d = setup()
        val me = d.player1
        // Library top to bottom: Lightning Bolt, Mountain, Artifact Creature, Mountains…
        d.putCardOnTopOfLibrary(me, "Artifact Creature")
        d.putCardOnTopOfLibrary(me, "Mountain")
        d.putCardOnTopOfLibrary(me, "Lightning Bolt")
        val librarySize = d.library(me).size

        val courser = d.putCardInHand(me, "Centaur Courser")
        d.giveMana(me, Color.GREEN, 3)
        d.castSpell(me, courser).error shouldBe null

        d.drain(me, cast = true) shouldBe 1

        withClue("the free Artifact Creature resolved; the original Courser never did") {
            (d.findPermanent(me, "Artifact Creature") != null) shouldBe true
            d.findPermanent(me, "Centaur Courser") shouldBe null
        }
        d.getExile(me) shouldBe emptyList()
        withClue("the original spell and the skipped cards went to the bottom") {
            d.names(d.library(me).takeLast(3)) shouldContainExactlyInAnyOrder
                listOf("Centaur Courser", "Lightning Bolt", "Mountain")
            d.library(me).size shouldBe librarySize
        }
    }

    test("declining the free cast puts the hit on the bottom too") {
        val d = setup()
        val me = d.player1
        val opponent = d.player2
        // Library top to bottom: Centaur Courser, Lightning Bolt, Mountains…
        d.putCardOnTopOfLibrary(me, "Lightning Bolt")
        d.putCardOnTopOfLibrary(me, "Centaur Courser")

        val bolt = d.putCardInHand(me, "Lightning Bolt")
        d.giveMana(me, Color.RED, 1)
        d.castSpell(me, bolt, listOf(opponent)).error shouldBe null

        d.drain(me, cast = false) shouldBe 1

        withClue("neither Bolt resolved") { d.getLifeTotal(opponent) shouldBe 20 }
        d.getExile(me) shouldBe emptyList()
        d.names(d.library(me).takeLast(3)) shouldContainExactlyInAnyOrder
            listOf("Lightning Bolt", "Lightning Bolt", "Centaur Courser")
    }

    test("an opponent casting from hand walks their own library and casts the hit themselves") {
        val d = setup()
        val me = d.player1
        val opponent = d.player2
        d.putCardOnTopOfLibrary(opponent, "Lightning Bolt")
        d.putCardOnTopOfLibrary(opponent, "Mountain")
        val myLibrary = d.library(me)

        // Hand priority to the opponent with an empty stack.
        d.passPriority(me)
        val bolt = d.putCardInHand(opponent, "Lightning Bolt")
        d.giveMana(opponent, Color.RED, 1)
        d.castSpell(opponent, bolt, listOf(me)).error shouldBe null

        d.drain(opponent, cast = true, boltTarget = me) shouldBe 1

        withClue("exactly one Bolt — the free one, cast from exile, which doesn't retrigger") {
            d.getLifeTotal(me) shouldBe 17
        }
        d.getExile(opponent) shouldBe emptyList()
        d.names(d.library(opponent).takeLast(2)) shouldContainExactlyInAnyOrder
            listOf("Lightning Bolt", "Mountain")
        withClue("Possibility Storm's controller's library is untouched") { d.library(me) shouldBe myLibrary }
    }
})
