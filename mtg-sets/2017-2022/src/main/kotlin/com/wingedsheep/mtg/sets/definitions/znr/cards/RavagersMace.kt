package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ravager's Mace
 * {1}{B}{R}
 * Artifact — Equipment
 * When this Equipment enters, attach it to target creature you control.
 * Equipped creature gets +1/+0 for each creature in your party and has menace.
 * Equip {2}{B}{R}
 *
 * The bonus is a continuous [GrantDynamicStats] reading [DynamicAmounts.partySize] for the
 * Mace's controller, so it is re-counted whenever the party changes (Bearded Axe's shape).
 */
val RavagersMace = card("Ravager's Mace") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, attach it to target creature you control.\n" +
        "Equipped creature gets +1/+0 for each creature in your party and has menace. " +
        "(Your party consists of up to one each of Cleric, Rogue, Warrior, and Wizard.)\n" +
        "Equip {2}{B}{R}"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.youControl())
        effect = Effects.AttachEquipment(t)
    }

    staticAbility {
        ability = GrantDynamicStats(
            filter = Filters.EquippedCreature,
            powerBonus = DynamicAmounts.partySize(),
            toughnessBonus = DynamicAmounts.fixed(0),
        )
    }

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE)
    }

    equipAbility("{2}{B}{R}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "235"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af484140-57d6-4505-9960-5e7b0306cc9b.jpg?1783929320"
        ruling("2020-09-25", "Attaching an Equipment with its enters-the-battlefield triggered ability isn't the same as using its equip ability. You don't pay mana for the attachment, and if the Equipment enters at a time you couldn't cast a sorcery, you can still attach it to a creature you control.")
        ruling("2020-09-25", "If the target creature becomes an illegal target, the Equipment remains on the battlefield unattached.")
        ruling("2020-09-25", "If a creature has more than one party creature type, and there are multiple ways to count that creature that could result in a different number of creatures in your party, the highest such number is used. For example, if you control a Cleric and a Cleric Wizard, the number of creatures in your party is two.")
    }
}
