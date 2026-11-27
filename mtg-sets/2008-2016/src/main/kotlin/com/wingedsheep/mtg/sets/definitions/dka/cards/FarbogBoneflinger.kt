package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Farbog Boneflinger
 * {4}{B}
 * Creature — Zombie
 * 2/2
 * When this creature enters, target creature gets -2/-2 until end of turn.
 */
val FarbogBoneflinger = card("Farbog Boneflinger") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, target creature gets -2/-2 until end of turn."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-2, -2, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Tomasz Jedruszek"
        flavorText = "\"Here are some bones for you to choke on, dear brother!\"\n—Gisa, to Geralf"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98d45316-b44a-4cf6-8cbe-b02fe6545141.jpg"
    }
}
