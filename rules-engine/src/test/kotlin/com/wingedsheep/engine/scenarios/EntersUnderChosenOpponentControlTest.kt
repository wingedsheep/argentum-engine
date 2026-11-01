package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * "This enters under the control of an opponent of your choice" —
 * [EntersWithChoice.chosenOpponentControls] (Captive Audience, Xantcha, Sleeper Agent).
 *
 * The rules it pins:
 *  - CR 110.2: a permanent's controller is by default the player under whose control it entered —
 *    here the chosen opponent, with no control-changing effect in play (layer 2 stays empty);
 *  - CR 614.12a: the choice is part of the replacement and is made before it enters, by the player
 *    who would otherwise control it (the caster, or the owner an effect returns it for);
 *  - CR 603.3a: its own "when this enters" trigger is controlled by the player controlling it as it
 *    entered — the opponent;
 *  - it arrives summoning sick for the opponent, and its owner is still the caster (CR 108.3).
 */
class EntersUnderChosenOpponentControlTest : FunSpec({
    val hostage = card("Test Hostage Ogre") {
        manaCost = "{R}"; typeLine = "Creature — Ogre"; power = 3; toughness = 3
        replacementEffect(EntersWithChoice(ChoiceType.OPPONENT, chosenOpponentControls = true))
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.DrawCards(1)
        }
    }
    val reanimate = card("Test Hostage Reanimate") {
        manaCost = "{R}"; typeLine = "Sorcery"
        spell { effect = Effects.PutOntoBattlefield(target(TargetFilter.CardInGraveyard)) }
    }

    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(hostage, reanimate))
        it.initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.chooseOpponent() {
        val decision = state.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        decision.playerId shouldBe player1
        submitDecision(player1, OptionChosenResponse(decision.id, 0)).error shouldBe null
    }

    test("cast, it enters controlled by the chosen opponent, summoning sick, owned by its caster") {
        val d = driver()
        val id = d.putCardInHand(d.player1, hostage.name)
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, id).error shouldBe null
        d.bothPass()
        d.chooseOpponent()

        d.state.projectedState.getController(id) shouldBe d.player2
        d.state.getEntity(id)!!.get<CardComponent>()!!.ownerId shouldBe d.player1
        d.state.getEntity(id)!!.has<SummoningSicknessComponent>() shouldBe true
        d.state.floatingEffects.isEmpty() shouldBe true
    }

    test("its enters trigger is the opponent's: they draw") {
        val d = driver()
        val id = d.putCardInHand(d.player1, hostage.name)
        val p1Hand = d.getHandSize(d.player1) - 1
        val p2Hand = d.getHandSize(d.player2)
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, id).error shouldBe null
        d.bothPass()
        d.chooseOpponent()
        d.bothPass()

        d.getHandSize(d.player2) shouldBe p2Hand + 1
        d.getHandSize(d.player1) shouldBe p1Hand
    }

    test("returned by an effect, it enters under the opponent its owner chooses") {
        val d = driver()
        val id = d.putCardInGraveyard(d.player1, hostage.name)
        val spell = d.putCardInHand(d.player1, reanimate.name)
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpellWithTargets(d.player1, spell, listOf(ChosenTarget.Card(id, d.player1, Zone.GRAVEYARD))).error shouldBe null
        d.bothPass()
        d.chooseOpponent()

        d.state.projectedState.getController(id) shouldBe d.player2
        (id in d.state.getBattlefield(d.player2)) shouldBe true
    }

    test("the chosen controller survives a state round-trip, and the SDK shape round-trips and reads") {
        val d = driver()
        val id = d.putCardInHand(d.player1, hostage.name)
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, id).error shouldBe null
        d.bothPass()
        d.chooseOpponent()
        val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }
        json.decodeFromString<GameState>(json.encodeToString(d.state)).projectedState.getController(id) shouldBe d.player2

        val replacement = EntersWithChoice(ChoiceType.OPPONENT, chosenOpponentControls = true)
        replacement.description shouldBe "This permanent enters under the control of an opponent of your choice"
        val sdkJson = Json { encodeDefaults = true }
        sdkJson.decodeFromString<EntersWithChoice>(sdkJson.encodeToString(replacement)) shouldBe replacement
        shouldThrow<IllegalArgumentException> { EntersWithChoice(ChoiceType.COLOR, chosenOpponentControls = true) }
        shouldThrow<IllegalArgumentException> {
            EntersWithChoice(ChoiceType.OPPONENT, chooser = Player.AnOpponent, chosenOpponentControls = true)
        }
    }
})
