package com.wingedsheep.mtg.sets.definitions.eve.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wake Thrasher
 * {2}{U}
 * Creature — Merfolk Soldier
 * 1/1
 *
 * Whenever a permanent you control becomes untapped, this creature gets +1/+1 until end of turn.
 */
val WakeThrasher = card("Wake Thrasher") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Merfolk Soldier"
    power = 1
    toughness = 1
    oracleText = "Whenever a permanent you control becomes untapped, this creature gets +1/+1 until end of turn."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.youControl()).becomesUntapped()
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "31"
        artist = "Jesper Ejsing"
        flavorText = "Anyone who thinks merrows have more brains than brawn has never been run over by one."
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93116209-636a-4b86-a8ed-d4a03e1d7212.jpg?1783942688"
        ruling("8/1/2008", "If permanents you control become untapped during your untap step, Wake Thrasher's ability will trigger that many times. However, since no player gets priority during the untap step, those abilities wait to be put on the stack until your upkeep starts. At that time, all your \"beginning of upkeep\" triggers will also trigger. You can put them and Wake Thrasher's abilities on the stack in any order.")
    }
}
