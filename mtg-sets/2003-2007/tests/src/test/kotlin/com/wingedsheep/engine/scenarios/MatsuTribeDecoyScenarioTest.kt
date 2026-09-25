package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.chk.cards.MatsuTribeDecoy
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Matsu-Tribe Decoy (CHK) — "{2}{G}: Target creature blocks this creature this turn if able.
 * Whenever this creature deals combat damage to a creature, tap that creature and it doesn't untap
 * during its controller's next untap step."
 *
 * The two abilities are meant to combine: lure a creature into blocking, then lock it down. The
 * 1/3 Decoy survives a 2/2 blocker, so the blocker survives too and its tapped state is readable.
 */
class MatsuTribeDecoyScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + MatsuTribeDecoy)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    test("the lured creature must block, then is tapped and skips its controller's next untap step") {
        val d = driver()
        val me = d.player1
        val opp = d.player2
        val decoy = d.putCreatureOnBattlefield(me, "Matsu-Tribe Decoy")
        d.removeSummoningSickness(decoy)
        val bear = d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        d.giveMana(me, Color.GREEN, 1)
        d.giveColorlessMana(me, 2)
        d.submit(
            ActivateAbility(
                playerId = me,
                sourceId = decoy,
                abilityId = MatsuTribeDecoy.activatedAbilities.first().id,
                targets = listOf(ChosenTarget.Permanent(bear)),
            )
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(decoy), defendingPlayer = opp).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        withClue("the bear is required to block the Decoy") {
            d.declareBlockers(opp, emptyMap()).outcome shouldNotBe Outcome.Done
            d.declareBlockers(opp, mapOf(bear to listOf(decoy))).error shouldBe null
        }

        d.passPriorityUntil(Step.END)
        withClue("the Decoy's 1 combat damage triggered the lock on the bear") {
            d.isTapped(bear) shouldBe true
        }

        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe opp
        withClue("the bear's controller's untap step skipped it") {
            d.isTapped(bear) shouldBe true
        }
    }
})
