package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CantReceiveCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RepeatKeywordAction
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Engine tests for `ProliferateEffect.storeAs` — proliferate publishing "the permanents that had a
 * counter put on them this way" as a pipeline collection (Ripples of Potential: "Proliferate, then
 * choose any number of permanents you control that had a counter put on them this way. Those
 * permanents phase out.").
 *
 * The rules pinned here (proliferate is CR 701.34a):
 * - only the permanents the proliferate actually gave a counter are stored — a permanent with a
 *   counter the controller didn't choose is not, a chosen one that can't have counters put on it
 *   is not, and neither is a chosen player;
 * - the follow-up's own filter ("you control") narrows them, so an opponent's proliferated
 *   permanent is never offered;
 * - choosing none in the follow-up does nothing;
 * - proliferating with nothing eligible stores nothing, so the follow-up has nothing to offer;
 * - under "proliferate twice instead" every permanent either proliferate gave a counter is stored;
 * - the targeted single-object form stores its recipient too.
 */
class ProliferateStoreAsTest : ScenarioTestBase() {

    private val ripples = card("Test Ripples") {
        manaCost = "{1}"
        typeLine = "Instant"
        oracleText = "Proliferate, then choose any number of permanents you control that had a " +
            "counter put on them this way. Those permanents phase out."
        spell {
            effect = Effects.Pipeline {
                val countered = runStoringCollection { Effects.Proliferate(storeAs = it) }
                val chosen = chooseAnyNumber(
                    from = countered,
                    filter = GameObjectFilter.Permanent.youControl(),
                    prompt = "Choose permanents to phase out",
                    useTargetingUI = true,
                )
                run(Effects.ForEachInCollection(chosen, Effects.PhaseOut(EffectTarget.IterationEntity)))
            }
        }
    }

    private val brokerRipples = card("Test Broker Ripples") {
        manaCost = "{1}"
        typeLine = "Instant"
        oracleText = "For each kind of counter on target permanent, give it another counter of " +
            "that kind. It phases out if it had a counter put on it this way."
        spell {
            val recipient = target(Targets.PermanentOrPlayer)
            effect = Effects.Pipeline {
                val countered = runStoringCollection { Effects.Proliferate(recipient, storeAs = it) }
                run(Effects.ForEachInCollection(countered, Effects.PhaseOut(EffectTarget.IterationEntity)))
            }
        }
    }

