package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.puzzles.advanceToPriority
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/** A future combat payoff must not inherit the timing bonus for drawing immediately. */
class TemporaryTriggerDrawTimingTest : ScenarioTestBase() {
    private val futureDraw = Effects.CreateGlobalTriggeredAbility(
        duration = Duration.EndOfTurn,
        ability = TriggeredAbility.create(
            id = AbilityId("timing-future-draw"),
            trigger = Triggers.oneOrMore(GameObjectFilter.Creature).dealCombatDamageToAPlayer(),
            effect = Effects.DrawCards(1),
        ),
    )
    private val spells = listOf("Instant", "Sorcery").flatMap { type ->
        listOf(false, true).map { delayed ->
            card("Timing ${if (delayed) "Future" else "Immediate"} $type") {
                manaCost = "{U}"
                typeLine = type
                spell { effect = if (delayed) futureDraw else Effects.DrawCards(1) }
            }
        }
    }
    private val artifacts = listOf(false, true).map { delayed ->
        card("Timing ${if (delayed) "Future" else "Immediate"} Artifact") {
            manaCost = "{1}"
            typeLine = "Artifact"
            activatedAbility {
                cost = Costs.Mana("{U}")
                effect = if (delayed) futureDraw else Effects.DrawCards(1)
            }
        }
    }
    private val intents by lazy { IntentCatalog.of(cardRegistry) }
    private val policy by lazy {
        HoldPolicy(intents, cashCantripsInTheEndStep = true, drawWithIdleMana = true)
    }

    private fun spellVerdict(name: String, activePlayer: Int, step: Step): TimingVerdict {
        val game = scenario().withPlayers().withActivePlayer(activePlayer)
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardInHand(1, name)
            .build().advanceToPriority(1, step)
        return policy.verdictFor(
            game.state, game.player1Id, name,
            cast = CastSpell(game.player1Id, game.findCardsInHand(1, name).first()),
        )
    }

    private fun activationVerdict(delayed: Boolean): TimingVerdict {
        val card = artifacts[if (delayed) 1 else 0]
        val game = scenario().withPlayers().withActivePlayer(2)
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardOnBattlefield(1, card.name)
            .build().advanceToPriority(1, Step.END)
        return policy.verdictFor(
            game.state, game.player1Id, card.name,
            activation = ActivateAbility(game.player1Id, game.findPermanent(card.name)!!,
                card.script.activatedAbilities.single().id),
        )
    }

    init {
        cardRegistry.register(spells + artifacts)

        test("only immediate instant draw receives the opponent end-step bonus") {
            spellVerdict("Timing Immediate Instant", 2, Step.END).shouldBeInstanceOf<TimingVerdict.Adjust>()
            spellVerdict("Timing Future Instant", 2, Step.END) shouldBe TimingVerdict.Neutral
        }

        test("only immediate draw activation receives the opponent end-step bonus") {
            activationVerdict(false).shouldBeInstanceOf<TimingVerdict.Adjust>()
            activationVerdict(true) shouldBe TimingVerdict.Neutral
        }

        test("only immediate sorcery draw receives the idle postcombat bonus") {
            spellVerdict("Timing Immediate Sorcery", 1, Step.POSTCOMBAT_MAIN)
                .shouldBeInstanceOf<TimingVerdict.Adjust>()
            spellVerdict("Timing Future Sorcery", 1, Step.POSTCOMBAT_MAIN) shouldBe TimingVerdict.Neutral
        }
    }
}
