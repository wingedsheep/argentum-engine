package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val TheFallOfKroog = card("The Fall of Kroog") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose target opponent. Destroy target land that player controls. The Fall of Kroog deals 3 damage to that player and 1 damage to each creature they control."

    spell {
        val opponent = target(Targets.Opponent)
        val land = target(TargetFilter(GameObjectFilter.Land.targetPlayerControls(opponent)))
        effect = Effects.Destroy(land) then
            Effects.DealDamage(3, opponent) then
            Patterns.Group.dealDamageToAll(1, GroupFilter(GameObjectFilter.Creature.targetPlayerControls(opponent)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "133"
        artist = "David Auden Nash"
        flavorText = "As retaliation for the Warlord's ambush at the peace summit, Mishra leveled Yotia's capital city."
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f454583d-d227-4fc8-842e-128e025880e4.jpg?1783920068"
    }
}
