package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.SyrCarahTheBold
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Syr Carah, the Bold (ELD #145):
 *   "Whenever Syr Carah or an instant or sorcery spell you control deals damage to a player, exile
 *    the top card of your library. You may play that card this turn.
 *    {T}: Syr Carah deals 1 damage to any target."
 *
 * The trigger is two abilities (self + your instant/sorcery spells) sharing one impulse effect; these
 * tests pin both halves, and that damage to a creature or an opponent's spell doesn't fire it.
 */
class SyrCarahTheBoldScenarioTest : FunSpec({

    fun start(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + SyrCarahTheBold)
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("Syr Carah pinging a player exiles your top card, and you may play it this turn") {
        val d = start()
        val you = d.player1
        val opp = d.getOpponent(you)
        val carah = d.putCreatureOnBattlefield(you, SyrCarahTheBold.name)
        d.removeSummoningSickness(carah)
        val top = d.putCardOnTopOfLibrary(you, "Mountain")

        val ping = SyrCarahTheBold.activatedAbilities.single()
        d.submitSuccess(ActivateAbility(you, carah, ping.id, listOf(ChosenTarget.Player(opp))))
        d.bothPass() // resolve the ping
        d.bothPass() // resolve the impulse trigger

        d.getLifeTotal(opp) shouldBe 19
        d.getExile(you) shouldContain top
        d.submitSuccess(PlayLand(you, top))
        d.getExile(you) shouldNotContain top
    }

    test("your instant dealing damage to a player triggers it") {
        val d = start()
        val you = d.player1
        val opp = d.getOpponent(you)
        d.putCreatureOnBattlefield(you, SyrCarahTheBold.name)
        val top = d.putCardOnTopOfLibrary(you, "Mountain")

        val bolt = d.putCardInHand(you, "Lightning Bolt")
        d.giveMana(you, Color.RED, 1)
        d.castSpell(you, bolt, listOf(opp)).error shouldBe null
        d.bothPass() // resolve Bolt
        d.bothPass() // resolve the impulse trigger

        d.getLifeTotal(opp) shouldBe 17
        d.getExile(you) shouldContain top
    }

    test("damage to a creature, or an opponent's spell hitting you, does not trigger it") {
        val d = start()
        val you = d.player1
        val opp = d.getOpponent(you)
        val carah = d.putCreatureOnBattlefield(you, SyrCarahTheBold.name)
        d.removeSummoningSickness(carah)
        val bears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val top = d.putCardOnTopOfLibrary(you, "Mountain")

        val ping = SyrCarahTheBold.activatedAbilities.single()
        d.submitSuccess(ActivateAbility(you, carah, ping.id, listOf(ChosenTarget.Permanent(bears))))
        d.bothPass()
        d.getExile(you) shouldNotContain top

        d.passPriority(you)
        val oppBolt = d.putCardInHand(opp, "Lightning Bolt")
        d.giveMana(opp, Color.RED, 1)
        d.castSpell(opp, oppBolt, listOf(you)).error shouldBe null
        d.bothPass()

        d.getLifeTotal(you) shouldBe 17
        d.getExile(you) shouldNotContain top
        d.getExile(opp) shouldBe emptyList()
    }
})
