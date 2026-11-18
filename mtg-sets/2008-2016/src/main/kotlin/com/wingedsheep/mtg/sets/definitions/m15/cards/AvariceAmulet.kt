package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Avarice Amulet
 * {4}
 * Artifact — Equipment
 *
 * Equipped creature gets +2/+0 and has vigilance and "At the beginning of your upkeep, draw a card."
 * Whenever equipped creature dies, target opponent gains control of this Equipment.
 * Equip {2}
 *
 * The quoted upkeep trigger is granted to the equipped creature, so "your upkeep" and the draw belong
 * to the creature's controller (the Farmstead / Reaver Cleaver shape). The dies trigger is the
 * Equipment's own (Skullclamp's `Triggers.attached.dies()`), and the control hand-off is Jinxed Idol's
 * `GiveControl` of the source to a target opponent.
 */
val AvariceAmulet = card("Avarice Amulet") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+0 and has vigilance and \"At the beginning of your upkeep, " +
        "draw a card.\"\n" +
        "Whenever equipped creature dies, target opponent gains control of this Equipment.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(2, 0, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.you.beginningOf(Step.UPKEEP),
                effect = Effects.DrawCards(1),
                descriptionOverride = "At the beginning of your upkeep, draw a card."
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        val opponent = target(Targets.Opponent)
        effect = Effects.GiveControl(
            permanent = EffectTarget.Self,
            newController = opponent
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "212"
        artist = "Steven Belledin"
        flavorText = "Designed by Penny Arcade"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec56727f-6280-4625-96b9-9b599af0dada.jpg?1783939159"
    }
}
