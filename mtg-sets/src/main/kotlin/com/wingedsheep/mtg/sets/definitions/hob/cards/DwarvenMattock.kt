package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Dwarven Mattock
 * {2}
 * Artifact — Equipment
 *
 * When this Equipment enters, attach it to target Dwarf you control.
 * Equipped creature gets +2/+2 and has ward {1}.
 * Equip {3}
 *
 * The Pirate's Cutlass shell: an enters trigger that targets a tribe member and auto-attaches, plus
 * the equipped-creature statics. Because the trigger *targets*, it is simply removed if no Dwarf you
 * control is a legal target when it would go on the stack (CR 603.3d) — the Equipment stays
 * unattached rather than grabbing an arbitrary creature.
 */
val DwarvenMattock = card("Dwarven Mattock") {
    manaCost = "{2}"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, attach it to target Dwarf you control.\n" +
        "Equipped creature gets +2/+2 and has ward {1}. (Whenever equipped creature becomes the " +
        "target of a spell or ability an opponent controls, counter it unless that player pays {1}.)\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val dwarf = target(
            "target Dwarf you control",
            TargetCreature(filter = TargetFilter.CreatureYouControl.withSubtype(Subtype.DWARF))
        )
        effect = Effects.AttachEquipment(dwarf)
    }

    staticAbility {
        ability = ModifyStats(2, 2, Filters.EquippedCreature)
    }
    staticAbility {
        ability = GrantWard(WardCost.Mana("{1}"), Filters.EquippedCreature)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "172"
        artist = "Nino Is"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92c6f09d-b525-4e8c-a87c-a74df9dc3b1e.jpg?1785412768"
    }
}
