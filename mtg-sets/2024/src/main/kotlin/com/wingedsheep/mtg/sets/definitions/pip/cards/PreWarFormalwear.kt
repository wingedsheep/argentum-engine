package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pre-War Formalwear — Fallout #21
 * {2}{W} · Artifact — Equipment
 *
 * When this Equipment enters, return target creature card with mana value 3 or less from your
 * graveyard to the battlefield and attach this Equipment to it.
 * Equipped creature gets +2/+2 and has vigilance.
 * Equip {3}
 *
 * The returned card keeps its entity id across the zone change (as Scythe of the Wretched relies
 * on), so the attach step reads the same target. It uses `AttachTargetEquipmentToCreature` rather
 * than `AttachEquipment` because that executor checks `AttachmentMover.canAttach`: both the
 * Equipment and the returned permanent must be on the battlefield and the host must be a creature.
 * So if the Formalwear left the battlefield before the trigger resolved, the creature still returns
 * and nothing is attached; if the returned permanent isn't a creature, the Formalwear stays
 * unattached (the 2024-03-08 ruling).
 */
val PreWarFormalwear = card("Pre-War Formalwear") {
    manaCost = "{2}{W}"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, return target creature card with mana value 3 or less " +
        "from your graveyard to the battlefield and attach this Equipment to it.\n" +
        "Equipped creature gets +2/+2 and has vigilance.\n" +
        "Equip {3}"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.ownedByYou().manaValueAtMost(3), zone = Zone.GRAVEYARD)
        )
        effect = Effects.PutOntoBattlefieldFromGraveyard(creature) then
            Effects.AttachTargetEquipmentToCreature(EffectTarget.Self, creature)
        description = "When this Equipment enters, return target creature card with mana value 3 or " +
            "less from your graveyard to the battlefield and attach this Equipment to it."
    }

    staticAbility {
        ability = ModifyStats(2, 2, Filters.EquippedCreature)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, Filters.EquippedCreature)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "21"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19018f23-b63b-45af-8419-8959f41472d4.jpg?1783912416"
        ruling(
            "2024-03-08",
            "If Pre-War Formalwear can't be attached to the permanent returned to the battlefield by its " +
                "first ability (perhaps because it's not currently a creature), Pre-War Formalwear will " +
                "remain on the battlefield unattached."
        )
    }
}
