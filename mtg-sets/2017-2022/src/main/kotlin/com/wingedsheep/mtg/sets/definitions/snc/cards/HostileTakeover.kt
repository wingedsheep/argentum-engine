package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Hostile Takeover
 * {2}{U}{B}{R}
 * Sorcery
 * Up to one target creature has base power and toughness 1/1 until end of turn. Up to one other
 * target creature has base power and toughness 4/4 until end of turn. Then Hostile Takeover deals
 * 3 damage to each creature.
 *
 * Two independent optional target words: the caster may fill either, both, or neither, and a
 * lone 4/4 target stays in the second group (`CastSpell.targetGroupCounts`). Both base-P/T
 * effects land in layer 7b before the sweep, so a creature made 4/4 survives (barring
 * counters/pumps already on it) and one made 1/1 dies.
 */
val HostileTakeover = card("Hostile Takeover") {
    manaCost = "{2}{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Sorcery"
    oracleText = "Up to one target creature has base power and toughness 1/1 until end of turn. " +
        "Up to one other target creature has base power and toughness 4/4 until end of turn. " +
        "Then Hostile Takeover deals 3 damage to each creature."

    spell {
        val shrunk = target(TargetFilter.Creature, optional = true)
        // "other target creature" — must differ from the first target (CR 601.2c)
        val grown = target(TargetOther(TargetObject(filter = TargetFilter.Creature, optional = true)))
        effect = Effects.SetBasePowerAndToughness(1, 1, shrunk) then
            Effects.SetBasePowerAndToughness(4, 4, grown) then
            Patterns.Group.dealDamageToAll(3, GroupFilter(GameObjectFilter.Creature))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "191"
        artist = "Vincent Proce"
        flavorText = "Ob Nixilis smiled. It had been over one thousand years since his last conquest."
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd7df727-50ea-4ea8-bdb9-d7ef16199d8a.jpg?1783923084"
    }
}
