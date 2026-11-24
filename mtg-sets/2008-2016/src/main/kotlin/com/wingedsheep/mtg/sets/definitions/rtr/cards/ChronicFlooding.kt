package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Chronic Flooding
 * {1}{U}
 * Enchantment — Aura
 * Enchant land
 * Whenever enchanted land becomes tapped, its controller mills three cards.
 *
 * The attachment trigger binds the enchanted land as the triggering entity, so "its controller"
 * is [EffectTarget.ControllerOfTriggeringEntity] (cf. Psychic Venom).
 */
val ChronicFlooding = card("Chronic Flooding") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\nWhenever enchanted land becomes tapped, its controller mills three cards."
    auraTarget = TargetObject(filter = TargetFilter.Land)

    triggeredAbility {
        trigger = Triggers.attached.becomesTapped()
        effect = Patterns.Library.mill(3, EffectTarget.ControllerOfTriggeringEntity)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "32"
        artist = "Scott Chou"
        flavorText = "With the Izzet occupied elsewhere, many of their public works fell into disrepair."
        imageUri = "https://cards.scryfall.io/normal/front/1/a/1a757425-3cf2-4aca-b415-5ec2d5f753fe.jpg?1783940370"
        ruling("2012-10-01", "Chronic Flooding's ability will trigger whenever the enchanted land becomes tapped for any reason, not just because its controller taps it for mana.")
        ruling("2012-10-01", "Chronic Flooding becoming attached to a tapped land won't cause its ability to immediately trigger. That land must go from untapped to tapped.")
    }
}
