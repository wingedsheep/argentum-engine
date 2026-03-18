package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Fall of the Thran
 * {5}{W}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Destroy all lands.
 * II, III — Each player returns two land cards from their graveyard to the battlefield.
 */
val FallOfTheThran = card("Fall of the Thran") {
    manaCost = "{5}{W}"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Destroy all lands.\n" +
        "II, III — Each player returns two land cards from their graveyard to the battlefield."

    sagaChapter(1) {
        effect = Effects.DestroyAll(GameObjectFilter.Land)
    }

    val returnLandsEffect = ForEachPlayerEffect(
        players = Player.Each,
        effects = listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Land),
                storeAs = "graveyardLands"
            ),
            SelectFromCollectionEffect(
                from = "graveyardLands",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(2)),
                storeSelected = "returningLands",
                prompt = "Choose up to two land cards to return to the battlefield"
            ),
            MoveCollectionEffect(
                from = "returningLands",
                destination = CardDestination.ToZone(Zone.BATTLEFIELD)
            )
        )
    )

    sagaChapter(2) {
        effect = returnLandsEffect
    }

    sagaChapter(3) {
        effect = returnLandsEffect
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3a613a01-6145-4e34-987c-c9bdcb068370.jpg?1562734219"
        ruling("2018-04-27", "If a player somehow has only one land card in their graveyard when either of Fall of the Thran's last two chapter abilities resolves, that player returns that one card to the battlefield.")
    }
}
