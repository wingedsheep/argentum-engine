package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Black Cat, Cunning Thief
 * {3}{B}{B}
 * Legendary Creature — Human Rogue Villain
 * 2/3
 *
 * When Black Cat enters, look at the top nine cards of target opponent's library, exile two of
 * them face down, then put the rest on the bottom of their library in a random order. You may play
 * the exiled cards for as long as they remain exiled. Mana of any type can be spent to cast spells
 * this way.
 *
 * ETB triggered ability targeting an opponent, composed from the standard look-at-top pipeline:
 *  - [GatherCardsEffect] over the opponent's [CardSource.TopOfLibrary] (nine), which the controller
 *    looks at ([com.wingedsheep.sdk.scripting.effects.LookAudience.Controller] default).
 *  - [SelectFromCollectionEffect] `ChooseExactly(2)` (`showAllCards` so all nine are visible) →
 *    `exiled`, remainder → `rest`.
 *  - `exiled` moved to the owner's exile face down ([FaceDownMode.HIDDEN]); `rest` to the bottom of
 *    that library in a random order ([CardOrder.Random]).
 *  - [GrantMayPlayFromExileEffect] over `exiled` with `Permanent` expiry (playable "for as long as
 *    they remain exiled") and `withAnyManaType = true` for "Mana of any type can be spent to cast
 *    spells this way" — the same impulse-from-an-opponent's-library primitive Laughing Jasper Flint
 *    and Cruelclaw's Heist use.
 */
val BlackCatCunningThief = card("Black Cat, Cunning Thief") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Rogue Villain"
    power = 2
    toughness = 3
    oracleText = "When Black Cat enters, look at the top nine cards of target opponent's library, " +
        "exile two of them face down, then put the rest on the bottom of their library in a random " +
        "order. You may play the exiled cards for as long as they remain exiled. Mana of any type " +
        "can be spent to cast spells this way."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target("target opponent", Targets.Opponent)
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(
                    count = DynamicAmount.Fixed(9),
                    player = Player.ContextPlayer(0),
                ),
                storeAs = "topNine",
            ),
            SelectFromCollectionEffect(
                from = "topNine",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(2)),
                chooser = Chooser.Controller,
                storeSelected = "exiled",
                storeRemainder = "rest",
                prompt = "Exile two of those cards face down.",
                showAllCards = true,
            ),
            MoveCollectionEffect(
                from = "exiled",
                destination = CardDestination.ToZone(Zone.EXILE, Player.ContextPlayer(0)),
                faceDown = FaceDownMode.HIDDEN,
            ),
            MoveCollectionEffect(
                from = "rest",
                destination = CardDestination.ToZone(
                    Zone.LIBRARY,
                    Player.ContextPlayer(0),
                    ZonePlacement.Bottom,
                ),
                order = CardOrder.Random,
            ),
            GrantMayPlayFromExileEffect(
                from = "exiled",
                expiry = MayPlayExpiry.Permanent,
                withAnyManaType = true,
            ),
        )
        description = "When Black Cat enters, look at the top nine cards of target opponent's " +
            "library, exile two of them face down, then put the rest on the bottom of their " +
            "library in a random order. You may play the exiled cards for as long as they remain " +
            "exiled. Mana of any type can be spent to cast spells this way."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Alessandra Pisano"
        flavorText = "\"Spider-Man, I'm truly sorry for all the bad luck you've been having.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ed36ada-22c8-4e40-86c5-c116a0bee1c2.jpg?1783905347"
    }
}
