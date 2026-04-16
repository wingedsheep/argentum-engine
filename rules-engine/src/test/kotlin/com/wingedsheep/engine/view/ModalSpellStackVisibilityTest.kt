package com.wingedsheep.engine.view

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.BrigidsCommand
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

/**
 * Tests B1 / B2 from [`backlog/modal-cast-time-choices-plan.md`]:
 *
 * - B1: opponents must see the chosen modes and per-mode targets for a choose-N
 *   modal spell on the stack. Without this, the cast-time selection flow fails
 *   the core goal of fixing 601.2b–c opacity — opponents would still pass
 *   priority blind.
 *
 * - B2: hidden-zone targets (a card in your opponent's hand/library) must be
 *   rendered with a generic placeholder ("a card in Player 1's hand") rather
 *   than leaking the card's identity.
 *
 * Both paths go through [ClientStateTransformer.transform] which populates
 * [ClientCard.chosenModeDescriptions] and [ClientCard.perModeTargets] when
 * a [SpellOnStackComponent] carries `chosenModes`.
 */
class ModalSpellStackVisibilityTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(BrigidsCommand)
        return d
    }

    fun transformer(d: GameTestDriver): ClientStateTransformer =
        ClientStateTransformer(cardRegistry = d.cardRegistry)

    fun nameOf(d: GameTestDriver, id: EntityId): String? =
        d.state.getEntity(id)?.get<CardComponent>()?.name

    test("B1 — opponent sees chosen mode descriptions and per-mode target groups on the stack") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 20, "Plains" to 20))
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Same board as A1: P1 owns Centaur Courser, P2 owns Goblin Guide.
        d.putCreatureOnBattlefield(p1, "Centaur Courser")
        d.putCreatureOnBattlefield(p2, "Goblin Guide")

        d.giveMana(p1, Color.WHITE, 1)
        d.giveMana(p1, Color.GREEN, 1)
        d.giveColorlessMana(p1, 1)
        val command = d.putCardInHand(p1, "Brigid's Command")

        val centaur = d.getCreatures(p1).first { nameOf(d, it) == "Centaur Courser" }
        val goblin = d.getCreatures(p2).first { nameOf(d, it) == "Goblin Guide" }

        d.submit(
            CastSpell(
                playerId = p1,
                cardId = command,
                targets = listOf(
                    ChosenTarget.Permanent(centaur),
                    ChosenTarget.Permanent(centaur),
                    ChosenTarget.Permanent(goblin)
                ),
                chosenModes = listOf(2, 3),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Permanent(centaur)),
                    listOf(ChosenTarget.Permanent(centaur), ChosenTarget.Permanent(goblin))
                )
            )
        ).isSuccess shouldBe true

        // Transform from the opponent's viewpoint — they must see everything the caster does
        // for the modes/targets, so they can knowingly decide whether to counter.
        val view = transformer(d).transform(d.state, viewingPlayerId = p2)
        val stackSpellId = d.state.stack.first()
        val card = view.cards[stackSpellId]
        card.shouldNotBeNull()

        card.chosenModeDescriptions.size shouldBe 2
        // Mode 2 is +3/+3 until end of turn; mode 3 is "fights". Runtime descriptions don't
        // have a single canonical string, so assert on substrings that must be present.
        card.chosenModeDescriptions[0].lowercase() shouldContain "+3"
        card.chosenModeDescriptions[1].lowercase() shouldContain "fight"

        card.perModeTargets.size shouldBe 2
        card.perModeTargets[0].modeIndex shouldBe 2
        card.perModeTargets[0].targets shouldBe listOf(ClientChosenTarget.Permanent(centaur))
        card.perModeTargets[0].targetNames shouldBe listOf("Centaur Courser")

        card.perModeTargets[1].modeIndex shouldBe 3
        card.perModeTargets[1].targets shouldBe listOf(
            ClientChosenTarget.Permanent(centaur),
            ClientChosenTarget.Permanent(goblin)
        )
        card.perModeTargets[1].targetNames shouldBe listOf("Centaur Courser", "Goblin Guide")
    }

    test("B1 — caster sees the same chosen mode and target breakdown as the opponent") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 20, "Plains" to 20))
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(p1, "Centaur Courser")
        d.putCreatureOnBattlefield(p2, "Goblin Guide")
        d.giveMana(p1, Color.WHITE, 1)
        d.giveMana(p1, Color.GREEN, 1)
        d.giveColorlessMana(p1, 1)
        val command = d.putCardInHand(p1, "Brigid's Command")
        val centaur = d.getCreatures(p1).first { nameOf(d, it) == "Centaur Courser" }
        val goblin = d.getCreatures(p2).first { nameOf(d, it) == "Goblin Guide" }
        d.submit(
            CastSpell(
                playerId = p1,
                cardId = command,
                targets = listOf(
                    ChosenTarget.Permanent(centaur),
                    ChosenTarget.Permanent(centaur),
                    ChosenTarget.Permanent(goblin)
                ),
                chosenModes = listOf(2, 3),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Permanent(centaur)),
                    listOf(ChosenTarget.Permanent(centaur), ChosenTarget.Permanent(goblin))
                )
            )
        )

        val casterView = transformer(d).transform(d.state, viewingPlayerId = p1)
        val opponentView = transformer(d).transform(d.state, viewingPlayerId = p2)
        val stackSpellId = d.state.stack.first()

        casterView.cards[stackSpellId]?.chosenModeDescriptions shouldBe
            opponentView.cards[stackSpellId]?.chosenModeDescriptions
        casterView.cards[stackSpellId]?.perModeTargets shouldBe
            opponentView.cards[stackSpellId]?.perModeTargets
    }

    test("B2 — opponent view redacts hidden-zone targets in per-mode target groups") {
        // Brigid's Command doesn't target hidden zones, but the rendering path must still be
        // correct for hypothetical future cards (e.g., Telepathy-style) that target a card in
        // an opponent's hand. We fabricate that scenario by overriding the spell's
        // modeTargetsOrdered with a ChosenTarget.Card pointing at P1's hand, then transforming
        // as P2 (the non-owner of the hidden zone).
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 20, "Plains" to 20))
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Give P1 a secret card in hand (the thing whose identity must stay hidden from P2).
        val secret = d.putCardInHand(p1, "Savannah Lions")
        d.putCreatureOnBattlefield(p1, "Centaur Courser")
        d.putCreatureOnBattlefield(p2, "Goblin Guide")

        d.giveMana(p1, Color.WHITE, 1)
        d.giveMana(p1, Color.GREEN, 1)
        d.giveColorlessMana(p1, 1)
        val command = d.putCardInHand(p1, "Brigid's Command")
        val centaur = d.getCreatures(p1).first { nameOf(d, it) == "Centaur Courser" }
        val goblin = d.getCreatures(p2).first { nameOf(d, it) == "Goblin Guide" }

        // Cast normally with modes [2, 3] (valid for Brigid's Command, which has
        // allowRepeat=false). We'll splice in a hidden-zone target afterwards.
        d.submit(
            CastSpell(
                playerId = p1,
                cardId = command,
                targets = listOf(
                    ChosenTarget.Permanent(centaur),
                    ChosenTarget.Permanent(centaur),
                    ChosenTarget.Permanent(goblin)
                ),
                chosenModes = listOf(2, 3),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Permanent(centaur)),
                    listOf(ChosenTarget.Permanent(centaur), ChosenTarget.Permanent(goblin))
                )
            )
        ).isSuccess shouldBe true

        // Now splice in a hidden-zone target on the second mode's target list. The DTO
        // transformer doesn't validate; it renders what's on the component. That gives us a
        // clean unit test of the hidden-info rendering path without needing a real
        // "target a card in hand" modal card.
        val stackSpellId = d.state.stack.first()
        val fabricated = d.state.updateEntity(stackSpellId) { container ->
            val spell = container.get<SpellOnStackComponent>()!!
            container.with(
                spell.copy(
                    modeTargetsOrdered = listOf(
                        listOf(ChosenTarget.Permanent(centaur)),
                        listOf(ChosenTarget.Card(secret, ownerId = p1, zone = Zone.HAND))
                    )
                )
            )
        }
        d.replaceState(fabricated)

        // As P1 (owner of the hand), the card identity is visible.
        val ownerView = transformer(d).transform(d.state, viewingPlayerId = p1)
        val ownerGroups = ownerView.cards[stackSpellId]?.perModeTargets
        ownerGroups.shouldNotBeNull()
        ownerGroups[1].targetNames shouldBe listOf("Savannah Lions")

        // As P2 (opponent), the card identity must be redacted — a generic "a card in X's hand"
        // string is shown while the mode description remains public.
        val opponentView = transformer(d).transform(d.state, viewingPlayerId = p2)
        val opponentGroups = opponentView.cards[stackSpellId]?.perModeTargets
        opponentGroups.shouldNotBeNull()
        opponentGroups[1].targetNames.size shouldBe 1
        val redactedName = opponentGroups[1].targetNames.single()
        redactedName shouldNotContain "Savannah Lions"
        redactedName.lowercase() shouldContain "card in"
        redactedName.lowercase() shouldContain "hand"

        // Mode descriptions are still public to the opponent — only the card identity is hidden.
        opponentView.cards[stackSpellId]?.chosenModeDescriptions?.size shouldBe 2
    }

    test("B1 — non-modal spell on stack has no perModeTargets or chosenModeDescriptions") {
        // Regression: the per-mode fields are opt-in. A plain (non-modal) spell must not
        // surface empty-but-present ClientPerModeTargetGroups, since the web client uses
        // .length to decide whether to render the new bulleted mode block.
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 20, "Mountain" to 20))
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(p2, "Goblin Guide")
        d.giveMana(p1, Color.RED, 1)
        val bolt = d.putCardInHand(p1, "Lightning Bolt")
        val goblin = d.getCreatures(p2).first()
        d.castSpell(p1, bolt, listOf(goblin))

        val view = transformer(d).transform(d.state, viewingPlayerId = p2)
        val stackSpellId = d.state.stack.first()
        val card = view.cards[stackSpellId]
        card.shouldNotBeNull()
        card.chosenModeDescriptions shouldBe emptyList()
        card.perModeTargets shouldBe emptyList()
    }
})
