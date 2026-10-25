package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dst.cards.MycosynthLattice
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Mycosynth Lattice (DST #130) — "All permanents are artifacts in addition to their other types.
 * All cards that aren't on the battlefield, spells, and permanents are colorless. Players may spend
 * mana as though it were mana of any color."
 *
 * The two engine halves are proved in `SetColorsOffBattlefieldTest` and `SpendManaAsAnyColorTest`;
 * this pins the card's wiring and its rulings: permanents become artifacts but cards elsewhere
 * don't; everything everywhere is colorless; and both players can pay colored costs with off-color
 * lands.
 */
class MycosynthLatticeScenarioTest : FunSpec({

    val predicates = PredicateEvaluator(cardRegistry = null)

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(MycosynthLattice))
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.matches(id: com.wingedsheep.sdk.model.EntityId, filter: GameObjectFilter) =
        predicates.matches(state, state.projectedState, id, filter, PredicateContext(controllerId = player1))

    test("permanents are colorless artifacts; a card in hand is colorless but not an artifact") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Mycosynth Lattice")
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        val island = d.putLandOnBattlefield(d.player2, "Island")
        val inHand = d.putCardInHand(d.player2, "Centaur Courser")

        d.state.projectedState.hasType(courser, "ARTIFACT") shouldBe true
        d.state.projectedState.isCreature(courser) shouldBe true
        d.state.projectedState.hasType(island, "ARTIFACT") shouldBe true
        d.state.projectedState.getColors(courser) shouldBe emptySet()
        d.matches(inHand, GameObjectFilter.Artifact) shouldBe false
        d.matches(inHand, GameObjectFilter.Any.withColor(Color.GREEN)) shouldBe false
    }

    test("an opponent taps Islands for a red spell, which is colorless on the stack") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Mycosynth Lattice")
        d.putLandOnBattlefield(d.player1, "Island")
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")

        d.castSpell(d.player1, bolt, listOf(d.player2)).error shouldBe null
        d.matches(bolt, GameObjectFilter.Any.withColor(Color.RED)) shouldBe false
        d.bothPass()
        d.getLifeTotal(d.player2) shouldBe 17
    }
})
