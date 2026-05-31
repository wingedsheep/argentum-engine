package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdm.cards.DragonfireBlade
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * Tests for Dragonfire Blade (TDM) — exercises gap item 18:
 * "Equip {4}. This ability costs {1} less to activate for each color of the creature it targets."
 * plus the granted "hexproof from monocolored".
 *
 * The cost reduction is the engine's per-activation [com.wingedsheep.sdk.scripting.ActivatedAbility.genericCostReduction]
 * fed `DynamicAmounts.targetColorCount()`, which reads the chosen equip target's color count.
 */
class DragonfireBladeTest : FunSpec({

    val equipAbilityId = DragonfireBlade.activatedAbilities.first().id

    // Vanilla test creatures of varying color counts (colors derive from the mana cost).
    val triColorBeast = CardDefinition.creature("Test Trio Beast", ManaCost.parse("{W}{U}{B}"), emptySet(), 3, 3)
    val gruulBeast = CardDefinition.creature("Test Gruul Beast", ManaCost.parse("{R}{G}"), emptySet(), 2, 2)
    val monoBear = CardDefinition.creature("Test Mono Bear", ManaCost.parse("{G}"), emptySet(), 2, 2)
    val colorlessGolem = CardDefinition.creature("Test Golem", ManaCost.parse("{2}"), emptySet(), 2, 2)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DragonfireBlade)
        driver.registerCard(triColorBeast)
        driver.registerCard(gruulBeast)
        driver.registerCard(monoBear)
        driver.registerCard(colorlessGolem)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("equip costs {1} less per color of the target — three-color creature pays {1}") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val blade = driver.putPermanentOnBattlefield(player, "Dragonfire Blade")
        val beast = driver.putCreatureOnBattlefield(player, "Test Trio Beast")

        // Equip {4} reduced by 3 colors = {1}. One mana is exactly enough.
        driver.giveColorlessMana(player, 1)
        val result = driver.submit(
            ActivateAbility(player, blade, equipAbilityId, targets = listOf(ChosenTarget.Permanent(beast)))
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        driver.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId shouldBe beast
    }

    test("equip onto a two-color creature pays {2}") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val blade = driver.putPermanentOnBattlefield(player, "Dragonfire Blade")
        val beast = driver.putCreatureOnBattlefield(player, "Test Gruul Beast")

        // {4} - 2 colors = {2}. One mana is not enough...
        driver.giveColorlessMana(player, 1)
        driver.submitExpectFailure(
            ActivateAbility(player, blade, equipAbilityId, targets = listOf(ChosenTarget.Permanent(beast)))
        )
        driver.state.getEntity(blade)?.get<AttachedToComponent>().shouldBeNull()

        // ...but two mana is.
        driver.giveColorlessMana(player, 1)
        driver.submit(
            ActivateAbility(player, blade, equipAbilityId, targets = listOf(ChosenTarget.Permanent(beast)))
        ).isSuccess shouldBe true
        driver.bothPass()
        driver.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId shouldBe beast
    }

    test("equip onto a colorless creature gets no reduction — pays the full {4}") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val blade = driver.putPermanentOnBattlefield(player, "Dragonfire Blade")
        val golem = driver.putCreatureOnBattlefield(player, "Test Golem")

        // Three mana is short of the unreduced {4}.
        driver.giveColorlessMana(player, 3)
        driver.submitExpectFailure(
            ActivateAbility(player, blade, equipAbilityId, targets = listOf(ChosenTarget.Permanent(golem)))
        )

        // The fourth mana covers it.
        driver.giveColorlessMana(player, 1)
        driver.submit(
            ActivateAbility(player, blade, equipAbilityId, targets = listOf(ChosenTarget.Permanent(golem)))
        ).isSuccess shouldBe true
        driver.bothPass()
        driver.state.getEntity(blade)?.get<AttachedToComponent>()?.targetId shouldBe golem
    }

    test("equipped creature has hexproof from monocolored — blocks monocolored opponents only") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val blade = driver.putPermanentOnBattlefield(player, "Dragonfire Blade")
        val beast = driver.putCreatureOnBattlefield(player, "Test Trio Beast")
        driver.giveColorlessMana(player, 1)
        driver.submit(
            ActivateAbility(player, blade, equipAbilityId, targets = listOf(ChosenTarget.Permanent(beast)))
        ).isSuccess shouldBe true
        driver.bothPass()

        val validator = TargetValidator()
        val target = listOf<ChosenTarget>(ChosenTarget.Permanent(beast))
        val req = listOf(TargetCreature())

        // A monocolored opponent's source can't target it.
        validator.validateTargets(driver.state, target, req, casterId = opponent, sourceColors = setOf(Color.RED))
            .shouldNotBeNull()

        // A multicolored opponent's source can.
        validator.validateTargets(driver.state, target, req, casterId = opponent, sourceColors = setOf(Color.RED, Color.WHITE))
            .shouldBeNull()

        // A colorless opponent's source can (colorless is not monocolored).
        validator.validateTargets(driver.state, target, req, casterId = opponent, sourceColors = emptySet())
            .shouldBeNull()

        // The controller can still target their own creature with a monocolored source.
        validator.validateTargets(driver.state, target, req, casterId = player, sourceColors = setOf(Color.RED))
            .shouldBeNull()

        // The client DTO surfaces the quality so the FE can render the hexproof-from-monocolored chip.
        val view = ClientStateTransformer(cardRegistry = driver.cardRegistry).transform(driver.state, viewingPlayerId = opponent)
        view.cards[beast]?.hexproofFromMonocolored shouldBe true
    }
})
