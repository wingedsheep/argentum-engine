package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Embercleave
 * {4}{R}{R}
 * Legendary Artifact — Equipment
 *
 * Flash
 * This spell costs {1} less to cast for each attacking creature you control.
 * When Embercleave enters, attach it to target creature you control.
 * Equipped creature gets +1/+1 and has double strike and trample.
 * Equip {3}
 */
val Embercleave = card("Embercleave") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Flash\n" +
        "This spell costs {1} less to cast for each attacking creature you control.\n" +
        "When Embercleave enters, attach it to target creature you control.\n" +
        "Equipped creature gets +1/+1 and has double strike and trample.\n" +
        "Equip {3}"

    keywords(Keyword.FLASH)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.PermanentsYouControlMatching(GameObjectFilter.Creature.attacking())
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature.youControl())
        effect = Effects.AttachEquipment(t)
    }

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.DOUBLE_STRIKE)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "120"
        artist = "Joe Slucher"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aaae15dd-11b6-4421-99e9-365c7fe4a5d6.jpg?1783932625"
    }
}
