package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Relic Axe — Zendikar Rising #248
 * {2} · Artifact — Equipment
 *
 * When this Equipment enters, attach it to target creature you control.
 * Equipped creature gets +1/+1. If it's a Warrior, it gets +2/+1 instead.
 * Equip {2}
 *
 * The "instead" is two mutually exclusive static bonuses keyed on whether the equipped creature is
 * (in projected state) a Warrior, so a creature that becomes or stops being a Warrior switches over.
 */
val RelicAxe = card("Relic Axe") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, attach it to target creature you control.\n" +
        "Equipped creature gets +1/+1. If it's a Warrior, it gets +2/+1 instead.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.youControl())
        effect = Effects.AttachEquipment(t)
    }

    val equippedIsWarrior = Conditions.EntityMatches(
        EffectTarget.EquippedCreature,
        GameObjectFilter.Creature.withSubtype(Subtype.WARRIOR),
    )
    staticAbility {
        condition = Conditions.Not(equippedIsWarrior)
        ability = ModifyStats(1, 1, Filters.EquippedCreature)
    }
    staticAbility {
        condition = equippedIsWarrior
        ability = ModifyStats(2, 1, Filters.EquippedCreature)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "248"
        artist = "Cristi Balanescu"
        flavorText = "\"It's light as a feather, but it hits with the force of a boulder. A very sharp boulder.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca8705e3-0e44-45c9-bcdf-c24507b038fe.jpg?1783929314"
        ruling("2020-09-25", "Attaching an Equipment with its enters-the-battlefield triggered ability isn't the same as using its equip ability. You don't pay mana for the attachment, and if the Equipment enters at a time you couldn't cast a sorcery, you can still attach it to a creature you control.")
        ruling("2020-09-25", "If the target creature becomes an illegal target, the Equipment remains on the battlefield unattached.")
    }
}
