package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Genesis Wave
 * {X}{G}{G}{G}
 * Sorcery
 *
 * Reveal the top X cards of your library. You may put any number of permanent cards with
 * mana value X or less from among them onto the battlefield. Then put all cards revealed
 * this way that weren't put onto the battlefield into your graveyard.
 *
 * Modelled as the stock Gather → Select → Move library pipeline (the Gishath shape), with
 * both X readings coming from the same resolution-time [DynamicAmount.XValue]: the gather
 * count, and the `manaValueAtMostX()` candidate predicate. `ChooseAnyNumber` honors the
 * "you may put any number" wording — declining is legal for every revealed card regardless
 * of its mana value (per ruling). `storeRemainder` collects everything not put onto the
 * battlefield — including permanent cards the player passed on — and sends it to the
 * graveyard. Gathering the top X naturally reveals fewer cards when the library is short.
 * All chosen cards move in one [MoveCollectionEffect], so they enter simultaneously and
 * see each other's enters-the-battlefield triggers.
 */
val GenesisWave = card("Genesis Wave") {
    manaCost = "{X}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Reveal the top X cards of your library. You may put any number of permanent " +
        "cards with mana value X or less from among them onto the battlefield. Then put all " +
        "cards revealed this way that weren't put onto the battlefield into your graveyard."

    spell {
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(count = DynamicAmount.XValue, player = Player.You),
                storeAs = "genesisWave_revealed",
                revealed = true
            ),
            SelectFromCollectionEffect(
                from = "genesisWave_revealed",
                selection = SelectionMode.ChooseAnyNumber,
                filter = GameObjectFilter.Permanent.manaValueAtMostX(),
                showAllCards = true,
                storeSelected = "genesisWave_toBattlefield",
                storeRemainder = "genesisWave_toGraveyard",
                prompt = "Put any number of permanent cards with mana value X or less onto the battlefield",
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Put into your graveyard"
            ),
            MoveCollectionEffect(
                from = "genesisWave_toBattlefield",
                destination = CardDestination.ToZone(Zone.BATTLEFIELD, Player.You)
            ),
            MoveCollectionEffect(
                from = "genesisWave_toGraveyard",
                destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.You)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "James Paick"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c920236f-c3d7-421c-b021-103996da790e.jpg?1783941717"
        ruling("2024-11-08", "If you have fewer than X cards in your library, you reveal all of them.")
        ruling("2024-11-08", "If a card in a player's library has {X} in its mana cost, X is considered to be 0.")
        ruling("2024-11-08", "A permanent card is an artifact, battle, creature, enchantment, land, or planeswalker card.")
        ruling(
            "2024-11-08",
            "If a permanent card in your library has no mana symbols in its upper right corner " +
                "(because it's a land card, for example), its mana value is 0. Such cards can always " +
                "be put onto the battlefield with Genesis Wave.",
        )
        ruling(
            "2024-11-08",
            "You don't have to put permanent cards revealed this way onto the battlefield if you " +
                "choose not to, regardless of their mana values.",
        )
        ruling(
            "2024-11-08",
            "All of the permanents put onto the battlefield this way enter at the same time. If any " +
                "have triggered abilities that trigger on something else entering, they'll see each other.",
        )
    }
}
