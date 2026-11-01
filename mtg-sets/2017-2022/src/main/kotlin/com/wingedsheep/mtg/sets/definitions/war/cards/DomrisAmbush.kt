package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Domri's Ambush
 * {R}{G}
 * Sorcery
 * Put a +1/+1 counter on target creature you control. Then that creature deals damage equal to its
 * power to target creature or planeswalker you don't control.
 */
val DomrisAmbush = card("Domri's Ambush") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on target creature you control. Then that creature deals damage equal to its power to target creature or planeswalker you don't control."
    spell {
        val yours = target(TargetFilter.Creature.youControl())
        val victim = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.opponentControls()))
        effect = Effects.AddCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 1, target = yours) then
            Effects.DealDamage(DynamicAmounts.powerOf(yours), victim, damageSource = yours)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "192"
        artist = "Victor Adame Minguez"
        flavorText = "\"When civilization teeters on the brink, I'll be there to kick it the rest of the way.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b0dcae59-e9be-45a8-8ed8-5b47309bb716.jpg"
    }
}
