package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * The Arkenstone // Seek the Heart — The Hobbit #170
 * {5} · Legendary Artifact · Mythic
 *
 * Creatures you control get +1/+1.
 * At the beginning of your end step, draw a card.
 *
 * Adventure: Seek the Heart — {2}{W}, Sorcery — Adventure
 * Search your library for a legendary creature card, reveal it, put it into your hand, then shuffle.
 *
 * Modeling notes:
 *  - The anthem is a group [ModifyStats] over `Creature.youControl()`, not an attachment bonus:
 *    The Arkenstone is a bare artifact with no Equipment subtype, so nothing is ever attached to it.
 *  - Both halves are plain compositions — the Adventure is the Time of Need script verbatim.
 *  - (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 *    caster cast it as the artifact while it remains in exile.)
 */
val TheArkenstone = card("The Arkenstone") {
    manaCost = "{5}"
    // Colorless front face, but the Adventure's {2}{W} puts the whole card in white's identity.
    colorIdentity = "W"
    typeLine = "Legendary Artifact"
    oracleText = "Creatures you control get +1/+1.\n" +
        "At the beginning of your end step, draw a card."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    triggeredAbility {
        trigger = Triggers.YourEndStep
        effect = Effects.DrawCards(1)
        description = "Draw a card."
    }

    adventure("Seek the Heart") {
        manaCost = "{2}{W}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Search your library for a legendary creature card, reveal it, put it into " +
            "your hand, then shuffle. (Then exile this card. You may cast the artifact later " +
            "from exile.)"
        spell {
            effect = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Creature.legendary(),
                destination = SearchDestination.HAND,
                reveal = true
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "170"
        artist = "Gaboleps"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a56a88ba-fcfa-4b56-bdae-a080b297b871.jpg?1783902783"
    }
}
