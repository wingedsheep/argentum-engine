package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nissa's Encouragement — Hour of Devastation #203 (planeswalker deck)
 * {4}{G} · Sorcery
 *
 * Search your library and graveyard for a card named Forest, a card named Brambleweft Behemoth,
 * and a card named Nissa, Genesis Mage. Reveal those cards, put them into your hand, then shuffle.
 *
 * One search across both zones at once ([CardSource.FromMultipleZones]): gather every card in
 * library and graveyard named any of the three → `ChooseUpTo(3)` under
 * [SelectionRestriction.OnePerCardName], so the pick holds at most one card of each name → move
 * them to hand revealed → shuffle. The search includes a hidden zone, so finding any or none of
 * the three is legal (2017-07-14 ruling). Name equality, not land type, so a nonbasic Forest such as
 * Sheltered Thicket can't be found.
 */
val NissasEncouragement = card("Nissa's Encouragement") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library and graveyard for a card named Forest, a card named Brambleweft " +
        "Behemoth, and a card named Nissa, Genesis Mage. Reveal those cards, put them into your hand, " +
        "then shuffle."

    spell {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromMultipleZones(
                    listOf(Zone.LIBRARY, Zone.GRAVEYARD),
                    Player.You,
                    GameObjectFilter.Any.named("Forest") or
                        GameObjectFilter.Any.named("Brambleweft Behemoth") or
                        GameObjectFilter.Any.named("Nissa, Genesis Mage"),
                ),
                search = true,
            )
            val found = chooseUpTo(
                3,
                from = searchable,
                restrictions = listOf(SelectionRestriction.OnePerCardName),
                prompt = "Search for a card named Forest, a card named Brambleweft Behemoth, " +
                    "and a card named Nissa, Genesis Mage",
            )
            toHand(found, revealed = true)
            run(Effects.ShuffleLibrary())
            // CR 701.23 — the search happened whether or not anything was found.
            run(EmitLibrarySearchedEventEffect)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "203"
        inBooster = false
        artist = "Tommy Arnold"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a489d5c7-ef57-4973-86ac-06234c308c3c.jpg?1783935985"
        ruling(
            "2017-07-14",
            "You can find any or all of the cards listed with Nissa's Encouragement. You could even " +
                "find none, but that wouldn't be very encouraging."
        )
        ruling(
            "2017-07-14",
            "Nissa's Encouragement can't be used to find a card with the land type Forest that isn't " +
                "also named Forest (such as Sheltered Thicket from the Amonkhet set)."
        )
    }
}
