package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.one.cards.DrivnodCarnageDominus
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Drivnod, Carnage Dominus (ONE #90) — {3}{B}{B} 8/3 Legendary Creature — Phyrexian Horror.
 *
 *   If a creature dying causes a triggered ability of a permanent you control to trigger, that
 *   ability triggers an additional time.
 *   {B/P}{B/P}, Exile three creature cards from your graveyard: Put an indestructible counter on Drivnod.
 *
 * The doubler is `AdditionalDeathTriggers(permanentsYouControl = Any)` (Teysa's shape). Per the
 * rulings it also doubles the dying creature's *own* "when this creature dies" ability, and it
 * applies when Drivnod dies at the same time — both leaves-the-battlefield look-backs.
 */
class DrivnodCarnageDominusScenarioTest : ScenarioTestBase() {

    private val observer = card("Test Death Observer") {
        manaCost = "{2}{W}"
        typeLine = "Creature — Human Cleric"
        power = 3
        toughness = 3
        oracleText = "Whenever another creature dies, you gain 2 life."
        triggeredAbility {
            trigger = Triggers.another(GameObjectFilter.Creature).dies()
            effect = Effects.GainLife(2)
        }
    }

    private val martyr = card("Test Martyr") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human"
        power = 2
        toughness = 2
        oracleText = "When this creature dies, you gain 3 life."
        triggeredAbility {
            trigger = Triggers.self.dies()
            effect = Effects.GainLife(3)
        }
    }

    init {
        cardRegistry.register(observer)
        cardRegistry.register(martyr)

        test("another permanent's dies trigger triggers an additional time") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Drivnod, Carnage Dominus")
                .withCardOnBattlefield(1, "Test Death Observer")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInHand(1, "Pyroclasm")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Pyroclasm").error shouldBe null
            game.resolveStack()

            withClue("Opponent's Bears died; the observer's gain-2 fires twice (20 + 4)") {
                game.findPermanents("Grizzly Bears").size shouldBe 0
                game.getLifeTotal(1) shouldBe 24
            }
        }

        test("the dying creature's own dies trigger triggers an additional time") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Drivnod, Carnage Dominus")
                .withCardOnBattlefield(1, "Test Martyr")
                .withCardInHand(1, "Pyroclasm")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Pyroclasm").error shouldBe null
            game.resolveStack()

            withClue("Martyr died with Drivnod out: gain 3 twice (20 + 6)") {
                game.getLifeTotal(1) shouldBe 26
            }
        }

        test("Drivnod dying at the same time still doubles") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Drivnod, Carnage Dominus")
                .withCardOnBattlefield(1, "Test Martyr")
                .withCardInHand(1, "Wrath of God")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Wrath of God").error shouldBe null
            game.resolveStack()

            withClue("Both died together; the Martyr's trigger still fires twice (20 + 6)") {
                game.findPermanents("Drivnod, Carnage Dominus").size shouldBe 0
                game.getLifeTotal(1) shouldBe 26
            }
        }

        test("an opponent's dies triggers are not doubled") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Drivnod, Carnage Dominus")
                .withCardOnBattlefield(2, "Test Martyr")
                .withCardInHand(1, "Pyroclasm")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Pyroclasm").error shouldBe null
            game.resolveStack()

            withClue("Opponent's Martyr fires once (20 + 3)") {
                game.getLifeTotal(2) shouldBe 23
            }
        }

        test("exiling three creature cards puts an indestructible counter on Drivnod") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Drivnod, Carnage Dominus")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Savannah Lions")
                .withCardInGraveyard(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val drivnod = game.findPermanent("Drivnod, Carnage Dominus")!!
            val exiled = listOf("Grizzly Bears", "Savannah Lions", "Hill Giant")
                .map { game.findCardsInGraveyard(1, it).single() }
            val result = game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = drivnod,
                    abilityId = DrivnodCarnageDominus.activatedAbilities.first().id,
                    costPayment = AdditionalCostPayment(exiledCards = exiled)
                )
            )
            withClue("activation: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.getEntity(drivnod)?.get<CountersComponent>()?.getCount(CounterType.INDESTRUCTIBLE) shouldBe 1
            exiled.forEach { game.state.getExile(game.player1Id).contains(it) shouldBe true }
        }
    }
}
