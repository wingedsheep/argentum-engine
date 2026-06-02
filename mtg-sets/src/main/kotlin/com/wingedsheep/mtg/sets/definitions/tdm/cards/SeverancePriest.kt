package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Severance Priest
 * {W}{B}{G}
 * Creature — Djinn Cleric
 * 3/3
 *
 * Deathtouch
 * When this creature enters, target opponent reveals their hand. You may choose a
 * nonland card from it. If you do, exile that card.
 * When this creature leaves the battlefield, the exiled card's owner creates an X/X
 * white Spirit creature token, where X is the mana value of the exiled card.
 *
 * Implemented as a pure pipeline chain — no bespoke executor:
 *  - ETB: reveal opponent's hand → gather it → controller picks up to one nonland →
 *    move it to exile linked to this creature (`MoveCollectionEffect.linkToSource`),
 *    so the chosen card's id lives in the priest's `LinkedExileComponent`.
 *  - LTB: re-gather the linked-exiled card (it stays in exile — Severance Priest never
 *    returns it) and create the Spirit. The token's count is gated on the collection
 *    size (`VariableReference("…_count")`, 0 when nothing was exiled), its P/T reads the
 *    exiled card's mana value (`StoredCardManaValue`), and its controller resolves to the
 *    card's owner (`ControllerOfPipelineTarget` falls back to `ownerId` for an exile-zone
 *    card with no controller).
 */
val SeverancePriest = card("Severance Priest") {
    manaCost = "{W}{B}{G}"
    colorIdentity = "WBG"
    typeLine = "Creature — Djinn Cleric"
    power = 3
    toughness = 3
    oracleText = "Deathtouch\n" +
        "When this creature enters, target opponent reveals their hand. You may choose a " +
        "nonland card from it. If you do, exile that card.\n" +
        "When this creature leaves the battlefield, the exiled card's owner creates an X/X " +
        "white Spirit creature token, where X is the mana value of the exiled card."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val opponent = target("target opponent", Targets.Opponent)
        effect = Effects.Composite(
            listOf(
                RevealHandEffect(opponent),
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                    storeAs = "revealedHand"
                ),
                SelectFromCollectionEffect(
                    from = "revealedHand",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Controller,
                    filter = GameObjectFilter.Nonland,
                    storeSelected = "exiledCard",
                    prompt = "You may exile a nonland card from target opponent's hand",
                    showAllCards = true,
                    alwaysPrompt = true
                ),
                MoveCollectionEffect(
                    from = "exiledCard",
                    destination = CardDestination.ToZone(Zone.EXILE, Player.ContextPlayer(0)),
                    linkToSource = true
                )
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromLinkedExile(),
                    storeAs = "exiledCard"
                ),
                CreateTokenEffect(
                    count = DynamicAmount.VariableReference("exiledCard_count"),
                    power = 0,
                    toughness = 0,
                    colors = setOf(Color.WHITE),
                    creatureTypes = setOf("Spirit"),
                    dynamicPower = DynamicAmount.StoredCardManaValue("exiledCard"),
                    dynamicToughness = DynamicAmount.StoredCardManaValue("exiledCard"),
                    controller = EffectTarget.ControllerOfPipelineTarget("exiledCard", 0),
                    imageUri = "https://cards.scryfall.io/normal/front/8/e/8ea4fc2f-95a4-49d0-b06e-b88d19637737.jpg?1743176763"
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "222"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc779a1b-128c-4c74-bebd-bdb687867f68.jpg?1743204878"
    }
}
