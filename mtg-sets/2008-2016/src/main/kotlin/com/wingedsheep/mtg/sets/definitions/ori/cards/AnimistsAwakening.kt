package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Animist's Awakening — Magic Origins #169
 * {X}{G} · Sorcery
 *
 * Reveal the top X cards of your library. Put all land cards from among them onto the battlefield
 * tapped and the rest on the bottom of your library in a random order.
 * Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, untap
 * those lands.
 *
 * A reveal → split → move pipeline. The land move is tracked (`moveTracked`) so "those lands" is
 * exactly what reached the battlefield — only those are untapped (ruling). Spell mastery is checked
 * at resolution (Dark Petition's condition); the spell itself is on the stack and never counts, and
 * the rest went to the library, not the graveyard. A short library reveals what there is (ruling).
 */
val AnimistsAwakening = card("Animist's Awakening") {
    manaCost = "{X}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Reveal the top X cards of your library. Put all land cards from among them onto the " +
        "battlefield tapped and the rest on the bottom of your library in a random order.\n" +
        "Spell mastery — If there are two or more instant and/or sorcery cards in your graveyard, " +
        "untap those lands."

    spell {
        effect = Effects.Pipeline {
            val revealed = gather(
                CardSource.TopOfLibrary(count = DynamicAmounts.xValue(), player = Player.You),
                revealed = true
            )
            val (lands, rest) = filterSplit(revealed, GameObjectFilter.Land)
            val landsPut = moveTracked(
                lands,
                CardDestination.ToZone(Zone.BATTLEFIELD, Player.You, ZonePlacement.Tapped)
            )
            move(
                rest,
                CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Bottom),
                order = CardOrder.Random
            )
            ifThen(Conditions.CardsInGraveyardMatchingAtLeast(2, GameObjectFilter.InstantOrSorcery)) {
                run(Effects.TapCollection(collection = landsPut, tap = false))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "169"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3b62616-c1a3-45f7-a329-f1305c462eaf.jpg?1783938325"
        ruling(
            "2015-06-22",
            "Check to see if there are two or more instant and/or sorcery cards in your graveyard as the " +
                "spell resolves to determine whether the spell mastery ability applies. The spell itself " +
                "won't count because it's still on the stack as you make this check."
        )
        ruling(
            "2015-06-22",
            "If the spell mastery ability applies, you untap only the lands put onto the battlefield " +
                "with Animist's Awakening."
        )
        ruling(
            "2015-06-22",
            "No player will know the order of the cards put on the bottom of your library. Practically " +
                "speaking, the cards should be shuffled (although this is not the game action of " +
                "\"shuffling\")."
        )
        ruling(
            "2015-06-22",
            "If you have X or less cards in your library, you'll reveal all the cards from your " +
                "library, put all land cards from among them onto the battlefield tapped, and then put " +
                "the rest of the cards back in your library in a random order. (This is effectively the " +
                "same as shuffling your library, although it's still not technically a shuffle.)"
        )
    }
}
