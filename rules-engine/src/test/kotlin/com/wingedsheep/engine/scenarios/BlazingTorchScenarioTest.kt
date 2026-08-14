package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.avr.cards.AngelsTomb
import com.wingedsheep.mtg.sets.definitions.emn.cards.GrappleWithThePast
import com.wingedsheep.mtg.sets.definitions.mid.cards.EccentricFarmer
import com.wingedsheep.mtg.sets.definitions.mid.cards.SiegeZombie
import com.wingedsheep.mtg.sets.definitions.zen.cards.BlazingTorch
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/** Scenario tests for Blazing Torch. */
class BlazingTorchScenarioTest : FunSpec({

    val batch = listOf(GrappleWithThePast, EccentricFarmer, SiegeZombie, AngelsTomb, BlazingTorch)
    val projector = StateProjector()

    fun setup(deck: Deck = Deck.of("Forest" to 40)): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + batch)
        initMirrorMatch(deck = deck, startingLife = 20, skipMulligans = true)
    }

    // ── Blazing Torch ────────────────────────────────────────────────────────

    test("Blazing Torch: bearer's granted ability sacrifices the Torch and deals 2 damage") {
        val d = setup(Deck.of("Mountain" to 40))
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.removeSummoningSickness(bear)
        val torch = d.putPermanentOnBattlefield(you, "Blazing Torch")

        val equipId = BlazingTorch.activatedAbilities.single { it.isEquipAbility }.id
        d.giveColorlessMana(you, 1)
        d.submit(
            ActivateAbility(
                playerId = you,
                sourceId = torch,
                abilityId = equipId,
                targets = listOf(ChosenTarget.Permanent(bear))
            )
        ).isSuccess shouldBe true
        d.bothPass()
        d.state.getEntity(torch)?.get<AttachedToComponent>()?.targetId shouldBe bear

        // The quoted ability lives on the *creature*, granted by the Equipment.
        val grantedId = BlazingTorch.staticAbilities
            .filterIsInstance<GrantActivatedAbility>()
            .single()
            .ability
            .id

        d.submit(
            ActivateAbility(
                playerId = you,
                sourceId = bear,
                abilityId = grantedId,
                targets = listOf(ChosenTarget.Player(opponent))
            )
        ).isSuccess shouldBe true

        // `{T}` taps the bearer; the Torch is sacrificed as part of the cost, before resolution.
        d.isTapped(bear) shouldBe true
        d.findPermanent(you, "Blazing Torch") shouldBe null
        d.getGraveyard(you) shouldContain torch

        d.bothPass()
        d.getLifeTotal(opponent) shouldBe 18
    }

    test("Blazing Torch: equipped creature can't be blocked by a Zombie") {
        val d = setup(Deck.of("Mountain" to 40))
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = d.putCreatureOnBattlefield(you, "Grizzly Bears")
        d.removeSummoningSickness(bear)
        val torch = d.putPermanentOnBattlefield(you, "Blazing Torch")
        val zombie = d.putCreatureOnBattlefield(opponent, "Siege Zombie")
        d.removeSummoningSickness(zombie)

        val equipId = BlazingTorch.activatedAbilities.single { it.isEquipAbility }.id
        d.giveColorlessMana(you, 1)
        d.submit(
            ActivateAbility(
                playerId = you,
                sourceId = torch,
                abilityId = equipId,
                targets = listOf(ChosenTarget.Permanent(bear))
            )
        ).isSuccess shouldBe true
        d.bothPass()

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(you, listOf(bear), opponent).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)

        // The Zombie is an otherwise-legal blocker, but the Torch's evasion forbids it.
        (d.declareBlockers(opponent, mapOf(zombie to listOf(bear))).error != null) shouldBe true
    }
})
