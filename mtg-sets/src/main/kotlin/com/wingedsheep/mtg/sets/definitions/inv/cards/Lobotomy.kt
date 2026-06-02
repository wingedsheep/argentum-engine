package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Lobotomy
 * {2}{U}{B}
 * Sorcery
 *
 * Target player reveals their hand, then you choose a card other than a basic land card
 * from it. Search that player's graveyard, hand, and library for all cards with the same
 * name as the chosen card and exile them. Then that player shuffles.
 *
 * Invasion engine gap #10 — the "capture a card's name" half of the "name a card" family.
 * Instead of typing a name, the controller picks a card and [Effects.StoreCardName] records
 * its name into `chosenValues`; the search then matches every card of that name via
 * [GameObjectFilter.namedFromVariable] ([CardPredicate.NameEqualsChosen]).
 */
val Lobotomy = card("Lobotomy") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Target player reveals their hand, then you choose a card other than a basic land card from it. " +
        "Search that player's graveyard, hand, and library for all cards with the same name as the chosen card " +
        "and exile them. Then that player shuffles."

    spell {
        val player = target("player", TargetPlayer())
        effect = Effects.Composite(
            listOf(
                // 1. Target player reveals their hand.
                RevealHandEffect(player),
                // 2. Gather their hand so the controller can choose a card.
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)),
                    storeAs = "hand"
                ),
                // 3. You choose a card other than a basic land card.
                SelectFromCollectionEffect(
                    from = "hand",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    chooser = Chooser.Controller,
                    filter = GameObjectFilter(cardPredicates = listOf(CardPredicate.Not(CardPredicate.IsBasicLand))),
                    storeSelected = "chosen",
                    prompt = "Choose a card other than a basic land card",
                    alwaysPrompt = true,
                    showAllCards = true
                ),
                // 4. Record the chosen card's name.
                Effects.StoreCardName(from = "chosen", storeAs = "chosenName"),
                // 5. Find every card of that name across their graveyard, hand, and library.
                GatherCardsEffect(
                    source = CardSource.FromMultipleZones(
                        zones = listOf(Zone.GRAVEYARD, Zone.HAND, Zone.LIBRARY),
                        player = Player.ContextPlayer(0),
                        filter = GameObjectFilter.Any.namedFromVariable("chosenName")
                    ),
                    storeAs = "toExile"
                ),
                // 6. Exile them.
                MoveCollectionEffect(
                    from = "toExile",
                    destination = CardDestination.ToZone(Zone.EXILE, Player.ContextPlayer(0))
                ),
                // 7. That player shuffles.
                ShuffleLibraryEffect(target = EffectTarget.ContextTarget(0))
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "255"
        artist = "D. Alexander Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff307dbb-4ab6-457b-be56-47106864bf61.jpg?1562946537"
    }
}
