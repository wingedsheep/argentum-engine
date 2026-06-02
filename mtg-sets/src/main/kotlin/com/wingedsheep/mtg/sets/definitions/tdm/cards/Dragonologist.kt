package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Dragonologist — Tarkir: Dragonstorm #42
 * {2}{U} · Creature — Human Wizard · 1/3
 *
 * When this creature enters, look at the top six cards of your library. You may reveal an
 * instant, sorcery, or Dragon card from among them and put it into your hand. Put the rest on
 * the bottom of your library in a random order.
 * Untapped Dragons you control have hexproof.
 *
 * The ETB is the standard "look at top N, may keep one matching, bottom the rest in random
 * order" pipeline (Gather top six → SelectUpTo(1) filtered to instant/sorcery/Dragon → move
 * the kept card revealed to hand → bottom the remainder randomly). The "you may reveal" is
 * `ChooseUpTo(1)`. The static grants hexproof to the group of untapped Dragons you control
 * via [GrantKeyword] with a [GroupFilter].
 */
val Dragonologist = card("Dragonologist") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "When this creature enters, look at the top six cards of your library. You may reveal " +
        "an instant, sorcery, or Dragon card from among them and put it into your hand. Put the rest on " +
        "the bottom of your library in a random order.\n" +
        "Untapped Dragons you control have hexproof."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(listOf(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(count = DynamicAmount.Fixed(6), player = Player.You),
                storeAs = "looked"
            ),
            SelectFromCollectionEffect(
                from = "looked",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                chooser = Chooser.Controller,
                filter = GameObjectFilter.InstantOrSorcery or
                    GameObjectFilter.Any.withSubtype(Subtype.DRAGON),
                storeSelected = "kept",
                storeRemainder = "toBottom",
                showAllCards = true,
                prompt = "You may reveal an instant, sorcery, or Dragon card to put into your hand.",
                selectedLabel = "Put into hand",
                remainderLabel = "Put on bottom"
            ),
            MoveCollectionEffect(
                from = "kept",
                destination = CardDestination.ToZone(Zone.HAND),
                revealed = true
            ),
            MoveCollectionEffect(
                from = "toBottom",
                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                order = CardOrder.Random
            )
        ))
        description = "When this creature enters, look at the top six cards of your library. You may reveal " +
            "an instant, sorcery, or Dragon card from among them and put it into your hand. Put the rest on " +
            "the bottom of your library in a random order."
    }

    staticAbility {
        ability = GrantKeyword(
            Keyword.HEXPROOF,
            GroupFilter.AllCreaturesYouControl.untapped().withSubtype(Subtype.DRAGON)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "42"
        artist = "Mila Pesic"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/8810ebb4-9e51-46f0-a54a-a0b4d77b762a.jpg?1743204126"
    }
}
