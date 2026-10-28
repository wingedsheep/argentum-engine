package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Type-2 Phaser
 * {2}
 * Artifact — Equipment
 *
 * When this Equipment enters, tap target creature an opponent controls and put a stun counter on it. (If a permanent with a stun counter would become untapped, remove one from it instead.)
 * Equipped creature gets +3/+0.
 * Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)
 */
val Type2Phaser = card("Type-2 Phaser") {
    manaCost = "{2}"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, tap target creature an opponent controls and put a stun counter on it. (If a permanent with a stun counter would become untapped, remove one from it instead.)\nEquipped creature gets +3/+0.\nEquip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"
    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature.opponentControls())
        effect = Effects.Tap(creature) then Effects.AddCounters(CounterType.STUN, 1, creature)
    }
    equipAbility("{3}")
    staticAbility { ability = ModifyStats(3, 0) }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "271"
        artist = "Wero Gallo"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c704af22-7c42-45c7-b3a4-b414c4dd8e05.jpg?1792757435"
    }
}

