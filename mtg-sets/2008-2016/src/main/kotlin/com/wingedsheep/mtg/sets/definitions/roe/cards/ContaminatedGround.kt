package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SetEnchantedLandType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Contaminated Ground
 * {1}{B}
 * Enchantment — Aura
 * Enchant land
 * Enchanted land is a Swamp.
 * Whenever enchanted land becomes tapped, its controller loses 2 life.
 *
 * Evil Presence's land-type overwrite plus Psychic Venom's cause-agnostic attached tap trigger; the
 * trigger binds the enchanted land as the triggering entity, so "its controller" is
 * [EffectTarget.ControllerOfTriggeringEntity].
 */
val ContaminatedGround = card("Contaminated Ground") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\nEnchanted land is a Swamp.\nWhenever enchanted land becomes tapped, its controller loses 2 life."
    auraTarget = TargetObject(filter = TargetFilter.Land)

    staticAbility {
        ability = SetEnchantedLandType("Swamp")
    }

    triggeredAbility {
        trigger = Triggers.attached.becomesTapped()
        effect = Effects.LoseLife(2, EffectTarget.ControllerOfTriggeringEntity)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Rob Alexander"
        flavorText = "\"The Na Plateau is lost. Do not ask what happened, and never return there.\"\n—Nirthu, lone missionary"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d2ba8f3-58f5-43e5-9201-974ba58f56f8.jpg?1783941986"
        ruling(
            "2010-06-15",
            "The enchanted land loses its existing land types and any abilities printed on it. It now has the land type Swamp and has the ability to tap to add {B}. Contaminated Ground doesn't change the enchanted land's name or whether it's legendary, basic, or snow.",
        )
        ruling(
            "2010-06-15",
            "Contaminated Ground's last ability triggers whenever the enchanted land becomes tapped for any reason, not just when it's tapped for mana.",
        )
        ruling(
            "2010-06-15",
            "If, while casting a spell or activating an ability, the enchanted land's controller taps the land for mana to pay for it, Contaminated Ground's ability triggers and goes on the stack on top of that spell or ability. Contaminated Ground's ability will resolve first.",
        )
        ruling(
            "2010-06-15",
            "On the other hand, the enchanted land's controller may tap the land for mana, let Contaminated Ground's ability trigger and go on the stack, then spend that mana to cast an instant or activate an ability in response. In that case, that instant or ability will resolve first.",
        )
    }
}
