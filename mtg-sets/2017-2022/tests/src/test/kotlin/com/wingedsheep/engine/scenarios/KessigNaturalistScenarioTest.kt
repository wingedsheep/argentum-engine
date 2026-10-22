package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ManaExpiry
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Kessig Naturalist — "Whenever this creature attacks, add {R} or {G}. Until end of turn, you don't
 * lose this mana as steps and phases end."
 *
 * The colour is chosen as the trigger resolves, and whichever mana is added must be tagged
 * [ManaExpiry.KEPT_UNTIL_END_OF_TURN] so it survives the step/phase emptying into the postcombat
 * main phase.
 */
class KessigNaturalistScenarioTest : FunSpec({

    fun GameTestDriver.kept(playerId: EntityId, color: Color) =
        (state.getEntity(playerId)?.get<ManaPoolComponent>()?.restrictedMana ?: emptyList())
            .count { it.color == color && it.expiry == ManaExpiry.KEPT_UNTIL_END_OF_TURN }

    fun GameTestDriver.attackAndChoose(optionIndex: Int): EntityId {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val attacker = activePlayer!!
        val defender = getOpponent(attacker)
        val naturalist = putCreatureOnBattlefield(attacker, "Kessig Naturalist")
        removeSummoningSickness(naturalist)

        passPriorityUntil(Step.DECLARE_ATTACKERS)
        declareAttackers(attacker, listOf(naturalist), defender)
        bothPass() // the attack trigger resolves and asks for {R} or {G}

        val choice = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        choice.options shouldBe listOf("Add {R}", "Add {G}")
        submitDecision(attacker, OptionChosenResponse(choice.id, optionIndex)).error shouldBe null
        return attacker
    }

    test("choosing {G} adds kept green mana that survives into the postcombat main phase") {
        val d = GameTestDriver()
        val attacker = d.attackAndChoose(optionIndex = 1)
        d.kept(attacker, Color.GREEN) shouldBe 1
        d.kept(attacker, Color.RED) shouldBe 0

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        d.kept(attacker, Color.GREEN) shouldBe 1
    }

    test("choosing {R} adds kept red mana, lost once the turn ends") {
        val d = GameTestDriver()
        val attacker = d.attackAndChoose(optionIndex = 0)
        d.kept(attacker, Color.RED) shouldBe 1
        d.kept(attacker, Color.GREEN) shouldBe 0

        d.passPriorityUntil(Step.END)
        d.kept(attacker, Color.RED) shouldBe 1

        d.passPriorityUntil(Step.UPKEEP)
        d.state.getEntity(attacker)?.get<ManaPoolComponent>()?.isEmpty shouldBe true
    }
})
