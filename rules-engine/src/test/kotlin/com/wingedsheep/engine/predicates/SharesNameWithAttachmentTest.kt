package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.predicates.EnchantRestriction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `CardPredicate.SharesNameWithAttachment` — "a Curse card that doesn't have the same name as a
 * Curse attached to enchanted player" (Curse of Misfortunes) — and the player-host branch of
 * [EnchantRestriction.couldAttach] that lets such a Curse be put onto the battlefield attached to a
 * player (CR 303.4f).
 *
 * The compared pool is the host's attachments whoever controls them, filtered by the predicate's
 * filter; a face-down attachment has no name (CR 708.2a). A player host takes only an "Enchant
 * player" / "Enchant opponent" Aura, "opponent" relative to the Aura's would-be controller, and
 * never a player who has left the game.
 */
class SharesNameWithAttachmentTest : FunSpec({

    val me = EntityId.generate()
    val opponent = EntityId.generate()

    val enchantPlayerCurse = card("Test Player Curse") {
        manaCost = "{B}"
        typeLine = "Enchantment — Aura Curse"
        auraTarget = Targets.Player
    }
    val enchantOpponentCurse = card("Test Opponent Curse") {
        manaCost = "{B}"
        typeLine = "Enchantment — Aura Curse"
        auraTarget = Targets.Opponent
    }
    val enchantCreature = card("Test Creature Aura") {
        manaCost = "{W}"
        typeLine = "Enchantment — Aura"
        auraTarget = TargetObject(filter = TargetFilter.Creature)
    }
    val registry = CardRegistry().apply { register(listOf(enchantPlayerCurse, enchantOpponentCurse, enchantCreature)) }
    val evaluator = PredicateEvaluator(cardRegistry = registry)

    fun curse(name: String, owner: EntityId) = ComponentContainer()
        .with(CardComponent(
            cardDefinitionId = name,
            name = name,
            manaCost = ManaCost(emptyList()),
            typeLine = TypeLine(cardTypes = setOf(CardType.ENCHANTMENT), subtypes = setOf(Subtype("Aura"), Subtype("Curse"))),
            ownerId = owner,
        ))
        .with(OwnerComponent(owner))
        .with(ControllerComponent(owner))

    fun GameState.put(id: EntityId, container: ComponentContainer, owner: EntityId, zone: Zone) =
        withEntity(id, container).addToZone(ZoneKey(owner, zone), id)

    fun GameState.attach(auraId: EntityId, hostId: EntityId): GameState {
        val existing = getEntity(hostId)?.get<AttachmentsComponent>()?.attachedIds.orEmpty()
        return updateEntity(auraId) { it.with(AttachedToComponent(hostId)) }
            .updateEntity(hostId) { it.with(AttachmentsComponent(existing + auraId)) }
    }

    val base = GameState(turnOrder = listOf(me, opponent))
        .withEntity(me, ComponentContainer())
        .withEntity(opponent, ComponentContainer())

    // On the opponent: a Curse I control and a Curse they control, plus a non-Curse Aura.
    val myCurseOnThem = EntityId.generate()
    val theirCurseOnThemselves = EntityId.generate()
    val plainAuraOnThem = EntityId.generate()
    val inLibraryA = EntityId.generate()
    val inLibraryB = EntityId.generate()
    val inLibraryC = EntityId.generate()
    val inLibraryD = EntityId.generate()

    val state = base
        .put(myCurseOnThem, curse("Curse of Alpha", me), me, Zone.BATTLEFIELD)
        .put(theirCurseOnThemselves, curse("Curse of Beta", opponent), opponent, Zone.BATTLEFIELD)
        .put(plainAuraOnThem, curse("Gamma", me).with(CardComponent(
            cardDefinitionId = "Gamma", name = "Gamma", manaCost = ManaCost(emptyList()),
            typeLine = TypeLine(cardTypes = setOf(CardType.ENCHANTMENT), subtypes = setOf(Subtype("Aura"))), ownerId = me,
        )), me, Zone.BATTLEFIELD)
        .attach(myCurseOnThem, opponent)
        .attach(theirCurseOnThemselves, opponent)
        .attach(plainAuraOnThem, opponent)
        .put(inLibraryA, curse("Curse of Alpha", me), me, Zone.LIBRARY)
        .put(inLibraryB, curse("Curse of Beta", me), me, Zone.LIBRARY)
        .put(inLibraryC, curse("Gamma", me), me, Zone.LIBRARY)
        .put(inLibraryD, curse("Curse of Delta", me), me, Zone.LIBRARY)

    val curseFilter = GameObjectFilter.Enchantment.withSubtype("Curse")
    val onOpponent = EffectTarget.SpecificEntity(opponent)
    val notShared = curseFilter.nameNotSharedWithAttachmentOf(onOpponent, curseFilter)
    val ctx = PredicateContext(controllerId = me)

    test("excludes names of Curses attached to the host, whoever controls them") {
        evaluator.matches(state, state.projectedState, inLibraryA, notShared, ctx) shouldBe false
        evaluator.matches(state, state.projectedState, inLibraryB, notShared, ctx) shouldBe false
        evaluator.matches(state, state.projectedState, inLibraryD, notShared, ctx) shouldBe true
    }

    test("only attachments matching the filter count") {
        val sharesWithAura = GameObjectFilter(cardPredicates = listOf(CardPredicate.SharesNameWithAttachment(onOpponent, curseFilter)))
        evaluator.matches(state, state.projectedState, inLibraryC, sharesWithAura, ctx) shouldBe false
        val anyAttachment = GameObjectFilter(cardPredicates = listOf(CardPredicate.SharesNameWithAttachment(onOpponent, GameObjectFilter.Any)))
        evaluator.matches(state, state.projectedState, inLibraryC, anyAttachment, ctx) shouldBe true
    }

    test("a host with nothing attached shares no name") {
        val onMe = curseFilter.nameNotSharedWithAttachmentOf(EffectTarget.SpecificEntity(me), curseFilter)
        evaluator.matches(state, state.projectedState, inLibraryA, onMe, ctx) shouldBe true
    }

    test("a face-down attachment has no name") {
        val faceDown = state.updateEntity(myCurseOnThem) { it.with(FaceDownComponent) }
        evaluator.matches(faceDown, faceDown.projectedState, inLibraryA, notShared, ctx) shouldBe true
    }

    test("the description reads as the Oracle clause") {
        CardPredicate.SharesNameWithAttachment(EffectTarget.PlayerRef(Player.EnchantedPlayer), curseFilter).description shouldBe
            "with the same name as a ${curseFilter.description} attached to ${Player.EnchantedPlayer.description}"
    }

    context("an Aura card enchanting a player") {
        fun aura(def: String) = ComponentContainer().with(CardComponent(
            cardDefinitionId = def, name = def, manaCost = ManaCost(emptyList()),
            typeLine = TypeLine(cardTypes = setOf(CardType.ENCHANTMENT), subtypes = setOf(Subtype("Aura"))), ownerId = me,
        )).get<CardComponent>()!!
        val id = EntityId.generate()

        fun could(def: String, host: EntityId, s: GameState = state) =
            EnchantRestriction.couldAttach(s, s.projectedState, evaluator, registry, id, aura(def), host, controllerId = me)

        test("enchant player takes either player") {
            could("Test Player Curse", opponent) shouldBe true
            could("Test Player Curse", me) shouldBe true
        }

        test("enchant opponent takes only an opponent of its controller") {
            could("Test Opponent Curse", opponent) shouldBe true
            could("Test Opponent Curse", me) shouldBe false
        }

        test("an Aura that doesn't enchant players can't be attached to one") {
            could("Test Creature Aura", opponent) shouldBe false
        }

        test("a player who has left the game is no host") {
            val gone = state.updateEntity(opponent) { it.with(PlayerLostComponent(LossReason.CONCESSION)) }
            could("Test Player Curse", opponent, gone) shouldBe false
        }
    }
})
