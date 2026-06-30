package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.BendersWaterskin
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bender's Waterskin — {3} Artifact.
 *   "Untap this artifact during each other player's untap step.
 *    {T}: Add one mana of any color."
 *
 * Covers (a) the any-color mana ability ([Effects.AddAnyColorMana]) and (b) the self-scoped
 * [UntapSelfDuringOtherUntapSteps] static: the artifact untaps on a *non-controller's* untap
 * step, while a plain artifact the same player controls stays tapped through it.
 */
class BendersWaterskinScenarioTest : FunSpec({

    val manaAbilityId = BendersWaterskin.activatedAbilities[0].id

    // A vanilla artifact (no self-untap static) — the negative control for the untap test.
    val PlainRock = card("Bender Test Plain Rock") {
        manaCost = "{2}"
        typeLine = "Artifact"
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(BendersWaterskin, PlainRock))
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
    }

    test("{T}: adds one mana of the chosen color") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val waterskin = d.putPermanentOnBattlefield(you, "Bender's Waterskin")

        val result = d.submit(ActivateAbility(playerId = you, sourceId = waterskin, abilityId = manaAbilityId))
        result.isPaused shouldBe true
        val decision = d.pendingDecision as ChooseColorDecision
        d.submitDecision(you, ColorChosenResponse(decision.id, Color.BLUE))

        val pool = d.state.getEntity(you)?.get<ManaPoolComponent>()!!
        pool.getAmount(Color.BLUE) shouldBe 1
        // Exactly one mana of the single chosen color — nothing else produced.
        pool.getAmount(Color.RED) shouldBe 0
    }

    test("untaps during another player's untap step, while a plain artifact stays tapped") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val waterskin = d.putPermanentOnBattlefield(you, "Bender's Waterskin")
        val plainRock = d.putPermanentOnBattlefield(you, "Bender Test Plain Rock")

        // Tap both on your own turn.
        d.tapPermanent(waterskin)
        d.tapPermanent(plainRock)
        d.isTapped(waterskin) shouldBe true
        d.isTapped(plainRock) shouldBe true

        // Advance into the opponent's turn — their untap step runs along the way.
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe opponent

        // The waterskin untapped during the opponent's untap step; the plain rock did not.
        d.isTapped(waterskin) shouldBe false
        d.isTapped(plainRock) shouldBe true
    }
})
