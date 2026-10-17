package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.atq.cards.MishrasFactory
import com.wingedsheep.mtg.sets.definitions.neo.cards.MindlinkMech
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class MindlinkMechScenarioTest : FunSpec({
    val legend = card("Test Crew Legend") {
        manaCost = "{1}{U}"
        typeLine = "Legendary Creature — Human Pilot"
        power = 2
        toughness = 2
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(MindlinkMech, MishrasFactory, legend))
        initMirrorMatch(Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.name(id: EntityId) =
        state.getEntity(id)?.get<CardComponent>()?.name

    test("becomes a 4/3 flying Vehicle artifact copy of the creature that crewed it, until end of turn") {
        val game = driver()
        val mech = game.putPermanentOnBattlefield(game.player1, "Mindlink Mech")
        val bears = game.putCreatureOnBattlefield(game.player1, "Grizzly Bears")

        game.submitSuccess(CrewVehicle(game.player1, mech, listOf(bears)))
        game.bothPass()

        game.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        game.submitTargetSelection(game.player1, listOf(bears)).error shouldBe null
        game.bothPass()

        val projected = game.state.projectedState
        game.name(mech) shouldBe "Grizzly Bears"
        projected.getPower(mech) shouldBe 4
        projected.getToughness(mech) shouldBe 3
        projected.hasKeyword(mech, Keyword.FLYING) shouldBe true
        projected.isCreature(mech) shouldBe true // the crew effect still applies on top of the copy
        projected.hasType(mech, "ARTIFACT") shouldBe true
        projected.hasSubtype(mech, "Vehicle") shouldBe true
        projected.hasSubtype(mech, "Bear") shouldBe true
        game.name(bears) shouldBe "Grizzly Bears" // the crewer is untouched

        game.passPriorityUntil(Step.UPKEEP)
        game.name(mech) shouldBe "Mindlink Mech"
        game.state.projectedState.isCreature(mech) shouldBe false
        game.state.projectedState.hasSubtype(mech, "Bear") shouldBe false
    }

    test("only a nonlegendary creature that crewed it this turn is a legal target") {
        val game = driver()
        val mech = game.putPermanentOnBattlefield(game.player1, "Mindlink Mech")
        val bears = game.putCreatureOnBattlefield(game.player1, "Grizzly Bears")
        val pilot = game.putCreatureOnBattlefield(game.player1, "Test Crew Legend")
        val bystander = game.putCreatureOnBattlefield(game.player1, "Grizzly Bears")

        game.submitSuccess(CrewVehicle(game.player1, mech, listOf(bears, pilot)))
        game.bothPass()

        val legal = game.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            .legalTargets.values.flatten()
        legal shouldContainExactly listOf(bears)
        legal shouldNotContain pilot
        legal shouldNotContain bystander
    }

    test("triggers only on the first crew each turn") {
        val game = driver()
        val mech = game.putPermanentOnBattlefield(game.player1, "Mindlink Mech")
        val first = game.putCreatureOnBattlefield(game.player1, "Grizzly Bears")
        val second = game.putCreatureOnBattlefield(game.player1, "Grizzly Bears")

        game.submitSuccess(CrewVehicle(game.player1, mech, listOf(first)))
        game.bothPass()
        game.submitTargetSelection(game.player1, listOf(first)).error shouldBe null

        // Crew it again while the trigger waits on the stack: the Mech is crewed a second time,
        // but "for the first time each turn" means no second trigger.
        game.submitSuccess(CrewVehicle(game.player1, mech, listOf(second)))
        game.bothPass()
        game.pendingDecision shouldBe null
        game.stackSize shouldBe 1

        game.bothPass()
        game.name(mech) shouldBe "Grizzly Bears"
        game.stackSize shouldBe 0
    }

    test("copying a crewing animated land leaves it a 0/0 that dies (ruling)") {
        val game = driver()
        val mech = game.putPermanentOnBattlefield(game.player1, "Mindlink Mech")
        val factory = game.putLandOnBattlefield(game.player1, "Mishra's Factory")
        game.giveColorlessMana(game.player1, 1)
        game.submitSuccess(
            ActivateAbility(game.player1, factory, MishrasFactory.activatedAbilities[1].id)
        )
        game.bothPass()
        game.state.projectedState.isCreature(factory) shouldBe true

        game.submitSuccess(CrewVehicle(game.player1, mech, listOf(factory)))
        game.bothPass()
        game.submitTargetSelection(game.player1, listOf(factory)).error shouldBe null
        game.bothPass()

        game.state.getZone(game.player1, Zone.GRAVEYARD) shouldContain mech
    }
})
