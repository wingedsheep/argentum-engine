package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Feature test for the predefined **Mutagen** token (Teenage Mutant Ninja Turtles).
 *
 * "It's an artifact with '{1}, {T}, Sacrifice this token: Put a +1/+1 counter on target
 *  creature. Activate only as a sorcery.'"
 *
 * Proves the three load-bearing clauses of the token:
 *  1. Activating the ability puts a +1/+1 counter on the target creature and the cost
 *     sacrifices the token (it leaves the battlefield) and pays {1} + {T}.
 *  2. "Activate only as a sorcery" — the ability is illegal outside the controller's main
 *     phase with an empty stack (here: during upkeep), per CR 307.5.
 *  3. The [Effects.CreateMutagenToken] facade actually produces a registered "Mutagen"
 *     artifact token (noncreature) end to end.
 */
class MutagenTokenScenarioTest : FunSpec({

    val projector = StateProjector()
    val mutagenAbilityId = PredefinedTokens.Mutagen.activatedAbilities.first().id

    // Inline maker that exercises the Effects.CreateMutagenToken() facade on ETB.
    val mutagenMaker = card("Mutagen Maker") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            effect = Effects.CreateMutagenToken()
            description = "When this creature enters, create a Mutagen token."
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCards(PredefinedTokens.allTokens) // GameTestDriver doesn't auto-register tokens
        d.registerCards(listOf(mutagenMaker))
        return d
    }

    test("{1}, {T}, Sacrifice this token: put a +1/+1 counter on target creature") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val p = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mutagen = d.putPermanentOnBattlefield(p, "Mutagen")
        val bear = d.putCreatureOnBattlefield(p, "Centaur Courser") // 3/3
        d.removeSummoningSickness(bear)
        d.giveMana(p, Color.BLUE, 1)

        d.submit(
            ActivateAbility(
                playerId = p,
                sourceId = mutagen,
                abilityId = mutagenAbilityId,
                targets = listOf(ChosenTarget.Permanent(bear))
            )
        ).isSuccess shouldBe true
        d.bothPass()

        // +1/+1 counter applied: 3/3 -> 4/4.
        projector.getProjectedPower(d.state, bear) shouldBe 4
        projector.getProjectedToughness(d.state, bear) shouldBe 4

        // The token paid its own sacrifice and is gone.
        d.state.getBattlefield().contains(mutagen) shouldBe false
    }

    test("activate only as a sorcery — illegal during the controller's upkeep") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val p = d.activePlayer!!
        d.passPriorityUntil(Step.UPKEEP)

        val mutagen = d.putPermanentOnBattlefield(p, "Mutagen")
        val bear = d.putCreatureOnBattlefield(p, "Centaur Courser")
        d.removeSummoningSickness(bear)
        d.giveMana(p, Color.BLUE, 1)

        // Upkeep is not a main phase, so the sorcery-speed ability must be rejected.
        d.submitExpectFailure(
            ActivateAbility(
                playerId = p,
                sourceId = mutagen,
                abilityId = mutagenAbilityId,
                targets = listOf(ChosenTarget.Permanent(bear))
            )
        )

        // Untouched: token still present, creature still 3/3.
        d.state.getBattlefield().contains(mutagen) shouldBe true
        projector.getProjectedPower(d.state, bear) shouldBe 3
    }

    test("Effects.CreateMutagenToken() creates a registered Mutagen artifact token") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val p = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val before = mutagenTokens(d.state, p).toSet()
        val maker = d.putCardInHand(p, "Mutagen Maker")
        d.giveMana(p, Color.BLUE, 2)
        d.castSpell(p, maker)
        // Resolve the spell, then its ETB trigger (each is a separate stack object).
        var guard = 0
        while (d.stackSize > 0 && guard++ < 6) d.bothPass()

        val created = mutagenTokens(d.state, p) - before
        created.size shouldBe 1
        val card = d.state.getEntity(created.first())!!.get<CardComponent>()!!
        card.name shouldBe "Mutagen"
        card.typeLine.isArtifact shouldBe true
        card.typeLine.isCreature shouldBe false
    }
})

private fun mutagenTokens(state: GameState, player: EntityId): List<EntityId> =
    state.getBattlefield().filter {
        val e = state.getEntity(it) ?: return@filter false
        e.has<TokenComponent>() &&
            e.get<ControllerComponent>()?.playerId == player &&
            e.get<CardComponent>()?.name == "Mutagen"
    }
