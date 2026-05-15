package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Kapow!
 *
 * Card reference:
 * - Kapow! ({2}{G}): Sorcery
 *   "Put a +1/+1 counter on target creature you control. It fights target creature
 *    an opponent controls."
 *
 * Partial-resolution rulings follow the Savage Stomp template (CR 608.2b):
 *   1. If the creature you control is illegal, the counter is not placed and no fight occurs.
 *   2. If only the opponent's creature is illegal, the counter is still placed; no fight occurs.
 *   3. The spell is only countered on resolution if BOTH targets are illegal.
 */
class KapowScenarioTest : ScenarioTestBase() {

    init {
        context("Kapow! counter then fight") {

            test("puts +1/+1 counter on ally then ally and foe fight and both are destroyed") {
                // GIVEN active player has Kapow! in hand, controls Grizzly Bears (2/2),
                // opponent controls Hill Giant (3/3), {2}{G} mana available
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Kapow!")
                    .withCardOnBattlefield(1, "Grizzly Bears")  // ally: 2/2 → 3/3 after counter
                    .withCardOnBattlefield(2, "Hill Giant")     // foe: 3/3
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val allyId = game.findPermanent("Grizzly Bears")!!
                val foeId = game.findPermanent("Hill Giant")!!

                // WHEN cast Kapow! targeting ally (Grizzly Bears) and foe (Hill Giant)
                val playerId = game.player1Id
                val cardId = game.state.getHand(playerId).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Kapow!"
                }!!

                val castResult = game.execute(
                    CastSpell(
                        playerId = playerId,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Permanent(allyId),
                            ChosenTarget.Permanent(foeId)
                        )
                    )
                )
                withClue("Casting Kapow! should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve: counter applied then fight; SBAs destroy both creatures.
                // Without the counter Grizzly Bears (2/2) would deal only 2 damage to Hill Giant
                // (3/3) — not lethal. Hill Giant dying proves the counter boosted Grizzly Bears to 3/3.
                game.resolveStack()

                // THEN both creatures are destroyed (each dealt 3 damage to the other)
                withClue("Grizzly Bears (3/3 after counter) should be destroyed by Hill Giant dealing 3 damage") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("Hill Giant should be destroyed by Grizzly Bears dealing 3 damage (counter applied)") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
                withClue("Kapow! should be in its owner's graveyard") {
                    game.isInGraveyard(1, "Kapow!") shouldBe true
                }
            }
        }

        context("Kapow! partial-resolution: only the fight target is illegal") {

            test("counter is still placed on ally when opponent gives foe hexproof in response (no fight)") {
                // GIVEN active player has Kapow! in hand, controls Grizzly Bears (2/2);
                // opponent controls Hill Giant (3/3) and has Blossoming Defense in hand with {G}
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Kapow!")
                    .withCardOnBattlefield(1, "Grizzly Bears")  // ally: 2/2
                    .withCardOnBattlefield(2, "Hill Giant")     // foe: 3/3
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInHand(2, "Blossoming Defense")
                    .withLandsOnBattlefield(2, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val allyId = game.findPermanent("Grizzly Bears")!!
                val foeId = game.findPermanent("Hill Giant")!!

                // WHEN active player casts Kapow! targeting Grizzly Bears and Hill Giant
                val playerId = game.player1Id
                val cardId = game.state.getHand(playerId).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Kapow!"
                }!!

                val castResult = game.execute(
                    CastSpell(
                        playerId = playerId,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Permanent(allyId),
                            ChosenTarget.Permanent(foeId)
                        )
                    )
                )
                withClue("Casting Kapow! should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Opponent responds: pass priority back to them, then cast Blossoming Defense
                // giving Hill Giant hexproof — illegal target for Kapow!'s fight on resolution
                game.passPriority()
                val defenseResult = game.castSpell(2, "Blossoming Defense", foeId)
                withClue("Blossoming Defense should be castable in response: ${defenseResult.error}") {
                    defenseResult.error shouldBe null
                }

                // Stack resolves LIFO: Blossoming Defense first (Hill Giant gets hexproof + 2/+2 EOT),
                // then Kapow! resolves. Ally is still a legal target → counter applies.
                // Foe is illegal → no fight occurs. The spell is NOT countered (one legal target remains).
                game.resolveStack()

                // THEN counter on Ally, no fight, both creatures survive
                withClue("Grizzly Bears should still be on the battlefield (no fight occurred)") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                val allyCounters = game.state.getEntity(allyId)?.get<CountersComponent>()
                val counterCount = allyCounters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
                withClue("Grizzly Bears should have one +1/+1 counter (CR 608.2b: legal target #0 still resolves)") {
                    counterCount shouldBe 1
                }
                withClue("Hill Giant should still be on the battlefield (no fight occurred)") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
                withClue("Kapow! should be in its owner's graveyard") {
                    game.isInGraveyard(1, "Kapow!") shouldBe true
                }
            }
        }

        context("Kapow! partial-resolution: only the counter target is illegal") {

            test("no counter and no fight when ally dies to Shock in response (foe survives)") {
                // GIVEN active player has Kapow! in hand, controls Grizzly Bears (2/2);
                // opponent controls Hill Giant (3/3) and has Shock in hand with {R}
                val game = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Kapow!")
                    .withCardOnBattlefield(1, "Grizzly Bears")  // ally: 2/2
                    .withCardOnBattlefield(2, "Hill Giant")     // foe: 3/3
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val allyId = game.findPermanent("Grizzly Bears")!!
                val foeId = game.findPermanent("Hill Giant")!!

                val playerId = game.player1Id
                val cardId = game.state.getHand(playerId).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Kapow!"
                }!!

                val castResult = game.execute(
                    CastSpell(
                        playerId = playerId,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Permanent(allyId),
                            ChosenTarget.Permanent(foeId)
                        )
                    )
                )
                withClue("Casting Kapow! should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Opponent responds: pass priority, then Shock Grizzly Bears (2 dmg to a 2/2 = lethal).
                // After Shock resolves and SBAs run, Bears is in the graveyard — target #0 illegal.
                game.passPriority()
                val shockResult = game.castSpell(2, "Shock", allyId)
                withClue("Shock should be castable in response: ${shockResult.error}") {
                    shockResult.error shouldBe null
                }

                // Stack resolves LIFO: Shock kills Bears, then Kapow! resolves with target #0 illegal
                // (and target #1 still legal). Spell is NOT countered — but neither effect applies.
                game.resolveStack()

                // THEN Bears in graveyard (from Shock), no counter, no fight, Foe untouched
                withClue("Grizzly Bears should be in caster's graveyard (killed by Shock)") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("Hill Giant should still be on the battlefield (no fight occurred)") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
                withClue("Kapow! should be in its owner's graveyard") {
                    game.isInGraveyard(1, "Kapow!") shouldBe true
                }
            }
        }
    }
}
