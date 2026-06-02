package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Illvoi Light Jammer
 * {1}{U}
 * Artifact — Equipment
 *
 * Flash
 * When this Equipment enters, attach it to target creature you control. That creature gains hexproof until end of turn. (It can't be the target of spells or abilities your opponents control.)
 * Equipped creature gets +1/+2.
 * Equip {3}
 */
val IllvoiLightJammer = card("Illvoi Light Jammer") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Equipment"
    oracleText = "Flash\nWhen this Equipment enters, attach it to target creature you control. That creature gains hexproof until end of turn. (It can't be the target of spells or abilities your opponents control.)\nEquipped creature gets +1/+2.\nEquip {3}"

    keywords(Keyword.FLASH)

    // ETB: attach to target creature you control, grant hexproof until end of turn
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = Effects.AttachEquipment(creature)
            .then(Effects.GrantKeyword(Keyword.HEXPROOF, creature))
    }

    // Equipped creature gets +1/+2
    staticAbility {
        ability = ModifyStats(+1, +2, Filters.EquippedCreature)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "David Álvarez"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/efb3d961-543c-4404-b4ed-1cb28ee411b3.jpg?1752946786"
    }
}
