package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.chosenOpponent
import com.wingedsheep.engine.state.components.battlefield.chosenPlayer
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "As this enters, choose a player" — [EntersWithChoice]`(ChoiceType.PLAYER)` (Stuffy Doll).
 *
 * Pins:
 *  - the choice is made as the permanent enters, before it is on the battlefield — a replacement
 *    effect's choice, like every other `EntersWithChoice` (Rule 614.12a);
 *  - every player is an option, the chooser included — unlike `ChoiceType.OPPONENT`;
 *  - the answer is recorded under `ChoiceSlot.PLAYER` and read back by `Player.ChosenPlayer`, and
 *    leaves the `OPPONENT` slot empty;
 *  - it is asked on both entry paths: cast and resolved, and put onto the battlefield by an effect.
 */
class EntersWithPlayerChoiceTest : FunSpec({
    val totem = card("Test Chosen Player Totem") {
        manaCost = "{1}"; typeLine = "Artifact"
        replacementEffect(EntersWithChoice(ChoiceType.PLAYER))
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.ChosenPlayer))
        }
    }
    val reanimate = card("Test Chosen Player Unearth") {
        manaCost = "{R}"; typeLine = "Sorcery"
        spell { effect = Effects.PutOntoBattlefield(target(TargetFilter.CardInGraveyard)) }
    }
    val tap = totem.activatedAbilities.single().id

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(totem, reanimate))
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Answer the pending "Choose a player" with [chosen], asserting its shape on the way. */
    fun GameTestDriver.choosePlayer(chosen: EntityId) {
        val decision = state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe player1
        decision.prompt shouldBe "Choose a player"
        decision.options.size shouldBe state.turnOrder.size
        submitDecision(player1, OptionChosenResponse(decision.id, state.turnOrder.indexOf(chosen))).error shouldBe null
    }

    fun GameTestDriver.castTotem(): EntityId {
        val id = putCardInHand(player1, totem.name)
        giveMana(player1, Color.RED, 1)
        castSpell(player1, id).error shouldBe null
        bothPass()
        return id
    }

    test("cast: every player is offered, and the chosen opponent is recorded as the chosen player") {
        val d = driver()
        val id = d.castTotem()
        d.choosePlayer(d.player2)

        d.state.getEntity(id)!!.chosenPlayer() shouldBe d.player2
        d.state.getEntity(id)!!.chosenOpponent() shouldBe null
        d.state.getBattlefield().contains(id) shouldBe true
    }

    test("the chooser may choose themselves, and the permanent then acts on them") {
        val d = driver()
        val id = d.castTotem()
        d.choosePlayer(d.player1)
        d.state.getEntity(id)!!.chosenPlayer() shouldBe d.player1

        val before1 = d.getLifeTotal(d.player1)
        val before2 = d.getLifeTotal(d.player2)
        d.submit(ActivateAbility(d.player1, id, tap)).outcome shouldBe Outcome.Done
        d.bothPass()
        listOf(d.getLifeTotal(d.player1), d.getLifeTotal(d.player2)) shouldContainExactly listOf(before1 - 1, before2)
    }

    test("put onto the battlefield by an effect, the choice is still asked and recorded") {
        val d = driver()
        val totemCard = d.putCardInGraveyard(d.player1, totem.name)
        val spell = d.putCardInHand(d.player1, reanimate.name)
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpellWithTargets(d.player1, spell, listOf(ChosenTarget.Card(totemCard, d.player1, com.wingedsheep.sdk.core.Zone.GRAVEYARD)))
            .error shouldBe null
        d.bothPass()
        d.choosePlayer(d.player2)

        val onField = d.state.getBattlefield().single { d.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == totem.name }
        d.state.getEntity(onField)!!.chosenPlayer() shouldBe d.player2
    }
})
