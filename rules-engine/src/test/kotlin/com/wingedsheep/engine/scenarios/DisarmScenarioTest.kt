package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Bonesplitter
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Disarm
import com.wingedsheep.mtg.sets.definitions.mrd.cards.LeoninScimitar
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Disarm — "Unattach all Equipment from target creature." ({U} instant, MRD #32)
 *
 * The card is a composition rather than a bespoke effect: `CardSource.AttachedTo` gathers the
 * Equipment on the target, then `ForEachInCollectionEffect` unattaches each one. These tests pin
 * the two things that composition could plausibly get wrong — that *every* Equipment comes off
 * (not just the first), and that unattaching does not move anything to another zone (the
 * 2004-12-01 ruling).
 */
class DisarmScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + Bonesplitter + LeoninScimitar + Disarm)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Equip [equipment] (already on the battlefield) to [creature], paying its {1} equip cost. */
    fun GameTestDriver.equip(
        player: com.wingedsheep.sdk.model.EntityId,
        equipment: com.wingedsheep.sdk.model.EntityId,
        abilityId: com.wingedsheep.sdk.scripting.AbilityId,
        creature: com.wingedsheep.sdk.model.EntityId,
    ) {
        giveColorlessMana(player, 1)
        submit(
            ActivateAbility(player, equipment, abilityId, targets = listOf(ChosenTarget.Permanent(creature)))
        ).isSuccess shouldBe true
        bothPass()
    }

    test("all Equipment comes off — both swords unattach and their buffs stop applying") {
        val driver = createDriver()
        val you = driver.activePlayer!!

        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser") // 3/3
        val splitter = driver.putPermanentOnBattlefield(you, "Bonesplitter")  // +2/+0
        val scimitar = driver.putPermanentOnBattlefield(you, "Leonin Scimitar") // +1/+1

        driver.equip(you, splitter, Bonesplitter.activatedAbilities.first().id, courser)
        driver.equip(you, scimitar, LeoninScimitar.activatedAbilities.first().id, courser)

        // Precondition: both attached, both buffs live (3/3 +2/+0 +1/+1 = 6/4).
        driver.state.getEntity(splitter)?.get<AttachedToComponent>()?.targetId shouldBe courser
        driver.state.getEntity(scimitar)?.get<AttachedToComponent>()?.targetId shouldBe courser
        projector.getProjectedPower(driver.state, courser) shouldBe 6
        projector.getProjectedToughness(driver.state, courser) shouldBe 4

        val disarm = driver.putCardInHand(you, "Disarm")
        driver.giveMana(you, com.wingedsheep.sdk.core.Color.BLUE, 1)
        driver.castSpellWithTargets(you, disarm, listOf(ChosenTarget.Permanent(courser)))
            .isSuccess shouldBe true
        driver.bothPass()

        // Every Equipment is unattached — the ForEach ran over the whole gathered collection,
        // not just its first element.
        driver.state.getEntity(splitter)?.get<AttachedToComponent>().shouldBeNull()
        driver.state.getEntity(scimitar)?.get<AttachedToComponent>().shouldBeNull()

        // Both buffs are gone: back to a base 3/3.
        projector.getProjectedPower(driver.state, courser) shouldBe 3
        projector.getProjectedToughness(driver.state, courser) shouldBe 3
    }

    test("the Equipment stays on the battlefield under its controller's control (2004-12-01 ruling)") {
        val driver = createDriver()
        val you = driver.activePlayer!!

        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser")
        val splitter = driver.putPermanentOnBattlefield(you, "Bonesplitter")
        driver.equip(you, splitter, Bonesplitter.activatedAbilities.first().id, courser)

        val disarm = driver.putCardInHand(you, "Disarm")
        driver.giveMana(you, com.wingedsheep.sdk.core.Color.BLUE, 1)
        driver.castSpellWithTargets(you, disarm, listOf(ChosenTarget.Permanent(courser)))
            .isSuccess shouldBe true
        driver.bothPass()

        // Unattached, but not destroyed, bounced, or exiled — still a permanent you control.
        driver.state.getEntity(splitter)?.get<AttachedToComponent>().shouldBeNull()
        driver.findPermanent(you, "Bonesplitter") shouldBe splitter
        driver.getController(splitter) shouldBe you
    }

    test("a creature carrying no Equipment is a legal target and a silent no-op") {
        val driver = createDriver()
        val you = driver.activePlayer!!

        val courser = driver.putCreatureOnBattlefield(you, "Centaur Courser")

        val disarm = driver.putCardInHand(you, "Disarm")
        driver.giveMana(you, com.wingedsheep.sdk.core.Color.BLUE, 1)
        driver.castSpellWithTargets(you, disarm, listOf(ChosenTarget.Permanent(courser)))
            .isSuccess shouldBe true
        driver.bothPass()

        // Nothing to unattach: the creature survives untouched.
        driver.findPermanent(you, "Centaur Courser") shouldBe courser
        projector.getProjectedPower(driver.state, courser) shouldBe 3
    }
})
