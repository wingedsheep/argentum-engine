package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sunbringer's Touch (Dragons of Tarkir #209) — {2}{G}{G} Sorcery.
 * "Bolster X, where X is the number of cards in your hand. Each creature you control with a +1/+1
 * counter on it gains trample until end of turn."
 *
 * Pins three things the script's shape decides: X is counted at resolution (the spell itself is
 * no longer in hand), the trample group is read after the bolster (so the bolstered creature gains
 * trample), and that group is fixed then (a creature given a counter later doesn't gain trample).
 */
class SunbringersTouchScenarioTest : ScenarioTestBase() {
    init {
        fun plusOnes(game: TestGame, id: EntityId) =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        fun addPlusOne(game: TestGame, id: EntityId) {
            game.state = game.state.updateEntity(id) { c ->
                val counters = c.get<CountersComponent>() ?: CountersComponent()
                c.with(counters.withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
            }
        }

        test("bolsters X = cards left in hand, then countered creatures you control gain trample") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Sunbringer's Touch")
                .withCardsInHand(1, "Forest", 3)
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withCardOnBattlefield(2, "Serra Angel")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val wurm = game.findPermanent("Craw Wurm")!!
            val angel = game.findPermanent("Serra Angel")!!
            addPlusOne(game, wurm)
            addPlusOne(game, angel)

            game.castSpell(1, "Sunbringer's Touch").error shouldBe null
            game.resolveStack()
            if (game.state.pendingDecision != null) {
                game.selectCards(listOf(bears))
                game.resolveStack()
            }

            withClue("X is the three cards left in hand once the spell is on the stack") {
                plusOnes(game, bears) shouldBe 3
            }
            withClue("only the least-toughness creature is bolstered") { plusOnes(game, giant) shouldBe 0 }

            val projected = game.state.projectedState
            withClue("the bolstered creature now has a counter, so it gains trample") {
                projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            }
            withClue("a creature you control that already had a counter gains trample") {
                projected.hasKeyword(wurm, Keyword.TRAMPLE) shouldBe true
            }
            withClue("a creature you control without a counter doesn't") {
                projected.hasKeyword(giant, Keyword.TRAMPLE) shouldBe false
            }
            withClue("the opponent's countered creature isn't yours") {
                projected.hasKeyword(angel, Keyword.TRAMPLE) shouldBe false
            }

            addPlusOne(game, giant)
            withClue("a creature that gets a counter after resolution doesn't gain trample") {
                game.state.projectedState.hasKeyword(giant, Keyword.TRAMPLE) shouldBe false
            }
        }

        test("with an empty hand X is 0: no counters are placed, already-countered creatures still gain trample") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Sunbringer's Touch")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Craw Wurm")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val wurm = game.findPermanent("Craw Wurm")!!
            addPlusOne(game, wurm)

            game.castSpell(1, "Sunbringer's Touch").error shouldBe null
            game.resolveStack()
            if (game.state.pendingDecision != null) {
                game.selectCards(listOf(bears))
                game.resolveStack()
            }

            withClue("bolster 0 puts no counters on the least-toughness creature") { plusOnes(game, bears) shouldBe 0 }
            val projected = game.state.projectedState
            withClue("so it doesn't gain trample") { projected.hasKeyword(bears, Keyword.TRAMPLE) shouldBe false }
            withClue("the creature that already had a counter does") {
                projected.hasKeyword(wurm, Keyword.TRAMPLE) shouldBe true
            }
        }
    }
}
