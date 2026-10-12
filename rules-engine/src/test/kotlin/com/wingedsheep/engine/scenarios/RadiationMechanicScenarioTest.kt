package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.event.RadiationAbilities
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe

class RadiationMechanicScenarioTest : ScenarioTestBase() {
    private val clear = card("Radiation Clear Probe") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.RemoveAllCountersOfType(CounterType.RAD, EffectTarget.Controller) }
    }
    private val add = card("Radiation Add Probe") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.AddCounters(CounterType.RAD, 1, EffectTarget.Controller) }
    }
    private val protect = card("Radiation Protect Probe") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.LockLifeLoss(EffectTarget.Controller) }
    }
    private fun TestGame.rads(n: Int, player: Int = 1) {
        val id = if (player == 1) player1Id else player2Id
        state = state.updateEntity(id) { it.with(CountersComponent(mapOf(CounterType.RAD to n))) }
    }
    private fun TestGame.radCount(player: Int = 1): Int =
        state.getEntity(if (player == 1) player1Id else player2Id)?.get<CountersComponent>()?.getCount(CounterType.RAD) ?: 0
    private fun board(vararg library: String): TestGame {
        val b = scenario().withPlayers()
            .withCardInHand(1, clear.name).withCardInHand(1, add.name).withCardInHand(1, protect.name)
            .withCardInLibrary(2, "Forest")
        library.forEach { b.withCardInLibrary(1, it) }
        return b.build().also { it.state = it.state.copy(phase = Phase.BEGINNING, step = Step.DRAW) }
    }
    private fun TestGame.beginMain() { passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN) }

    init {
        listOf(clear, add, protect).forEach { cardRegistry.register(it) }
        test("radiation uses the stack before priority and mills only on resolution") {
            val g = board("Forest", "Grizzly Bears", "Mountain"); g.rads(3)
            g.beginMain()
            g.state.stack.size shouldBe 1
            g.getLifeTotal(1) shouldBe 20
            g.state.getZone(ZoneKey(g.player1Id, Zone.LIBRARY)).size shouldBe 3
            val events = g.resolveStack().flatMap { it.events }
            g.getLifeTotal(1) shouldBe 19; g.radCount() shouldBe 2
            events.filterIsInstance<LifeChangedEvent>().single().reason shouldBe LifeChangeReason.RADIATION
            events.filterIsInstance<CountersRemovedEvent>().single().amount shouldBe 1
            events.filterIsInstance<CountersRemovedEvent>().single().entityName shouldBe "Player1"
        }
        test("zero counters do not trigger") {
            val g = board("Grizzly Bears"); g.beginMain(); g.state.stack.size shouldBe 0
        }
        test("counters gained after the beginning do not create a retroactive trigger") {
            val g = board("Grizzly Bears"); g.beginMain(); g.rads(1)
            g.passPriority(); g.state.stack.size shouldBe 0
        }
        test("removing counters in response makes the intervening if fail on resolution") {
            val g = board("Grizzly Bears"); g.rads(1); g.beginMain()
            g.castSpell(1, clear.name).error shouldBe null; g.resolveStack()
            g.radCount() shouldBe 0; g.getLifeTotal(1) shouldBe 20
            g.state.getZone(ZoneKey(g.player1Id, Zone.LIBRARY)).size shouldBe 1
        }
        test("the mill amount reads counters on resolution rather than at triggering") {
            val g = board("Grizzly Bears", "Grizzly Bears"); g.rads(1); g.beginMain()
            g.castSpell(1, add.name).error shouldBe null; g.resolveStack()
            g.getLifeTotal(1) shouldBe 18; g.radCount() shouldBe 0
            g.state.getZone(ZoneKey(g.player1Id, Zone.GRAVEYARD)).size shouldBe 3 // two bears plus the instant
        }
        test("lands neither lose life nor remove counters") {
            val g = board("Forest", "Mountain"); g.rads(2); g.beginMain(); g.resolveStack()
            g.getLifeTotal(1) shouldBe 20; g.radCount() shouldBe 2
        }
        test("empty and short libraries mill as many as possible without causing a failed draw") {
            for (library in listOf(emptyArray<String>(), arrayOf("Grizzly Bears"))) {
                val g = board(*library); g.rads(5); g.beginMain(); g.resolveStack()
                g.state.gameOver shouldBe false; g.radCount() shouldBe 5 - library.size
                g.getLifeTotal(1) shouldBe 20 - library.size
            }
        }
        test("counter removal still happens when life loss is prohibited") {
            val g = board("Grizzly Bears"); g.rads(1); g.beginMain()
            g.castSpell(1, protect.name).error shouldBe null; g.resolveStack()
            g.getLifeTotal(1) shouldBe 20; g.radCount() shouldBe 0
        }
        test("nonactive players keep their radiation during this player's main phase") {
            val g = board("Grizzly Bears"); g.rads(1, 2); g.beginMain()
            g.state.stack.size shouldBe 0; g.radCount(2) shouldBe 1
        }
        test("a graveyard to exile replacement still counts the identifiable nonland milled") {
            val g = scenario().withPlayers().withCardOnBattlefield(2, "Leyline of the Void")
                .withCardInLibrary(1, "Grizzly Bears").withCardInLibrary(2, "Forest").build()
            g.state = g.state.copy(phase = Phase.BEGINNING, step = Step.DRAW)
            g.rads(1); g.beginMain(); g.resolveStack()
            g.state.getZone(ZoneKey(g.player1Id, Zone.EXILE)).size shouldBe 1
            g.getLifeTotal(1) shouldBe 19; g.radCount() shouldBe 0
        }
        test("shared team turns give each player a separate trigger they control") {
            val g = board("Forest"); g.rads(1); g.rads(2, 2)
            g.state = g.state.copy(format = Format.TwoHeadedGiant())
            for (player in listOf(g.player1Id, g.player2Id)) {
                g.state = g.state.updateEntity(player) {
                    it.with(com.wingedsheep.engine.state.components.identity.TeamComponent(0))
                }
            }
            val triggers = services.triggerDetector.detectPhaseStepTriggers(g.state, Step.PRECOMBAT_MAIN, g.player1Id)
                .filter { it.ability.id == RadiationAbilities.ID }
            triggers.map { it.controllerId }.toSet() shouldBe setOf(g.player1Id, g.player2Id)
            triggers.all { it.sourceId == it.controllerId } shouldBe true
            g.rads(0, 2)
            services.triggerDetector.detectPhaseStepTriggers(g.state, Step.PRECOMBAT_MAIN, g.player1Id)
                .count { it.ability.id == RadiationAbilities.ID } shouldBe 1
        }
        test("player radiation is public in both players' transformed state") {
            val g = board("Forest"); g.rads(4)
            for (viewer in listOf(g.player1Id, g.player2Id)) {
                stateTransformer.transform(g.state, viewer).players.single { it.playerId == g.player1Id }.radCounters shouldBe 4
            }
        }
        test("proliferate can increase rad counters and counters persist into postcombat") {
            val probe = card("Radiation Proliferate Probe") {
                manaCost = "{0}"
                typeLine = "Sorcery"
                spell { effect = Effects.Proliferate() }
            }
            cardRegistry.register(probe)
            val g = scenario().withPlayers().withCardInHand(1, probe.name)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest").build()
            g.rads(2); g.castSpell(1, probe.name).error shouldBe null
            g.resolveStack()
            g.selectCards(listOf(g.player1Id)).error shouldBe null
            g.resolveStack(); g.radCount() shouldBe 3
            g.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            g.radCount() shouldBe 3
        }
        test("a mill amount replacement counts all actual nonlands and clamps rad removal") {
            val amplifier = card("Radiation Mill Amplifier") {
                manaCost = "{0}"
                typeLine = "Enchantment"
                replacementEffect(com.wingedsheep.sdk.scripting.ModifyKeywordActionAmount(
                    com.wingedsheep.sdk.scripting.EventPattern.MillEvent(), modifier = 2))
            }
            cardRegistry.register(amplifier)
            val g = scenario().withPlayers().withCardOnBattlefield(1, amplifier.name)
                .withCardInLibrary(1, "Grizzly Bears").withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears").withCardInLibrary(2, "Forest").build()
            g.state = g.state.copy(phase = Phase.BEGINNING, step = Step.DRAW)
            g.rads(1); g.beginMain(); g.resolveStack()
            g.getLifeTotal(1) shouldBe 17; g.radCount() shouldBe 0
        }
        test("an opponent copying radiation still mills the player the original trigger named") {
            val copier = card("Radiation Copy Probe") {
                manaCost = "{0}"
                typeLine = "Instant"
                spell {
                    val ability = target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.ActivatedOrTriggeredAbilityOnStack)
                    effect = Effects.CopyTargetTriggeredAbility(ability)
                }
            }
            cardRegistry.register(copier)
            val g = scenario().withPlayers().withCardInHand(2, copier.name)
                .withCardInLibrary(1, "Grizzly Bears").withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Grizzly Bears").withCardInLibrary(2, "Forest").build()
            g.state = g.state.copy(phase = Phase.BEGINNING, step = Step.DRAW)
            g.rads(2); g.beginMain()
            val radiation = g.state.stack.single()
            g.passPriority()
            val copy = g.findCardsInHand(2, copier.name).single()
            g.execute(CastSpell(g.player2Id, copy, targets = listOf(
                com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell(radiation)
            ))).error shouldBe null
            g.resolveStack()
            g.getLifeTotal(1) shouldBe 18; g.getLifeTotal(2) shouldBe 20
            g.radCount() shouldBe 0
            g.state.getZone(ZoneKey(g.player2Id, Zone.LIBRARY)).size shouldBe 1
        }
        test("copied radiation never reveals cards redirected into the original player's hand") {
            val copier = card("Radiation Hidden Mill Copy Probe") {
                manaCost = "{0}"
                typeLine = "Instant"
                spell {
                    val ability = target(com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.ActivatedOrTriggeredAbilityOnStack)
                    effect = Effects.CopyTargetTriggeredAbility(ability)
                }
            }
            val redirect = card("Radiation Hidden Mill Redirect Probe") {
                manaCost = "{0}"
                typeLine = "Enchantment"
                replacementEffect(com.wingedsheep.sdk.scripting.RedirectZoneChange(
                    newDestination = Zone.HAND,
                    appliesTo = com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent(
                        from = Zone.LIBRARY, to = Zone.GRAVEYARD
                    )
                ))
            }
            listOf(copier, redirect).forEach { cardRegistry.register(it) }
            val g = scenario().withPlayers().withCardInHand(2, copier.name)
                .withCardOnBattlefield(1, redirect.name)
                .withCardInLibrary(1, "Grizzly Bears").withCardInLibrary(2, "Forest").build()
            val bear = g.state.getZone(ZoneKey(g.player1Id, Zone.LIBRARY)).single()
            g.state = g.state.copy(phase = Phase.BEGINNING, step = Step.DRAW)
            g.rads(1); g.beginMain()
            val radiation = g.state.stack.single()
            g.passPriority()
            val copy = g.findCardsInHand(2, copier.name).single()
            g.execute(CastSpell(g.player2Id, copy, targets = listOf(
                com.wingedsheep.engine.state.components.stack.ChosenTarget.Spell(radiation)
            ))).error shouldBe null
            g.resolveStack()
            g.state.getZone(ZoneKey(g.player1Id, Zone.HAND)).contains(bear) shouldBe true
            g.getLifeTotal(1) shouldBe 20; g.radCount() shouldBe 1
            stateTransformer.transform(g.state, g.player1Id).cards.containsKey(bear) shouldBe true
            stateTransformer.transform(g.state, g.player2Id).cards.containsKey(bear) shouldBe false
        }
        test("postcombat main never triggers radiation") {
            val g = board("Forest"); g.rads(1)
            services.triggerDetector.detectPhaseStepTriggers(g.state, Step.POSTCOMBAT_MAIN, g.player1Id)
                .any { it.ability.id == RadiationAbilities.ID } shouldBe false
        }
    }
}
