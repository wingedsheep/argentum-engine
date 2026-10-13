package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.MeldedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Urza, Lord Protector (BRO #225) — {1}{W}{U} Legendary Creature — Human Artificer, 2/4.
 *
 *   Artifact, instant, and sorcery spells you cast cost {1} less to cast.
 *   {7}: If you both own and control Urza, Lord Protector and an artifact named The Mightstone and
 *   Weakstone, exile them, then meld them into Urza, Planeswalker. Activate only as a sorcery.
 *
 * Pins the meld activation (the pair becomes Urza, Planeswalker with seven loyalty), the
 * sorcery-speed restriction, the no-partner fizzle, and the one-generic discount over the
 * artifact-or-instant-or-sorcery union.
 */
class UrzaLordProtectorScenarioTest : ScenarioTestBase() {
    private val urza = "Urza, Lord Protector"

    private fun board(
        withMightstone: Boolean = true,
        phase: Phase = Phase.PRECOMBAT_MAIN,
        step: Step = Step.PRECOMBAT_MAIN,
    ): TestGame {
        var b = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, urza)
            .withLandsOnBattlefield(1, "Island", 7)
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(phase, step)
        if (withMightstone) b = b.withCardOnBattlefield(1, "The Mightstone and Weakstone")
        return b.build()
    }

    private fun TestGame.meldAction() = ActivateAbility(
        player1Id,
        findPermanent(urza)!!,
        cardRegistry.getCard(urza)!!.script.activatedAbilities[0].id,
    )

    private fun cost(game: TestGame, card: String) = CostCalculator(cardRegistry, services.predicateEvaluator)
        .calculateEffectiveCost(game.state, cardRegistry.getCard(card)!!, game.player1Id)

    init {
        test("{7}: Urza and The Mightstone and Weakstone meld into Urza, Planeswalker") {
            val game = board()

            game.execute(game.meldAction()).error shouldBe null
            game.resolveStack()

            val planeswalker = game.findPermanent("Urza, Planeswalker").shouldNotBeNull()
            game.findPermanent(urza) shouldBe null
            game.findPermanent("The Mightstone and Weakstone") shouldBe null
            game.isInExile(1, urza) shouldBe false
            game.isInExile(1, "The Mightstone and Weakstone") shouldBe false
            game.state.getEntity(planeswalker)!!.has<MeldedComponent>() shouldBe true
            withClue("the melded planeswalker enters with its printed loyalty") {
                game.state.getEntity(planeswalker)!!.get<CountersComponent>()!!
                    .getCount(CounterType.LOYALTY) shouldBe 7
            }
        }

        test("without The Mightstone and Weakstone the ability resolves and does nothing") {
            val game = board(withMightstone = false)

            game.execute(game.meldAction()).error shouldBe null
            game.resolveStack()

            game.findPermanent(urza).shouldNotBeNull()
            game.findPermanent("Urza, Planeswalker") shouldBe null
            game.isInExile(1, urza) shouldBe false
        }

        test("activate only as a sorcery: not with priority outside a main phase") {
            val game = board(phase = Phase.COMBAT, step = Step.BEGIN_COMBAT)

            game.execute(game.meldAction()).error shouldNotBe null
            game.findPermanent(urza).shouldNotBeNull()
        }

        test("artifact, instant and sorcery spells cost {1} less; colored mana and other spells are untouched") {
            val game = board(withMightstone = false)

            cost(game, "Millstone") shouldBe ManaCost.parse("{1}")
            cost(game, "Divination") shouldBe ManaCost.parse("{1}{U}")
            cost(game, "Cancel") shouldBe ManaCost.parse("{U}{U}")
            cost(game, "Counterspell") shouldBe ManaCost.parse("{U}{U}")
            cost(game, "Grizzly Bears") shouldBe ManaCost.parse("{1}{G}")
        }
    }
}
