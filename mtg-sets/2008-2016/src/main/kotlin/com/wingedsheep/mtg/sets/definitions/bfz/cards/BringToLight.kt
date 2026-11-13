package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bring to Light
 * {3}{G}{U}
 * Sorcery
 * Converge — Search your library for a creature, instant, or sorcery card with mana value less
 * than or equal to the number of colors of mana spent to cast this spell, exile that card, then
 * shuffle. You may cast that card without paying its mana cost.
 *
 * Converge's cap is the `manaValueAtMostColorsSpent(EffectTarget.Self)` predicate, read off the
 * resolving spell's own payment (a copy, or a free cast, has 0 colors, so only mana value 0 cards
 * qualify). The tutor is the Kasmina, Enigma Sage shape: gather → choose up to one (a search may
 * fail to find) → exile → shuffle → [Effects.May] around
 * [Effects.CastFromCollectionWithoutPayingCost]. The cast happens during resolution with timing
 * restrictions ignored; a card left uncast stays in exile.
 */
val BringToLight = card("Bring to Light") {
    manaCost = "{3}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Sorcery"
    oracleText = "Converge — Search your library for a creature, instant, or sorcery card with mana " +
        "value less than or equal to the number of colors of mana spent to cast this spell, exile " +
        "that card, then shuffle. You may cast that card without paying its mana cost."

    spell {
        effect = Effects.Pipeline {
            val bringToLightSearch = gather(
                CardSource.FromZone(
                    Zone.LIBRARY,
                    Player.You,
                    (GameObjectFilter.Creature or GameObjectFilter.InstantOrSorcery)
                        .manaValueAtMostColorsSpent(EffectTarget.Self)
                ),
                search = true
            )
            val bringToLightFound = chooseUpTo(
                1,
                from = bringToLightSearch,
                prompt = "Search for a creature, instant, or sorcery card with mana value at most " +
                    "the number of colors of mana spent to cast Bring to Light"
            )
            val bringToLightExiled = moveTracked(bringToLightFound, CardDestination.ToZone(Zone.EXILE))
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
            run(Effects.May(Effects.CastFromCollectionWithoutPayingCost(bringToLightExiled)))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d25b13a4-6282-4426-8b01-9550f7d52d16.jpg?1783938180"

        ruling(
            "2015-08-25",
            "If you cast a card \"without paying its mana cost,\" you can't choose to cast it for any " +
                "alternative costs, such as awaken costs. You can, however, pay additional costs, such " +
                "as kicker costs. If the card has any mandatory additional costs, you must pay those to " +
                "cast the card."
        )
        ruling("2015-08-25", "If the card has {X} in its mana cost, you must choose 0 as the value of X.")
        ruling(
            "2015-08-25",
            "If you cast the exiled card, you do so as part of the resolution of Bring to Light. You " +
                "can't wait to cast it later in the turn. Timing restrictions based on the card's type " +
                "are ignored, but other restrictions (such as \"Cast [this card] only during combat\") " +
                "are not."
        )
        ruling("2015-08-25", "If you don't cast the card, it remains exiled.")
        ruling(
            "2015-08-25",
            "If you cast an instant or sorcery card this way, it goes to your graveyard as normal. It " +
                "doesn't return to exile."
        )
        ruling(
            "2015-08-25",
            "If a spell with a converge ability is copied, no mana was spent to cast the copy, so the " +
                "number of colors of mana spent to cast the spell will be zero. The number of colors " +
                "spent to cast the original spell is not copied."
        )
    }
}
