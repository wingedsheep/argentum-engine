package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Nissa, Ascended Animist (ONE #175, {3}{G}{G}{G/P}{G/P}, loyalty 7).
 *
 *   Compleated — each {G/P} paid with 2 life takes two loyalty counters off her entry (CR 702.150a).
 *   +1: Create an X/X green Phyrexian Horror creature token, where X is Nissa's loyalty.
 *   −1: Destroy target artifact or enchantment.
 *   −7: Until end of turn, creatures you control get +1/+1 for each Forest you control and gain trample.
 */
class NissaAscendedAnimistScenarioTest : ScenarioTestBase() {

    init {
        val abilities = cardRegistry.getCard("Nissa, Ascended Animist")!!.script.activatedAbilities
        fun idFor(change: Int) = abilities.single { (it.cost as? AbilityCost.Loyalty)?.change == change }.id

        fun TestGame.loyalty(id: EntityId): Int =
            state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0

        /** Casts Nissa from hand, tapping [forests] and paying [lifePips] {G/P} pips with life. */
        fun castWithLife(lifePips: Int, forests: Int): Pair<TestGame, EntityId> {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Nissa, Ascended Animist")
                .withLandsOnBattlefield(1, "Forest", forests)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val nissa = game.findCardsInHand(1, "Nissa, Ascended Animist").single()
            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = nissa,
                    paymentStrategy = PaymentStrategy.Explicit(
                        manaAbilitiesToActivate = game.findPermanents("Forest"),
                        phyrexianLifePayments = List(lifePips) { Color.GREEN }
                    )
                )
            ).error shouldBe null
            game.resolveStack()
            return game to game.findPermanent("Nissa, Ascended Animist")!!
        }

        test("paying every pip with mana, she enters with her full seven loyalty") {
            val (game, nissa) = castWithLife(lifePips = 0, forests = 7)
            game.getLifeTotal(1) shouldBe 20
            game.loyalty(nissa) shouldBe 7
        }

        test("each {G/P} paid with life takes two loyalty counters off her entry") {
            val (one, nissaOne) = castWithLife(lifePips = 1, forests = 6)
            withClue("one pip paid with 2 life: 7 − 2") {
                one.getLifeTotal(1) shouldBe 18
                one.loyalty(nissaOne) shouldBe 5
            }

            val (two, nissaTwo) = castWithLife(lifePips = 2, forests = 5)
            withClue("both pips paid with 4 life: 7 − 4") {
                two.getLifeTotal(1) shouldBe 16
                two.loyalty(nissaTwo) shouldBe 3
            }
        }

        test("auto-pay that covers the {G/P} pips with life reduces her loyalty the same way") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Nissa, Ascended Animist")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(1, "Nissa, Ascended Animist").error shouldBe null
            game.resolveStack()

            withClue("five Forests can't pay seven mana, so both pips go to life") {
                game.getLifeTotal(1) shouldBe 16
                game.loyalty(game.findPermanent("Nissa, Ascended Animist")!!) shouldBe 3
            }
        }

        test("a Nissa put onto the battlefield without being cast enters with full loyalty") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Nissa, Ascended Animist")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.loyalty(game.findPermanent("Nissa, Ascended Animist")!!) shouldBe 7
        }

        test("+1 creates a green Phyrexian Horror whose size is her loyalty after the cost") {
            val (game, nissa) = castWithLife(lifePips = 2, forests = 5)
            game.execute(ActivateAbility(game.player1Id, nissa, idFor(+1))).error shouldBe null
            game.resolveStack()

            game.loyalty(nissa) shouldBe 4
            val horror = game.state.getBattlefield().single { game.state.projectedState.hasSubtype(it, "Horror") }
            val projected = game.state.projectedState
            projected.getPower(horror) shouldBe 4
            projected.getToughness(horror) shouldBe 4
            projected.hasSubtype(horror, "Phyrexian") shouldBe true
        }

        test("−7 gives creatures you control +1/+1 per Forest and trample") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Nissa, Ascended Animist")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val nissa = game.findPermanent("Nissa, Ascended Animist")!!
            game.execute(ActivateAbility(game.player1Id, nissa, idFor(-7))).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            val bears = game.findPermanent("Grizzly Bears")!!
            projected.getPower(bears) shouldBe 5
            projected.getToughness(bears) shouldBe 5
            projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            withClue("an opponent's creature is untouched") {
                projected.getPower(game.findPermanent("Hill Giant")!!) shouldBe 3
            }
        }
    }
}
