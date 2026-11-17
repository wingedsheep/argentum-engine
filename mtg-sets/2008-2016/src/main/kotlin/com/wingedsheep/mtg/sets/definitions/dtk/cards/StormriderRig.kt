package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stormrider Rig — Dragons of Tarkir #245
 * {2} · Artifact — Equipment
 *
 * Equipped creature gets +1/+1.
 * Whenever a creature you control enters, you may attach this Equipment to it.
 * Equip {2}
 */
val StormriderRig = card("Stormrider Rig") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1.\n" +
        "Whenever a creature you control enters, you may attach this Equipment to it.\n" +
        "Equip {2}"

    staticAbility {
        ability = ModifyStats(1, 1, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        optional = true
        effect = Effects.AttachEquipment(EffectTarget.TriggeringEntity)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "245"
        artist = "Min Yum"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/38b08344-780d-4bd7-882a-dcadeb40f159.jpg?1783938567"
        ruling(
            "2015-02-25",
            "If the creature that entered the battlefield is no longer on the battlefield as Stormrider Rig's " +
                "second ability resolves, Stormrider Rig doesn't move. This is also true if that creature can't " +
                "legally be equipped by Stormrider Rig."
        )
    }
}
