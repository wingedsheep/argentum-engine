package com.wingedsheep.engine.mechanics.sba

import com.wingedsheep.engine.mechanics.sba.zone.PhantomCardCopiesCheck
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for [PhantomCardCopiesCheck] (Rule 707.10a — a copy of a card in any zone other
 * than the stack or the battlefield ceases to exist).
 */
class PhantomCardCopiesCheckTest : FunSpec({

    val p1 = EntityId.generate()

    fun card(name: String) = CardComponent(
        cardDefinitionId = name,
        name = name,
        manaCost = ManaCost.parse("{1}"),
        typeLine = TypeLine.parse("Instant"),
    )

    fun baseState(): GameState = GameState()
        .withEntity(p1, ComponentContainer.of(PlayerComponent("P1", 20)))
        .copy(turnOrder = listOf(p1))

    fun stackCopy(originalSnapshot: CardComponent? = null) = ComponentContainer.of(
        card("Phantom Bolt"),
        CopyOfComponent("Phantom Bolt", "Phantom Bolt", originalCardComponent = originalSnapshot),
    )

    test("removes a stack-style card copy left in exile") {
        val copyId = EntityId.generate()
        val state = baseState()
            .withEntity(copyId, stackCopy())
            .addToZone(ZoneKey(p1, Zone.EXILE), copyId)

        val result = PhantomCardCopiesCheck().check(state)

        result.newState.getEntity(copyId) shouldBe null
        result.newState.getZone(ZoneKey(p1, Zone.EXILE)).contains(copyId) shouldBe false
    }

    test("removes a stack-style card copy left in the graveyard") {
        val copyId = EntityId.generate()
        val state = baseState()
            .withEntity(copyId, stackCopy())
            .addToZone(ZoneKey(p1, Zone.GRAVEYARD), copyId)

        PhantomCardCopiesCheck().check(state).newState.getEntity(copyId) shouldBe null
    }

    test("spares a copy on the battlefield (Rule 707.10a only sweeps off-stack/off-battlefield)") {
        val copyId = EntityId.generate()
        val state = baseState()
            .withEntity(copyId, stackCopy())
            .addToZone(ZoneKey(p1, Zone.BATTLEFIELD), copyId)

        PhantomCardCopiesCheck().check(state).newState.getEntity(copyId) shouldBe state.getEntity(copyId)
    }

    test("spares a Clone-style copy in exile (it carries a pre-copy snapshot)") {
        val copyId = EntityId.generate()
        val state = baseState()
            .withEntity(copyId, stackCopy(originalSnapshot = card("Clone")))
            .addToZone(ZoneKey(p1, Zone.EXILE), copyId)

        PhantomCardCopiesCheck().check(state).newState.getEntity(copyId) shouldBe state.getEntity(copyId)
    }

    test("spares a token in exile (left to the 704.5s token check)") {
        val tokenId = EntityId.generate()
        val state = baseState()
            .withEntity(tokenId, stackCopy().with(TokenComponent))
            .addToZone(ZoneKey(p1, Zone.EXILE), tokenId)

        PhantomCardCopiesCheck().check(state).newState.getEntity(tokenId) shouldBe state.getEntity(tokenId)
    }

    test("leaves an ordinary (non-copy) card in the graveyard untouched") {
        val cardId = EntityId.generate()
        val state = baseState()
            .withEntity(cardId, ComponentContainer.of(card("Real Bolt")))
            .addToZone(ZoneKey(p1, Zone.GRAVEYARD), cardId)

        PhantomCardCopiesCheck().check(state).newState.getEntity(cardId) shouldBe state.getEntity(cardId)
    }
})
