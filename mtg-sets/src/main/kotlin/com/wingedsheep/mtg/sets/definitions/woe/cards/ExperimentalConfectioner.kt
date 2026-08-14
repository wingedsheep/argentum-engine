package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Experimental Confectioner
 * {2}{B}
 * Creature — Human Peasant
 * 2/3
 *
 * When this creature enters, create a Food token.
 * Whenever you sacrifice a Food, create a 1/1 black Rat creature token with "This token can't block."
 *
 * "Whenever you sacrifice **a** Food" is the per-permanent template, not the batch one
 * ([Triggers.YouSacrificeA], CR 603.2c): sacrificing three Foods at once to something like Feasting
 * Troll King's activated cost makes three Rats, not one. Per the 2024-11-08 ruling a "Food" is any
 * Food *artifact*, so the filter keys on the subtype rather than on token-ness — Tough Cookie (an
 * Artifact Creature — Food Golem) counts.
 */
val ExperimentalConfectioner = card("Experimental Confectioner") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Peasant"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, create a Food token. (It's an artifact with \"{2}, " +
        "{T}, Sacrifice this token: You gain 3 life.\")\n" +
        "Whenever you sacrifice a Food, create a 1/1 black Rat creature token with \"This token " +
        "can't block.\""

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateFood()
    }

    triggeredAbility {
        trigger = Triggers.YouSacrificeA(GameObjectFilter.Artifact.withSubtype("Food"))
        effect = woeRatToken()
        description = "Whenever you sacrifice a Food, create a 1/1 black Rat creature token with " +
            "\"This token can't block.\""
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "314"
        artist = "Gaboleps"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49aaf5db-9418-4b97-97da-736c674905d4.jpg?1783915040"

        ruling(
            "2024-11-08",
            "If an effect refers to a Food, it means any Food artifact, not just a Food artifact " +
                "token. For example, you can sacrifice Tough Cookie (an Artifact Creature — Food " +
                "Golem) to activate an ability with \"Sacrifice a Food\" in its cost."
        )
        ruling(
            "2024-11-08",
            "Food is an artifact type. Even though it appears on some creatures, it's never a " +
                "creature type."
        )
    }
}
