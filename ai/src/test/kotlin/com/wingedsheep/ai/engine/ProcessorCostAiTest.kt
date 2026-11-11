package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bfz.cards.CrypticCruiser
import com.wingedsheep.mtg.sets.definitions.bfz.cards.MistIntruder
import com.wingedsheep.mtg.sets.definitions.bfz.cards.ProcessorAssault
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The BFZ processor cost — "put a card an opponent owns from exile into that player's graveyard" —
 * as the AI reads it: a payoff on the [SynergyAxis.OPPONENT_EXILE] axis that ingest and exile
 * removal feed, and a cost the AI pays with the opponent's exiled card when it casts the spell.
 */
class ProcessorCostAiTest : FunSpec({

    test("processors reward cards opponents own in exile; ingest feeds them, a vanilla body doesn't") {
        SynergyAxes.of(CrypticCruiser).rewards shouldContain SynergyAxis.OPPONENT_EXILE
        SynergyAxes.of(ProcessorAssault).rewards shouldContain SynergyAxis.OPPONENT_EXILE
        SynergyAxes.of(MistIntruder).feeds shouldContain SynergyAxis.OPPONENT_EXILE
        SynergyAxes.of(TestCards.all.single { it.name == "Grizzly Bears" }).feeds shouldNotContain
            SynergyAxis.OPPONENT_EXILE
        // A processor doesn't feed itself: the cost spends the resource.
        SynergyAxes.of(CrypticCruiser).feeds shouldNotContain SynergyAxis.OPPONENT_EXILE
    }

    test("the AI casts Processor Assault, paying with the opponent's exiled card, at the opponent's creature") {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ProcessorAssault)
        d.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)
        repeat(2) { d.putLandOnBattlefield(player, "Mountain") }
        val courser = d.putCreatureOnBattlefield(opponent, "Centaur Courser")
        d.putCardInExile(player, "Mountain")
        val theirs = d.putCardInExile(opponent, "Mountain")
        val assault = d.putCardInHand(player, ProcessorAssault.name)

        val chosen = AIPlayer.create(d.services.cardRegistry, player).chooseAction(d.state)
            .shouldBeInstanceOf<CastSpell>()
        chosen.cardId shouldBe assault
        chosen.targets shouldContainExactly listOf(ChosenTarget.Permanent(courser))
        chosen.additionalCostPayment!!.exiledCards shouldContainExactly listOf(theirs)

        d.submit(chosen).error shouldBe null
        d.getGraveyard(opponent) shouldContain theirs
    }
})
