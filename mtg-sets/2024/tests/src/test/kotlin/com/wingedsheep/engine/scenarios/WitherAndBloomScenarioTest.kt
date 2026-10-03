package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.WitherAndBloom
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Wither and Bloom — "Target creature gets -3/-3 until end of turn." plus the graveyard ability
 * "{1}{B}, Exile this card from your graveyard: Put a +1/+1 counter on target creature you control.
 * Activate only as a sorcery."
 */
class WitherAndBloomScenarioTest : FunSpec({

    val projector = StateProjector()
    val graveyardAbilityId = WitherAndBloom.activatedAbilities.first().id

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(WitherAndBloom))
        initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
    }

    test("the instant gives target creature -3/-3 until end of turn") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val giant = d.putCreatureOnBattlefield(you, "Hill Giant") // 3/3
        val spell = d.putCardInHand(you, "Wither and Bloom")
        d.giveMana(you, Color.BLACK, 2)

        d.castSpell(you, spell, listOf(giant)).outcome shouldBe Outcome.Done
        d.bothPass()

        d.findPermanent(you, "Hill Giant") shouldBe null
    }

    test("from the graveyard, exile it to put a +1/+1 counter on a creature you control") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = d.putCardInGraveyard(you, "Wither and Bloom")
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.giveMana(you, Color.BLACK, 2)

        d.submit(
            ActivateAbility(
                playerId = you,
                sourceId = card,
                abilityId = graveyardAbilityId,
                targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(bear)),
            )
        ).outcome shouldBe Outcome.Done
        d.getGraveyard(you).contains(card) shouldBe false
        d.bothPass()

        val projected = projector.project(d.state)
        projected.getPower(bear) shouldBe 3
        projected.getToughness(bear) shouldBe 3
    }

    test("the graveyard ability cannot target a creature an opponent controls") {
        val d = driver()
        val you = d.activePlayer!!
        val opp = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val card = d.putCardInGraveyard(you, "Wither and Bloom")
        val theirBear = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.giveMana(you, Color.BLACK, 2)

        d.submit(
            ActivateAbility(
                playerId = you,
                sourceId = card,
                abilityId = graveyardAbilityId,
                targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(theirBear)),
            )
        ).outcome shouldNotBe Outcome.Done
    }

    test("the graveyard ability cannot be activated outside a sorcery window") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.UPKEEP, maxPasses = 200)

        val card = d.putCardInGraveyard(you, "Wither and Bloom")
        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.giveMana(you, Color.BLACK, 2)

        d.submit(
            ActivateAbility(
                playerId = you,
                sourceId = card,
                abilityId = graveyardAbilityId,
                targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Permanent(bear)),
            )
        ).outcome shouldNotBe Outcome.Done
    }
})
