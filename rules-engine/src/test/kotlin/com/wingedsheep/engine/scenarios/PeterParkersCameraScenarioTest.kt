package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.PeterParkersCamera
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Peter Parker's Camera (SPM #171) — {1} Artifact.
 *
 * The whole card is covered here:
 *  - "This artifact enters with three film counters on it." — an [EntersWithCounters] replacement
 *    (`Counters.FILM`, count 3, self-only), exercised via a real cast from hand so the counters are
 *    present immediately (replacement, not a trigger).
 *  - "{2}, {T}, Remove a film counter from this artifact: Copy target activated or triggered ability
 *    you control. You may choose new targets for the copy." — the cost pays {2}, taps the Camera, and
 *    removes one film counter; [Effects.CopyTargetSpellOrAbility] against
 *    [Targets.ActivatedOrTriggeredAbilityYouControl] copies the chosen ability on the stack and
 *    prompts for new targets (CR 707.10c).
 */
class PeterParkersCameraScenarioTest : FunSpec({

    // {T}: Target creature you control gets +1/+0 until end of turn — drives the copy ability's
    // targeted path so we can prove the copy takes a *different* target than the original.
    val testPumper = card("PPC Test Pumper") {
        manaCost = "{1}"
        typeLine = "Creature — Soldier"
        power = 1
        toughness = 1
        oracleText = "{T}: Target creature you control gets +1/+0 until end of turn."
        activatedAbility {
            cost = AbilityCost.Tap
            effect = Effects.ModifyStats(1, 0, EffectTarget.ContextTarget(0))
            target = Targets.CreatureYouControl
            timing = TimingRule.InstantSpeed
        }
    }

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(PeterParkersCamera, testPumper))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        return driver
    }

    val copyAbilityId = PeterParkersCamera.activatedAbilities.single().id

    fun filmCounters(driver: GameTestDriver, id: EntityId): Int =
        driver.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.FILM) ?: 0

    test("enters with three film counters when cast (replacement effect)") {
        val driver = setup()
        val me = driver.activePlayer!!

        val cameraCard = driver.putCardInHand(me, "Peter Parker's Camera")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveColorlessMana(me, 1)

        driver.castSpell(me, cameraCard).error shouldBe null
        driver.bothPass()

        val container = driver.state.getEntity(cameraCard)
        container.shouldNotBeNull()
        container.get<CardComponent>()!!.name shouldBe "Peter Parker's Camera"
        driver.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)).contains(cameraCard) shouldBe true
        filmCounters(driver, cameraCard) shouldBe 3
    }

    test("copy ability: copies a targeted activated ability I control, retargets the copy, and removes a film counter") {
        val driver = setup()
        val me = driver.activePlayer!!

        val camera = driver.putPermanentOnBattlefield(me, "Peter Parker's Camera")
        driver.replaceState(driver.state.updateEntity(camera) {
            it.with(CountersComponent(mapOf(CounterType.FILM to 3)))
        })

        val pumper = driver.putCreatureOnBattlefield(me, "PPC Test Pumper")
        driver.removeSummoningSickness(pumper)
        val creatureA = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val creatureB = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Activate the pumper targeting creatureA — its pump ability sits on the stack.
        val pumpAbilityId = driver.cardRegistry.requireCard("PPC Test Pumper").activatedAbilities[0].id
        driver.submitSuccess(
            ActivateAbility(
                playerId = me, sourceId = pumper, abilityId = pumpAbilityId,
                targets = listOf(ChosenTarget.Permanent(creatureA))
            )
        )
        val pumpOnStack = driver.getTopOfStack()!!

        // In response, activate the Camera's copy ability ({2}, {T}, remove a film counter),
        // targeting the pump ability on the stack.
        driver.giveColorlessMana(me, 2)
        driver.submitSuccess(
            ActivateAbility(
                playerId = me, sourceId = camera, abilityId = copyAbilityId,
                targets = listOf(ChosenTarget.Spell(pumpOnStack))
            )
        )

        // Cost paid: Camera tapped and one film counter removed.
        driver.isTapped(camera) shouldBe true
        filmCounters(driver, camera) shouldBe 2

        // Resolve the copy ability → prompt for new targets for the copy; aim it at creatureB.
        var guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
            driver.bothPass(); guard++
        }
        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(me, listOf(creatureB)).isSuccess shouldBe true

        // Resolve the copy (creatureB) then the original (creatureA): both get +1/+0.
        guard = 0
        while (driver.stackSize > 0 && guard < 20) { driver.bothPass(); guard++ }

        driver.state.projectedState.getPower(creatureA) shouldBe 3
        driver.state.projectedState.getPower(creatureB) shouldBe 3
    }
})
