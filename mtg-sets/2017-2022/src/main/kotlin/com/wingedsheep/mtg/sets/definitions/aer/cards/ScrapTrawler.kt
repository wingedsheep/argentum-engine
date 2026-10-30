package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Scrap Trawler
 * {3}
 * Artifact Creature — Construct
 * 3/2
 *
 * Whenever this creature dies or another artifact you control is put into a graveyard from the
 * battlefield, return to your hand target artifact card in your graveyard with lesser mana value.
 *
 * Scrap Trawler is itself an artifact, so "this creature dies or another artifact you control"
 * is one ANY-bound subject (Tablet of Epityr, Jackdaw Savior). The cap is the triggering
 * artifact's mana value as it last existed on the battlefield (2017-02-09 ruling), read through
 * `triggeringManaValue()`'s last-known-information fallback.
 */
val ScrapTrawler = card("Scrap Trawler") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    oracleText = "Whenever this creature dies or another artifact you control is put into a graveyard from the battlefield, return to your hand target artifact card in your graveyard with lesser mana value."
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.youControl()).dies()
        val artifactCard = target(
            TargetFilter.ArtifactInYourGraveyard.manaValueAtMostDynamic(
                DynamicAmounts.triggeringManaValue() - 1
            ),
        )
        effect = Effects.ReturnToHand(artifactCard)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "175"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68abc75f-596f-4169-96fc-ada941ef47ed.jpg?1783936719"

        ruling("2017-02-09", "If Scrap Trawler and another artifact you control are put into a graveyard at the same time, Scrap Trawler's ability triggers for each of them.")
        ruling("2017-02-09", "The target artifact card must have a lesser mana value than the artifact that caused Scrap Trawler's ability to trigger by being put into a graveyard. Use the artifact's mana value as it last existed on the battlefield to determine what may be returned.")
    }
}