    private val doubler = card("Test Proliferate Doubler") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "If you would proliferate, proliferate twice instead."
        replacementEffect(RepeatKeywordAction(times = 2, appliesTo = EventPattern.ProliferatedEvent()))
    }

    private val warded = card("Test Warded Idol") {
        manaCost = "{2}"
        typeLine = "Artifact Creature — Construct"
        power = 1
        toughness = 1
        oracleText = "Test Warded Idol can't have counters put on it."
        staticAbility {
            ability = CantReceiveCounters(GroupFilter.source())
        }
    }

    private fun seed(game: TestGame, id: EntityId, type: CounterType, amount: Int = 1) {
        game.state = game.state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, amount))
        }
    }

    private fun counters(game: TestGame, id: EntityId, type: CounterType): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun phasedOut(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.has<PhasedOutComponent>() == true

    private fun pendingOptions(game: TestGame): List<EntityId> =
        (game.getPendingDecision() as SelectCardsDecision).options

    private fun baseScenario() = scenario()
        .withPlayers("Player1", "Player2")
        .withLandsOnBattlefield(1, "Forest", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        listOf(ripples, brokerRipples, doubler, warded).forEach(cardRegistry::register)

        test("only permanents you control that proliferate gave a counter are offered, and the chosen ones phase out") {
            val game = baseScenario()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardInHand(1, "Test Ripples")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val elves = game.findPermanent("Llanowar Elves")!!
            val lions = game.findPermanent("Savannah Lions")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE)
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE)
            seed(game, elves, CounterType.PLUS_ONE_PLUS_ONE)
            seed(game, lions, CounterType.PLUS_ONE_PLUS_ONE)
            seed(game, game.player2Id, CounterType.POISON)

            game.castSpell(1, "Test Ripples").error shouldBe null
            game.resolveStack()
            // Proliferate: bears, giant, the opponent's lions and the opponent — not the elves.
            game.selectCards(listOf(bears, giant, lions, game.player2Id)).error shouldBe null
            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            counters(game, elves, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            counters(game, game.player2Id, CounterType.POISON) shouldBe 2

            withClue("the elves weren't proliferated, the lions aren't yours, players aren't permanents") {
                pendingOptions(game) shouldContainExactlyInAnyOrder listOf(bears, giant)
            }
            game.selectCards(listOf(bears)).error shouldBe null

            phasedOut(game, bears) shouldBe true
            phasedOut(game, giant) shouldBe false
            phasedOut(game, elves) shouldBe false
            phasedOut(game, lions) shouldBe false
        }

        test("a chosen permanent that can't have counters put on it got none this way and is not offered") {
            val game = baseScenario()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Test Warded Idol")
                .withCardInHand(1, "Test Ripples")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val idol = game.findPermanent("Test Warded Idol")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE)
            // Seeded directly: the prohibition stops placement, not a counter already there.
            seed(game, idol, CounterType.CHARGE)

            game.castSpell(1, "Test Ripples").error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(bears, idol)).error shouldBe null
            counters(game, idol, CounterType.CHARGE) shouldBe 1

            pendingOptions(game) shouldContainExactlyInAnyOrder listOf(bears)
        }

        test("choosing none in the follow-up phases nothing out") {
            val game = baseScenario()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Test Ripples")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE)

            game.castSpell(1, "Test Ripples").error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(bears)).error shouldBe null
            game.selectCards(emptyList()).error shouldBe null

            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            phasedOut(game, bears) shouldBe false
            game.hasPendingDecision() shouldBe false
        }

        test("a permanent chosen for no counters this way — proliferating nothing — is never offered") {
            val game = baseScenario()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Test Ripples")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE)

            game.castSpell(1, "Test Ripples").error shouldBe null
            game.resolveStack()
            game.selectCards(emptyList()).error shouldBe null

            withClue("nothing was given a counter, so there is nothing to choose") {
                game.hasPendingDecision() shouldBe false
            }
            phasedOut(game, bears) shouldBe false
        }

        test("with nothing eligible to proliferate, nothing is stored and nothing is asked") {
            val game = baseScenario()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Test Ripples")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Test Ripples").error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            phasedOut(game, bears) shouldBe false
        }

        test("proliferating twice stores every permanent either proliferate gave a counter") {
            val game = baseScenario()
                .withCardOnBattlefield(1, "Test Proliferate Doubler")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardInHand(1, "Test Ripples")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE)
            seed(game, giant, CounterType.PLUS_ONE_PLUS_ONE)

            game.castSpell(1, "Test Ripples").error shouldBe null
            game.resolveStack()
            game.selectCards(listOf(bears)).error shouldBe null
            game.selectCards(listOf(giant)).error shouldBe null

            pendingOptions(game) shouldContainExactlyInAnyOrder listOf(bears, giant)
            game.selectCards(listOf(bears, giant)).error shouldBe null
            phasedOut(game, bears) shouldBe true
            phasedOut(game, giant) shouldBe true
        }

        test("the targeted form stores its recipient once it is given a counter") {
            val game = baseScenario()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Test Broker Ripples")
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            seed(game, bears, CounterType.PLUS_ONE_PLUS_ONE)

            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.findCardsInHand(1, "Test Broker Ripples").first(),
                    targets = listOf(ChosenTarget.Permanent(bears)),
                )
            ).error shouldBe null
            game.resolveStack()

            counters(game, bears, CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
            phasedOut(game, bears) shouldBe true
        }
    }
}
