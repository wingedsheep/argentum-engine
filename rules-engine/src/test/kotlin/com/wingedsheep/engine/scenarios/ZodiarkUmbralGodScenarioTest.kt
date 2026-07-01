package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.fin.cards.ZodiarkUmbralGod
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Zodiark, Umbral God (FIN) — {B}{B}{B}{B}{B} Legendary Creature — God 5/5, Indestructible.
 *
 * Two abilities under test:
 *  - The enter trigger: "each player sacrifices half the non-God creatures they control of their
 *    choice, rounded down." Composed from `ForEachPlayerEffect(Player.Each)` + `Effects.Sacrifice`
 *    with a `Divide(<their non-God creatures>, 2, roundUp = false)` dynamic count. The rounding is
 *    the interesting part (3 → 1, not 2), and Zodiark (a God) is excluded from the count and the
 *    sacrifice.
 *  - The reactive counter trigger: "Whenever a player sacrifices another creature, put a +1/+1
 *    counter on Zodiark." This exercises the new `PermanentsSacrificedEvent(byAnyPlayer = true)`
 *    scope — an *opponent's* sacrifice (not just the controller's) must trigger it.
 */
class ZodiarkUmbralGodScenarioTest : ScenarioTestBase() {

    // A {0} sorcery whose caster sacrifices one of their own creatures of their choice.
    private val ritualSacrifice = card("Ritual Sacrifice") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.Sacrifice(
                GameObjectFilter.Creature,
                1,
                EffectTarget.PlayerRef(Player.You)
            )
        }
    }

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    // Resolve the stack while auto-answering the decisions these scenarios raise: sacrifice-choice
    // (SelectCardsDecision → take the minimum required) and any mana-sources auto-pay.
    private fun TestGame.resolveAll() {
        var guard = 0
        while ((state.stack.isNotEmpty() || hasPendingDecision()) && guard++ < 60) {
            when (val d = getPendingDecision()) {
                is SelectCardsDecision -> selectCards(d.options.take(d.minSelections))
                null -> resolveStack()
                else ->
                    if (d::class.simpleName?.contains("Mana") == true) submitManaSourcesAutoPay()
                    else error("Unexpected decision: ${d::class.simpleName}")
            }
        }
    }

    init {
        cardRegistry.register(ZodiarkUmbralGod)
        cardRegistry.register(ritualSacrifice)

        context("Zodiark — 'whenever a player sacrifices another creature'") {

            test("an OPPONENT's sacrifice puts a +1/+1 counter on Zodiark (any player, not just you)") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "Zodiark, Umbral God", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(2, "Ritual Sacrifice")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val zodiark = game.findPermanent("Zodiark, Umbral God")!!
                game.plusOneCounters(zodiark) shouldBe 0

                game.castSpell(2, "Ritual Sacrifice")
                game.resolveAll()

                withClue("the opponent sacrificing their own creature triggers Zodiark's counter") {
                    game.plusOneCounters(zodiark) shouldBe 1
                }
            }

            test("your OWN sacrifice also puts a counter on Zodiark") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardOnBattlefield(1, "Zodiark, Umbral God", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardInHand(1, "Ritual Sacrifice")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val zodiark = game.findPermanent("Zodiark, Umbral God")!!
                game.castSpell(1, "Ritual Sacrifice")
                game.resolveAll()

                game.plusOneCounters(zodiark) shouldBe 1
            }
        }

        context("Zodiark — enter trigger: each player sacrifices half their non-God creatures, rounded down") {

            test("rounds down: you (4 creatures) sacrifice 2, opponent (3 creatures) sacrifices 1") {
                var builder = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Zodiark, Umbral God")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(4) { builder = builder.withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) }
                repeat(3) { builder = builder.withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false) }
                val game = builder.build()

                game.castSpell(1, "Zodiark, Umbral God")
                game.resolveAll()

                withClue("you had 4 non-God creatures → sacrifice floor(4/2) = 2") {
                    game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 2
                }
                withClue("opponent had 3 non-God creatures → sacrifice floor(3/2) = 1 (rounded down)") {
                    game.findCardsInGraveyard(2, "Grizzly Bears").size shouldBe 1
                }
                withClue("Zodiark is a God — excluded from its own edict and still on the battlefield") {
                    game.findPermanent("Zodiark, Umbral God") shouldNotBe null
                }
            }
        }
    }
}
