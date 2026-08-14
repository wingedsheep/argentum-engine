package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.gift
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ControlEnchantedPermanent
import com.wingedsheep.sdk.scripting.GiftKind
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kitnap
 * {2}{U}{U}
 * Enchantment — Aura
 *
 * Gift a card (You may promise an opponent a gift as you cast this spell.
 * If you do, when it enters, they draw a card.)
 *
 * Enchant creature
 * When this Aura enters, tap enchanted creature. If the gift wasn't promised,
 * put three stun counters on it.
 * You control enchanted creature.
 *
 * The gift is promised as you cast (CR 702.174a) — `gift(...)` adds both the additional cost and
 * the "when this permanent enters, if the gift was promised, the chosen player draws a card"
 * ability. The printed enters ability below reads the same promise back through
 * [Conditions.GiftWasPromised] for its "if the gift wasn't promised" rider.
 */
val Kitnap = card("Kitnap") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Gift a card (You may promise an opponent a gift as you cast this spell. If you do, when it enters, they draw a card.)\nEnchant creature\nWhen this Aura enters, tap enchanted creature. If the gift wasn't promised, put three stun counters on it.\nYou control enchanted creature."

    auraTarget = Targets.Creature

    gift(GiftKind.CARD)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
            .then(
                ConditionalEffect(
                    condition = Conditions.Not(Conditions.GiftWasPromised),
                    effect = Effects.AddCounters("STUN", 3, EffectTarget.EnchantedCreature)
                )
            )
    }

    staticAbility {
        ability = ControlEnchantedPermanent
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "53"
        artist = "Irina Nordsol"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/085be5d1-fd85-46d1-ad39-a8aa75a06a96.jpg?1721426110"

        ruling("2024-07-26", "As an additional cost to cast a spell with gift, you can promise the listed gift to an opponent. That opponent is chosen as part of that additional cost.")
        ruling("2024-07-26", "For permanent spells with gift, an ability triggers when that permanent enters if the gift was promised. When that ability resolves, the gift is given to the appropriate opponent.")
        ruling("2024-07-26", "If a spell for which the gift was promised is countered, doesn't resolve, or is otherwise removed from the stack, the gift won't be given.")
    }
}
