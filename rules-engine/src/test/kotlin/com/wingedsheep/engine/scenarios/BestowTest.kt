package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.effects.permanent.attachments.AttachmentMover
import com.wingedsheep.engine.core.PermanentAttachedEvent
import com.wingedsheep.engine.core.PermanentUnattachedEvent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.TransformPermanent
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Bestow's casting, resolution, attachment, and copy boundaries (CR 702.103a–f). */
class BestowTest : FunSpec({
    val bestowCreature = card("Bestow Test Spirit") {
        manaCost = "{4}{W}"
        typeLine = "Enchantment Creature — Spirit"
        power = 2
        toughness = 3
        keywordAbility(KeywordAbility.bestow("{1}{W}"))
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Effects.GainLife(1)
        }
    }
    val xCreature = card("Bestow X Spirit") {
        manaCost = "{4}{W}"
        typeLine = "Enchantment Creature — Spirit"
        power = 2
        toughness = 3
        keywordAbility(KeywordAbility.bestow("{X}{W}"))
    }
    val lifeCreature = card("Bestow Life Spirit") {
        manaCost = "{W}"
        typeLine = "Enchantment Creature — Spirit"
        power = 2
        toughness = 3
        keywordAbility(KeywordAbility.bestow("{0}", Costs.additional.PayLife(3)))
    }
    val destroy = card("Bestow Test Removal") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val host = target(TargetFilter.Creature)
            effect = Effects.Move(host, Zone.GRAVEYARD)
        }
    }
    val protection = card("Bestow Test Protection") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val host = target(TargetFilter.Creature)
            effect = Effects.GrantProtectionFromColor(Color.WHITE, host)
        }
    }
    val enchantmentProtection = card("Bestow Test Enchantment Protection") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val host = target(TargetFilter.Creature)
            effect = Effects.GrantProtectionFromCardType(CardType.ENCHANTMENT, host)
        }
    }
    val spellCopy = card("Bestow Test Spell Copy") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val original = target(TargetFilter.SpellOnStack)
            effect = Effects.CopyTargetSpell(original)
        }
    }
    val permanentCopy = card("Bestow Test Permanent Copy") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val original = target(TargetFilter.Permanent)
            effect = Effects.CreateTokenCopyOfTarget(original)
        }
    }

    val landAura = card("Bestow Test Land Aura") {
        manaCost = "{0}"
        typeLine = "Enchantment — Aura"
        auraTarget = TargetObject(filter = TargetFilter.Permanent)
        staticAbility { ability = TransformPermanent(setCardTypes = setOf("LAND")) }
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(bestowCreature, xCreature, lifeCreature, destroy, protection, enchantmentProtection, spellCopy, permanentCopy, landAura))
        it.initMirrorMatch(deck = Deck.of("Plains" to 40))
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.bestow(cardId: EntityId, host: EntityId, x: Int = 0) = submit(CastSpell(
        playerId = activePlayer!!,
        cardId = cardId,
        targets = listOf(ChosenTarget.Permanent(host)),
        useAlternativeCost = true,
        alternativeCostType = AlternativeCostType.BESTOW,
        paymentStrategy = PaymentStrategy.FromPool,
        xValue = x
    ))
    fun GameTestDriver.resolveAll() {
        repeat(20) {
            val decision = state.pendingDecision
            if (decision is ChooseTargetsDecision) {
                submitDecision(decision.playerId, TargetsResponse(decision.id,
                    decision.legalTargets.mapValues { (_, targets) -> targets.take(1) })).error shouldBe null
            } else if (decision != null) autoResolveDecision()
            else if (stackSize > 0) bothPass()
            else return
        }
        error("Bestow test did not finish resolving")
    }
    fun GameTestDriver.castFree(name: String, target: EntityId) {
        val player = activePlayer!!
        castSpell(player, putCardInHand(player, name), listOf(target)).outcome shouldBe Outcome.Done
    }

    test("normal casting produces a creature without an attachment") {
        val game = driver()
        val player = game.activePlayer!!
        val spirit = game.putCardInHand(player, bestowCreature.name)
        game.giveMana(player, Color.WHITE, 5)
        game.castSpell(player, spirit).outcome shouldBe Outcome.Done
        game.resolveAll()
        val permanent = game.findPermanent(player, bestowCreature.name)!!
        game.state.projectedState.isCreature(permanent) shouldBe true
        game.state.projectedState.hasSubtype(permanent, "Aura") shouldBe false
        game.state.getEntity(permanent)?.get<AttachedToComponent>() shouldBe null
        game.getLifeTotal(player) shouldBe 21
    }

    test("bestow pays its alternative cost and resolves as a noncreature Aura") {
        val game = driver()
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spirit = game.putCardInHand(player, bestowCreature.name)
        game.giveMana(player, Color.WHITE, 2)
        game.bestow(spirit, host).outcome shouldBe Outcome.Done
        val spell = game.state.getEntity(game.state.stack.single())!!.get<CardComponent>()!!
        spell.typeLine.isCreature shouldBe false
        spell.typeLine.isAura shouldBe true
        game.resolveAll()
        val permanent = game.findPermanent(player, bestowCreature.name)!!
        game.state.projectedState.isCreature(permanent) shouldBe false
        game.state.projectedState.hasSubtype(permanent, "Aura") shouldBe true
        game.state.getEntity(permanent)?.get<AttachedToComponent>()?.targetId shouldBe host
        game.getLifeTotal(player) shouldBe 21
    }

    test("bestow requires a legal creature target at casting time") {
        val game = driver()
        val player = game.activePlayer!!
        val spirit = game.putCardInHand(player, bestowCreature.name)
        val land = game.putLandOnBattlefield(player, "Plains")
        game.giveMana(player, Color.WHITE, 2)
        game.bestow(spirit, land).error shouldNotBe null
        game.submit(CastSpell(playerId = player, cardId = spirit, useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.BESTOW, paymentStrategy = PaymentStrategy.FromPool)).error shouldNotBe null
        game.stackSize shouldBe 0
    }

    for (response in listOf(destroy.name, protection.name, enchantmentProtection.name)) {
        test("an illegal target from $response resolves bestow as a creature") {
            val game = driver()
            val player = game.activePlayer!!
            val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
            val spirit = game.putCardInHand(player, bestowCreature.name)
            game.giveMana(player, Color.WHITE, 2)
            game.bestow(spirit, host).outcome shouldBe Outcome.Done
            game.castFree(response, host)
            game.resolveAll()
            val permanent = game.findPermanent(player, bestowCreature.name)!!
            game.state.projectedState.isCreature(permanent) shouldBe true
            game.state.projectedState.hasSubtype(permanent, "Aura") shouldBe false
            game.state.getEntity(permanent)?.get<AttachedToComponent>() shouldBe null
            game.getLifeTotal(player) shouldBe 21
        }
        test("$response detaches a resolved bestowed Aura without a second entry") {
            val game = driver()
            val player = game.activePlayer!!
            val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
            val spirit = game.putCardInHand(player, bestowCreature.name)
            game.giveMana(player, Color.WHITE, 2)
            game.bestow(spirit, host).outcome shouldBe Outcome.Done
            game.resolveAll()
            val original = game.findPermanent(player, bestowCreature.name)!!
            game.castFree(response, host)
            game.resolveAll()
            game.findPermanent(player, bestowCreature.name) shouldBe original
            game.state.projectedState.isCreature(original) shouldBe true
            game.state.projectedState.hasSubtype(original, "Aura") shouldBe false
            game.state.getEntity(original)?.get<AttachedToComponent>() shouldBe null
            game.getLifeTotal(player) shouldBe 21
        }
    }

    test("X in the bestow cost uses the chosen value") {
        val game = driver()
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spirit = game.putCardInHand(player, xCreature.name)
        game.giveMana(player, Color.WHITE, 3)
        game.bestow(spirit, host, x = 3).error shouldNotBe null
        game.giveMana(player, Color.WHITE, 1)
        game.bestow(spirit, host, x = 3).outcome shouldBe Outcome.Done
        game.state.getEntity(player)?.get<ManaPoolComponent>()?.white shouldBe 0
        game.resolveAll()
        game.state.projectedState.isCreature(game.findPermanent(player, xCreature.name)!!) shouldBe false
    }

    test("nonmana bestow costs are paid only when casting bestowed") {
        val game = driver()
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spirit = game.putCardInHand(player, lifeCreature.name)
        game.bestow(spirit, host).outcome shouldBe Outcome.Done
        game.getLifeTotal(player) shouldBe 17
        game.resolveAll()
        val normal = game.putCardInHand(player, lifeCreature.name)
        game.giveMana(player, Color.WHITE, 1)
        game.castSpell(player, normal).outcome shouldBe Outcome.Done
        game.getLifeTotal(player) shouldBe 17
    }

    test("copying a bestowed spell preserves bestow and attachment") {
        val game = driver()
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spirit = game.putCardInHand(player, bestowCreature.name)
        game.giveMana(player, Color.WHITE, 2)
        game.bestow(spirit, host).outcome shouldBe Outcome.Done
        val originalSpell = game.state.stack.single()
        val copy = game.putCardInHand(player, spellCopy.name)
        game.castSpellWithTargets(player, copy, listOf(ChosenTarget.Spell(originalSpell))).outcome shouldBe Outcome.Done
        game.resolveAll()
        val copies = game.state.entities.filter { (_, components) ->
            components.get<CardComponent>()?.name == bestowCreature.name && components.get<AttachedToComponent>()?.targetId == host
        }.keys
        copies.size shouldBe 2
        copies.forEach { game.state.projectedState.isCreature(it) shouldBe false }
        game.getLifeTotal(player) shouldBe 22
    }

    test("copying a bestowed permanent copies the creature without bestow status") {
        val game = driver()
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spirit = game.putCardInHand(player, bestowCreature.name)
        game.giveMana(player, Color.WHITE, 2)
        game.bestow(spirit, host).outcome shouldBe Outcome.Done
        game.resolveAll()
        val original = game.findPermanent(player, bestowCreature.name)!!
        game.castFree(permanentCopy.name, original)
        game.resolveAll()
        val copies = game.state.entities.filter { (id, components) ->
            id != original && components.get<CardComponent>()?.name == bestowCreature.name && game.state.projectedState.isCreature(id)
        }.keys
        copies.size shouldBe 1
        game.state.getEntity(copies.single())?.get<AttachedToComponent>() shouldBe null
        game.state.projectedState.isCreature(original) shouldBe false
    }
    test("a host that stops being a creature detaches bestow without a new entry") {
        val game = driver()
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spirit = game.putCardInHand(player, bestowCreature.name)
        game.giveMana(player, Color.WHITE, 2)
        game.bestow(spirit, host).outcome shouldBe Outcome.Done
        game.resolveAll()
        val original = game.findPermanent(player, bestowCreature.name)!!
        game.castFree(landAura.name, host)
        game.resolveAll()
        game.state.projectedState.isCreature(host) shouldBe false
        game.findPermanent(player, bestowCreature.name) shouldBe original
        game.state.projectedState.isCreature(original) shouldBe true
        game.state.getEntity(original)?.get<AttachedToComponent>() shouldBe null
        game.getLifeTotal(player) shouldBe 21
    }

    for (filter in listOf(GameObjectFilter.Creature, GameObjectFilter.Enchantment)) {
        test("casting restrictions evaluate bestowed characteristics for ${filter.description}") {
            val game = driver()
            val player = game.activePlayer!!
            val restriction = card("Bestow Test Cast Restriction") {
                manaCost = "{0}"
                typeLine = "Artifact"
                staticAbility { ability = PlayersCantCastSpells(affected = Player.Each, spellFilter = filter) }
            }
            game.registerCards(listOf(restriction))
            game.putPermanentOnBattlefield(player, restriction.name)
            val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
            val spirit = game.putCardInHand(player, bestowCreature.name)
            game.giveMana(player, Color.WHITE, 5)
            game.castSpell(player, spirit).error shouldNotBe null
            val result = game.bestow(spirit, host)
            if (filter == GameObjectFilter.Creature) result.outcome shouldBe Outcome.Done
            else result.error shouldNotBe null
        }

        test("cost reductions evaluate bestowed characteristics for ${filter.description}") {
            val game = driver()
            val player = game.activePlayer!!
            val reducer = card("Bestow Test Cost Reducer") {
                manaCost = "{0}"
                typeLine = "Artifact"
                staticAbility { ability = ModifySpellCost(SpellCostTarget.YouCast(filter), CostModification.ReduceGeneric(1)) }
            }
            game.registerCards(listOf(reducer))
            game.putPermanentOnBattlefield(player, reducer.name)
            val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
            val spirit = game.putCardInHand(player, bestowCreature.name)
            game.giveMana(player, Color.WHITE, 1)
            val result = game.bestow(spirit, host)
            if (filter == GameObjectFilter.Enchantment) result.outcome shouldBe Outcome.Done
            else result.error shouldNotBe null
        }
    }

    test("a bestowed card in exile can use ordinary permission to cast for bestow") {
        val game = driver()
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spirit = game.putCardInExile(player, bestowCreature.name)
        game.replaceState(game.state.addMayPlayPermission(MayPlayPermission(
            id = EntityId.generate(), cardIds = setOf(spirit), controllerId = player,
            timestamp = game.state.timestamp
        )))
        game.giveMana(player, Color.WHITE, 2)
        game.legalActions(player).any {
            val action = it.action as? CastSpell
            action?.cardId == spirit && action.alternativeCostType == AlternativeCostType.BESTOW
        } shouldBe true
        game.bestow(spirit, host).outcome shouldBe Outcome.Done
        game.resolveAll()
        game.state.projectedState.isCreature(game.findPermanent(player, bestowCreature.name)!!) shouldBe false
    }

    test("losing enchant creature detaches bestow and restores the creature") {
        val game = driver()
        val player = game.activePlayer!!
        val blankingAura = card("Bestow Test Blanking Aura") {
            manaCost = "{0}"
            typeLine = "Enchantment — Aura"
            auraTarget = TargetObject(filter = TargetFilter.Permanent)
            staticAbility { ability = LoseAllAbilities() }
        }
        game.registerCards(listOf(blankingAura))
        val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spirit = game.putCardInHand(player, bestowCreature.name)
        game.giveMana(player, Color.WHITE, 2)
        game.bestow(spirit, host).outcome shouldBe Outcome.Done
        game.resolveAll()
        val original = game.findPermanent(player, bestowCreature.name)!!
        game.castFree(blankingAura.name, original)
        game.resolveAll()
        game.findPermanent(player, bestowCreature.name) shouldBe original
        game.state.projectedState.isCreature(original) shouldBe true
        game.state.getEntity(original)?.get<AttachedToComponent>() shouldBe null
        game.findPermanent(player, "Grizzly Bears") shouldBe host
        game.getLifeTotal(player) shouldBe 21
    }

    test("a zero toughness creature survives as an Aura then dies when bestow ends") {
        val game = driver()
        val player = game.activePlayer!!
        val zero = card("Bestow Test Zero Toughness") {
            manaCost = "{W}"
            typeLine = "Enchantment Creature — Spirit"
            power = 0
            toughness = 0
            keywordAbility(KeywordAbility.bestow("{0}"))
        }
        game.registerCards(listOf(zero))
        val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spirit = game.putCardInHand(player, zero.name)
        game.bestow(spirit, host).outcome shouldBe Outcome.Done
        game.resolveAll()
        game.findPermanent(player, zero.name) shouldNotBe null
        game.castFree(destroy.name, host)
        game.resolveAll()
        game.findPermanent(player, zero.name) shouldBe null
        game.state.getGraveyard(player).any { game.state.getEntity(it)?.get<CardComponent>()?.name == zero.name } shouldBe true
    }

    for ((label, effect) in listOf("graveyard" to Effects.CounterSpell(), "hand" to Effects.CounterSpellToHand(),
        "exile" to Effects.CounterSpellToExile())) {
        test("countering a bestowed spell to $label restores its creature characteristics") {
            val game = driver()
            val player = game.activePlayer!!
            val counter = card("Bestow Test Counter") {
                manaCost = "{0}"
                typeLine = "Instant"
                spell {
                    target(TargetFilter.SpellOnStack)
                    this.effect = effect
                }
            }
            game.registerCards(listOf(counter))
            val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
            val spirit = game.putCardInHand(player, bestowCreature.name)
            game.giveMana(player, Color.WHITE, 2)
            game.bestow(spirit, host).outcome shouldBe Outcome.Done
            val target = game.state.stack.single()
            game.castSpellWithTargets(player, game.putCardInHand(player, counter.name),
                listOf(ChosenTarget.Spell(target))).outcome shouldBe Outcome.Done
            game.resolveAll()
            game.state.getEntity(spirit)?.get<CardComponent>()?.typeLine?.isCreature shouldBe true
            game.state.getEntity(spirit)?.get<CardComponent>()?.typeLine?.isAura shouldBe false
        }
    }

    for (type in listOf(CardType.CREATURE, CardType.ENCHANTMENT)) {
        test("restricted $type mana uses the Aura characteristics for bestow and enumeration") {
            val game = driver()
            val player = game.activePlayer!!
            val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
            val spirit = game.putCardInHand(player, bestowCreature.name)
            game.giveRestrictedMana(player, Color.WHITE, 2, ManaRestriction.CardTypeSpellsOrAbilitiesOnly(type))
            game.legalActions(player).any {
                val action = it.action as? CastSpell
                action?.cardId == spirit && action.alternativeCostType == AlternativeCostType.BESTOW
            } shouldBe (type == CardType.ENCHANTMENT)
            val result = game.bestow(spirit, host)
            if (type == CardType.ENCHANTMENT) result.outcome shouldBe Outcome.Done
            else result.error shouldNotBe null
        }
    }

    test("a self graveyard permission conditioned on Aura allows only bestow") {
        val game = driver()
        val player = game.activePlayer!!
        val phoenix = card("Bestow Test Graveyard Permission") {
            manaCost = "{W}"
            typeLine = "Enchantment Creature — Spirit"
            power = 2
            toughness = 3
            keywordAbility(KeywordAbility.bestow("{W}"))
            staticAbility {
                ability = MayCastSelfFromZones(
                    zones = listOf(Zone.GRAVEYARD),
                    condition = Conditions.SourceMatches(GameObjectFilter.Enchantment.withSubtype("Aura"))
                )
            }
        }
        game.registerCards(listOf(phoenix))
        val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spirit = game.putCardInGraveyard(player, phoenix.name)
        game.giveMana(player, Color.WHITE, 1)
        val actions = game.legalActions(player).mapNotNull { it.action as? CastSpell }.filter { it.cardId == spirit }
        actions.any { it.alternativeCostType == AlternativeCostType.BESTOW } shouldBe true
        actions.any { !it.useAlternativeCost } shouldBe false
        game.castSpell(player, spirit).error shouldNotBe null
        game.bestow(spirit, host).outcome shouldBe Outcome.Done
        game.resolveAll()
        val permanent = game.findPermanent(player, phoenix.name)!!
        game.state.projectedState.isCreature(permanent) shouldBe false
        game.state.getEntity(permanent)?.get<AttachedToComponent>()?.targetId shouldBe host
    }

    test("bestow life cost must be affordable in both enumeration and execution") {
        val game = driver()
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spirit = game.putCardInHand(player, lifeCreature.name)
        game.setLifeTotal(player, 2)
        game.legalActions(player).any {
            val action = it.action as? CastSpell
            action?.cardId == spirit && action.alternativeCostType == AlternativeCostType.BESTOW
        } shouldBe false
        game.bestow(spirit, host).error shouldNotBe null
        game.getLifeTotal(player) shouldBe 2
        game.stackSize shouldBe 0
    }

    test("bestow cannot be combined with without paying its mana cost") {
        val game = driver()
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spirit = game.putCardInHand(player, bestowCreature.name)
        game.giveMana(player, Color.WHITE, 2)
        game.submit(CastSpell(
            playerId = player, cardId = spirit,
            targets = listOf(ChosenTarget.Permanent(host)),
            useAlternativeCost = true, alternativeCostType = AlternativeCostType.BESTOW,
            useWithoutPayingManaCost = true, paymentStrategy = PaymentStrategy.FromPool
        )).error shouldNotBe null
        game.stackSize shouldBe 0
        game.state.getEntity(spirit)?.get<CardComponent>()?.typeLine?.isCreature shouldBe true
    }

    test("an enchantment discount reduces X mana while preserving chosen X and expands maximum X") {
        val game = driver()
        val player = game.activePlayer!!
        val reducer = card("Bestow Test X Discount") {
            manaCost = "{0}"
            typeLine = "Artifact"
            staticAbility {
                ability = ModifySpellCost(SpellCostTarget.YouCast(GameObjectFilter.Enchantment), CostModification.ReduceGeneric(2))
            }
        }
        game.registerCards(listOf(reducer))
        game.putPermanentOnBattlefield(player, reducer.name)
        val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spirit = game.putCardInHand(player, xCreature.name)
        game.giveMana(player, Color.WHITE, 2)
        val offered = game.legalActions(player).single {
            val action = it.action as? CastSpell
            action?.cardId == spirit && action.alternativeCostType == AlternativeCostType.BESTOW
        }
        offered.maxAffordableX shouldBe 3
        game.bestow(spirit, host, x = 3).outcome shouldBe Outcome.Done
        game.state.getEntity(game.state.stack.single())?.get<SpellOnStackComponent>()?.xValue shouldBe 3
        game.state.getEntity(player)?.get<ManaPoolComponent>()?.white shouldBe 0
        game.resolveAll()
        game.state.projectedState.isCreature(game.findPermanent(player, xCreature.name)!!) shouldBe false
    }

    test("a bestowed permanent controlled since the prior turn can attack after detaching") {
        val game = driver()
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spirit = game.putCardInHand(player, bestowCreature.name)
        game.giveMana(player, Color.WHITE, 2)
        game.bestow(spirit, host).outcome shouldBe Outcome.Done
        game.resolveAll()
        val original = game.findPermanent(player, bestowCreature.name)!!
        game.state.getEntity(original)?.has<SummoningSicknessComponent>() shouldBe true
        game.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        game.passPriorityUntil(Step.PRECOMBAT_MAIN)
        game.activePlayer shouldBe game.getOpponent(player)
        game.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        game.passPriorityUntil(Step.PRECOMBAT_MAIN)
        game.activePlayer shouldBe player
        game.state.getEntity(original)?.has<SummoningSicknessComponent>() shouldBe false
        game.castFree(destroy.name, host)
        game.resolveAll()
        game.findPermanent(player, bestowCreature.name) shouldBe original
        game.state.projectedState.isCreature(original) shouldBe true
        game.passPriorityUntil(Step.DECLARE_ATTACKERS)
        game.declareAttackers(player, listOf(original), game.getOpponent(player)).error shouldBe null
    }

    test("moving a bestowed Aura between hosts ends bestow before it can attach again") {
        val game = driver()
        val player = game.activePlayer!!
        val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
        val otherHost = game.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spirit = game.putCardInHand(player, bestowCreature.name)
        game.giveMana(player, Color.WHITE, 2)
        game.bestow(spirit, host).outcome shouldBe Outcome.Done
        game.resolveAll()
        val original = game.findPermanent(player, bestowCreature.name)!!
        val (moved, events) = AttachmentMover.attach(game.state, original, otherHost, player)
        moved.projectedState.isCreature(original) shouldBe true
        moved.getEntity(original)?.get<AttachedToComponent>() shouldBe null
        moved.getEntity(host)?.get<AttachmentsComponent>()?.attachedIds.orEmpty().contains(original) shouldBe false
        moved.getEntity(otherHost)?.get<AttachmentsComponent>()?.attachedIds.orEmpty().contains(original) shouldBe false
        events.filterIsInstance<PermanentUnattachedEvent>().size shouldBe 1
        events.filterIsInstance<PermanentAttachedEvent>().size shouldBe 0
    }

    test("a bestowed Aura that phases in after its host leaves becomes a creature") {
        val game = driver()
        val player = game.activePlayer!!
        val phase = card("Bestow Test Phase Out") {
            manaCost = "{0}"
            typeLine = "Instant"
            spell {
                val permanent = target(TargetFilter.Permanent)
                effect = Effects.PhaseOut(permanent)
            }
        }
        game.registerCards(listOf(phase))
        val host = game.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spirit = game.putCardInHand(player, bestowCreature.name)
        game.giveMana(player, Color.WHITE, 2)
        game.bestow(spirit, host).outcome shouldBe Outcome.Done
        game.resolveAll()
        val original = game.findPermanent(player, bestowCreature.name)!!
        game.castFree(phase.name, original)
        game.resolveAll()
        game.state.getEntity(original)?.has<PhasedOutComponent>() shouldBe true
        game.castFree(destroy.name, host)
        game.resolveAll()
        game.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        game.passPriorityUntil(Step.PRECOMBAT_MAIN)
        game.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        game.passPriorityUntil(Step.PRECOMBAT_MAIN)
        game.activePlayer shouldBe player
        game.state.getEntity(original)?.has<PhasedOutComponent>() shouldBe false
        game.findPermanent(player, bestowCreature.name) shouldBe original
        game.state.projectedState.isCreature(original) shouldBe true
        game.state.getEntity(original)?.get<AttachedToComponent>() shouldBe null
        game.getLifeTotal(player) shouldBe 21
    }

})
