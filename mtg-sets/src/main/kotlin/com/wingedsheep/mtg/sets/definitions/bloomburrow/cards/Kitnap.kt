package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ControlEnchantedPermanent
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.references.Player
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
 * Gift is modeled as a modal choice on the ETB trigger.
 * Mode 1 (no gift): tap enchanted + 3 stun counters.
 * Mode 2 (gift): opponent draws + tap enchanted (no stun counters).
 */
val Kitnap = card("Kitnap") {
    manaCost = "{2}{U}{U}"
    typeLine = "Enchantment — Aura"
    oracleText = "Gift a card (You may promise an opponent a gift as you cast this spell. If you do, when it enters, they draw a card.)\nEnchant creature\nWhen this Aura enters, tap enchanted creature. If the gift wasn't promised, put three stun counters on it.\nYou control enchanted creature."

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ModalEffect.chooseOne(
            // Mode 1: No gift — tap enchanted creature + 3 stun counters
            Mode.noTarget(
                Effects.Tap(EffectTarget.EnchantedCreature)
                    .then(Effects.AddCounters("STUN", 3, EffectTarget.EnchantedCreature)),
                "Don't promise a gift — tap enchanted creature and put three stun counters on it"
            ),
            // Mode 2: Gift a card — opponent draws a card, tap enchanted creature (no stun counters)
            Mode.noTarget(
                DrawCardsEffect(1, EffectTarget.PlayerRef(Player.EachOpponent))
                    .then(Effects.Tap(EffectTarget.EnchantedCreature))
                    .then(Effects.GiftGiven()),
                "Promise a gift — an opponent draws a card, tap enchanted creature"
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
