package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

val InfiltrationLens = card("Infiltration Lens") {
    manaCost = "{1}"
    typeLine = "Artifact — Equipment"
    oracleText = "Whenever equipped creature becomes blocked by a creature, you may draw two cards.\nEquip {1}"

    triggeredAbility {
        trigger = Triggers.attached.becomesBlocked(by = GameObjectFilter.Creature)
        effect = Effects.May(Effects.DrawCards(2))
    }
    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "167"
        artist = "Izzy"
        flavorText = "Neurok spies carry devices that let them look a few moments into the future, giving them an almost insurmountable edge."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1baa10da-2733-4657-a1ea-74eb5a5a82b1.jpg?1783941707"
        ruling("2011-01-01", "If the equipped creature becomes blocked by multiple creatures, Infiltration Lens’s ability triggers that many times.")
    }
}
