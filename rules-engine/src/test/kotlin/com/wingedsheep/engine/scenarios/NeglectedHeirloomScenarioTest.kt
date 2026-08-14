package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.soi.cards.NeglectedHeirloom
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Neglected Heirloom // Ashmouth Blade (SOI) — "When **equipped creature** transforms, transform
 * this Equipment."
 *
 * Exercises the new `Triggers.transforms(binding = TriggerBinding.ATTACHED)` shape end to end: the
 * Equipment watches the permanent it's attached to, and a transform of that permanent flips the
 * Equipment. A transform happens in place, so the Equipment is still attached when the event fires.
 */
class NeglectedHeirloomScenarioTest : FunSpec({

    val projector = StateProjector()

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to driver.activePlayer!!
    }

    fun faceName(driver: GameTestDriver, id: EntityId): String? =
        driver.state.getEntity(id)?.get<CardComponent>()?.name

    fun attachedTo(driver: GameTestDriver, id: EntityId): EntityId? =
        driver.state.getEntity(id)?.get<AttachedToComponent>()?.targetId

    fun drain(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 20 && (driver.state.stack.isNotEmpty() || driver.isPaused)) {
            if (driver.isPaused) driver.autoResolveDecision() else driver.bothPass()
        }
    }

    /** Put the Heirloom onto the battlefield and equip it to [creature]. */
    fun equipTo(driver: GameTestDriver, player: EntityId, creature: EntityId): EntityId {
        val heirloom = driver.putPermanentOnBattlefield(player, "Neglected Heirloom")
        driver.giveColorlessMana(player, 1)
        val equipId = NeglectedHeirloom.activatedAbilities.first { it.isEquipAbility }.id
        driver.submitSuccess(
            ActivateAbility(
                playerId = player,
                sourceId = heirloom,
                abilityId = equipId,
                targets = listOf(ChosenTarget.Permanent(creature)),
            )
        )
        drain(driver)
        return heirloom
    }

    test("equipped creature gets +1/+1 from the front face") {
        val (driver, you) = newGame()
        val creature = driver.putCreatureOnBattlefield(you, "Test DFC Front")
        val heirloom = equipTo(driver, you, creature)

        attachedTo(driver, heirloom) shouldBe creature
        val projected = projector.project(driver.state)
        projected.getPower(creature) shouldBe 3 // 2/2 + 1/+1
        projected.getToughness(creature) shouldBe 3
    }

    test("the equipped creature transforming flips the Equipment into Ashmouth Blade") {
        val (driver, you) = newGame()
        val creature = driver.putCreatureOnBattlefield(you, "Test DFC Front")
        val heirloom = equipTo(driver, you, creature)

        // Transform the equipped creature (2/2 Human -> 4/4 Werewolf back face).
        val spell = driver.putCardInHand(you, "Transform Target Creature")
        driver.giveMana(you, Color.BLUE, 1)
        driver.giveColorlessMana(you, 1)
        driver.castSpellWithTargets(you, spell, listOf(ChosenTarget.Permanent(creature)))
        drain(driver)

        faceName(driver, creature) shouldBe "Test DFC Back"
        faceName(driver, heirloom) shouldBe "Ashmouth Blade"

        // Still attached, now granting +3/+3 and first strike.
        attachedTo(driver, heirloom) shouldBe creature
        val projected = projector.project(driver.state)
        projected.getPower(creature) shouldBe 7 // 4/4 + 3/+3
        projected.getToughness(creature) shouldBe 7
        projected.hasKeyword(creature, Keyword.FIRST_STRIKE) shouldBe true
    }

    test("an unequipped Heirloom is not flipped when some other permanent transforms") {
        val (driver, you) = newGame()
        val creature = driver.putCreatureOnBattlefield(you, "Test DFC Front")
        val heirloom = driver.putPermanentOnBattlefield(you, "Neglected Heirloom")

        val spell = driver.putCardInHand(you, "Transform Target Creature")
        driver.giveMana(you, Color.BLUE, 1)
        driver.giveColorlessMana(you, 1)
        driver.castSpellWithTargets(you, spell, listOf(ChosenTarget.Permanent(creature)))
        drain(driver)

        faceName(driver, creature) shouldBe "Test DFC Back"
        faceName(driver, heirloom) shouldBe "Neglected Heirloom"
    }
})
