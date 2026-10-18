package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kami of Transience
 * {1}{G}
 * Creature — Spirit
 * 2/2
 * Trample
 * Whenever you cast an enchantment spell, put a +1/+1 counter on this creature.
 * At the beginning of each end step, if an enchantment was put into your graveyard from the
 * battlefield this turn, you may return this card from your graveyard to your hand.
 *
 * The end-step ability functions only from the graveyard (`triggerZone = GRAVEYARD`); its
 * intervening-if reads the owner-keyed turn history, so the enchantment need not still be there.
 */
val KamiOfTransience = card("Kami of Transience") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 2
    oracleText = "Trample\n" +
        "Whenever you cast an enchantment spell, put a +1/+1 counter on this creature.\n" +
        "At the beginning of each end step, if an enchantment was put into your graveyard from the " +
        "battlefield this turn, you may return this card from your graveyard to your hand."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Enchantment)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        triggerZone = Zone.GRAVEYARD
        interveningIf = Conditions.PermanentTypePutIntoGraveyardFromBattlefieldThisTurn(CardType.ENCHANTMENT)
        effect = Effects.May(Effects.Move(target = EffectTarget.Self, destination = Zone.HAND))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "197"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d038a59-e589-45f4-b5ef-6d2f9875d560.jpg?1783923846"
    }
}
