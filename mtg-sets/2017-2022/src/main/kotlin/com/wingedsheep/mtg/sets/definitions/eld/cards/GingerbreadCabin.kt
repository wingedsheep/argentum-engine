package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Gingerbread Cabin
 * Land — Forest
 *
 * ({T}: Add {G}.)
 * This land enters tapped unless you control three or more other Forests.
 * When this land enters untapped, create a Food token.
 *
 * Same shape as [MysticSanctuary]: the Forest subtype supplies the {G} ability, and "enters
 * untapped" is the `untapped()` state predicate on the self enters trigger.
 */
val GingerbreadCabin = card("Gingerbread Cabin") {
    manaCost = ""
    colorIdentity = "G"
    typeLine = "Land — Forest"
    oracleText = "({T}: Add {G}.)\n" +
        "This land enters tapped unless you control three or more other Forests.\n" +
        "When this land enters untapped, create a Food token. (It's an artifact with " +
        "\"{2}, {T}, Sacrifice this token: You gain 3 life.\")"

    replacementEffect(EntersTapped(
        unlessCondition = Conditions.YouControlOtherAtLeast(3, GameObjectFilter.Land.withSubtype(Subtype.FOREST))
    ))

    triggeredAbility {
        trigger = Triggers.self.matching(GameObjectFilter.Any.untapped()).enters()
        effect = Effects.CreateFood()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "245"
        artist = "Chris Ostrowski"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b11f5296-5f7d-41ca-a67d-e976273d7386.jpg?1783932577"
        ruling(
            "2019-10-04",
            "As these lands are entering the battlefield, they check for lands that are already on the " +
                "battlefield. They won't see lands that are entering the battlefield at the same time " +
                "(due to Scapeshift, for example).",
        )
        ruling(
            "2019-10-04",
            "If another effect puts these lands onto the battlefield tapped, they enter tapped, even if " +
                "you control enough lands with the appropriate basic land type.",
        )
    }
}
