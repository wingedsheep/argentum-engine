package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.stx.cards.ElementalExpressionist
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Elemental Expressionist (STX #181).
 *
 * The magecraft target gains, for the turn, an exile-instead replacement scoped to itself and a
 * "when this is put into exile" token maker. The rulings' two load-bearing points: each resolution
 * on the same creature adds another instance of the token trigger, and only the targeted creature
 * is exiled instead of dying.
 */
class ElementalExpressionistScenarioTest : FunSpec({

    data class Setup(val d: GameTestDriver, val me: EntityId, val opp: EntityId)

    fun setup(): Setup {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(ElementalExpressionist))
        d.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        return Setup(d, me, d.getOpponent(me))
    }

    /** Cast Lightning Bolt at [boltTarget], pointing the magecraft trigger at [magecraftTarget], and resolve all. */
    fun GameTestDriver.boltWithMagecraft(me: EntityId, boltTarget: ChosenTarget, magecraftTarget: EntityId) {
        val bolt = putCardInHand(me, "Lightning Bolt")
        giveMana(me, Color.RED, 1)
        castSpellWithTargets(me, bolt, listOf(boltTarget)).error shouldBe null
        pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        submitTargetSelection(me, listOf(magecraftTarget)).error shouldBe null
        while (stackSize > 0) bothPass()
    }

    fun GameTestDriver.elementalTokens(player: EntityId) = getCreatures(player).filter {
        state.getEntity(it)?.has<TokenComponent>() == true &&
            state.projectedState.getPower(it) == 4 && state.projectedState.getToughness(it) == 4
    }

    test("a creature killed after the magecraft resolves is exiled and leaves a 4/4 Elemental") {
        val (d, me, _) = setup()
        d.putCreatureOnBattlefield(me, "Elemental Expressionist")
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val other = d.putCreatureOnBattlefield(me, "Grizzly Bears")

        d.boltWithMagecraft(me, ChosenTarget.Permanent(bears), bears)

        d.getExile(me) shouldContain bears
        d.getGraveyard(me) shouldNotContain bears
        d.elementalTokens(me).size shouldBe 1

        // The other Bears never gained the replacement: a second bolt at it, with the magecraft
        // pointed elsewhere, sends it to the graveyard.
        d.boltWithMagecraft(me, ChosenTarget.Permanent(other), d.findPermanent(me, "Elemental Expressionist")!!)
        d.getGraveyard(me) shouldContain other
    }

    test("two resolutions on the same creature make two Elementals") {
        val (d, me, opp) = setup()
        d.putCreatureOnBattlefield(me, "Elemental Expressionist")
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")

        d.boltWithMagecraft(me, ChosenTarget.Player(opp), bears)
        d.boltWithMagecraft(me, ChosenTarget.Permanent(bears), bears)

        d.getExile(me) shouldContain bears
        d.elementalTokens(me).size shouldBe 2
    }
})
