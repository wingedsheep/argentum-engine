package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eoe.cards.ChromeCompanion
import com.wingedsheep.mtg.sets.definitions.eoe.cards.SteelswarmOperator
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Steelswarm Operator's two restricted mana abilities.
 *
 * Steelswarm Operator ({1}{U}, 1/1 Artifact Creature — Robot Soldier, Flying):
 *   {T}: Add {U}. Spend this mana only to cast an artifact spell.
 *   {T}: Add {U}{U}. Spend this mana only to activate abilities of artifact sources.
 *
 * Bug guarded against: when both abilities live on the same source, the auto-tap
 * solver used to collapse "different restrictions" to "unrestricted" and treat the
 * source as a 2-blue producer, leaving stray blue mana in the pool after casting an
 * artifact spell. The fix filters mana abilities by the spell payment context before
 * combining them into a single ManaSource.
 *
 * Coverage:
 *  - Auto-tap pays an artifact spell with Steelswarm + Island, no leftover mana
 *    (regression for the original bug).
 *  - Solver rejects a non-artifact {U} cost when Steelswarm is the only source
 *    (negative test: restricted mana can't fund off-type spells).
 *  - Steelswarm's {U}{U} ability pays for Chrome Companion's {2},{T} artifact
 *    activated ability end-to-end (graveyard target is moved on resolution).
 *  - Leftover bonus mana retains the source's restriction in
 *    [com.wingedsheep.engine.mechanics.mana.ManaSolution.remainingBonusMana].
 *  - Card-definition sanity: both activated abilities are mana abilities.
 *
 * Not exercised end-to-end here: the *activate* flow's residual-restricted-mana path
 * (a cheaper-than-{2} artifact activated ability would let us assert a restricted
 * leftover in the player's pool after activation). No such ability is implemented in
 * EOE yet; the solver test above pins the upstream contract, and [ActivateAbilityHandler]
 * trusts it. Add an end-to-end check if a {0}/{1}/{T} artifact ability lands.
 */
class SteelswarmOperatorTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Grizzly Bears" to 20),
            skipMulligans = true,
        )
        return driver
    }

    test("auto-tap casting an artifact spell only uses the artifact-spell-only ability, no leftover mana") {
        val driver = createDriver()
        val caster = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val operator = driver.putPermanentOnBattlefield(caster, "Steelswarm Operator")
        val island = driver.putLandOnBattlefield(caster, "Island")
        val relic = driver.putCardInHand(caster, "Cryogen Relic")

        val castResult = driver.castSpell(caster, relic)
        castResult.isSuccess shouldBe true

        // Both sources tapped.
        driver.state.getEntity(operator)?.has<TappedComponent>() shouldBe true
        driver.state.getEntity(island)?.has<TappedComponent>() shouldBe true

        // Mana pool should be empty — the bug left 1 leftover blue here because the
        // solver assumed Steelswarm Operator's {U}{U} ability could pay for the spell.
        val pool = driver.state.getEntity(caster)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        pool.blue shouldBe 0
        pool.colorless shouldBe 0
        pool.restrictedMana.size shouldBe 0

        // Resolve the stack — Cryogen Relic should now be on the battlefield.
        driver.bothPass()
        driver.state.getZone(caster, Zone.BATTLEFIELD).contains(relic) shouldBe true
    }

    test("solver reports a non-artifact {U} cost as unaffordable when only Steelswarm Operator is available") {
        val driver = createDriver()
        val caster = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(caster, "Steelswarm Operator")

        // Steelswarm is the only source. Both abilities restrict to artifact contexts;
        // a non-artifact creature spell should report unaffordable.
        val solver = ManaSolver(driver.cardRegistry)
        val cost = ManaCost.parse("{U}")
        val creatureSpellContext = SpellPaymentContext(
            isCreature = true,
            cardTypes = setOf(CardType.CREATURE),
        )
        val artifactSpellContext = SpellPaymentContext(
            cardTypes = setOf(CardType.ARTIFACT),
        )

        solver.canPay(driver.state, caster, cost, spellContext = creatureSpellContext) shouldBe false
        solver.canPay(driver.state, caster, cost, spellContext = artifactSpellContext) shouldBe true
    }

    test("Steelswarm's {U}{U} mana pays for Chrome Companion's {2},{T} artifact ability") {
        val driver = createDriver()
        val caster = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val operator = driver.putPermanentOnBattlefield(caster, "Steelswarm Operator")
        val chromeCompanion = driver.putPermanentOnBattlefield(caster, "Chrome Companion")
        val graveyardCard = driver.putCardInGraveyard(caster, "Bombard")

        // Activate Chrome Companion's {2},{T} ability — the auto-tap solver should fund
        // the {2} from Steelswarm Operator's second mana ability (the {U}{U} restricted to
        // abilities of artifact sources), since Chrome Companion is itself an artifact.
        val abilityToPayFor = ChromeCompanion.activatedAbilities.first().id

        val activateResult = driver.submit(
            ActivateAbility(
                playerId = caster,
                sourceId = chromeCompanion,
                abilityId = abilityToPayFor,
                targets = listOf(ChosenTarget.Card(graveyardCard, caster, Zone.GRAVEYARD)),
            )
        )
        activateResult.isSuccess shouldBe true

        // Steelswarm Operator must have been tapped to produce the {U}{U} that paid the
        // {2} portion of Chrome Companion's cost.
        driver.state.getEntity(operator)?.has<TappedComponent>() shouldBe true
        driver.state.getEntity(chromeCompanion)?.has<TappedComponent>() shouldBe true

        // No leftover mana in pool.
        val pool = driver.state.getEntity(caster)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        pool.blue shouldBe 0
        pool.colorless shouldBe 0
        pool.restrictedMana.size shouldBe 0

        // Drain the stack — Chrome Companion's "becomes tapped → gain 1 life" trigger
        // also fired when its cost tapped it, so the stack contains both the triggered
        // ability AND the activated ability we're trying to resolve.
        var safety = 0
        while (driver.state.stack.isNotEmpty() && safety++ < 10) {
            driver.bothPass()
        }
        driver.state.getZone(caster, Zone.GRAVEYARD).contains(graveyardCard) shouldBe false
        driver.state.getZone(caster, Zone.LIBRARY).contains(graveyardCard) shouldBe true
    }

    test("leftover bonus mana from Steelswarm's {U}{U} ability keeps its artifact-only restriction") {
        // Solving a {U} ability-activation cost taps Steelswarm B (produces 2 BLUE);
        // the unused BLUE is reported in `remainingBonusMana` and must carry the
        // ArtifactSourceAbilitiesOnly restriction so the caller can route it back into
        // the pool via ManaPool.addRestricted — otherwise the player would be able to
        // spend that leftover blue on anything.
        val driver = createDriver()
        val caster = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(caster, "Steelswarm Operator")

        val solver = ManaSolver(driver.cardRegistry)
        val cost = ManaCost.parse("{U}")
        val abilityContext = SpellPaymentContext(
            isAbilityActivation = true,
            abilitySourceCardTypes = setOf(CardType.ARTIFACT),
        )
        val solution = solver.solve(driver.state, caster, cost, spellContext = abilityContext)
            ?: error("expected a solution")

        solution.remainingBonusMana.size shouldBe 1
        val leftover = solution.remainingBonusMana.first()
        leftover.color shouldBe Color.BLUE
        leftover.amount shouldBe 1
        leftover.restriction shouldBe ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
            cardType = CardType.ARTIFACT,
            allowSpells = false,
            allowAbilities = true,
        )
    }

    test("card definition exposes two activated mana abilities") {
        // Sanity: SteelswarmOperator's two abilities are both manaAbility=true.
        SteelswarmOperator.activatedAbilities.size shouldBe 2
        SteelswarmOperator.activatedAbilities.all { it.isManaAbility } shouldBe true
    }
})
