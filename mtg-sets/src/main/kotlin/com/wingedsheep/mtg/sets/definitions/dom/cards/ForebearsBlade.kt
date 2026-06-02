package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggerBinding

/**
 * Forebear's Blade
 * {3}
 * Artifact — Equipment
 * Equipped creature gets +3/+0 and has vigilance and trample.
 * Whenever equipped creature dies, attach Forebear's Blade to target creature you control.
 * Equip {3}
 */
val ForebearsBlade = card("Forebear's Blade") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +3/+0 and has vigilance and trample.\n" +
        "Whenever equipped creature dies, attach Forebear's Blade to target creature you control.\n" +
        "Equip {3}"

    staticAbility {
        ability = ModifyStats(+3, +0, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(to = Zone.GRAVEYARD, binding = TriggerBinding.ATTACHED)
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = Effects.AttachEquipment(creature)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "214"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52212fd5-551e-4bc1-9dac-6361e27c27ad.jpg?1681500928"
    }
}
