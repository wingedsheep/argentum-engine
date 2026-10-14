package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.pip.cards.RexCyberHound
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Rex, Cyber-Hound (PIP #117) — combat damage mills the damaged player two and gives you {E}{E};
 * {E}{E} exiles a creature card from a graveyard with a brain counter at sorcery speed; Rex has
 * every activated ability of every brain-countered card in exile (`DonorCards.ALL_EXILE`).
 */
class RexCyberHoundScenarioTest : FunSpec({

    val medic = card("Rex Test Medic") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
        oracleText = "{T}: You gain 2 life."
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.GainLife(2)
        }
    }
    val medicAbility = medic.activatedAbilities[0].id
    val exileAbility = RexCyberHound.activatedAbilities[0].id

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(RexCyberHound, medic))
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.energy(): Int =
        state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0
    fun GameTestDriver.giveEnergy(n: Int) = replaceState(state.updateEntity(player1) {
        it.with(CountersComponent(mapOf(CounterType.ENERGY to n)))
    })
    fun GameTestDriver.brainCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.BRAIN) ?: 0

    test("combat damage to a player mills them two cards and gives you {E}{E}") {
        val d = driver()
        val rex = d.putPermanentOnBattlefield(d.player1, "Rex, Cyber-Hound")
        val libraryBefore = d.state.getLibrary(d.player2).size

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(rex), d.player2).error shouldBe null
        d.passPriorityUntil(Step.END)

        d.getLifeTotal(d.player2) shouldBe 18
        d.state.getLibrary(d.player2).size shouldBe libraryBefore - 2
        d.state.getGraveyard(d.player2).size shouldBe 2
        d.energy() shouldBe 2
    }

    test("{E}{E} exiles a creature card from an opponent's graveyard with a brain counter, and Rex gains its ability") {
        val d = driver()
        val rex = d.putPermanentOnBattlefield(d.player1, "Rex, Cyber-Hound")
        val theirMedic = d.putCardInGraveyard(d.player2, "Rex Test Medic")

        withClue("before the exile Rex has no such ability") {
            d.submit(ActivateAbility(d.player1, rex, medicAbility)).error shouldNotBe null
        }
        d.giveEnergy(1)
        withClue("one energy can't pay {E}{E}") {
            d.submit(
                ActivateAbility(d.player1, rex, exileAbility, targets = listOf(ChosenTarget.Card(theirMedic, d.player2, Zone.GRAVEYARD)))
            ).error shouldNotBe null
        }

        d.giveEnergy(2)
        d.submit(
            ActivateAbility(d.player1, rex, exileAbility, targets = listOf(ChosenTarget.Card(theirMedic, d.player2, Zone.GRAVEYARD)))
        ).error shouldBe null
        d.bothPass()

        d.energy() shouldBe 0
        d.state.getExile(d.player2) shouldBe listOf(theirMedic)
        d.brainCounters(theirMedic) shouldBe 1

        val lifeBefore = d.getLifeTotal(d.player1)
        d.submit(ActivateAbility(d.player1, rex, medicAbility)).error shouldBe null
        d.bothPass()
        d.getLifeTotal(d.player1) shouldBe lifeBefore + 2
        d.isTapped(rex) shouldBe true
    }

    test("the exile ability is sorcery-speed: not while a spell or ability is on the stack") {
        val d = driver()
        val rex = d.putPermanentOnBattlefield(d.player1, "Rex, Cyber-Hound")
        val first = d.putCardInGraveyard(d.player2, "Rex Test Medic")
        val second = d.putCardInGraveyard(d.player2, "Rex Test Medic")
        d.giveEnergy(4)

        d.submit(
            ActivateAbility(d.player1, rex, exileAbility, targets = listOf(ChosenTarget.Card(first, d.player2, Zone.GRAVEYARD)))
        ).error shouldBe null
        d.submit(
            ActivateAbility(d.player1, rex, exileAbility, targets = listOf(ChosenTarget.Card(second, d.player2, Zone.GRAVEYARD)))
        ).error shouldNotBe null
    }

    test("a creature card exiled without a brain counter lends Rex nothing") {
        val d = driver()
        val rex = d.putPermanentOnBattlefield(d.player1, "Rex, Cyber-Hound")
        d.putCardInExile(d.player2, "Rex Test Medic")

        d.submit(ActivateAbility(d.player1, rex, medicAbility)).error shouldNotBe null
    }
})
