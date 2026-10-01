package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe

class InstillEnergyScenarioTest : ScenarioTestBase() {
    init {
        val untapper = card("Instill Test Untapper") {
            manaCost = "{G}"
            typeLine = "Creature — Elf"
            power = 1
            toughness = 1
            activatedAbility {
                cost = Costs.Untap
                effect = Effects.Untap(EffectTarget.Self)
            }
        }
        val blankingAura = card("Instill Test Blanking Aura") {
            manaCost = "{U}"
            typeLine = "Enchantment — Aura"
            staticAbility { ability = LoseAllAbilities() }
        }
        cardRegistry.register(untapper)
        cardRegistry.register(blankingAura)

        fun board(creature: String = "Llanowar Elves", tapped: Boolean = false,
                  creatureOwner: Int = 1, active: Int = 1, blankHost: Boolean = false) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(creatureOwner, creature, tapped = tapped, summoningSickness = true)
            .withCardAttachedTo(1, "Instill Energy", creature)
            .apply { if (blankHost) withCardAttachedTo(1, "Instill Test Blanking Aura", creature) }
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(active)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun activation(game: TestGame) = ActivateAbility(
            game.player1Id, game.findPermanent("Instill Energy")!!,
            cardRegistry.getCard("Instill Energy")!!.script.activatedAbilities.single().id
        )

        test("a summoning-sick enchanted creature can attack without gaining haste") {
            val game = board()
            val elf = game.findPermanent("Llanowar Elves")!!
            game.state.getEntity(elf)!!.has<SummoningSicknessComponent>() shouldBe true
            StateProjector().project(game.state).hasKeyword(elf, Keyword.HASTE) shouldBe false
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.execute(DeclareAttackers(game.player1Id, mapOf(elf to game.player2Id))).error shouldBe null
        }

        test("attack permission survives the enchanted creature losing all abilities") {
            val game = board(blankHost = true)
            val elf = game.findPermanent("Llanowar Elves")!!
            game.state.getEntity(elf)!!.has<SummoningSicknessComponent>() shouldBe true
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.execute(DeclareAttackers(game.player1Id, mapOf(elf to game.player2Id))).error shouldBe null
        }

        test("attack permission does not enable a summoning-sick creature's tap ability") {
            val game = board()
            val elf = game.findPermanent("Llanowar Elves")!!
            game.state.getEntity(elf)!!.has<SummoningSicknessComponent>() shouldBe true
            val ability = cardRegistry.getCard("Llanowar Elves")!!.script.activatedAbilities.single().id
            game.getLegalActions(1).filter { it.isAffordable }.map { it.action }.filterIsInstance<ActivateAbility>()
                .any { it.sourceId == elf } shouldBe false
            game.execute(ActivateAbility(game.player1Id, elf, ability)).error shouldBe "This creature has summoning sickness"
        }

        test("attack permission does not enable a summoning-sick creature's untap-symbol ability") {
            val game = board(creature = untapper.name, tapped = true)
            val creature = game.findPermanent(untapper.name)!!
            game.state.getEntity(creature)!!.has<SummoningSicknessComponent>() shouldBe true
            game.getLegalActions(1).filter { it.isAffordable }.map { it.action }.filterIsInstance<ActivateAbility>()
                .any { it.sourceId == creature } shouldBe false
            game.execute(ActivateAbility(game.player1Id, creature, untapper.script.activatedAbilities.single().id))
                .error shouldBe "This creature has summoning sickness"
        }

        test("zero-cost untap resolves on the stack and can be activated only once each turn") {
            val game = board(tapped = true)
            val elf = game.findPermanent("Llanowar Elves")!!
            game.state.getEntity(elf)!!.has<SummoningSicknessComponent>() shouldBe true
            game.execute(activation(game)).error shouldBe null
            game.state.getEntity(elf)!!.has<TappedComponent>() shouldBe true
            // The activation limit is spent even while the first activation is still on the stack.
            game.execute(activation(game)).error shouldBe "This ability can only be activated once each turn"
            game.resolveStack()
            game.state.getEntity(elf)!!.has<TappedComponent>() shouldBe false
            game.execute(activation(game)).error shouldBe "This ability can only be activated once each turn"
        }

        test("Aura controller can untap an opponent's creature during their own turn") {
            val game = board(tapped = true, creatureOwner = 2)
            val elf = game.findPermanent("Llanowar Elves")!!
            game.state.getEntity(elf)!!.has<SummoningSicknessComponent>() shouldBe true
            game.execute(activation(game)).error shouldBe null
            game.resolveStack()
            game.state.getEntity(elf)!!.has<TappedComponent>() shouldBe false
        }

        test("untap cannot be activated during the opponent's turn") {
            val game = board(tapped = true, active = 2)
            val aura = game.findPermanent("Instill Energy")!!
            game.getLegalActions(1).filter { it.isAffordable }.map { it.action }.filterIsInstance<ActivateAbility>()
                .any { it.sourceId == aura } shouldBe false
            game.execute(activation(game)).error shouldBe "This ability can only be activated during your turn"
        }

        test("casting attaches the Aura and destroying it removes the attack permission") {
            val game = scenario().withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Llanowar Elves", summoningSickness = true)
                .withCardInHand(1, "Instill Energy").withCardInHand(1, "Disenchant")
                .withLandsOnBattlefield(1, "Forest", 1).withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val elf = game.findPermanent("Llanowar Elves")!!
            game.state.getEntity(elf)!!.has<SummoningSicknessComponent>() shouldBe true
            game.castSpell(1, "Instill Energy", targetId = elf).error shouldBe null
            game.resolveStack()
            val aura = game.findPermanent("Instill Energy")!!
            game.state.getEntity(aura)!!.get<AttachedToComponent>()!!.targetId shouldBe elf
            game.castSpell(1, "Disenchant", targetId = aura).error shouldBe null
            game.resolveStack()
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.execute(DeclareAttackers(game.player1Id, mapOf(elf to game.player2Id))).error shouldBe "Llanowar Elves has summoning sickness"
        }
    }
}
