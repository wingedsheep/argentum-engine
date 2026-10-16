package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Goaded
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Acquired Mutation
 * {2}{R}
 * Enchantment — Aura
 *
 * Enchant creature
 * Enchanted creature gets +2/+2 and is goaded.
 * Whenever enchanted creature attacks, defending player gets two rad counters.
 *
 * [Goaded] scoped to the attached creature makes the Aura's controller its goader for as long as
 * the Aura stays on it (Martial Impetus shape). The attack trigger is the Aura's own, bound to the
 * enchanted creature; "defending player" resolves from that attacker's combat assignment.
 */
val AcquiredMutation = card("Acquired Mutation") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+2 and is goaded. (It attacks each combat if able and attacks a " +
        "player other than you if able.)\n" +
        "Whenever enchanted creature attacks, defending player gets two rad counters."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 2)
    }

    staticAbility {
        ability = Goaded()
    }

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.AddCounters(CounterType.RAD, 2, EffectTarget.PlayerRef(Player.DefendingPlayer))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "53"
        artist = "Xavier Ribeiro"
        flavorText = "The radiation of the wasteland has changed you!"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19586946-a6a6-4154-a544-15a052483f96.jpg?1783912408"
        ruling("2024-03-08", "Being goaded isn't an ability the creature has. Once it's been goaded, it must attack as detailed above even if it loses all abilities.")
        ruling("2024-03-08", "Attacking with a goaded creature doesn't cause it to stop being goaded. If there is an additional combat phase that turn, or if another player gains control of it before it stops being goaded, it must attack again if able.")
    }
}
