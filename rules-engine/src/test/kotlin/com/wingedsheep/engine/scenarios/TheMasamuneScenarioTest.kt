package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * The Masamune (FIN #264) - {3} Legendary Artifact - Equipment.
 *
 * "As long as equipped creature is attacking, it has first strike and must be blocked if able.
 *  Equipped creature has 'If a creature dying causes a triggered ability of this creature or an
 *  emblem you own to trigger, that ability triggers an additional time.'
 *  Equip {2}"
 *
 * Exercises the filtered MustBeBlocked static, the attacking-gated first-strike grant, and the new
 * AdditionalDeathTriggers death-cause trigger doubler.
 */
class TheMasamuneScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private val graveChronicler = CardDefinition.creature(
        name = "Grave Chronicler",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype("Human"), Subtype("Cleric")),
        power = 3,
        toughness = 3,
        oracleText = "Whenever a creature dies, you gain 2 life.",
        script = CardScript.creature(
            TriggeredAbility.create(
                trigger = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Creature,
                    from = Zone.BATTLEFIELD,
                    to = Zone.GRAVEYARD
                ),
                binding = TriggerBinding.ANY,
                effect = GainLifeEffect(2)
            )
        )
    )

    init {
        cardRegistry.register(graveChronicler)

        context("The Masamune") {
            test("equipped creature has first strike while attacking but not otherwise") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "The Masamune", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Not attacking (main phase) -> no first strike") {
                    projector.project(game.state).hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe false
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null

                withClue("While attacking -> has first strike") {
                    projector.project(game.state).hasKeyword(bears, Keyword.FIRST_STRIKE) shouldBe true
                }
            }

            test("equipped attacker must be blocked if able") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "The Masamune", "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("Defender has an able blocker -> declining to block is illegal") {
                    game.declareNoBlockers().isSuccess shouldBe false
                }
                withClue("Blocking the equipped attacker is legal") {
                    game.declareBlockers(mapOf("Savannah Lions" to listOf("Grizzly Bears"))).isSuccess shouldBe true
                }
            }

            test("equipped creature's death trigger triggers an additional time") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grave Chronicler")
                    .withCardAttachedTo(1, "The Masamune", "Grave Chronicler")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Pyroclasm")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Pyroclasm").error shouldBe null
                game.resolveStack()

                withClue("The 2/2 Grizzly Bears died; the 3/3 Chronicler survived") {
                    game.findPermanents("Grizzly Bears").size shouldBe 0
                    (game.findPermanent("Grave Chronicler") != null) shouldBe true
                }
                withClue("One creature dying fires the gain-2 trigger twice (20 + 2 + 2 = 24)") {
                    game.getLifeTotal(1) shouldBe 24
                }
            }

            test("without The Masamune the death trigger fires only once") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Grave Chronicler")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Pyroclasm")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Pyroclasm").error shouldBe null
                game.resolveStack()

                withClue("No doubler -> gain 2 once (20 + 2 = 22)") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }
        }
    }
}
