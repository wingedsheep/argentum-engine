package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Proves [DynamicAmount.CastX] — "the X this object was cast with" — resolves correctly across every
 * site the cast-time-X design must reach, using minimal inline cards so each context is isolated:
 *
 *  - the spell's own resolution (X read off the stack object),
 *  - an enters-the-battlefield *trigger* (X read off the durable component on the new permanent),
 *  - a later activated ability (same durable component, after the spell has fully resolved),
 *  - a dies trigger (X read as last-known information after the permanent has left the battlefield).
 *
 * Hydroid Krasis itself ([HydroidKrasisScenarioTest]) covers the "when you cast this spell" trigger
 * and the enters-with-counters replacement.
 */
class CastXDurableValueTest : ScenarioTestBase() {

    init {
        // Instant: "You gain X life." Reads CastX during the spell's own resolution.
        cardRegistry.register(
            card("CastX Lifebolt") {
                manaCost = "{X}{G}"
                typeLine = "Instant"
                spell {
                    effect = Effects.GainLife(DynamicAmount.CastX)
                }
            }
        )

        // 2/2 creature: "When this enters, draw X cards." Reads CastX from an ETB trigger.
        cardRegistry.register(
            card("CastX Drawer") {
                manaCost = "{X}{G}"
                typeLine = "Creature — Sponge"
                power = 2
                toughness = 2
                triggeredAbility {
                    trigger = Triggers.EntersBattlefield
                    effect = Effects.DrawCards(DynamicAmount.CastX)
                }
            }
        )

        // 2/2 creature: "{1}: You gain X life." Reads CastX from a later activated ability.
        cardRegistry.register(
            card("CastX Fountain") {
                manaCost = "{X}{G}"
                typeLine = "Creature — Sponge"
                power = 2
                toughness = 2
                activatedAbility {
                    cost = Costs.Mana("{1}")
                    effect = Effects.GainLife(DynamicAmount.CastX)
                }
            }
        )

        // 0/0 creature: "When this dies, you gain X life." A 0/0 dies to SBAs the instant it
        // enters, so CastX must be read as last-known information after the entity has left.
        cardRegistry.register(
            card("CastX Martyr") {
                manaCost = "{X}{G}"
                typeLine = "Creature — Sponge"
                power = 0
                toughness = 0
                triggeredAbility {
                    trigger = Triggers.Dies
                    effect = Effects.GainLife(DynamicAmount.CastX)
                }
            }
        )

        context("DynamicAmount.CastX") {

            test("resolves during the spell's own resolution (CastX Lifebolt cast for X=4 gains 4 life)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "CastX Lifebolt")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "CastX Lifebolt", xValue = 4).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 24
            }

            test("resolves from an enters-the-battlefield trigger (CastX Drawer cast for X=2 draws 2)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "CastX Drawer")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)
                game.castXSpell(1, "CastX Drawer", xValue = 2).error shouldBe null
                game.resolveStack()

                withClue("ETB trigger drew CastX = 2 cards") {
                    game.handSize(1) shouldBe (handBefore - 1 + 2)
                }
            }

            test("resolves from a later activated ability (CastX Fountain cast for X=3, then {1} gains 3 life)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "CastX Fountain")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "CastX Fountain", xValue = 3).error shouldBe null
                game.resolveStack()
                withClue("the activated ability has not resolved yet, so no life gained from it") {
                    game.getLifeTotal(1) shouldBe 20
                }

                val fountain = game.findPermanent("CastX Fountain")!!
                val abilityId = cardRegistry.getCard("CastX Fountain")!!.activatedAbilities[0].id
                game.execute(ActivateAbility(game.player1Id, fountain, abilityId)).error shouldBe null
                game.resolveStack()

                withClue("the activated ability read the durable cast X = 3 off the permanent") {
                    game.getLifeTotal(1) shouldBe 23
                }
            }

            test("resolves from a dies trigger as last-known information (CastX Martyr cast for X=5 gains 5 life)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "CastX Martyr")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castXSpell(1, "CastX Martyr", xValue = 5).error shouldBe null
                game.resolveStack()

                withClue("the 0/0 died, and its dies trigger read the cast X = 5 from last-known info") {
                    game.getLifeTotal(1) shouldBe 25
                    game.findPermanent("CastX Martyr") shouldBe null
                }
            }
        }
    }
}
