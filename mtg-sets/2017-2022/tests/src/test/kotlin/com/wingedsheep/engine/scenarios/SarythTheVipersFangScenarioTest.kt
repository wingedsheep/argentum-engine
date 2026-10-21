package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mid.cards.SarythTheVipersFang
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Scenario tests for Saryth, the Viper's Fang. */
class SarythTheVipersFangScenarioTest : FunSpec({

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(SarythTheVipersFang))
        initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("tapped other creatures have deathtouch, untapped ones hexproof, Saryth neither") {
        val d = setup()
        val you = d.activePlayer!!
        val saryth = d.putCreatureOnBattlefield(you, "Saryth, the Viper's Fang")
        val tappedBear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        val untappedGiant = d.putCreatureOnBattlefield(you, "Hill Giant")
        val opposingBear = d.putCreatureOnBattlefield(d.getOpponent(you), "Grizzly Bears")
        d.tapPermanent(tappedBear)

        val projected = d.state.projectedState
        projected.hasKeyword(tappedBear, Keyword.DEATHTOUCH) shouldBe true
        projected.hasKeyword(tappedBear, Keyword.HEXPROOF) shouldBe false
        projected.hasKeyword(untappedGiant, Keyword.HEXPROOF) shouldBe true
        projected.hasKeyword(untappedGiant, Keyword.DEATHTOUCH) shouldBe false
        projected.hasKeyword(saryth, Keyword.HEXPROOF) shouldBe false
        projected.hasKeyword(saryth, Keyword.DEATHTOUCH) shouldBe false
        projected.hasKeyword(opposingBear, Keyword.HEXPROOF) shouldBe false

        // Tapping Saryth doesn't give it deathtouch either.
        d.tapPermanent(saryth)
        d.state.projectedState.hasKeyword(saryth, Keyword.DEATHTOUCH) shouldBe false
    }

    test("{1}, {T}: untaps another target land you control") {
        val d = setup()
        val you = d.activePlayer!!
        val saryth = d.putCreatureOnBattlefield(you, "Saryth, the Viper's Fang")
        d.removeSummoningSickness(saryth)
        val forest = d.putLandOnBattlefield(you, "Forest")
        d.tapPermanent(forest)
        d.giveColorlessMana(you, 1)

        d.submitSuccess(
            ActivateAbility(
                playerId = you,
                sourceId = saryth,
                abilityId = SarythTheVipersFang.activatedAbilities.single().id,
                targets = listOf(ChosenTarget.Permanent(forest))
            )
        )
        d.isTapped(saryth) shouldBe true
        d.bothPass()

        d.isTapped(forest) shouldBe false
    }
})
