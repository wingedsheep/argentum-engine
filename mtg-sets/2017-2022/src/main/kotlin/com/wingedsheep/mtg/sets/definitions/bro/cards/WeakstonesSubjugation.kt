package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Weakstone's Subjugation
 * {U}
 * Enchantment — Aura
 * Enchant artifact or creature
 * When this Aura enters, you may pay {3}. If you do, tap enchanted permanent.
 * Enchanted permanent doesn't untap during its controller's untap step.
 *
 * Stop Cold's shape with the enter-tap gated on an optional {3} payment.
 */
val WeakstonesSubjugation = card("Weakstone's Subjugation") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact or creature\n" +
        "When this Aura enters, you may pay {3}. If you do, tap enchanted permanent.\n" +
        "Enchanted permanent doesn't untap during its controller's untap step."

    auraTarget = TargetObject(filter = TargetFilter.CreatureOrArtifact)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.MayPay(ManaCost.parse("{3}"), Effects.Tap(EffectTarget.EnchantedPermanent))
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef93ac79-8575-40f8-a222-63c2ffb30f60.jpg?1783920103"
        ruling(
            "2022-10-14",
            "The enchanted permanent doesn't untap during its controller's untap step even if you didn't pay {3}."
        )
    }
}
