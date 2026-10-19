package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Enormous Energy Blade — Kamigawa: Neon Dynasty #96 (canonical printing)
 * {2}{B} · Artifact — Equipment
 *
 * Equipped creature gets +4/+0.
 * Whenever this Equipment becomes attached to a creature, tap that creature.
 * Equip {2}
 *
 * "That creature" is the new host, read off the attach event via
 * [EffectTarget.AttachedToTriggeringPermanent].
 */
val EnormousEnergyBlade = card("Enormous Energy Blade") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +4/+0.\n" +
        "Whenever this Equipment becomes attached to a creature, tap that creature.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(4, 0, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.self.becomesAttached(to = GameObjectFilter.Creature)
        effect = Effects.Tap(EffectTarget.AttachedToTriggeringPermanent)
        description = "Whenever this Equipment becomes attached to a creature, tap that creature."
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Campbell White"
        flavorText = "\"Heavy? Maybe for you.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/9/991370bd-a96a-4ed0-bdf9-f03b98191abf.jpg?1783923888"
    }
}
