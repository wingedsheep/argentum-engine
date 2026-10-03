package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.state.*
import com.wingedsheep.engine.state.components.player.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.emerge
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.*
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

class ForcedPlayTest : FunSpec({
    val paid = card("Forced Paid Probe") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell { effect = Effects.DrawCards(1) }
    }
    val instruction = card("Play Instruction Probe") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            effect = Effects.Pipeline {
                val chosen = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Any.named(paid.name)))
                run(Effects.ForcePlay(chosen))
                run(Effects.GainLife(2))
            }
        }
    }
    val modal = card("Forced Modal Probe") {
        manaCost = "{G}"
        typeLine = "Sorcery"
        spell { effect = Effects.Modal(listOf(Mode.noTarget(Effects.GainLife(2)), Mode.noTarget(Effects.GainLife(4)))) }
    }
    val kicked = card("Forced Kicker Probe") {
        manaCost = "{G}"
        typeLine = "Creature — Elf"
        power = 1; toughness = 1
        keywordAbility(com.wingedsheep.sdk.scripting.KeywordAbility.OptionalAdditionalCost(ManaCost.parse("{1}")))
    }
    val evoked = card("Forced Evoke Probe") {
        manaCost = "{5}{G}"
        typeLine = "Creature — Elemental"
        power = 2; toughness = 2
        evoke = "{G}"
    }
    val xspell = card("Forced X Probe") {
        manaCost = "{X}{G}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(com.wingedsheep.sdk.scripting.values.DynamicAmount.XValue) }
    }
    val faceDown = card("Forced Morph Probe") {
        manaCost = "{5}{G}"; typeLine = "Creature — Beast"; power = 3; toughness = 3; morph = "{G}"
    }
    val later = card("Later Controlled Probe") {
        manaCost = "{G}"; typeLine = "Sorcery"
        spell { effect = Effects.May(Effects.GainLife(1)) }
    }
    val foe = EffectTarget.PlayerRef(Player.AnOpponent)
    val controlInstruction = card("Controlled Play Instruction Probe") {
        manaCost = "{0}"; typeLine = "Instant"
        spell {
            effect = Effects.Pipeline {
                val chosen = gather(CardSource.FromZone(Zone.HAND, Player.AnOpponent, GameObjectFilter.Any.named(later.name)))
                run(Effects.ControlPlayerDuringResolution(foe))
                run(Effects.ForcePlay(chosen.key, foe, "played"))
                run(Effects.ControlPlayerDuringResolution(foe, EffectTarget.PipelineTarget("played")))
            }
        }
    }
    val emergeProbe = card("Forced Emerge Probe") {
        manaCost = "{8}{G}"; typeLine = "Creature — Horror"; power = 5; toughness = 5
        emerge("{4}{G}")
    }
    val costBody = card("Forced Cost Body") {
        manaCost = "{4}"; typeLine = "Creature — Beast"; power = 2; toughness = 2
    }
    val splitProbe = card("Forced Split Second Probe") {
        manaCost = "{0}"; typeLine = "Instant"; keywords(Keyword.SPLIT_SECOND)
        spell { effect = Effects.GainLife(1) }
    }
    val manaGrant = card("Forced Entry Counters Grant") {
        manaCost = "{0}"; typeLine = "Enchantment"
        staticAbility { ability = com.wingedsheep.sdk.scripting.AdditionalManaForEntryCounters(GameObjectFilter.Creature) }
    }
    fun driver() = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(paid, instruction, modal, kicked, evoked, xspell, faceDown, later, controlInstruction, emergeProbe, costBody, splitProbe, manaGrant))
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun force(d: GameTestDriver, id: EntityId, player: EntityId = d.activePlayer!!): EffectResult {
        val result = d.services.effectExecutorRegistry.execute(d.state,
            ForcePlayEffect("chosen", EffectTarget.ContextTarget(0)),
            EffectContext(sourceId = null, controllerId = d.activePlayer!!, targets = listOf(ChosenTarget.Player(player)),
                pipeline = PipelineState(storedCollections = mapOf("chosen" to listOf(id)))))
        result.error shouldBe null
        d.replaceState(result.state)
        return result
    }
    fun play(d: GameTestDriver, action: GameAction): ExecutionResult {
        val decision = d.pendingDecision as PlayCardDecision
        return d.submitDecision(decision.playerId, PlayCardResponse(decision.id, action))
    }
    val json = Json { serializersModule = engineSerializersModule; allowStructuredMapKeys = true; encodeDefaults = true }

    test("a paid sorcery is played in another player's combat with its own resources and control") {
        val d = driver(); val actor = d.activePlayer!!; val caster = d.getOpponent(actor)
        d.replaceState(d.state.copy(step = Step.BEGIN_COMBAT))
        val id = d.putCardInHand(caster, paid.name)
        d.giveMana(caster, Color.GREEN, 1)
        force(d, id, caster).pendingDecision!!.playerId shouldBe caster
        d.services.legalActionEnumerator.enumerate(d.state, caster).all { (it.action as CastSpell).cardId == id } shouldBe true
        play(d, CastSpell(caster, id)).error shouldBe null
        (id in d.state.stack) shouldBe true
        d.state.getEntity(caster)!!.get<ManaPoolComponent>()!!.total shouldBe 0
        d.state.continuationStack shouldBe emptyList()
        d.state.getEntity(id)!!.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()!!.playerId shouldBe caster
    }
    test("ordinary player actions cannot bypass the mandatory choice or cast a different card") {
        val d = driver(); val p = d.activePlayer!!
        val id = d.putCardInHand(p, paid.name); val other = d.putCardInHand(p, paid.name)
        d.giveMana(p, Color.GREEN, 2); force(d, id)
        d.submit(CastSpell(p, other)).error.isNullOrEmpty() shouldBe false
        d.submit(PassPriority(p)).error.isNullOrEmpty() shouldBe false
        val before = d.state
        play(d, CastSpell(p, other)).error.isNullOrEmpty() shouldBe false
        d.state shouldBe before
        play(d, CastSpell(p, id)).error shouldBe null
    }
    test("an unaffordable chosen spell does nothing and leaves no permission") {
        val d = driver(); val p = d.activePlayer!!; val id = d.putCardInHand(p, paid.name)
        val before = d.state
        force(d, id).pendingDecision shouldBe null
        d.state shouldBe before
    }
    test("a prohibition takes precedence over the play instruction") {
        val d = driver(); val p = d.activePlayer!!; val id = d.putCardInHand(p, paid.name)
        d.giveMana(p, Color.GREEN, 1)
        d.replaceState(d.state.updateEntity(p) { it.with(CantCastSpellsComponent()) })
        force(d, id).pendingDecision shouldBe null
        (id in d.state.getHand(p)) shouldBe true
    }
    test("a land can be played outside the main phase on its player's turn and consumes a land play") {
        val d = driver(); val p = d.activePlayer!!; val id = d.putCardInHand(p, "Forest")
        d.replaceState(d.state.copy(step = Step.BEGIN_COMBAT))
        force(d, id).pendingDecision!!.playerId shouldBe p
        play(d, PlayLand(p, id)).error shouldBe null
        (id in d.state.getBattlefield()) shouldBe true
        d.state.getEntity(p)!!.get<LandDropsComponent>()!!.remaining shouldBe 0
    }
    test("lands cannot be played on another player's turn even by an instruction") {
        val d = driver(); val p = d.getOpponent(d.activePlayer!!); val id = d.putCardInHand(p, "Forest")
        force(d, id, p).pendingDecision shouldBe null
        (id in d.state.getHand(p)) shouldBe true
    }
    test("lands cannot be played after the affected player uses their last land play") {
        val d = driver(); val p = d.activePlayer!!; val id = d.putCardInHand(p, "Forest")
        d.replaceState(d.state.updateEntity(p) { it.with(LandDropsComponent(remaining = 0)) })
        force(d, id).pendingDecision shouldBe null
    }
    test("a targeted cast validates and preserves the player's selected target") {
        val d = driver(); val p = d.activePlayer!!; val victim = d.getOpponent(p)
        val id = d.putCardInHand(p, "Shock"); d.giveMana(p, Color.RED, 1)
        force(d, id)
        play(d, CastSpell(p, id, listOf(ChosenTarget.Player(victim)))).error shouldBe null
        val resolved = d.services.stackResolver.resolveTop(d.state)
        resolved.error shouldBe null
        resolved.events.filterIsInstance<DamageDealtEvent>().single().amount shouldBe 2
    }
    test("invalid targets reject atomically and keep the instructed card choice") {
        val d = driver(); val p = d.activePlayer!!; val id = d.putCardInHand(p, "Shock")
        d.giveMana(p, Color.RED, 1); force(d, id)
        val before = d.state
        play(d, CastSpell(p, id)).error.isNullOrEmpty() shouldBe false
        d.state shouldBe before
        d.pendingDecision shouldBe before.pendingDecision
    }
    test("a later zone visit cannot use an old play decision") {
        val d = driver(); val p = d.activePlayer!!; val id = d.putCardInHand(p, paid.name)
        d.giveMana(p, Color.GREEN, 1); force(d, id)
        val moved = d.state.moveToZone(id, ZoneKey(p, Zone.HAND), ZoneKey(p, Zone.GRAVEYARD))
            .moveToZone(id, ZoneKey(p, Zone.GRAVEYARD), ZoneKey(p, Zone.HAND))
        d.replaceState(moved)
        play(d, CastSpell(p, id)).error.isNullOrEmpty() shouldBe false
        (id in d.state.getHand(p)) shouldBe true
    }
    test("paused instructions round trip and resume the enclosing resolution before the new spell") {
        val d = driver(); val p = d.activePlayer!!
        val chosen = d.putCardInHand(p, paid.name); val source = d.putCardInHand(p, instruction.name)
        d.giveMana(p, Color.GREEN, 1)
        d.castSpell(p, source).error shouldBe null
        d.bothPass()
        (d.pendingDecision is PlayCardDecision) shouldBe true
        val life = d.state.getEntity(p)!!.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()!!.life
        d.replaceState(json.decodeFromString<GameState>(json.encodeToString(d.state)))
        play(d, CastSpell(p, chosen)).error shouldBe null
        d.state.getEntity(p)!!.get<com.wingedsheep.engine.state.components.identity.LifeTotalComponent>()!!.life shouldBe life + 2
        (source in d.state.getGraveyard(p)) shouldBe true
        (chosen in d.state.stack) shouldBe true
        d.state.continuationStack shouldBe emptyList()
    }
    test("an opponent cannot submit the affected player's instruction") {
        val d = driver(); val p = d.activePlayer!!; val other = d.getOpponent(p)
        val id = d.putCardInHand(p, paid.name); d.giveMana(p, Color.GREEN, 1); force(d, id)
        val decision = d.pendingDecision!!
        d.submitDecision(other, PlayCardResponse(decision.id, CastSpell(p, id))).error.isNullOrEmpty() shouldBe false
        play(d, CastSpell(p, id)).error shouldBe null
    }
    test("an instructed play never grants an absent zone permission") {
        val d = driver(); val p = d.activePlayer!!; val id = d.putCardInExile(p, paid.name)
        d.giveMana(p, Color.GREEN, 1)
        force(d, id).pendingDecision shouldBe null
    }
    test("modal and X casting choices retain their announced values during the instruction") {
        val d = driver(); val p = d.activePlayer!!
        val id = d.putCardInHand(p, modal.name); d.giveMana(p, Color.GREEN, 1); force(d, id)
        play(d, CastSpell(p, id, chosenModes = listOf(1))).error shouldBe null
        val resolved = d.services.stackResolver.resolveTop(d.state)
        resolved.events.filterIsInstance<LifeChangedEvent>().map { it.newLife - it.oldLife }.single() shouldBe 4
        d.replaceState(resolved.state)
        val xid = d.putCardInHand(p, xspell.name); d.giveMana(p, Color.GREEN, 4); force(d, xid)
        play(d, CastSpell(p, xid, xValue = 3)).error shouldBe null
        val xr = d.services.stackResolver.resolveTop(d.state)
        xr.events.filterIsInstance<LifeChangedEvent>().map { it.newLife - it.oldLife }.single() shouldBe 3
    }
    test("optional additional costs and an affordable alternative cost remain real choices") {
        val d = driver(); val p = d.activePlayer!!
        val id = d.putCardInHand(p, kicked.name); d.giveMana(p, Color.GREEN, 2); force(d, id)
        val offers = d.services.legalActionEnumerator.enumerate(d.state, p)
        offers.any { (it.action as CastSpell).declaredCostSlot != null } shouldBe true
        val action = offers.first { (it.action as CastSpell).declaredCostSlot != null }.action
        play(d, action).error shouldBe null
        d.state.getEntity(p)!!.get<ManaPoolComponent>()!!.total shouldBe 0
        val resolved = d.services.stackResolver.resolveTop(d.state); d.replaceState(resolved.state)
        val alt = d.putCardInHand(p, evoked.name); d.giveMana(p, Color.GREEN, 1); force(d, alt)
        val alternative = d.services.legalActionEnumerator.enumerate(d.state, p)
            .first { it.affordable && (it.action as CastSpell).alternativeCostType == AlternativeCostType.EVOKE }.action
        play(d, alternative).error shouldBe null
        (alt in d.state.stack) shouldBe true
    }
    test("cancelling the mandatory instruction is rejected without spending mana") {
        val d = driver(); val p = d.activePlayer!!; val id = d.putCardInHand(p, paid.name)
        d.giveMana(p, Color.GREEN, 1); force(d, id)
        val before = d.state
        d.submitDecision(p, CancelDecisionResponse(d.pendingDecision!!.id)).error.isNullOrEmpty() shouldBe false
        d.state shouldBe before
    }

    test("a face-down casting choice waives type timing and pays its actual face-down cost") {
        val d = driver(); val p = d.getOpponent(d.activePlayer!!)
        val id = d.putCardInHand(p, faceDown.name); d.giveMana(p, Color.GREEN, 3); force(d, id, p)
        play(d, CastSpell(p, id, castFaceDown = true)).error shouldBe null
        (id in d.state.stack) shouldBe true
        d.state.getEntity(p)!!.get<ManaPoolComponent>()!!.total shouldBe 0
        d.state.getEntity(id)!!.get<com.wingedsheep.engine.state.components.stack.SpellOnStackComponent>()!!.castFaceDown shouldBe true
    }
    test("completed play publication schedules control for the new spell after the original instruction ends") {
        val d = driver(); val actor = d.activePlayer!!; val affected = d.getOpponent(actor)
        val chosen = d.putCardInHand(affected, later.name); val source = d.putCardInHand(actor, controlInstruction.name)
        d.giveMana(affected, Color.GREEN, 1)
        d.castSpell(actor, source).error shouldBe null; d.bothPass()
        d.state.actorFor(affected) shouldBe actor
        d.services.legalActionEnumerator.enumerate(d.state, actor) shouldBe emptyList()
        play(d, CastSpell(affected, chosen)).error shouldBe null
        d.state.actorFor(affected) shouldBe affected
        (source in d.state.getGraveyard(actor)) shouldBe true
        d.bothPass()
        d.pendingDecision!!.playerId shouldBe affected
        d.state.actorFor(affected) shouldBe actor
        d.submitYesNo(affected, false).error shouldBe null
        d.state.actorFor(affected) shouldBe affected
        d.state.resolutionControls shouldBe emptyList()
    }

    test("split second prevents both offering and executing an instructed cast") {
        val d = driver(); val p = d.activePlayer!!
        val lock = d.putCardInHand(p, splitProbe.name)
        d.castSpell(p, lock).error shouldBe null
        val chosen = d.putCardInHand(p, paid.name); d.giveMana(p, Color.GREEN, 1)
        val before = d.state
        force(d, chosen).pendingDecision shouldBe null
        d.state shouldBe before
        d.services.castSpellHandler.executeDuringResolution(d.state, CastSpell(p, chosen)).error shouldBe
            com.wingedsheep.engine.mechanics.SplitSecond.REJECTION
        (chosen in d.state.getHand(p)) shouldBe true
    }

    test("an instruction offers emerge when the printed cost is unaffordable") {
        val d = driver(); val p = d.getOpponent(d.activePlayer!!)
        val body = d.putCreatureOnBattlefield(p, costBody.name)
        val chosen = d.putCardInHand(p, emergeProbe.name); d.giveMana(p, Color.GREEN, 1)
        force(d, chosen, p).pendingDecision!!.playerId shouldBe p
        val offer = d.services.legalActionEnumerator.enumerate(d.state, p).single { legal ->
            (legal.action as? CastSpell)?.alternativeCostType == AlternativeCostType.EMERGE
        }
        val action = (offer.action as CastSpell).copy(additionalCostPayment =
            com.wingedsheep.sdk.scripting.AdditionalCostPayment(sacrificedPermanents = listOf(body)))
        play(d, action).error shouldBe null
        (body in d.state.getGraveyard(p)) shouldBe true
        (chosen in d.state.stack) shouldBe true
        d.state.getEntity(p)!!.get<ManaPoolComponent>()!!.total shouldBe 0
    }

    test("instructed casts retain optional entry counter mana annotations") {
        val d = driver(); val p = d.activePlayer!!
        d.putPermanentOnBattlefield(p, manaGrant.name)
        val chosen = d.putCardInHand(p, costBody.name); d.giveMana(p, Color.GREEN, 6)
        force(d, chosen)
        d.services.legalActionEnumerator.enumerate(d.state, p).single().maxAdditionalManaForCounters shouldBe 2
    }

    test("cancelling a nested modal picker returns to the mandatory instruction") {
        val d = driver(); val p = d.activePlayer!!
        val chosen = d.putCardInHand(p, modal.name); d.giveMana(p, Color.GREEN, 1)
        force(d, chosen)
        play(d, CastSpell(p, chosen)).error shouldBe null
        (d.pendingDecision is ChooseOptionDecision) shouldBe true
        d.submitDecision(p, CancelDecisionResponse(d.pendingDecision!!.id)).error shouldBe null
        (d.pendingDecision is PlayCardDecision) shouldBe true
        d.state.getEntity(p)!!.get<ManaPoolComponent>()!!.total shouldBe 1
        play(d, CastSpell(p, chosen, chosenModes = listOf(0))).error shouldBe null
        (chosen in d.state.stack) shouldBe true
        d.state.continuationStack shouldBe emptyList()
    }

    test("answering a nested modal picker completes the forced play continuation") {
        val d = driver(); val p = d.activePlayer!!
        val chosen = d.putCardInHand(p, modal.name); d.giveMana(p, Color.GREEN, 1)
        force(d, chosen)
        play(d, CastSpell(p, chosen)).error shouldBe null
        (d.pendingDecision is ChooseOptionDecision) shouldBe true
        d.submitDecision(p, OptionChosenResponse(d.pendingDecision!!.id, 0)).error shouldBe null
        (chosen in d.state.stack) shouldBe true
        d.state.continuationStack shouldBe emptyList()
    }

})
