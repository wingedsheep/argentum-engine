package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.mtg.sets.definitions.m10.cards.ChildOfNight
import com.wingedsheep.mtg.sets.definitions.m10.cards.VampireNocturnus
import com.wingedsheep.mtg.sets.definitions.m10.cards.WarpathGhoul
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Vampire Nocturnus: the buff reads the hidden-zone top card of its controller's library, so the
 * condition is evaluated against a card no battlefield filter sees.
 */
class VampireNocturnusScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(VampireNocturnus, ChildOfNight, WarpathGhoul))
        initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("black top card buffs Nocturnus and other Vampires you control, not other creatures") {
        val d = driver()
        val nocturnus = d.putCreatureOnBattlefield(d.player1, "Vampire Nocturnus")
        val child = d.putCreatureOnBattlefield(d.player1, "Child of Night")
        val ghoul = d.putCreatureOnBattlefield(d.player1, "Warpath Ghoul")
        val enemyChild = d.putCreatureOnBattlefield(d.player2, "Child of Night")
        d.putCardOnTopOfLibrary(d.player1, "Warpath Ghoul")

        val p = d.state.projectedState
        p.getPower(nocturnus) shouldBe 5
        p.getToughness(nocturnus) shouldBe 4
        p.hasKeyword(nocturnus, Keyword.FLYING) shouldBe true
        p.getPower(child) shouldBe 4
        p.getToughness(child) shouldBe 2
        p.hasKeyword(child, Keyword.FLYING) shouldBe true
        // Black, but not a Vampire.
        p.getPower(ghoul) shouldBe 3
        p.hasKeyword(ghoul, Keyword.FLYING) shouldBe false
        // A Vampire, but not yours.
        p.getPower(enemyChild) shouldBe 2
        p.hasKeyword(enemyChild, Keyword.FLYING) shouldBe false
    }

    test("nonblack or colorless top card gives nothing, and the bonus tracks the top card") {
        val d = driver()
        val nocturnus = d.putCreatureOnBattlefield(d.player1, "Vampire Nocturnus")
        val child = d.putCreatureOnBattlefield(d.player1, "Child of Night")

        d.putCardOnTopOfLibrary(d.player1, "Giant Growth")
        d.state.projectedState.getPower(nocturnus) shouldBe 3
        d.state.projectedState.hasKeyword(nocturnus, Keyword.FLYING) shouldBe false
        d.state.projectedState.getPower(child) shouldBe 2

        d.putCardOnTopOfLibrary(d.player1, "Child of Night")
        d.state.projectedState.getPower(nocturnus) shouldBe 5
        d.state.projectedState.hasKeyword(child, Keyword.FLYING) shouldBe true

        d.putCardOnTopOfLibrary(d.player1, "Forest")
        d.state.projectedState.getPower(nocturnus) shouldBe 3
        d.state.projectedState.hasKeyword(child, Keyword.FLYING) shouldBe false
    }

    test("an opponent's black top card does not turn it on") {
        val d = driver()
        val nocturnus = d.putCreatureOnBattlefield(d.player1, "Vampire Nocturnus")
        d.putCardOnTopOfLibrary(d.player1, "Giant Growth")
        d.putCardOnTopOfLibrary(d.player2, "Child of Night")
        d.state.projectedState.getPower(nocturnus) shouldBe 3
    }

    test("empty library gives no bonus") {
        val d = driver()
        val nocturnus = d.putCreatureOnBattlefield(d.player1, "Vampire Nocturnus")
        d.replaceState(d.state.copy(zones = d.state.zones + (ZoneKey(d.player1, Zone.LIBRARY) to emptyList())))
        d.state.projectedState.getPower(nocturnus) shouldBe 3
        d.state.projectedState.hasKeyword(nocturnus, Keyword.FLYING) shouldBe false
    }

    test("top card is revealed to both players while Nocturnus is on the battlefield") {
        val d = driver()
        val nocturnus = d.putCreatureOnBattlefield(d.player1, "Vampire Nocturnus")
        val top = d.putCardOnTopOfLibrary(d.player1, "Child of Night")
        val transformer = ClientStateTransformer(cardRegistry = d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))
        transformer.transform(d.state, viewingPlayerId = d.player2).cards.keys shouldContain top
        d.moveToGraveyard(nocturnus)
        transformer.transform(d.state, viewingPlayerId = d.player2).cards.keys shouldNotContain top
    }
})
