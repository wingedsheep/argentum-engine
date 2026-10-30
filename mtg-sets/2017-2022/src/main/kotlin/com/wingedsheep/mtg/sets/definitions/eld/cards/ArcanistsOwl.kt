package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Arcanist's Owl
 * {W/U}{W/U}{W/U}{W/U}
 * Artifact Creature — Bird
 * 3/3
 *
 * Flying
 * When this creature enters, look at the top four cards of your library. You may reveal an
 * artifact or enchantment card from among them and put it into your hand. Put the rest on the
 * bottom of your library in a random order.
 */
val ArcanistsOwl = card("Arcanist's Owl") {
    manaCost = "{W/U}{W/U}{W/U}{W/U}"
    colorIdentity = "UW"
    typeLine = "Artifact Creature — Bird"
    oracleText = "Flying\n" +
        "When this creature enters, look at the top four cards of your library. You may reveal an artifact or enchantment card from among them and put it into your hand. Put the rest on the bottom of your library in a random order."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 4,
            filter = GameObjectFilter.ArtifactOrEnchantment,
            prompt = "You may reveal an artifact or enchantment card from among them and put it into your hand",
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "206"
        artist = "Véronique Meignaud"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b42e1359-c79d-4848-a0f1-3f274e0bf0d9.jpg?1783932591"
    }
}
