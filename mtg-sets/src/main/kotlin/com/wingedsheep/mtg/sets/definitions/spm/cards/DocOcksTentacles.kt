package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Doc Ock's Tentacles
 * {1}
 * Artifact — Equipment
 * Whenever a creature you control with mana value 5 or greater enters, you may attach this Equipment to it.
 * Equipped creature gets +4/+4.
 * Equip {5}
 */
val DocOcksTentacles = card("Doc Ock's Tentacles") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Whenever a creature you control with mana value 5 or greater enters, you may attach this Equipment to it.\nEquipped creature gets +4/+4.\nEquip {5}"
    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.manaValueAtLeast(5).youControl(),
            binding = TriggerBinding.ANY
        )
        optional = true
        effect = Effects.AttachEquipment(EffectTarget.TriggeringEntity)
    }
    staticAbility {
        ability = ModifyStats(4, 4)
    }
    equipAbility("{5}")
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "David Álvarez"
        flavorText = "Once powerful tools of science, Dr. Otto Octavius's mechanical arms became his most formidable weapons."
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fed9547c-9d0d-4e62-9639-887ed09231a2.jpg?1757377996"
    }
}
