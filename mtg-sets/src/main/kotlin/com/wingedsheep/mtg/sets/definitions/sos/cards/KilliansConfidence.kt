package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.EventPattern.OneOrMoreDealCombatDamageToPlayerEvent
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Killian's Confidence
 * {W}{B}
 * Sorcery
 * Target creature gets +1/+1 until end of turn. Draw a card.
 * Whenever one or more creatures you control deal combat damage to a player, you may pay {W/B}.
 * If you do, return this card from your graveyard to your hand.
 *
 * The recursion ability functions only while this card is in the graveyard
 * ([activeZone] = [Zone.GRAVEYARD], CR 113.6). The optional hybrid payment is modeled with
 * [MayPayManaEffect]; on payment the card returns itself ([EffectTarget.Self]) to its owner's hand.
 */
val KilliansConfidence = card("Killian's Confidence") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "Target creature gets +1/+1 until end of turn. Draw a card.\n" +
        "Whenever one or more creatures you control deal combat damage to a player, you may pay " +
        "{W/B}. If you do, return this card from your graveyard to your hand."

    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = Effects.ModifyStats(1, 1, t) then Effects.DrawCards(1)
    }

    triggeredAbility {
        triggerZone = Zone.GRAVEYARD
        trigger = TriggerSpec(
            OneOrMoreDealCombatDamageToPlayerEvent(
                sourceFilter = GameObjectFilter.Creature.youControl()
            ),
            TriggerBinding.ANY
        )
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{W/B}"),
            effect = Effects.Move(EffectTarget.Self, Zone.HAND)
        )
        description = "Whenever one or more creatures you control deal combat damage to a player, " +
            "you may pay {W/B}. If you do, return this card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "197"
        artist = "Jodie Muir"
        flavorText = "\"Don't hold back, because I certainly won't.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55ff776a-fc3b-4338-8864-d57a85b3f123.jpg?1775938369"
    }
}
