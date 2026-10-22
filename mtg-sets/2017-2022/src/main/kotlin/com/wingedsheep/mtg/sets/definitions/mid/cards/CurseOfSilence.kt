package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Curse of Silence
 * {W}
 * Enchantment — Aura Curse
 * Enchant player
 * As this Aura enters, choose a card name.
 * Spells with the chosen name enchanted player casts cost {2} more to cast.
 * Whenever enchanted player casts a spell with the chosen name, you may sacrifice this Aura. If you
 * do, draw a card.
 *
 * The tax and the trigger are both scoped to the *enchanted* player, not to the Curse's controller's
 * opponents: [SpellCostTarget.PlayerCasts] over [Player.EnchantedPlayer], and a cast trigger keyed to
 * the same player. The name is the as-enters [EntersWithChoice] `CARD_NAME` slot both read through
 * `namedFromChosenComponent()` (Disruptor Flute's shape).
 */
val CurseOfSilence = card("Curse of Silence") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\n" +
        "As this Aura enters, choose a card name.\n" +
        "Spells with the chosen name enchanted player casts cost {2} more to cast.\n" +
        "Whenever enchanted player casts a spell with the chosen name, you may sacrifice this Aura. " +
        "If you do, draw a card."

    auraTarget = Targets.Player

    // As this Aura enters, choose a card name.
    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.CARD_NAME,
            cardNamePool = CardNamePool.ANY,
        )
    )

    // Spells with the chosen name enchanted player casts cost {2} more to cast.
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.PlayerCasts(
                player = Player.EnchantedPlayer,
                filter = GameObjectFilter.Any.namedFromChosenComponent(),
            ),
            modification = CostModification.IncreaseGeneric(2),
        )
    }

    // Whenever enchanted player casts a spell with the chosen name, you may sacrifice this Aura.
    // If you do, draw a card.
    triggeredAbility {
        trigger = Triggers.player(Player.EnchantedPlayer).casts(GameObjectFilter.Any.namedFromChosenComponent())
        effect = Effects.MayPay(
            cost = SacrificeSelfEffect,
            then = Effects.DrawCards(1),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Irina Nordsol"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e7819d5-12a9-4719-91b2-8dd1ba6f3800.jpg?1783925660"
    }
}
