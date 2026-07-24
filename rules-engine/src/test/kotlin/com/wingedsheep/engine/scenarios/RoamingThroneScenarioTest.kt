package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Roaming Throne (LCI #258) — {4} 4/4 Artifact Creature — Golem.
 *
 * "Ward {2}
 *  As this creature enters, choose a creature type.
 *  This creature is the chosen type in addition to its other types.
 *  If a triggered ability of another creature you control of the chosen type triggers, it
 *  triggers an additional time."
 *
 * Exercises the [com.wingedsheep.sdk.scripting.AdditionalSourceTriggers] doubler keyed to the
 * chosen creature type stored on this permanent (`GameObjectFilter.Creature.withChosenSubtype()`).
 * A "Goblin Chronicler" with a death-watch trigger stands in for the chosen-type creature; the
 * board wipe kills one creature, and the chronicler's "gain 2 life" fires an extra time when
 * Roaming Throne has chosen Goblin.
 */
class RoamingThroneScenarioTest : ScenarioTestBase() {

    // A Goblin whose triggered ability we can count firings of.
    private val goblinChronicler = CardDefinition.creature(
        name = "Goblin Chronicler",
        manaCost = ManaCost.parse("{2}{R}"),
        subtypes = setOf(Subtype("Goblin")),
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

    /** Set the creature type Roaming Throne chose as it entered. */
    private fun TestGame.chooseType(type: String) {
        val throne = findPermanent("Roaming Throne")!!
        state = state.updateEntity(throne) { c ->
            c.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.CREATURE_TYPE to ChoiceValue.TextChoice(type))))
        }
    }

    init {
        cardRegistry.register(goblinChronicler)

        context("Roaming Throne") {
            test("a chosen-type creature you control has its trigger doubled") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Roaming Throne")
                    .withCardOnBattlefield(1, "Goblin Chronicler")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Pyroclasm")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.chooseType("Goblin")

                game.castSpell(1, "Pyroclasm").error shouldBe null
                game.resolveStack()

                withClue("Only the 2/2 Grizzly Bears died; Chronicler (3/3) and Throne (4/4) survived") {
                    game.findPermanents("Grizzly Bears").size shouldBe 0
                    (game.findPermanent("Goblin Chronicler") != null) shouldBe true
                    (game.findPermanent("Roaming Throne") != null) shouldBe true
                }
                withClue("Chronicler is a Goblin (the chosen type) -> gain-2 fires twice (20 + 2 + 2 = 24)") {
                    game.getLifeTotal(1) shouldBe 24
                }
            }

            test("without Roaming Throne the trigger fires only once") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Goblin Chronicler")
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

            test("a creature you control NOT of the chosen type is not doubled") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Roaming Throne")
                    .withCardOnBattlefield(1, "Goblin Chronicler")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Pyroclasm")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Throne chose Elf; the Chronicler is a Goblin, so its trigger is NOT doubled.
                game.chooseType("Elf")

                game.castSpell(1, "Pyroclasm").error shouldBe null
                game.resolveStack()

                withClue("Chronicler isn't the chosen type -> gain 2 once (20 + 2 = 22)") {
                    game.getLifeTotal(1) shouldBe 22
                }
            }

            test("an opponent's chosen-type creature is not doubled (only creatures you control)") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Roaming Throne")
                    .withCardOnBattlefield(2, "Goblin Chronicler")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInHand(1, "Pyroclasm")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.chooseType("Goblin")

                game.castSpell(1, "Pyroclasm").error shouldBe null
                game.resolveStack()

                withClue("Opponent controls the Chronicler; your Throne can't double their trigger (20 + 2 = 22)") {
                    game.getLifeTotal(2) shouldBe 22
                }
            }
        }
    }
}
