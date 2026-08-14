package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.msh.cards.SuperAdaptoid
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Super-Adaptoid (MSH #250).
 *
 * {2} Legendary Artifact Creature — Robot Villain * /2
 * "Super-Adaptoid's power is equal to the number of legendary creatures you control.
 *  Whenever Super-Adaptoid enters or attacks, choose another target creature. If that creature has
 *  haste and Super-Adaptoid doesn't, put a haste counter on Super-Adaptoid. Do the same for flying,
 *  first strike, double strike, deathtouch, indestructible, lifelink, menace, reach, trample, and
 *  vigilance."
 *
 * Covers the characteristic-defining power, the enters *and* attacks absorption, that only keywords
 * the target actually has are copied, and that a keyword he already carries as a counter is skipped
 * rather than doubled (the "and Super-Adaptoid doesn't" clause).
 */
class SuperAdaptoidScenarioTest : ScenarioTestBase() {

    private val donor = card("Test Keyword Donor") {
        manaCost = "{2}{G}"
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2
        keywords(Keyword.FLYING, Keyword.TRAMPLE, Keyword.HASTE)
    }

    // Overlaps the first donor on flying so the "and Super-Adaptoid doesn't" clause has something
    // to decline, and adds menace so the attacks trigger has something to take.
    private val menaceDonor = card("Test Menace Donor") {
        manaCost = "{2}{U}"
        typeLine = "Creature — Bird Rogue"
        power = 2
        toughness = 2
        keywords(Keyword.FLYING, Keyword.MENACE)
    }

    private val vanilla = card("Test Vanilla Body") {
        manaCost = "{2}"
        typeLine = "Creature — Golem"
        power = 2
        toughness = 2
    }

    private val legend = card("Test Legendary Ally") {
        manaCost = "{1}{W}"
        typeLine = "Legendary Creature — Human Soldier"
        power = 1
        toughness = 1
    }

    private fun counters(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    init {
        cardRegistry.register(SuperAdaptoid)
        cardRegistry.register(donor)
        cardRegistry.register(menaceDonor)
        cardRegistry.register(vanilla)
        cardRegistry.register(legend)

        context("Super-Adaptoid") {

            test("his power is the number of legendary creatures you control, counting himself") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Super-Adaptoid")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val adaptoid = game.findPermanent("Super-Adaptoid")!!
                withClue("alone he is the only legendary creature you control") {
                    game.state.projectedState.getPower(adaptoid) shouldBe 1
                    game.state.projectedState.getToughness(adaptoid) shouldBe 2
                }

                val game2 = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Super-Adaptoid")
                    .withCardOnBattlefield(1, "Test Legendary Ally")
                    .withCardOnBattlefield(1, "Test Vanilla Body")
                    .withCardOnBattlefield(2, "Test Legendary Ally")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val adaptoid2 = game2.findPermanent("Super-Adaptoid")!!
                withClue("two legendary creatures YOU control; the nonlegend and the opponent's don't count") {
                    game2.state.projectedState.getPower(adaptoid2) shouldBe 2
                }
            }

            test("entering absorbs exactly the keywords the target has, as counters") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Super-Adaptoid")
                    .withCardOnBattlefield(1, "Test Keyword Donor")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val donorId = game.findPermanent("Test Keyword Donor")!!
                game.castSpell(1, "Super-Adaptoid").error shouldBe null
                game.resolveStack()

                val adaptoid = game.findPermanent("Super-Adaptoid")!!
                // The enters trigger pauses to pick "another target creature".
                game.getPendingDecision().shouldNotBeNull()
                game.selectTargets(listOf(donorId)).error shouldBe null
                game.resolveStack()

                withClue("flying, trample and haste are copied as keyword counters") {
                    counters(game, adaptoid, CounterType.FLYING) shouldBe 1
                    counters(game, adaptoid, CounterType.TRAMPLE) shouldBe 1
                    counters(game, adaptoid, CounterType.HASTE) shouldBe 1
                }
                withClue("keywords the donor doesn't have are not copied") {
                    counters(game, adaptoid, CounterType.DEATHTOUCH) shouldBe 0
                    counters(game, adaptoid, CounterType.LIFELINK) shouldBe 0
                    counters(game, adaptoid, CounterType.MENACE) shouldBe 0
                }
                withClue("and the counters actually grant the keywords (CR 122.1b)") {
                    game.state.projectedState.hasKeyword(adaptoid, Keyword.FLYING) shouldBe true
                    game.state.projectedState.hasKeyword(adaptoid, Keyword.HASTE) shouldBe true
                }
            }

            test("attacking triggers again, and a keyword he already has is not doubled") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "Super-Adaptoid")
                    .withCardOnBattlefield(1, "Test Keyword Donor")
                    .withCardOnBattlefield(1, "Test Menace Donor")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val keywordDonor = game.findPermanent("Test Keyword Donor")!!
                val menaceDonor = game.findPermanent("Test Menace Donor")!!

                game.castSpell(1, "Super-Adaptoid").error shouldBe null
                game.resolveStack()
                val adaptoid = game.findPermanent("Super-Adaptoid")!!
                game.getPendingDecision().shouldNotBeNull()
                game.selectTargets(listOf(keywordDonor)).error shouldBe null
                game.resolveStack()

                withClue("the enters trigger took flying, trample and haste off the first donor") {
                    counters(game, adaptoid, CounterType.FLYING) shouldBe 1
                    counters(game, adaptoid, CounterType.TRAMPLE) shouldBe 1
                    counters(game, adaptoid, CounterType.HASTE) shouldBe 1
                    counters(game, adaptoid, CounterType.MENACE) shouldBe 0
                }

                // The absorbed haste counter is what lets him attack the turn he landed, which is
                // also how the printed card reaches its own attacks trigger on turn one.
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Super-Adaptoid" to 2)).error shouldBe null

                // Point the attacks trigger at a donor sharing flying but adding menace.
                game.getPendingDecision().shouldNotBeNull()
                game.selectTargets(listOf(menaceDonor)).error shouldBe null
                game.resolveStack()

                withClue("the attacks trigger absorbs the keyword he was missing") {
                    counters(game, adaptoid, CounterType.MENACE) shouldBe 1
                    game.state.projectedState.hasKeyword(adaptoid, Keyword.MENACE) shouldBe true
                }
                withClue(
                    "\"and Super-Adaptoid doesn't\" — flying is already his via the counter from " +
                        "the enters trigger, so the second donor's flying adds nothing"
                ) {
                    counters(game, adaptoid, CounterType.FLYING) shouldBe 1
                }
                withClue("keywords neither donor has are still absent") {
                    counters(game, adaptoid, CounterType.DEATHTOUCH) shouldBe 0
                    counters(game, adaptoid, CounterType.VIGILANCE) shouldBe 0
                }
            }
        }
    }
}
