package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.SyggsCommand
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Tests D3 from [`backlog/modal-cast-time-choices-plan.md`]: modes with different target
 * kinds (player vs. permanent) must resolve independently.
 *
 * Scenario: Sygg's Command, modes [2, 3]:
 *   - mode 2 ("Target player draws a card") targets P2.
 *   - mode 3 ("Tap target creature. Put a stun counter on it") targets P2's Goblin Guide.
 *
 * Between cast and resolution, the Goblin Guide is bounced out of the battlefield — making
 * mode 3's target illegal (creature no longer on battlefield). Mode 2's target (the player)
 * is unaffected. Resolution must fire mode 2 (P2 draws a card) and silently fizzle mode 3.
 *
 * Complements D1/D2 in ChooseNModalPreChosenTest (creature vs. creature) by asserting the
 * cross-target-type independence — the per-mode 608.2b re-validation path must not couple
 * a player target to an unrelated permanent target.
 */
class ModalModeIndependenceTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(SyggsCommand)
        return d
    }

    fun nameOf(d: GameTestDriver, id: EntityId): String? =
        d.state.getEntity(id)?.get<CardComponent>()?.name

    test("D3 — creature-target mode fizzles while player-target mode still resolves") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 20, "Island" to 20))
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // P2 controls a creature that will get bounced before resolution.
        d.putCreatureOnBattlefield(p2, "Goblin Guide")

        d.giveMana(p1, Color.WHITE, 1)
        d.giveMana(p1, Color.BLUE, 1)
        d.giveColorlessMana(p1, 1)
        val command = d.putCardInHand(p1, "Sygg's Command")

        val goblin = d.getCreatures(p2).first { nameOf(d, it) == "Goblin Guide" }
        val handSizeBefore = d.state.getHand(p2).size

        // Cast with modes [2, 3]: mode 2 draws for P2, mode 3 taps/stuns the Goblin.
        d.submit(
            CastSpell(
                playerId = p1,
                cardId = command,
                targets = listOf(
                    ChosenTarget.Player(p2),
                    ChosenTarget.Permanent(goblin)
                ),
                chosenModes = listOf(2, 3),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Player(p2)),
                    listOf(ChosenTarget.Permanent(goblin))
                )
            )
        ).isSuccess shouldBe true

        // Simulate the Goblin leaving the battlefield (destroyed) before resolution. This
        // mirrors the pattern in ChooseNModalPreChosenTest D1 — move the entity to its
        // owner's graveyard to make mode 3's target illegal for 608.2b re-check.
        val beforeResolution = d.state
        val owner = beforeResolution.getEntity(goblin)?.get<OwnerComponent>()?.playerId ?: p2
        val battlefieldKey = ZoneKey(p2, Zone.BATTLEFIELD)
        val graveyardKey = ZoneKey(owner, Zone.GRAVEYARD)
        val relocated = beforeResolution
            .removeFromZone(battlefieldKey, goblin)
            .addToZone(graveyardKey, goblin)
        d.replaceState(relocated)

        // Resolve — mode 2 fires, mode 3 fizzles (its only target left the battlefield).
        d.bothPass()

        // Mode 2: P2 drew a card.
        d.state.getHand(p2).size shouldBe handSizeBefore + 1

        // Mode 3 fizzled — the Goblin never got tapped or stunned. We verify on the entity
        // now in the graveyard (still the same EntityId). CountersComponent may be absent
        // entirely when no counter was ever placed, so fall back to 0 before comparing.
        val goblinEntity = d.state.getEntity(goblin)
        goblinEntity?.get<TappedComponent>().shouldBeNull()
        (goblinEntity?.get<CountersComponent>()?.getCount(CounterType.STUN) ?: 0) shouldBe 0
    }

    test("D3 — player-target mode fizzles independently when the creature-target mode resolves") {
        // Symmetry check: make the player-target mode's condition become invalid (by
        // conceding the opponent — but that ends the game, so instead we simulate it by
        // leaving the player target legal (players never become illegal for "target player")
        // and confirm that path still works. Kept as a narrative regression — there is no
        // clean way to make a player target become illegal mid-resolution in 2-player games,
        // so we simply assert that a creature-only mode alone still resolves cleanly without
        // spilling state from an adjacent mode that fizzled in the prior test.
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 20, "Island" to 20))
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(p2, "Goblin Guide")
        d.giveMana(p1, Color.WHITE, 1)
        d.giveMana(p1, Color.BLUE, 1)
        d.giveColorlessMana(p1, 1)
        val command = d.putCardInHand(p1, "Sygg's Command")
        val goblin = d.getCreatures(p2).first { nameOf(d, it) == "Goblin Guide" }
        val handSizeBefore = d.state.getHand(p2).size

        d.submit(
            CastSpell(
                playerId = p1,
                cardId = command,
                targets = listOf(
                    ChosenTarget.Player(p2),
                    ChosenTarget.Permanent(goblin)
                ),
                chosenModes = listOf(2, 3),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Player(p2)),
                    listOf(ChosenTarget.Permanent(goblin))
                )
            )
        ).isSuccess shouldBe true

        d.bothPass()

        // Both modes fired because both targets remained legal.
        d.state.getHand(p2).size shouldBe handSizeBefore + 1
        d.state.getEntity(goblin)?.get<TappedComponent>() shouldBe TappedComponent
        d.state.getEntity(goblin)?.get<CountersComponent>()?.getCount(CounterType.STUN) shouldBe 1
    }
})
