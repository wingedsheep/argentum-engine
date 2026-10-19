package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Black Dragon
 * {5}{B}{B}
 * Creature — Dragon
 * 4/4
 * Flying
 * Acid Breath — When this creature enters, target creature an opponent controls gets -3/-3 until end of turn.
 */
val BlackDragon = card("Black Dragon") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Dragon"
    oracleText = "Flying\nAcid Breath — When this creature enters, target creature an opponent controls gets -3/-3 until end of turn."
    power = 4
    toughness = 4
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.opponentControls())
        effect = Effects.ModifyStats(-3, -3, t)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "90"
        artist = "Mark Zug"
        flavorText = "Black dragons revel in the collapse of humanoid kingdoms."
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aaca283a-39f1-4ae7-98fb-7821a927ef86.jpg?1783926500"
    }
}
