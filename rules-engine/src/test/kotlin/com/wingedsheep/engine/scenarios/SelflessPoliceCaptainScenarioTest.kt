package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Selfless Police Captain (SPM #12).
 *
 * "{1}{W} Creature — Human Detective 1/1.
 *  This creature enters with a +1/+1 counter on it.
 *  When this creature leaves the battlefield, put its +1/+1 counters on target creature you control."
 *
 * Exercises the `EntersWithCounters(count = 1, selfOnly = true)` replacement plus the
 * `LeavesBattlefield` trigger that moves the creature's last-known +1/+1 counters onto a target
 * creature. Unlike Servant of the Scale (a *dies* trigger), this fires on ANY departure, so the
 * second test bounces the Captain rather than killing it.
 */
class SelflessPoliceCaptainScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature(
                name = "Counterless Bear",
                manaCost = ManaCost.parse("{1}{G}"),
                subtypes = setOf(Subtype("Bear")),
                power = 2,
                toughness = 2
            )
        )

        fun plusOne(game: TestGame, id: com.wingedsheep.sdk.model.EntityId): Int =
            game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

        context("Selfless Police Captain") {

            test("enters with a +1/+1 counter (a 1/1 base that resolves as a 2/2)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Selfless Police Captain")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Selfless Police Captain").error shouldBe null
                game.resolveStack()

                val captain = game.findPermanent("Selfless Police Captain")
                withClue("Selfless Police Captain is on the battlefield") {
                    captain shouldBe game.findPermanent("Selfless Police Captain")
                }
                withClue("Selfless Police Captain enters with one +1/+1 counter") {
                    plusOne(game, captain!!) shouldBe 1
                }
            }

            test("when it leaves the battlefield (bounced), its +1/+1 counter moves to target creature you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Selfless Police Captain")
                    .withCardOnBattlefield(1, "Counterless Bear", summoningSickness = false)
                    .withCardInHand(1, "Unsummon")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast the Captain so it enters with its +1/+1 counter via the replacement effect.
                game.castSpell(1, "Selfless Police Captain").error shouldBe null
                game.resolveStack()

                val captain = game.findPermanent("Selfless Police Captain")!!
                val bear = game.findPermanent("Counterless Bear")!!
                withClue("Captain has one +1/+1 counter before leaving") { plusOne(game, captain) shouldBe 1 }

                // Bounce the Captain to hand — a non-death departure that still fires "leaves the battlefield".
                game.castSpell(1, "Unsummon", captain)
                game.resolveStack()

                withClue("Captain left the battlefield") { game.findPermanent("Selfless Police Captain") shouldBe null }

                // The leaves trigger moves the last-known counter onto the only valid target (the Bear).
                if (game.hasPendingDecision()) {
                    game.selectTargets(listOf(bear))
                    game.resolveStack()
                }

                withClue("Captain's +1/+1 counter moved onto the Bear") {
                    plusOne(game, bear) shouldBe 1
                }
            }
        }
    }
}
