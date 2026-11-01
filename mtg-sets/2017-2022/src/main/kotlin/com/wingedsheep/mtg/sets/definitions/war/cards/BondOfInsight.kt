package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Bond of Insight
 * {3}{U}
 * Sorcery
 *
 * Each player mills four cards. Return up to two instant and/or sorcery cards from your graveyard
 * to your hand. Exile Bond of Insight.
 *
 * The return doesn't target, so it's a resolution-time choice made after the mill (cards you just
 * milled are eligible): gather the instants/sorceries in your graveyard, choose up to two, move them
 * to hand. Bond of Insight itself is still on the stack while resolving, so it can't pick itself;
 * [selfExile] sends it to exile instead of the graveyard.
 */
val BondOfInsight = card("Bond of Insight") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Each player mills four cards. Return up to two instant and/or sorcery cards from " +
        "your graveyard to your hand. Exile Bond of Insight."

    spell {
        effect = Effects.Pipeline {
            mill(4, Player.Each)
            val spells = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.InstantOrSorcery)
            )
            val chosen = chooseUpTo(
                2,
                from = spells,
                showAllCards = true,
                prompt = "Return up to two instant and/or sorcery cards from your graveyard to your hand",
                selectedLabel = "Return to hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(chosen)
        }
        selfExile()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "Joe Slucher"
        flavorText = "\"Together we can unlock secrets no one has even dreamed of. I might even let " +
            "you remember some of them.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/4/648629af-e911-49e1-b564-c98f339b84a1.jpg?1783933467"
    }
}
