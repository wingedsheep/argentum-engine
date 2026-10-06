package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Farmstead — Limited Edition Alpha #19 (canonical printing)
 * {W}{W}{W} · Enchantment — Aura
 *
 * Enchant land
 * Enchanted land has "At the beginning of your upkeep, you may pay {W}{W}. If you do, you gain 1 life."
 *
 * The trigger is granted to the enchanted land (Relic Bane's shape), so "your" is the land's
 * controller, not the Aura's.
 */
val Farmstead = card("Farmstead") {
    manaCost = "{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "Enchanted land has \"At the beginning of your upkeep, you may pay {W}{W}. If you do, you gain 1 life.\""

    auraTarget = TargetObject(filter = TargetFilter.Land)

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.you.beginningOf(Step.UPKEEP),
                effect = Effects.MayPay(ManaCost.parse("{W}{W}"), Effects.GainLife(1)),
            ),
            filter = GroupFilter.attachedCreature(),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/3455b006-9ea5-4aef-8ad2-d0701eb0cacf.jpg?1783948714"
    }
}
