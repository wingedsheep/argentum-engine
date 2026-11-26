package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Void Stalker
 * {1}{U}
 * Creature — Elemental
 * 2/1
 * {2}{U}, {T}: Put this creature and target creature on top of their owners' libraries, then those
 * players shuffle their libraries.
 *
 * "Put on top, then shuffle" is a shuffle into the library. Each move is gated on the battlefield,
 * so if Void Stalker has left before resolution only the target goes (ruling), and targeting
 * Void Stalker itself moves it once.
 */
val VoidStalker = card("Void Stalker") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    power = 2
    toughness = 1
    oracleText = "{2}{U}, {T}: Put this creature and target creature on top of their owners' libraries, " +
        "then those players shuffle their libraries."

    activatedAbility {
        val creature = target(TargetFilter.Creature)
        cost = Costs.Composite(Costs.Mana("{2}{U}"), Costs.Tap)
        effect = Effects.ShuffleIntoLibrary(EffectTarget.Self, fromZone = Zone.BATTLEFIELD) then
            Effects.ShuffleIntoLibrary(creature, fromZone = Zone.BATTLEFIELD)
        description = "{2}{U}, {T}: Put this creature and target creature on top of their owners' " +
            "libraries, then those players shuffle their libraries."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "77"
        artist = "Marco Nelor"
        flavorText = "Now you see it, now you're gone."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7fc30e31-4796-4e98-992c-a56cd51ad3c9.jpg?1783940500"
        ruling("2012-07-01", "You can activate Void Stalker's ability targeting a creature you control, including Void Stalker itself. In that case, only you will shuffle your library.")
        ruling("2012-07-01", "If the target creature is an illegal target when the ability tries to resolve, it won't resolve and none of its effects will happen. No creature will leave the battlefield and no library will be shuffled.")
        ruling("2012-07-01", "If Void Stalker leaves the battlefield before its ability resolves, the target creature will be shuffled into its owner's library but Void Stalker will remain wherever it is.")
    }
}
