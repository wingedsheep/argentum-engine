package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.msh.cards.EchoPerceptiveProdigy
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Echo, Perceptive Prodigy (MSH #51) — {2}{U} Legendary Creature — Human Hero, 1/4.
 *
 * Vigilance
 * {1}, {T}: Copy target activated or triggered ability you control from a creature source. You may
 * choose new targets for the copy.
 *
 * The new piece under test is the source restriction: `CardPredicate.AbilitySourceMatches(Creature)`
 * narrowing the existing "target activated or triggered ability you control". Covered here:
 *  - a creature-source activated ability is *enumerated* as a legal target and copies correctly,
 *    with the copy retargeted (CR 707.10c);
 *  - an artifact-source ability is neither enumerated nor accepted — the restriction actually bites;
 *  - a dead creature's dies trigger is still "from a creature source": the source is in the
 *    graveyard while the trigger is on the stack, so the match runs on last known information
 *    (CR 113.7a).
 */
class EchoPerceptiveProdigyScenarioTest : FunSpec({

    // Creature source: {T}: Target creature you control gets +1/+0 until end of turn.
    val testPumper = card("Echo Test Pumper") {
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

    // Artifact source: the same ability on a noncreature artifact — the negative control.
    val testLens = card("Echo Test Lens") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "{T}: Target creature you control gets +1/+0 until end of turn."
        activatedAbility {
            cost = AbilityCost.Tap
            effect = Effects.ModifyStats(1, 0, EffectTarget.ContextTarget(0))
            target = Targets.CreatureYouControl
            timing = TimingRule.InstantSpeed
        }
    }

    // Creature source whose ability is a *triggered* one, and whose source is already in the
    // graveyard by the time the trigger is on the stack — the last-known-information case.
    val testMartyr = card("Echo Test Martyr") {
        manaCost = "{1}"
        typeLine = "Creature — Soldier"
        power = 1
        toughness = 1
        oracleText = "When this creature dies, draw a card."
        triggeredAbility {
            trigger = Triggers.Dies
            effect = Effects.DrawCards(1)
            description = "When this creature dies, draw a card."
        }
    }

    val copyAbilityId = EchoPerceptiveProdigy.activatedAbilities.single().id

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(EchoPerceptiveProdigy, testPumper, testLens, testMartyr))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        return driver
    }

    /** Echo on the battlefield, untapped and able to tap for its ability. */
    fun deployEcho(driver: GameTestDriver, me: EntityId): EntityId {
        val echo = driver.putCreatureOnBattlefield(me, "Echo, Perceptive Prodigy")
        driver.removeSummoningSickness(echo)
        return echo
    }

    /** The legal targets the enumerator offers for Echo's copy ability right now. */
    fun offeredCopyTargets(driver: GameTestDriver, me: EntityId, echo: EntityId): List<EntityId> =
        driver.legalActions(me)
            .filter { (it.action as? ActivateAbility)?.sourceId == echo }
            .flatMap { it.validTargets ?: emptyList() }

    test("has vigilance") {
        val driver = setup()
        val me = driver.activePlayer!!
        val echo = deployEcho(driver, me)
        driver.state.projectedState.hasKeyword(echo, Keyword.VIGILANCE) shouldBe true
    }

    test("copies a creature-source activated ability and retargets the copy") {
        val driver = setup()
        val me = driver.activePlayer!!

        val echo = deployEcho(driver, me)
        val pumper = driver.putCreatureOnBattlefield(me, "Echo Test Pumper")
        driver.removeSummoningSickness(pumper)
        val creatureA = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val creatureB = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val pumpAbilityId = driver.cardRegistry.requireCard("Echo Test Pumper").activatedAbilities[0].id
        driver.submitSuccess(
            ActivateAbility(
                playerId = me, sourceId = pumper, abilityId = pumpAbilityId,
                targets = listOf(ChosenTarget.Permanent(creatureA))
            )
        )
        val pumpOnStack = driver.getTopOfStack()!!

        // The server must *offer* the ability on the stack as a legal target — not merely accept it
        // when an action is submitted. This is what makes the card clickable in the UI.
        driver.giveColorlessMana(me, 1)
        offeredCopyTargets(driver, me, echo) shouldContain pumpOnStack

        driver.submitSuccess(
            ActivateAbility(
                playerId = me, sourceId = echo, abilityId = copyAbilityId,
                targets = listOf(ChosenTarget.Spell(pumpOnStack))
            )
        )
        driver.isTapped(echo) shouldBe true

        // Resolve the copy ability → prompt for new targets for the copy; aim it at creatureB.
        var guard = 0
        while (driver.state.pendingDecision !is ChooseTargetsDecision && guard < 20) {
            driver.bothPass(); guard++
        }
        (driver.state.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(me, listOf(creatureB)).isSuccess shouldBe true

        guard = 0
        while (driver.stackSize > 0 && guard < 20) { driver.bothPass(); guard++ }

        driver.state.projectedState.getPower(creatureA) shouldBe 3
        driver.state.projectedState.getPower(creatureB) shouldBe 3
    }

    test("an artifact-source ability is neither offered nor accepted as a target") {
        val driver = setup()
        val me = driver.activePlayer!!

        val echo = deployEcho(driver, me)
        val lens = driver.putPermanentOnBattlefield(me, "Echo Test Lens")
        val creatureA = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val lensAbilityId = driver.cardRegistry.requireCard("Echo Test Lens").activatedAbilities[0].id
        driver.submitSuccess(
            ActivateAbility(
                playerId = me, sourceId = lens, abilityId = lensAbilityId,
                targets = listOf(ChosenTarget.Permanent(creatureA))
            )
        )
        val lensOnStack = driver.getTopOfStack()!!

        driver.giveColorlessMana(me, 1)
        // Not offered: the only ability on the stack has an artifact source.
        offeredCopyTargets(driver, me, echo).contains(lensOnStack) shouldBe false

        // ...and hand-submitting it is rejected too, so the restriction is not merely cosmetic.
        driver.submitExpectFailure(
            ActivateAbility(
                playerId = me, sourceId = echo, abilityId = copyAbilityId,
                targets = listOf(ChosenTarget.Spell(lensOnStack))
            )
        )
    }

    test("last known information: a dead creature's dies trigger is still a creature source") {
        val driver = setup()
        val me = driver.activePlayer!!

        val echo = deployEcho(driver, me)
        val martyr = driver.putCreatureOnBattlefield(me, "Echo Test Martyr")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Kill the Martyr for real so its dies trigger goes on the stack with the source already in
        // the graveyard (CR 113.7a — the ability exists independently of its source).
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(martyr)).error shouldBe null
        var guard = 0
        while (!driver.getGraveyard(me).contains(martyr) && guard < 20) {
            driver.bothPass(); guard++
        }
        driver.getGraveyard(me).contains(martyr) shouldBe true

        val diesTrigger = driver.getTopOfStack()!!

        val handBefore = driver.getHandSize(me)
        driver.giveColorlessMana(me, 1)
        offeredCopyTargets(driver, me, echo) shouldContain diesTrigger

        driver.submitSuccess(
            ActivateAbility(
                playerId = me, sourceId = echo, abilityId = copyAbilityId,
                targets = listOf(ChosenTarget.Spell(diesTrigger))
            )
        )
        guard = 0
        while (driver.stackSize > 0 && guard < 30) { driver.bothPass(); guard++ }

        // Original + copy each draw a card.
        driver.getHandSize(me) shouldBe handBefore + 2
    }
})
