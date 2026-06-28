package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * True Ancestry
 * {1}{G}
 * Sorcery — Lesson
 *
 * Return up to one target permanent card from your graveyard to your hand.
 * Create a Clue token. (It's an artifact with "{2}, Sacrifice this token: Draw a card.")
 *
 * "Up to one target" makes the return optional, so the spell still resolves (and the Clue is
 * still created) when there's no permanent card in the graveyard to return.
 */
val TrueAncestry = card("True Ancestry") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery — Lesson"
    oracleText = "Return up to one target permanent card from your graveyard to your hand.\n" +
        "Create a Clue token. (It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        val permanentCard = target(
            "permanent card from your graveyard",
            TargetObject(
                optional = true,
                filter = TargetFilter(
                    baseFilter = GameObjectFilter.Permanent.ownedByYou(),
                    zone = Zone.GRAVEYARD,
                ),
            ),
        )
        effect = Effects.Composite(
            Effects.ReturnToHand(permanentCard),
            Effects.CreateClue(),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "199"
        artist = "Chibi"
        flavorText = "\"Understanding the struggle between your two great-grandfathers can help you " +
            "better understand the battle within yourself.\"\n—Iroh"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c55b333-dc8b-4332-895b-eec5eb45543f.jpg?1764121352"
    }
}
