package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.Wildcall
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Wildcall (FRF) — {X}{G}{G} Sorcery. "Manifest the top card of your library, then put X +1/+1
 * counters on it."
 *
 * Proves the spell's X reaches the counters placed on the `"manifested"` collection: the face-down
 * permanent is a 2/2 that ends up with exactly X +1/+1 counters, and X = 0 just manifests.
 */
class WildcallScenarioTest : FunSpec({

    fun castWildcall(x: Int): Pair<GameTestDriver, EntityId> {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + Wildcall)
            initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        }
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val top = d.putCardOnTopOfLibrary(you, "Savannah Lions")
        val spell = d.putCardInHand(you, "Wildcall")
        d.giveMana(you, Color.GREEN, 2 + x)
        d.castXSpell(you, spell, x).error shouldBe null
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        d.state.stack.isEmpty() shouldBe true
        d.getPermanents(you) shouldContain top
        val entity = d.state.getEntity(top)
        entity?.get<FaceDownComponent>() shouldBe FaceDownComponent
        entity?.get<FaceDownModeComponent>()?.mode shouldBe FaceDownMode.MANIFEST
        return d to top
    }

    test("manifests the top card and puts X +1/+1 counters on it") {
        val (d, manifested) = castWildcall(3)
        d.state.getEntity(manifested)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
        d.state.projectedState.getPower(manifested) shouldBe 5
        d.state.projectedState.getToughness(manifested) shouldBe 5
    }

    test("X = 0 just manifests a vanilla 2/2") {
        val (d, manifested) = castWildcall(0)
        (d.state.getEntity(manifested)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
        d.state.projectedState.getPower(manifested) shouldBe 2
        d.state.projectedState.getToughness(manifested) shouldBe 2
    }
})
