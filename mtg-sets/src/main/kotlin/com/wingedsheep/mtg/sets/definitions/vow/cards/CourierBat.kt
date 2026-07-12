package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Courier Bat
 * {2}{B}
 * Creature — Bat
 * 2/2
 * Flying
 * When this creature enters, if you gained life this turn, return up to one target creature card
 * from your graveyard to your hand.
 */
val CourierBat = card("Courier Bat") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Bat"
    oracleText = "Flying\nWhen this creature enters, if you gained life this turn, return up to one target creature card from your graveyard to your hand."
    power = 2
    toughness = 2
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.YouGainedLifeThisTurn
        val t = target("target", TargetObject(optional = true, filter = TargetFilter.CreatureInYourGraveyard))
        effect = Effects.Move(t, Zone.HAND)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Ilse Gort"
        flavorText = "Swarms of bats poured from Lurenbraum Fortress carrying wedding invitations to vampires of every bloodline."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d9a8dc7-1ee9-4731-bd72-3d02f4e7c6c4.jpg?1782703119"
    }
}
