package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.msh.cards.ScientistSupremeOfAim
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
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
 * Scientist Supreme of A.I.M. (MSH #225) — {U}{B} Legendary Creature — Human Scientist Villain, 2/2.
 *
 * Pay 2 life: Copy target activated or triggered ability you control from an artifact source. You
 * may choose new targets for the copy. Activate only during your turn and only once each turn.
 *
 * The artifact-source twin of Echo, Perceptive Prodigy. Covered here:
 *  - an artifact-source ability is enumerated and copied, and the copy is retargeted (CR 707.10c);
 *  - a creature-source ability is neither offered nor accepted;
 *  - last known information (CR 113.7a): an artifact that sacrificed *itself* to pay for the
 *    ability is still an artifact source, both as a plain card (which lands in the graveyard) and
 *    as a **token**, whose entity CR 704.5d deletes outright — the cracked-Clue line this card is
 *    printed for;
 *  - the cost is 2 life and the ability is once-per-turn and your-turn-only.
 */
class ScientistSupremeOfAimScenarioTest : FunSpec({

    // Artifact source: {T}: Target creature you control gets +1/+0 until end of turn.
    val testLens = card("Scientist Test Lens") {
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

    // Creature source: the negative control.
    val testPumper = card("Scientist Test Pumper") {
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

    // Nontoken artifact that eats itself to pay for its own ability — the last-known-information
    // case for a source that is in the graveyard by the time the ability is targeted.
    val testCache = card("Scientist Test Cache") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = "{2}, Sacrifice this artifact: Draw a card."
        activatedAbility {
            cost = Costs.Composite(Costs.Mana("{2}"), Costs.SacrificeSelf)
            effect = Effects.DrawCards(1)
            timing = TimingRule.InstantSpeed
        }
    }

    // Sorcery that makes a Clue token, so the token-source case can be built the way a real game
    // builds it (MSH's own Agent 13, Sharon Carter / Panther Pounce investigate).
    val testDossier = card("Scientist Test Dossier") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Investigate."
        spell { effect = Effects.Investigate() }
    }

    val copyAbilityId = ScientistSupremeOfAim.activatedAbilities.single().id
    val clueSacAbilityId = PredefinedTokens.Clue.activatedAbilities.single().id
    val cacheSacAbilityId = testCache.activatedAbilities.single().id

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(ScientistSupremeOfAim, testLens, testPumper, testCache, testDossier)
        )
        driver.registerCard(PredefinedTokens.Clue)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        return driver
    }

    /** The legal targets the enumerator offers for the Scientist's copy ability right now. */
    fun offeredCopyTargets(driver: GameTestDriver, me: EntityId, scientist: EntityId): List<EntityId> =
        driver.legalActions(me)
            .filter { (it.action as? ActivateAbility)?.sourceId == scientist }
            .flatMap { it.validTargets ?: emptyList() }

    test("copies an artifact-source ability, retargets the copy, and costs 2 life") {
        val driver = setup()
        val me = driver.activePlayer!!

        // No tap in the cost, so summoning sickness is irrelevant — deliberately not cleared.
        val scientist = driver.putCreatureOnBattlefield(me, "Scientist Supreme of A.I.M.")
        val lens = driver.putPermanentOnBattlefield(me, "Scientist Test Lens")
        val creatureA = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val creatureB = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val lensAbilityId = driver.cardRegistry.requireCard("Scientist Test Lens").activatedAbilities[0].id
        driver.submitSuccess(
            ActivateAbility(
                playerId = me, sourceId = lens, abilityId = lensAbilityId,
                targets = listOf(ChosenTarget.Permanent(creatureA))
            )
        )
        val lensOnStack = driver.getTopOfStack()!!

        // Enumerated as a legal target, not merely accepted on submit.
        offeredCopyTargets(driver, me, scientist) shouldContain lensOnStack

        val lifeBefore = driver.getLifeTotal(me)
        driver.submitSuccess(
            ActivateAbility(
                playerId = me, sourceId = scientist, abilityId = copyAbilityId,
                targets = listOf(ChosenTarget.Spell(lensOnStack))
            )
        )
        driver.getLifeTotal(me) shouldBe lifeBefore - 2

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

    test("a creature-source ability is neither offered nor accepted as a target") {
        val driver = setup()
        val me = driver.activePlayer!!

        val scientist = driver.putCreatureOnBattlefield(me, "Scientist Supreme of A.I.M.")
        val pumper = driver.putCreatureOnBattlefield(me, "Scientist Test Pumper")
        driver.removeSummoningSickness(pumper)
        val creatureA = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val pumpAbilityId = driver.cardRegistry.requireCard("Scientist Test Pumper").activatedAbilities[0].id
        driver.submitSuccess(
            ActivateAbility(
                playerId = me, sourceId = pumper, abilityId = pumpAbilityId,
                targets = listOf(ChosenTarget.Permanent(creatureA))
            )
        )
        val pumpOnStack = driver.getTopOfStack()!!

        offeredCopyTargets(driver, me, scientist).contains(pumpOnStack) shouldBe false

        driver.submitExpectFailure(
            ActivateAbility(
                playerId = me, sourceId = scientist, abilityId = copyAbilityId,
                targets = listOf(ChosenTarget.Spell(pumpOnStack))
            )
        )
    }

    test("an artifact that sacrificed itself to pay for its own ability is still an artifact source") {
        val driver = setup()
        val me = driver.activePlayer!!

        val scientist = driver.putCreatureOnBattlefield(me, "Scientist Supreme of A.I.M.")
        val cache = driver.putPermanentOnBattlefield(me, "Scientist Test Cache")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val handBefore = driver.getHandSize(me)
        driver.giveColorlessMana(me, 2)
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = cache, abilityId = cacheSacAbilityId))

        // The source paid for itself; it is in the graveyard, not on the battlefield (CR 113.7a).
        driver.findPermanent(me, "Scientist Test Cache") shouldBe null
        val drawOnStack = driver.getTopOfStack()!!

        offeredCopyTargets(driver, me, scientist) shouldContain drawOnStack
        driver.submitSuccess(
            ActivateAbility(
                playerId = me, sourceId = scientist, abilityId = copyAbilityId,
                targets = listOf(ChosenTarget.Spell(drawOnStack))
            )
        )

        var guard = 0
        while (driver.stackSize > 0 && guard < 20) { driver.bothPass(); guard++ }

        // The copy plus the original: two cards drawn.
        driver.getHandSize(me) shouldBe handBefore + 2
    }

    test("a Clue token's draw ability is still an artifact source after CR 704.5d deletes the token") {
        val driver = setup()
        val me = driver.activePlayer!!

        val scientist = driver.putCreatureOnBattlefield(me, "Scientist Supreme of A.I.M.")
        // A second, unrelated ability to stack on top of the Clue's, purely so *it* is what
        // resolves first (see below) and the draw ability outlives the token's cleanup.
        val lens = driver.putPermanentOnBattlefield(me, "Scientist Test Lens")
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val dossier = driver.putCardInHand(me, "Scientist Test Dossier")
        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, dossier).isSuccess shouldBe true
        driver.bothPass() // Investigate resolves
        val clue = driver.findPermanent(me, "Clue")!!

        val handBefore = driver.getHandSize(me)
        driver.giveColorlessMana(me, 2)
        driver.submitSuccess(ActivateAbility(playerId = me, sourceId = clue, abilityId = clueSacAbilityId))
        val drawOnStack = driver.getTopOfStack()!!

        // State-based actions (CR 117.5, CR 704.5d) are applied on the engine's post-resolution
        // pass, so the token entity is still readable at the instant of activation. Reproduce the
        // ordinary table sequence that outlives it: put a second ability on top of the draw, let
        // *that* resolve — the SBA pass sweeps the sacrificed token then — and the Clue's draw
        // ability is still sitting on the stack with no source entity behind it.
        val lensAbilityId = driver.cardRegistry.requireCard("Scientist Test Lens").activatedAbilities[0].id
        driver.submitSuccess(
            ActivateAbility(
                playerId = me, sourceId = lens, abilityId = lensAbilityId,
                targets = listOf(ChosenTarget.Permanent(bear))
            )
        )
        driver.bothPass() // the pump ability resolves; the sacrificed token ceases to exist

        // CR 704.5d has now swept the sacrificed token out of the graveyard: there is no entity
        // left to read, only the EntitySnapshot the activation froze.
        driver.state.getEntity(clue) shouldBe null

        offeredCopyTargets(driver, me, scientist) shouldContain drawOnStack
        driver.submitSuccess(
            ActivateAbility(
                playerId = me, sourceId = scientist, abilityId = copyAbilityId,
                targets = listOf(ChosenTarget.Spell(drawOnStack))
            )
        )

        var guard = 0
        while (driver.stackSize > 0 && guard < 20) { driver.bothPass(); guard++ }

        driver.getHandSize(me) shouldBe handBefore + 2
    }

    test("activate only once each turn") {
        val driver = setup()
        val me = driver.activePlayer!!

        val scientist = driver.putCreatureOnBattlefield(me, "Scientist Supreme of A.I.M.")
        val lens = driver.putPermanentOnBattlefield(me, "Scientist Test Lens")
        val lens2 = driver.putPermanentOnBattlefield(me, "Scientist Test Lens")
        val creatureA = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val lensAbilityId = driver.cardRegistry.requireCard("Scientist Test Lens").activatedAbilities[0].id
        driver.submitSuccess(
            ActivateAbility(
                playerId = me, sourceId = lens, abilityId = lensAbilityId,
                targets = listOf(ChosenTarget.Permanent(creatureA))
            )
        )
        driver.submitSuccess(
            ActivateAbility(
                playerId = me, sourceId = scientist, abilityId = copyAbilityId,
                targets = listOf(ChosenTarget.Spell(driver.getTopOfStack()!!))
            )
        )

        // Second artifact ability on the stack, but the Scientist is spent for the turn.
        driver.submitSuccess(
            ActivateAbility(
                playerId = me, sourceId = lens2, abilityId = lensAbilityId,
                targets = listOf(ChosenTarget.Permanent(creatureA))
            )
        )
        val secondLensOnStack = driver.getTopOfStack()!!

        offeredCopyTargets(driver, me, scientist).contains(secondLensOnStack) shouldBe false
        driver.submitExpectFailure(
            ActivateAbility(
                playerId = me, sourceId = scientist, abilityId = copyAbilityId,
                targets = listOf(ChosenTarget.Spell(secondLensOnStack))
            )
        )
    }

    test("can't be activated during an opponent's turn") {
        val driver = setup()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val scientist = driver.putCreatureOnBattlefield(opponent, "Scientist Supreme of A.I.M.")
        val lens = driver.putPermanentOnBattlefield(opponent, "Scientist Test Lens")
        val creatureA = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // Hand priority to the non-active player so they can use their own artifact at instant speed.
        driver.passPriority(me)

        val lensAbilityId = driver.cardRegistry.requireCard("Scientist Test Lens").activatedAbilities[0].id
        driver.submitSuccess(
            ActivateAbility(
                playerId = opponent, sourceId = lens, abilityId = lensAbilityId,
                targets = listOf(ChosenTarget.Permanent(creatureA))
            )
        )
        val lensOnStack = driver.getTopOfStack()!!

        // It is `me`'s turn, so the Scientist's controller may not activate it.
        offeredCopyTargets(driver, opponent, scientist).contains(lensOnStack) shouldBe false
        driver.submitExpectFailure(
            ActivateAbility(
                playerId = opponent, sourceId = scientist, abilityId = copyAbilityId,
                targets = listOf(ChosenTarget.Spell(lensOnStack))
            )
        )
    }
})
