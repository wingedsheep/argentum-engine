package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Triggered abilities granted to a **card in exile** ("It gains '…'", Uvilda, Dean of Perfection).
 *
 * The grant lands on the exiled card, the per-event trigger pass reads it there (an exile-active
 * `CountersRemovedEvent(lastRemoved)` fires however the last counter left, not only on an upkeep),
 * and the grant ends when the card leaves exile (CR 400.7: it becomes a new object).
 */
class ExileGrantedTriggeredAbilityTest : ScenarioTestBase() {

    private val refinery = card("Exile Grant Refinery") {
        manaCost = "{0}"
        typeLine = "Artifact"
        val payoff = grantedTriggeredAbility {
            trigger = Triggers.self.losesCounters(CounterType.REFINE, lastRemoved = true)
            triggerZone = Zone.EXILE
            interveningIf = Conditions.SourceInZone(Zone.EXILE)
            effect = Effects.GainLife(3)
            description = "When the last refine counter is removed from this card, if it's exiled, you gain 3 life."
        }

        /** A card that only works on the battlefield, granted to the exiled card: it must stay inert. */
        val battlefieldOnly = grantedTriggeredAbility {
            trigger = Triggers.self.losesCounters(CounterType.REFINE, lastRemoved = true)
            effect = Effects.GainLife(100)
            description = "When the last refine counter is removed from this, you gain 100 life."
        }

        activatedAbility {
            cost = Costs.Tap
            effect = Effects.Pipeline {
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Any))
                val chosen = chooseUpTo(1, from = hand)
                exile(chosen)
                run(
                    Effects.ForEachInCollection(
                        chosen,
                        Effects.Composite(
                            listOf(
                                Effects.AddCounters(CounterType.REFINE, 2, EffectTarget.IterationEntity),
                                Effects.GrantTriggeredAbility(payoff, EffectTarget.IterationEntity, Duration.Permanent),
                                Effects.GrantTriggeredAbility(battlefieldOnly, EffectTarget.IterationEntity, Duration.Permanent),
                            )
                        ),
                    )
                )
            }
        }
        activatedAbility {
            cost = Costs.Mana("{1}")
            effect = Effects.Pipeline {
                val refined = gather(
                    CardSource.FromZone(Zone.EXILE, Player.You, GameObjectFilter.Any.withCounter(CounterType.REFINE))
                )
                run(Effects.ForEachInCollection(refined, Effects.RemoveCounters(CounterType.REFINE, 1, EffectTarget.IterationEntity)))
            }
        }
        activatedAbility {
            cost = Costs.Mana("{2}")
            effect = Effects.Pipeline {
                toHand(gather(CardSource.FromZone(Zone.EXILE, Player.You, GameObjectFilter.Any)))
            }
        }
    }

    private val trinket = card("Exile Grant Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }

    init {
        cardRegistry.register(refinery)
        cardRegistry.register(trinket)

        fun TestGame.activate(index: Int) {
            val source = findPermanent("Exile Grant Refinery")!!
            execute(ActivateAbility(player1Id, source, refinery.activatedAbilities[index].id)).error shouldBe null
            resolveStack()
        }

        fun TestGame.exiledTrinket(): EntityId =
            state.getExile(player1Id).first { state.getEntity(it)!!.get<CardComponent>()!!.name == "Exile Grant Trinket" }

        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Exile Grant Refinery")
            .withLandsOnBattlefield(1, "Plains", 4)
            .withCardInHand(1, "Exile Grant Trinket")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.exileTrinket(): EntityId {
            activate(0)
            selectCards(findCardsInHand(1, "Exile Grant Trinket"))
            resolveStack()
            return exiledTrinket()
        }

        test("an exile-active granted trigger fires from exile when another effect removes the last counter") {
            val game = board()
            val trinket = game.exileTrinket()
            withClue("both abilities are granted to the exiled card") {
                game.state.grantedTriggeredAbilities.count { it.entityId == trinket } shouldBe 2
            }
            val life = game.getLifeTotal(1)

            game.activate(1)
            withClue("one counter left — not the last") { game.getLifeTotal(1) shouldBe life }

            game.activate(1)
            game.resolveStack()
            withClue("the last removal fires the exile-active grant only; the battlefield-only one stays inert") {
                game.getLifeTotal(1) shouldBe life + 3
            }
        }

        test("the grants end when the card leaves exile") {
            val game = board()
            val trinket = game.exileTrinket()
            game.activate(2)
            game.isInHand(1, "Exile Grant Trinket") shouldBe true
            game.state.grantedTriggeredAbilities.filter { it.entityId == trinket }.shouldBeEmpty()
        }
    }
}
