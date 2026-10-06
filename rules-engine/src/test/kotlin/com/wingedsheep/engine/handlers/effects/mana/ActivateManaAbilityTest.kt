package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Engine coverage for `ActivateManaAbilityEffect` — "<its controller> activates a mana ability of
 * <permanent>", an instructed activation during resolution, resolving at once (CR 605.3b). Driven through a Drain
 * Power-shaped test spell: for each land the target player controls, that player activates one of
 * its mana abilities they can activate; the activation is theirs, so every choice is theirs.
 */
class ActivateManaAbilityTest : FunSpec({

    val drain = card("Test Drain") {
        manaCost = "{0}"
        typeLine = "Sorcery"
        spell {
            val player = target(Targets.Player)
            effect = Effects.Pipeline {
                val lands = gather(CardSource.ControlledPermanents(Player.TargetPlayer, GameObjectFilter.Land))
                run(Effects.ForEachInCollection(lands, Effects.ActivateManaAbility(EffectTarget.IterationEntity)))
            } then Effects.LoseUnspentMana(player, transferTo = EffectTarget.Controller)
        }
    }
    val painLand = card("Test Pain Land") {
        typeLine = "Land"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddColorlessMana(1)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
        activatedAbility {
            cost = Costs.Composite(Costs.Tap, Costs.PayLife(1))
            effect = Effects.AddMana(Color.WHITE)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }
    val prismLand = card("Test Prism Land") {
        typeLine = "Land"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddAnyColorMana()
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }
    val barrenLand = card("Test Barren Land") { typeLine = "Land" }
    val rock = card("Test Mana Rock") {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddColorlessMana(1)
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(drain, painLand, prismLand, barrenLand, rock))
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.pool(player: EntityId): ManaPoolComponent =
        state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    fun GameTestDriver.cast(caster: EntityId, target: EntityId) {
        val spell = putCardInHand(caster, "Test Drain")
        castSpellWithTargets(caster, spell, listOf(ChosenTarget.Player(target))).error shouldBe null
        bothPass()
    }

    test("each untapped land is activated; tapped lands, lands without mana abilities and nonlands are not") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        val forests = List(2) { d.putLandOnBattlefield(opponent, "Forest") }
        val tapped = d.putLandOnBattlefield(opponent, "Mountain")
        d.tapPermanent(tapped)
        val barren = d.putPermanentOnBattlefield(opponent, "Test Barren Land")
        val artifact = d.putPermanentOnBattlefield(opponent, "Test Mana Rock")

        d.cast(you, opponent)

        d.pendingDecision shouldBe null
        forests.all { d.isTapped(it) } shouldBe true
        withClue("a land that can't pay its {T} cost, a land with no mana ability and a nonland stay put") {
            d.isTapped(barren) shouldBe false
            d.isTapped(artifact) shouldBe false
        }
        withClue("the activated mana moved to the caster, and only that mana") {
            d.pool(you).green shouldBe 2
            d.pool(you).total shouldBe 2
            d.pool(opponent).total shouldBe 0
        }
    }

    test("mana already floating in the target's pool is lost and added too") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putLandOnBattlefield(opponent, "Swamp")
        d.giveMana(opponent, Color.RED, 2)

        d.cast(you, opponent)

        d.pool(you).black shouldBe 1
        d.pool(you).red shouldBe 2
        d.pool(opponent).total shouldBe 0
    }

    test("a land with several mana abilities asks its controller which to activate") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        val land = d.putPermanentOnBattlefield(opponent, "Test Pain Land")

        d.cast(you, opponent)

        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe opponent
        decision.options.size shouldBe 2
        val painIndex = decision.options.indexOfFirst { "W" in it }
        withClue("options: ${decision.options}") { (painIndex >= 0) shouldBe true }
        d.submitDecision(opponent, OptionChosenResponse(decision.id, painIndex)).error shouldBe null

        d.pendingDecision shouldBe null
        d.isTapped(land) shouldBe true
        d.getLifeTotal(opponent) shouldBe 19
        d.pool(you).white shouldBe 1
        d.pool(opponent).total shouldBe 0
    }

    test("a colour choice in the activated ability belongs to the land's controller") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putPermanentOnBattlefield(opponent, "Test Prism Land")
        d.putLandOnBattlefield(opponent, "Forest")

        d.cast(you, opponent)

        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseColorDecision>()
        decision.playerId shouldBe opponent
        d.submitDecision(opponent, ColorChosenResponse(decision.id, Color.RED)).error shouldBe null

        withClue("the iteration resumed after the pause: the Forest was activated too, then the mana moved") {
            d.pendingDecision shouldBe null
            d.pool(you).red shouldBe 1
            d.pool(you).green shouldBe 1
            d.pool(opponent).total shouldBe 0
        }
    }

    test("targeting yourself activates your own lands and keeps the mana") {
        val d = driver()
        val you = d.activePlayer!!
        val islands = List(2) { d.putLandOnBattlefield(you, "Island") }

        d.cast(you, you)

        islands.all { d.isTapped(it) } shouldBe true
        d.pool(you).blue shouldBe 2
    }

    test("priority stays with the resolving spell's controller") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.putLandOnBattlefield(opponent, "Forest")

        d.cast(you, opponent)

        d.state.priorityPlayerId shouldBe you
    }
})
