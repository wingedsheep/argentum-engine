package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.rna.cards.DomriChaosBringer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Domri, Chaos Bringer (RNA #166) — {2}{R}{G} planeswalker, loyalty 5.
 *
 * The +1's mana gives the creature spell it pays for riot: the creature asks counter-or-haste as
 * it enters. The −8's emblem makes a 4/4 Beast at each end step.
 */
class DomriChaosBringerScenarioTest : ScenarioTestBase() {

    private val plusOne = DomriChaosBringer.activatedAbilities[0].id
    private val minusEight = DomriChaosBringer.activatedAbilities[2].id

    init {
        context("Domri, Chaos Bringer") {

            test("+1: the mana spent on a creature spell gives it riot — choosing haste") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Domri, Chaos Bringer")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val domri = game.findPermanent("Domri, Chaos Bringer")!!

                game.execute(ActivateAbility(game.player1Id, domri, plusOne)).error shouldBe null
                game.resolveStack()
                val color = game.getPendingDecision()!!
                game.submitDecision(ColorChosenResponse(color.id, Color.GREEN)).error shouldBe null
                withClue("+1 raises loyalty to 6") {
                    game.state.getEntity(domri)!!.get<CountersComponent>()!!.getCount(CounterType.LOYALTY) shouldBe 6
                }

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                val riot = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
                game.submitDecision(OptionChosenResponse(riot.id, 1)).error shouldBe null

                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.hasKeyword(bears, Keyword.HASTE) shouldBe true
                (game.state.getEntity(bears)!!.get<CountersComponent>()
                    ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 0
            }

            test("−8: the emblem creates a 4/4 trampling Beast at the beginning of each end step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Domri, Chaos Bringer")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val domri = game.findPermanent("Domri, Chaos Bringer")!!
                game.state = game.state.updateEntity(domri) { c ->
                    c.with(CountersComponent().withAdded(CounterType.LOYALTY, 8))
                }

                game.execute(ActivateAbility(game.player1Id, domri, minusEight)).error shouldBe null
                game.resolveStack()
                withClue("Domri went to 0 loyalty and died") { game.isInGraveyard(1, "Domri, Chaos Bringer") shouldBe true }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                val beasts = game.findPermanents("Beast Token")
                beasts.size shouldBe 1
                val beast = beasts.single()
                game.state.projectedState.getPower(beast) shouldBe 4
                game.state.projectedState.getToughness(beast) shouldBe 4
                game.state.projectedState.hasKeyword(beast, Keyword.TRAMPLE) shouldBe true
            }
        }
    }
}
