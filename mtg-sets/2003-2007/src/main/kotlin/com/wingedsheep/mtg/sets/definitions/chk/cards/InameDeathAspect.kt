package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Iname, Death Aspect
 * {4}{B}{B}
 * Legendary Creature — Spirit
 * 4/4
 * When Iname enters, you may search your library for any number of Spirit cards, put them into
 * your graveyard, then shuffle.
 *
 * An unbounded library search, written inline via `Effects.Pipeline` (as Grozoth does) because
 * `Patterns.Library.searchLibrary` only offers `ChooseUpTo(count)`. The consent gate wraps the
 * whole search, so declining skips the shuffle too; `chooseAnyNumber` separately allows finding
 * nothing. "Spirit cards" is a bare tribal noun — any card with the Spirit subtype qualifies.
 */
val InameDeathAspect = card("Iname, Death Aspect") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Spirit"
    power = 4
    toughness = 4
    oracleText = "When Iname enters, you may search your library for any number of Spirit cards, " +
        "put them into your graveyard, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Effects.Pipeline {
                val searchable = gather(
                    CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Any.withSubtype(Subtype.SPIRIT))
                )
                val found = chooseAnyNumber(
                    from = searchable,
                    prompt = "Search your library for any number of Spirit cards",
                )
                toGraveyard(found)
                run(Effects.ShuffleLibrary())
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "118"
        artist = "Justin Sweet"
        flavorText = "Iname revels in sadistic glee at the crushing of souls, but soon mourns the " +
            "lives so cruelly cut short. So the cycle begins anew."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2dd67e11-3dad-4e59-b15d-02911e53fbbf.jpg?1783944313"
    }
}
