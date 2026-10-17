package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.neo.cards.CloudsteelKirin
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Cloudsteel Kirin — {2}{W} Artifact Creature — Equipment Kirin 3/2
 * Flying. Equipped creature has flying and "You can't lose the game and your opponents can't win
 * the game." Reconfigure {5}.
 *
 * The lock lives on the equipped creature: unattached, Kirin protects nobody, and unattaching it
 * at 0 or less life loses the game (2022-02-18 ruling).
 */
class CloudsteelKirinScenarioTest : FunSpec({

    val loseThirty = card("Kirin Test Lifeloss") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.LoseLife(30, EffectTarget.Controller) }
    }

    val victory = card("Kirin Test Victory") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.WinGame(EffectTarget.Controller) }
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(CloudsteelKirin, loseThirty, victory))
        initMirrorMatch(deck = Deck.of("Plains" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.settle() {
        var guard = 0
        while (!state.gameOver && state.stack.isNotEmpty() && guard++ < 10) bothPass()
    }

    fun GameTestDriver.castInstant(player: EntityId, name: String) {
        val holder = state.priorityPlayerId
        if (holder != null && holder != player) passPriority(holder)
        castSpell(player, putCardInHand(player, name)).outcome shouldBe Outcome.Done
        settle()
    }

    fun GameTestDriver.equip(): Pair<EntityId, EntityId> {
        val kirin = putPermanentOnBattlefield(player1, CloudsteelKirin.name)
        val bears = putCreatureOnBattlefield(player1, "Grizzly Bears")
        val attach = CloudsteelKirin.activatedAbilities.first()
        giveColorlessMana(player1, 5)
        submit(ActivateAbility(player1, kirin, attach.id, listOf(ChosenTarget.Permanent(bears)))).outcome shouldBe Outcome.Done
        settle()
        return kirin to bears
    }

    test("equipped creature flies and its controller survives at 0 or less life; unattaching ends it") {
        val d = driver()
        val (kirin, bears) = d.equip()
        d.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true

        d.castInstant(d.player1, "Kirin Test Lifeloss")
        d.getLifeTotal(d.player1) shouldBe -10
        d.state.gameOver.shouldBeFalse()

        val unattach = CloudsteelKirin.activatedAbilities[1]
        d.giveColorlessMana(d.player1, 5)
        d.submit(ActivateAbility(d.player1, kirin, unattach.id)).outcome shouldBe Outcome.Done
        d.settle()
        d.state.gameOver.shouldBeTrue()
        d.state.winnerId shouldBe d.player2
    }

    test("an opponent's 'you win the game' does nothing while a creature is equipped") {
        val d = driver()
        d.equip()
        d.castInstant(d.player2, "Kirin Test Victory")
        d.state.gameOver.shouldBeFalse()
    }

    test("unattached, Kirin protects nobody") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, CloudsteelKirin.name)
        d.castInstant(d.player1, "Kirin Test Lifeloss")
        d.state.gameOver.shouldBeTrue()
    }
})
