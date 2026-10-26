package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.state.components.battlefield.ChoiceValue
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.khm.cards.KolvoriGodOfKinship
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Kolvori, God of Kinship // The Ringhart Crest.
 *
 * The Crest's {G}: "Spend this mana only to cast a creature spell of the chosen type or a legendary
 * creature spell." Each test floats one Crest mana and tries to cast a one-mana spell with it, with no
 * other mana source in play. Kolvori's front: +4/+2 and vigilance with three or more legendary
 * creatures (itself included).
 */
class KolvoriGodOfKinshipScenarioTest : ScenarioTestBase() {

    private val crestManaAbilityId = KolvoriGodOfKinship.backFace!!.activatedAbilities.single().id

    private val elf = card("Kolvori Test Elf") {
        manaCost = "{1}"
        typeLine = "Creature — Elf"
        power = 1
        toughness = 1
    }
    private val goblin = card("Kolvori Test Goblin") {
        manaCost = "{1}"
        typeLine = "Creature — Goblin"
        power = 1
        toughness = 1
    }
    private val legend = card("Kolvori Test Legend") {
        manaCost = "{1}"
        typeLine = "Legendary Creature — Human"
        power = 1
        toughness = 1
    }
    private val otherLegend = card("Kolvori Test Other Legend") {
        manaCost = "{1}"
        typeLine = "Legendary Creature — Dwarf"
        power = 1
        toughness = 1
    }
    private val legendarySorcery = card("Kolvori Test Legendary Sorcery") {
        manaCost = "{1}"
        typeLine = "Legendary Sorcery"
        spell { effect = Effects.GainLife(1) }
    }

    private fun crestBoard(chosenType: String?): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "The Ringhart Crest")
            .withCardInHand(1, elf.name)
            .withCardInHand(1, goblin.name)
            .withCardInHand(1, legend.name)
            .withCardInHand(1, legendarySorcery.name)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        if (chosenType != null) {
            val crest = game.findPermanent("The Ringhart Crest")!!
            game.state = game.state.updateEntity(crest) {
                it.with(CastChoicesComponent(chosen = mapOf(ChoiceSlot.CREATURE_TYPE to ChoiceValue.TextChoice(chosenType))))
            }
        }
        return game
    }

    private fun TestGame.floatCrestMana() {
        val crest = findPermanent("The Ringhart Crest")!!
        execute(ActivateAbility(player1Id, crest, crestManaAbilityId)).error shouldBe null
    }

    private fun TestGame.castFromHand(name: String, strategy: PaymentStrategy = PaymentStrategy.FromPool) =
        execute(
            CastSpell(
                player1Id,
                state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == name },
                paymentStrategy = strategy,
            )
        )

    init {
        cardRegistry.register(KolvoriGodOfKinship)
        cardRegistry.register(listOf(elf, goblin, legend, otherLegend, legendarySorcery))

        context("The Ringhart Crest") {
            test("its mana pays for a creature spell of the chosen type") {
                val game = crestBoard("Elf")
                game.floatCrestMana()
                game.castFromHand(elf.name).error shouldBe null
            }

            test("its mana pays for a legendary creature spell not of the chosen type") {
                val game = crestBoard("Elf")
                game.floatCrestMana()
                game.castFromHand(legend.name).error shouldBe null
            }

            test("its mana can't pay for another creature or a legendary non-creature spell") {
                val game = crestBoard("Elf")
                game.floatCrestMana()
                withClue("non-legendary Goblin") { game.castFromHand(goblin.name).error shouldNotBe null }
                withClue("legendary sorcery") { game.castFromHand(legendarySorcery.name).error shouldNotBe null }
            }

            test("auto-pay taps it for a chosen-type creature but not for another creature") {
                val game = crestBoard("Elf")
                game.castFromHand(goblin.name, PaymentStrategy.AutoPay).error shouldNotBe null
                game.castFromHand(elf.name, PaymentStrategy.AutoPay).error shouldBe null
            }

            test("with no creature type chosen its mana still pays for a legendary creature spell (ruling)") {
                val game = crestBoard(null)
                game.floatCrestMana()
                game.castFromHand(elf.name).error shouldNotBe null
                game.castFromHand(legend.name).error shouldBe null
            }

            test("it adds green mana") {
                val game = crestBoard("Elf")
                game.floatCrestMana()
                val pool = game.state.getEntity(game.player1Id)!!
                    .get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()!!
                pool.restrictedMana.single().color shouldBe Color.GREEN
            }
        }

        context("Kolvori, God of Kinship") {
            fun kolvoriBoard(vararg otherCreatures: String): Pair<TestGame, EntityId> {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Kolvori, God of Kinship")
                for (name in otherCreatures) builder = builder.withCardOnBattlefield(1, name)
                val game = builder
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                return game to game.findPermanent("Kolvori, God of Kinship")!!
            }

            test("is a 2/4 without vigilance with only two legendary creatures") {
                val (game, kolvori) = kolvoriBoard(legend.name, elf.name)
                val projected = game.state.projectedState
                projected.getPower(kolvori) shouldBe 2
                projected.getToughness(kolvori) shouldBe 4
                projected.hasKeyword(kolvori, Keyword.VIGILANCE) shouldBe false
            }

            test("gets +4/+2 and vigilance with three legendary creatures, itself included") {
                val (game, kolvori) = kolvoriBoard(legend.name, otherLegend.name)
                val projected = game.state.projectedState
                projected.getPower(kolvori) shouldBe 6
                projected.getToughness(kolvori) shouldBe 6
                projected.hasKeyword(kolvori, Keyword.VIGILANCE) shouldBe true
            }
        }
    }
}
