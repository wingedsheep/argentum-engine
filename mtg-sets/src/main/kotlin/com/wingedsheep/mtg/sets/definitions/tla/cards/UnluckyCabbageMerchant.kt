package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Unlucky Cabbage Merchant
 * {1}{G}
 * Creature — Human Citizen
 * 2/2
 *
 * When this creature enters, create a Food token. (It's an artifact with "{2}, {T},
 * Sacrifice this token: You gain 3 life.")
 * Whenever you sacrifice a Food, you may search your library for a basic land card and put it
 * onto the battlefield tapped. If you search your library this way, put this creature on the
 * bottom of its owner's library, then shuffle.
 *
 * The sacrifice trigger is modeled with [Triggers.YouSacrificeOneOrMore] filtered to Foods.
 * The whole search-and-bounce is wrapped in a single [MayEffect] so the optional "you may search"
 * decision gates the self-bounce: declining keeps the creature, while accepting always puts it on
 * the bottom of the library (even if no basic land is found), matching "If you search this way".
 * The search's own shuffle is folded into the explicit "then shuffle" via the trailing
 * [ShuffleLibraryEffect].
 */
val UnluckyCabbageMerchant = card("Unlucky Cabbage Merchant") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Citizen"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "Whenever you sacrifice a Food, you may search your library for a basic land card and put it onto the battlefield tapped. If you search your library this way, put this creature on the bottom of its owner's library, then shuffle."

    // When this creature enters, create a Food token.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateFood(1)
    }

    // Whenever you sacrifice a Food, you may search your library for a basic land card and put it
    // onto the battlefield tapped. If you search your library this way, put this creature on the
    // bottom of its owner's library, then shuffle.
    triggeredAbility {
        trigger = Triggers.YouSacrificeOneOrMore(GameObjectFilter.Artifact.withSubtype("Food"))
        effect = MayEffect(
            effect = Effects.Composite(
                Patterns.Library.searchLibrary(
                    filter = Filters.BasicLand,
                    destination = SearchDestination.BATTLEFIELD,
                    entersTapped = true,
                    shuffleAfter = false
                ),
                Effects.PutOnBottomOfLibrary(EffectTarget.Self),
                ShuffleLibraryEffect()
            ),
            descriptionOverride = "You may search your library for a basic land card and put it onto the battlefield tapped",
            hint = "If you search this way, put this creature on the bottom of its owner's library, then shuffle"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Thomas Chamberlain-Keen"
        flavorText = "\"My cabbages!\""
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a838009-b115-4497-b35d-682e41ead7db.jpg?1764121365"
    }
}
