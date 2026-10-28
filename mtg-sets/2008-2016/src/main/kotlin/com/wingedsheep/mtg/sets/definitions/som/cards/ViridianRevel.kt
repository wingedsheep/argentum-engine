package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Viridian Revel — Scars of Mirrodin #132 (canonical printing)
 * {1}{G}{G} · Enchantment
 *
 * Whenever an artifact is put into an opponent's graveyard from the battlefield, you may draw a card.
 *
 * A card always goes to its owner's graveyard, so "an opponent's graveyard" is an owner test
 * (`ownedByOpponent()`), not a controller test — per the ruling, who controlled the artifact
 * doesn't matter.
 */
val ViridianRevel = card("Viridian Revel") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever an artifact is put into an opponent's graveyard from the battlefield, " +
        "you may draw a card."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.ownedByOpponent()).dies()
        effect = Effects.May(Effects.DrawCards(1))
        description = "Whenever an artifact is put into an opponent's graveyard from the battlefield, " +
            "you may draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "rk post"
        flavorText = "\"Let them have their revel. Soon those who won't submit will be turned. " +
            "Those who won't be turned will die.\"\n—Vorinclex, Voice of Hunger"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d7f565e-0fb8-40c8-9540-213d35af846a.jpg?1783941715"
        ruling("2011-01-01", "It doesn't matter who controlled the artifact while it was on the battlefield, only whose graveyard it was put into.")
    }
}
