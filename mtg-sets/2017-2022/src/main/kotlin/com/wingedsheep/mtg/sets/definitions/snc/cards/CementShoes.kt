package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cement Shoes
 * {1}
 * Artifact — Equipment
 * Equipped creature gets +3/+3 and has "At the beginning of your end step, tap this creature."
 * Equipped creature doesn't untap during its controller's untap step.
 * Equip {2}
 *
 * The end-step trigger is granted to the equipped creature, so "your" is that creature's
 * controller and "this creature" is [EffectTarget.Self] of the granted ability.
 */
val CementShoes = card("Cement Shoes") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +3/+3 and has \"At the beginning of your end step, tap this creature.\"\n" +
        "Equipped creature doesn't untap during its controller's untap step.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(3, 3)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.you.beginningOf(Step.END),
                effect = Effects.Tap(EffectTarget.Self),
                descriptionOverride = "At the beginning of your end step, tap this creature."
            )
        )
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "235"
        artist = "Mark Behm"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1100dba-de2d-481e-b29c-71f6d34663ba.jpg"
    }
}
