package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.mechanics.mana.ChosenTypeManaRestriction
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.mechanics.mana.isSatisfiedBy
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.identity.CantBeCounteredComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.avr.cards.CavernOfSouls
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.effects.ManaSpellRider
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Engine tests for [ManaRestriction.OfChosenType] — "spend this mana only to cast a [creature]
 * spell of the chosen type" — and its binding by [ChosenTypeManaRestriction] to the producing
 * source's chosen creature type (CR 614.12 "as enters, choose a creature type").
 *
 * Covers: binding (bare, nested in AnyOf/AllOf, no type chosen, no source); the unbound form
 * paying for nothing; [ManaRestriction.LegendarySpellsOnly]'s creatureOnly axis; and Cavern of
 * Souls end to end on both payment paths — mana floated by tapping it (executor binds), and
 * auto-pay tapping it as a solver source (solver binds) — including its uncounterable rider.
 */
class ChosenTypeManaRestrictionTest : ScenarioTestBase() {

    private val cavernManaAbilityId = CavernOfSouls.activatedAbilities[1].id

    private val testElf = card("Chosen Type Test Elf") {
        manaCost = "{G}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
    }
    private val testGoblin = card("Chosen Type Test Goblin") {
        manaCost = "{G}"
        typeLine = "Creature — Goblin"
        power = 1
        toughness = 1
    }

    private fun TestGame.chooseType(source: EntityId, type: String) {
        state = state.updateEntity(source) {
            it.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.CREATURE_TYPE to ChoiceValue.TextChoice(type))))
        }
    }

    private fun cavernGame(chosenType: String?): Pair<TestGame, EntityId> {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Cavern of Souls")
            .withCardInHand(1, testElf.name)
            .withCardInHand(1, testGoblin.name)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val cavern = game.findPermanent("Cavern of Souls")!!
        if (chosenType != null) game.chooseType(cavern, chosenType)
        return game to cavern
    }

    private fun TestGame.handCard(name: String): EntityId =
        state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    private fun TestGame.cast(name: String, strategy: PaymentStrategy) =
        execute(CastSpell(player1Id, handCard(name), paymentStrategy = strategy))

    init {
        cardRegistry.register(CavernOfSouls)
        cardRegistry.register(listOf(testElf, testGoblin))

        context("binding") {
            test("binds to the source's chosen type, also inside AnyOf and AllOf") {
                val (game, cavern) = cavernGame("Elf")
                val bound = ManaRestriction.SubtypeSpellsOrAbilitiesOnly("Elf", creatureOnly = true)

                ChosenTypeManaRestriction.bind(ManaRestriction.OfChosenType(creatureOnly = true), game.state, cavern) shouldBe bound
                ChosenTypeManaRestriction.bind(
                    ManaRestriction.AnyOf(listOf(ManaRestriction.OfChosenType(true), ManaRestriction.LegendarySpellsOnly(true))),
                    game.state, cavern,
                ) shouldBe ManaRestriction.AnyOf(listOf(bound, ManaRestriction.LegendarySpellsOnly(true)))
                ChosenTypeManaRestriction.bind(
                    ManaRestriction.AllOf(listOf(ManaRestriction.SpellsOnly, ManaRestriction.OfChosenType(false))),
                    game.state, cavern,
                ) shouldBe ManaRestriction.AllOf(
                    listOf(ManaRestriction.SpellsOnly, ManaRestriction.SubtypeSpellsOrAbilitiesOnly("Elf", false))
                )
            }

            test("leaves restrictions without the chosen type, and sources without a choice, alone") {
                val (game, cavern) = cavernGame(null)
                ChosenTypeManaRestriction.bind(null, game.state, cavern) shouldBe null
                ChosenTypeManaRestriction.bind(ManaRestriction.CreatureSpellsOnly, game.state, cavern) shouldBe
                    ManaRestriction.CreatureSpellsOnly
                ChosenTypeManaRestriction.bind(ManaRestriction.OfChosenType(true), game.state, cavern) shouldBe
                    ManaRestriction.OfChosenType(true)
                ChosenTypeManaRestriction.bind(ManaRestriction.OfChosenType(true), game.state, null) shouldBe
                    ManaRestriction.OfChosenType(true)
            }
        }

        context("spend contexts") {
            val elfSpell = SpellPaymentContext(isCreature = true, cardTypes = setOf(CardType.CREATURE), subtypes = setOf("Elf"))
            val legendaryCreature = SpellPaymentContext(isCreature = true, isLegendary = true, cardTypes = setOf(CardType.CREATURE))
            val legendarySorcery = SpellPaymentContext(isLegendary = true, cardTypes = setOf(CardType.SORCERY))

            test("the unbound form pays for nothing") {
                ManaRestriction.OfChosenType(creatureOnly = false).isSatisfiedBy(elfSpell) shouldBe false
                ManaRestriction.OfChosenType(creatureOnly = true).isSatisfiedBy(elfSpell) shouldBe false
            }

            test("LegendarySpellsOnly(creatureOnly) admits legendary creature spells only") {
                ManaRestriction.LegendarySpellsOnly(creatureOnly = true).isSatisfiedBy(legendaryCreature) shouldBe true
                ManaRestriction.LegendarySpellsOnly(creatureOnly = true).isSatisfiedBy(legendarySorcery) shouldBe false
                ManaRestriction.LegendarySpellsOnly(creatureOnly = true).isSatisfiedBy(elfSpell) shouldBe false
                ManaRestriction.LegendarySpellsOnly().isSatisfiedBy(legendarySorcery) shouldBe true
                ManaRestriction.LegendarySpellsOnly(creatureOnly = true).description shouldBe
                    "Spend this mana only to cast legendary creature spells"
            }
        }

        context("Cavern of Souls end to end") {
            test("tapped mana is bound to the chosen type and carries the uncounterable rider") {
                val (game, cavern) = cavernGame("Elf")
                game.execute(
                    ActivateAbility(game.player1Id, cavern, cavernManaAbilityId, manaColorChoice = Color.GREEN)
                ).error shouldBe null

                val entry = game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.restrictedMana.single()
                entry.restriction shouldBe ManaRestriction.SubtypeSpellsOrAbilitiesOnly("Elf", creatureOnly = true)
                entry.riders shouldBe setOf(ManaSpellRider.MakesSpellUncounterable())

                game.cast(testGoblin.name, PaymentStrategy.FromPool).error shouldNotBe null
                game.cast(testElf.name, PaymentStrategy.FromPool).error shouldBe null
                val elfOnStack = game.state.stack.single()
                game.state.getEntity(elfOnStack)!!.has<CantBeCounteredComponent>() shouldBe true
            }

            test("auto-pay taps it for a creature of the chosen type, but not for another creature") {
                val (game, _) = cavernGame("Elf")
                game.cast(testGoblin.name, PaymentStrategy.AutoPay).error shouldNotBe null
                game.cast(testElf.name, PaymentStrategy.AutoPay).error shouldBe null
                game.state.getEntity(game.state.stack.single())!!.has<CantBeCounteredComponent>() shouldBe true
            }

            test("with no type chosen its mana pays for nothing") {
                val (game, cavern) = cavernGame(null)
                game.execute(
                    ActivateAbility(game.player1Id, cavern, cavernManaAbilityId, manaColorChoice = Color.GREEN)
                ).error shouldBe null
                game.cast(testElf.name, PaymentStrategy.FromPool).error shouldNotBe null
            }
        }
    }
}
