package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Inventor's Axe
 * {R}
 * Artifact — Equipment
 * Flash
 * When this Equipment enters, you get {E}{E} (two energy counters).
 * When this Equipment enters, attach it to target creature you control.
 * Equipped creature gets +2/+0.
 * Equip—Pay {E}{E}.
 *
 * The equip cost is a non-mana cost, so it is an equip-flagged, sorcery-speed activated ability
 * (CR 702.6a) whose cost pays two energy counters, rather than `equipAbility("{…}")`. The two
 * enters triggers are separate abilities, as printed: the energy arrives even when there is no
 * creature to attach to.
 */
val InventorsAxe = card("Inventor's Axe") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "Flash\n" +
        "When this Equipment enters, you get {E}{E} (two energy counters).\n" +
        "When this Equipment enters, attach it to target creature you control.\n" +
        "Equipped creature gets +2/+0.\n" +
        "Equip—Pay {E}{E}."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GetEnergy(2)
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AttachEquipment(creature)
    }

    staticAbility {
        ability = ModifyStats(2, 0, Filters.EquippedCreature)
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 2)
        isEquipAbility = true
        timing = TimingRule.SorcerySpeed
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AttachEquipment(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "Filipe Pagliuso"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b2f918c-bd02-4a63-b4c2-ca204c77b208.jpg?1783911269"
    }
}
