package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.gtc.cards.IllusionistsBracers
import com.wingedsheep.mtg.sets.definitions.lea.cards.ProdigalSorcerer
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Illusionist's Bracers (GTC #231) — "Whenever an ability of equipped creature is activated, if it
 * isn't a mana ability, copy that ability. You may choose new targets for the copy. Equip {3}"
 *
 * The attached activation trigger has to hand the copy the ability on the stack (not the creature),
 * so the happy path asserts two pings landed; a mana ability and an unequipped creature's ability
 * must not trigger it.
 */
class IllusionistsBracersScenarioTest : FunSpec({

    val pingAbility = ProdigalSorcerer.activatedAbilities.single().id
    val equipAbility = IllusionistsBracers.activatedAbilities.single().id
    val elvesMana = TestCards.LlanowarElves.activatedAbilities.single().id

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + ProdigalSorcerer + IllusionistsBracers)
        d.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.equip(bracers: EntityId, creature: EntityId) {
        giveColorlessMana(player1, 3)
        submit(
            ActivateAbility(player1, bracers, equipAbility, targets = listOf(ChosenTarget.Permanent(creature)))
        ).outcome shouldBe Outcome.Done
        bothPass()
        state.getEntity(bracers)?.get<AttachedToComponent>()?.targetId shouldBe creature
    }

    test("an equipped creature's activated ability is copied — the opponent takes the ping twice") {
        val d = driver()
        val bracers = d.putPermanentOnBattlefield(d.player1, "Illusionist's Bracers")
        val tim = d.putCreatureOnBattlefield(d.player1, "Prodigal Sorcerer")
        d.removeSummoningSickness(tim)
        d.equip(bracers, tim)

        d.submit(
            ActivateAbility(d.player1, tim, pingAbility, targets = listOf(ChosenTarget.Player(d.player2)))
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        // The copy offers new targets (CR 707.10c); keep the opponent.
        if (d.pendingDecision != null) {
            d.submitTargetSelection(d.player1, listOf(d.player2))
        }
        d.bothPass()
        d.bothPass()

        withClue("the copy and the original each deal 1 — 20 - 2 = 18") {
            d.getLifeTotal(d.player2) shouldBe 18
        }
    }

    test("an unequipped creature's ability is not copied") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Illusionist's Bracers")
        val tim = d.putCreatureOnBattlefield(d.player1, "Prodigal Sorcerer")
        d.removeSummoningSickness(tim)

        d.submit(
            ActivateAbility(d.player1, tim, pingAbility, targets = listOf(ChosenTarget.Player(d.player2)))
        ).outcome shouldBe Outcome.Done
        d.bothPass()

        withClue("only the original ping resolves") {
            d.getLifeTotal(d.player2) shouldBe 19
        }
    }

    test("a mana ability of the equipped creature never triggers it") {
        val d = driver()
        val bracers = d.putPermanentOnBattlefield(d.player1, "Illusionist's Bracers")
        val elves = d.putCreatureOnBattlefield(d.player1, "Llanowar Elves")
        d.removeSummoningSickness(elves)
        d.equip(bracers, elves)

        d.submit(ActivateAbility(d.player1, elves, elvesMana)).outcome shouldBe Outcome.Done

        withClue("a mana ability doesn't use the stack, so nothing is copied") {
            d.pendingDecision shouldBe null
            d.state.stack.isEmpty() shouldBe true
        }
    }
})
