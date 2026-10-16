package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.dsl.CollectionSlot
import io.kotest.matchers.shouldBe

/**
 * Mill as an observable event (CR 701.17): "whenever a player mills a nonland card" (once per
 * card), "whenever one or more nonland cards are milled" (once per mill instruction, CR 603.2c),
 * and the milled-this-turn graveyard predicate. Only an instruction to mill counts — surveil and
 * other top-of-library moves to the graveyard don't.
 */
class MillTriggerScenarioTest : ScenarioTestBase() {
    private val perCard = card("Mill Per-Card Watcher") {
        manaCost = "{0}"; typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.anyPlayer.mills(GameObjectFilter.Nonland)
            effect = Effects.GainLife(1)
        }
    }
    private val opponentOnly = card("Mill Opponent Watcher") {
        manaCost = "{0}"; typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.anOpponent.mills(GameObjectFilter.Nonland)
            effect = Effects.GainLife(1)
        }
    }
    private val batch = card("Mill Batch Watcher") {
        manaCost = "{0}"; typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.anyPlayer.mills(GameObjectFilter.Nonland, batch = true)
            effect = Effects.GainLife(DynamicAmounts.distinctEntitiesIn(CollectionSlot.TriggerCaptured))
        }
    }
    private val graveyardWatcher = card("Mill Graveyard Watcher") {
        manaCost = "{0}"; typeLine = "Creature — Insect"; power = 1; toughness = 1
        triggeredAbility {
            trigger = Triggers.anOpponent.mills(GameObjectFilter.Nonland)
            triggerZone = Zone.GRAVEYARD
            effect = Effects.GainLife(1)
        }
    }
    // The trigger's captured batch sizes its target count when the trigger goes on the stack
    // (CR 603.3d applying 601.2c) — "up to X target creatures, where X is the number of nonland
    // cards milled this way" and the "exactly X" shape.
    private val batchUpToX = card("Mill Batch Up-To-X Targeter") {
        manaCost = "{0}"; typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.anyPlayer.mills(GameObjectFilter.Nonland, batch = true)
            targets(TargetFilter.Creature, optional = true,
                dynamicMaxCount = DynamicAmounts.distinctEntitiesIn(CollectionSlot.TriggerCaptured))
            effect = Effects.ForEachTarget(Effects.Tap(com.wingedsheep.sdk.scripting.targets.EffectTarget.ContextTarget(0)))
        }
    }
    private val batchExactlyX = card("Mill Batch Exactly-X Targeter") {
        manaCost = "{0}"; typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.anyPlayer.mills(GameObjectFilter.Nonland, batch = true)
            targets(TargetFilter.Creature,
                exactly = DynamicAmounts.distinctEntitiesIn(CollectionSlot.TriggerCaptured))
            effect = Effects.ForEachTarget(Effects.Tap(com.wingedsheep.sdk.scripting.targets.EffectTarget.ContextTarget(0)))
        }
    }
    private val millTwo = card("Mill Two Probe") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.Pipeline { mill(2, Player.Each) } }
    }
    private val millOneTwice = card("Mill Twice Probe") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.Pipeline { mill(1); mill(1) } }
    }
    private val millSelfThree = card("Mill Self Probe") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.Pipeline { mill(3) } }
    }
    private val surveil = card("Surveil Probe") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.Surveil(1) }
    }
    private val putTop = card("Put Top Probe") {
        manaCost = "{0}"; typeLine = "Instant"
        spell {
            effect = Effects.Pipeline {
                val top = gather(CardSource.TopOfLibrary(1, Player.You))
                move(top, CardDestination.ToZone(Zone.GRAVEYARD, Player.You))
            }
        }
    }
    private val millCost = card("Mill Cost Probe") {
        manaCost = "{0}"; typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Mill(1)
            effect = Effects.DrawCards(0)
        }
    }

    private val damageMiller = card("Mill Damage Replacer") {
        manaCost = "{0}"; typeLine = "Creature — Vampire"; power = 1; toughness = 1
        replacementEffect(
            com.wingedsheep.sdk.scripting.ReplaceDamageWithCounters(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                appliesTo = com.wingedsheep.sdk.scripting.EventPattern.DamageEvent(
                    recipient = com.wingedsheep.sdk.scripting.events.Recipient.AnyPlayer,
                    source = GameObjectFilter.Any.sourceItself(),
                ),
                damagedPlayerMills = true,
            )
        )
        activatedAbility {
            cost = Costs.Free
            val player = target(Targets.Player)
            effect = Effects.DealDamage(2, player)
        }
    }
    private val payOrMill = card("Mill Pay Or Suffer Probe") {
        manaCost = "{0}"; typeLine = "Instant"
        spell {
            effect = Effects.PayOrSuffer(
                cost = Costs.pay.Atom(com.wingedsheep.sdk.scripting.costs.CostAtom.Mill(1)),
                suffer = Effects.LoseLife(5, com.wingedsheep.sdk.scripting.targets.EffectTarget.Controller),
            )
        }
    }

    private fun TestGame.zone(player: Int, zone: Zone) =
        state.getZone(ZoneKey(if (player == 1) player1Id else player2Id, zone))

    private fun TestGame.milledThisTurn(cardId: com.wingedsheep.sdk.model.EntityId): Boolean =
        services.predicateEvaluator.matches(
            state, state.projectedState, cardId, GameObjectFilter.Any.milledThisTurn(),
            com.wingedsheep.engine.handlers.PredicateContext(controllerId = player1Id)
        )

    init {
        listOf(perCard, opponentOnly, batch, batchUpToX, batchExactlyX, graveyardWatcher, millTwo, millOneTwice, millSelfThree, surveil, putTop, millCost, damageMiller, payOrMill)
            .forEach { cardRegistry.register(it) }

        test("a per-card mill trigger fires once for each nonland card milled, binding that card") {
            val g = scenario().withPlayers().withCardOnBattlefield(1, perCard.name)
                .withCardInHand(1, millSelfThree.name)
                .withCardInLibrary(1, "Grizzly Bears").withCardInLibrary(1, "Forest").withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(2, "Forest").build()
            g.castSpell(1, millSelfThree.name).error shouldBe null
            g.resolveStack()
            g.getLifeTotal(1) shouldBe 22
        }

        test("an opponent-scoped mill trigger ignores your own mill and sees the opponent's") {
            val g = scenario().withPlayers().withCardOnBattlefield(1, opponentOnly.name)
                .withCardInHand(1, millSelfThree.name).withCardInHand(1, millTwo.name)
                .withCardInLibrary(1, "Grizzly Bears").withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears").withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Grizzly Bears").withCardInLibrary(2, "Forest").withCardInLibrary(2, "Forest").build()
            g.castSpell(1, millSelfThree.name).error shouldBe null
            g.resolveStack()
            g.getLifeTotal(1) shouldBe 20
            // Each player mills two: only the opponent's one nonland card counts.
            g.castSpell(1, millTwo.name).error shouldBe null
            g.resolveStack()
            g.getLifeTotal(1) shouldBe 21
        }

        test("a batch trigger fires once per mill instruction, even when every player mills") {
            val g = scenario().withPlayers().withCardOnBattlefield(1, batch.name)
                .withCardInHand(1, millTwo.name)
                .withCardInLibrary(1, "Grizzly Bears").withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(2, "Grizzly Bears").withCardInLibrary(2, "Forest").build()
            g.castSpell(1, millTwo.name).error shouldBe null
            g.resolveStack()
            // One trigger; three nonland cards captured.
            g.getLifeTotal(1) shouldBe 23
        }

        test("a batch trigger's captured cards size its target count when targets are chosen") {
            val g = scenario().withPlayers().withCardOnBattlefield(1, batchUpToX.name)
                .withCardOnBattlefield(1, "Grizzly Bears").withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardInHand(1, millSelfThree.name)
                .withCardInLibrary(1, "Grizzly Bears").withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest").build()
            g.castSpell(1, millSelfThree.name).error shouldBe null
            g.resolveStack()
            val info = (g.state.pendingDecision as ChooseTargetsDecision).targetRequirements.single()
            info.maxTargets shouldBe 2
            info.minTargets shouldBe 0
            val creatures = listOf("Grizzly Bears", "Hill Giant", "Savannah Lions").map { g.findPermanent(it)!! }
            (g.selectTargets(creatures).error != null) shouldBe true
            g.selectTargets(creatures.take(2)).error shouldBe null
            g.resolveStack()
            creatures.map { g.state.getEntity(it)!!.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() } shouldBe
                listOf(true, true, false)
        }

        test("\"exactly X targets\" over a batch trigger's captured cards requires all X") {
            val g = scenario().withPlayers().withCardOnBattlefield(1, batchExactlyX.name)
                .withCardOnBattlefield(1, "Grizzly Bears").withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(2, "Savannah Lions")
                .withCardInHand(1, millTwo.name)
                .withCardInLibrary(1, "Grizzly Bears").withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Lightning Bolt").withCardInLibrary(2, "Forest").build()
            g.castSpell(1, millTwo.name).error shouldBe null
            g.resolveStack()
            val info = (g.state.pendingDecision as ChooseTargetsDecision).targetRequirements.single()
            info.minTargets shouldBe 2
            info.maxTargets shouldBe 2
            val bears = g.findPermanent("Grizzly Bears")!!
            val giant = g.findPermanent("Hill Giant")!!
            (g.selectTargets(listOf(bears)).error != null) shouldBe true
            g.selectTargets(listOf(bears, giant)).error shouldBe null
            g.resolveStack()
            g.state.getEntity(giant)!!.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() shouldBe true
        }

        test("two mill instructions in one resolution are two batch events") {
            val g = scenario().withPlayers().withCardOnBattlefield(1, batch.name)
                .withCardInHand(1, millOneTwice.name)
                .withCardInLibrary(1, "Grizzly Bears").withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Forest").build()
            g.castSpell(1, millOneTwice.name).error shouldBe null
            g.resolveStack()
            g.getLifeTotal(1) shouldBe 22
        }

        test("a batch trigger needs a matching card: an all-land mill does not fire it") {
            val g = scenario().withPlayers().withCardOnBattlefield(1, batch.name)
                .withCardInHand(1, millTwo.name)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Mountain").build()
            g.castSpell(1, millTwo.name).error shouldBe null
            g.resolveStack()
            g.getLifeTotal(1) shouldBe 20
        }

        test("surveil and an unworded top-card move to the graveyard are not mills") {
            val g = scenario().withPlayers().withCardOnBattlefield(1, perCard.name)
                .withCardInHand(1, surveil.name).withCardInHand(1, putTop.name)
                .withCardInLibrary(1, "Grizzly Bears").withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(2, "Forest").build()
            g.castSpell(1, surveil.name).error shouldBe null
            g.resolveStack()
            g.selectCards(g.zone(1, Zone.LIBRARY).take(1)).error shouldBe null
            g.resolveStack()
            g.castSpell(1, putTop.name).error shouldBe null
            g.resolveStack()
            g.zone(1, Zone.GRAVEYARD).count { g.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Grizzly Bears" } shouldBe 2
            g.getLifeTotal(1) shouldBe 20
            g.zone(1, Zone.GRAVEYARD).none { g.milledThisTurn(it) } shouldBe true
        }

        test("milling as a cost is a mill") {
            val g = scenario().withPlayers().withCardOnBattlefield(1, perCard.name)
                .withCardOnBattlefield(1, millCost.name)
                .withCardInLibrary(1, "Grizzly Bears").withCardInLibrary(2, "Forest").build()
            val source = g.findPermanent(millCost.name)!!
            val ability = cardRegistry.getCard(millCost.name)!!.script.activatedAbilities.first()
            g.execute(ActivateAbility(playerId = g.player1Id, sourceId = source, abilityId = ability.id)).error shouldBe null
            if (g.state.pendingDecision != null) g.answerYesNo(true).error shouldBe null
            g.resolveStack()
            g.getLifeTotal(1) shouldBe 21
            g.zone(1, Zone.GRAVEYARD).single().let { g.milledThisTurn(it) } shouldBe true
        }

        test("a pay-or-suffer mill cost is a mill") {
            val g = scenario().withPlayers().withCardOnBattlefield(1, perCard.name)
                .withCardInHand(1, payOrMill.name)
                .withCardInLibrary(1, "Grizzly Bears").withCardInLibrary(2, "Forest").build()
            g.castSpell(1, payOrMill.name).error shouldBe null
            g.resolveStack()
            g.answerYesNo(true).error shouldBe null
            g.resolveStack()
            g.getLifeTotal(1) shouldBe 21
        }

        test("damage replaced by a mill (\"that player mills that many cards\") is a mill") {
            val g = scenario().withPlayers().withCardOnBattlefield(1, perCard.name)
                .withCardOnBattlefield(1, damageMiller.name)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Grizzly Bears").withCardInLibrary(2, "Lightning Bolt").build()
            val source = g.findPermanent(damageMiller.name)!!
            val ability = cardRegistry.getCard(damageMiller.name)!!.script.activatedAbilities.first()
            g.execute(ActivateAbility(playerId = g.player1Id, sourceId = source, abilityId = ability.id,
                targets = listOf(com.wingedsheep.engine.state.components.stack.ChosenTarget.Player(g.player2Id)))).error shouldBe null
            g.resolveStack()
            g.getLifeTotal(2) shouldBe 20
            g.zone(2, Zone.GRAVEYARD).size shouldBe 2
            g.getLifeTotal(1) shouldBe 22
        }

        test("a card a replacement exiles instead is still a milled card (CR 701.17c)") {
            val g = scenario().withPlayers().withCardOnBattlefield(1, perCard.name)
                .withCardOnBattlefield(1, "Leyline of the Void")
                .withCardInHand(1, millTwo.name)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Grizzly Bears").build()
            g.castSpell(1, millTwo.name).error shouldBe null
            g.resolveStack()
            g.zone(2, Zone.EXILE).size shouldBe 1
            g.getLifeTotal(1) shouldBe 21
        }

        test("a graveyard-active mill trigger fires from its owner's graveyard") {
            val g = scenario().withPlayers().withCardInGraveyard(1, graveyardWatcher.name)
                .withCardInHand(1, millTwo.name)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Grizzly Bears").withCardInLibrary(2, "Lightning Bolt").build()
            g.castSpell(1, millTwo.name).error shouldBe null
            g.resolveStack()
            g.getLifeTotal(1) shouldBe 22
        }

        test("milled this turn holds only while the milled card stays in the graveyard, and only this turn") {
            val g = scenario().withPlayers().withCardInHand(1, millSelfThree.name)
                .withCardInLibrary(1, "Grizzly Bears").withCardInLibrary(1, "Forest").withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest").withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest").withCardInLibrary(2, "Forest").build()
            g.castSpell(1, millSelfThree.name).error shouldBe null
            g.resolveStack()
            val milled = g.zone(1, Zone.GRAVEYARD).filter { it != g.zone(1, Zone.GRAVEYARD).lastOrNull { id ->
                g.state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == millSelfThree.name } }
            milled.size shouldBe 3
            milled.all { g.milledThisTurn(it) } shouldBe true
            // The instant that resolved went to the graveyard without being milled.
            g.zone(1, Zone.GRAVEYARD).filterNot { it in milled }.none { g.milledThisTurn(it) } shouldBe true
            g.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            milled.none { g.milledThisTurn(it) } shouldBe true
        }
    }
}
