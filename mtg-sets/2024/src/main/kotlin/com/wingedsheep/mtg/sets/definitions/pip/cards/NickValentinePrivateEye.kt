package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedExceptBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Nick Valentine, Private Eye — Fallout #35
 * {2}{U} · Legendary Artifact Creature — Synth Detective · 2/2
 *
 * Nick Valentine can't be blocked except by artifact creatures.
 * Whenever Nick Valentine or another artifact creature you control dies, you may investigate.
 *
 * "Nick Valentine or another artifact creature you control" is a single ANY-bound dies trigger over
 * artifact creatures you control — Nick is one himself, so the filter already covers him.
 */
val NickValentinePrivateEye = card("Nick Valentine, Private Eye") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Artifact Creature — Synth Detective"
    power = 2
    toughness = 2
    oracleText = "Nick Valentine can't be blocked except by artifact creatures.\n" +
        "Whenever Nick Valentine or another artifact creature you control dies, you may investigate. " +
        "(To investigate, create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    staticAbility {
        ability = CantBeBlockedExceptBy(blockerFilter = GameObjectFilter.ArtifactCreature)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.ArtifactCreature.youControl()).dies()
        effect = Effects.May(Effects.Investigate())
        description = "Whenever Nick Valentine or another artifact creature you control dies, you may investigate."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "Viko Menezes"
        flavorText = "\"Your case is my top concern.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a7c9553-d4a8-4d51-8e8a-8a0ffbdf1715.jpg?1783912413"
    }
}
