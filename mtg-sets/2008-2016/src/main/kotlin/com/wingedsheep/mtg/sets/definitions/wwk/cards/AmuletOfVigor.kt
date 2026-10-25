package com.wingedsheep.mtg.sets.definitions.wwk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Amulet of Vigor
 * {1}
 * Artifact
 * Whenever a permanent you control enters tapped, untap it.
 *
 * `Triggers.a(Permanent.youControl()).entersTapped()` matches by how the permanent *entered*, so one
 * that enters untapped and is tapped afterwards doesn't trigger it (the card's ruling).
 */
val AmuletOfVigor = card("Amulet of Vigor") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Whenever a permanent you control enters tapped, untap it."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.youControl()).entersTapped()
        effect = Effects.Untap(EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "121"
        artist = "Warren Mahy"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/997bc933-ac30-477b-a4e1-5333b796a99d.jpg?1783942040"
    }
}
