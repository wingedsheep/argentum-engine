package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Relic Bind — Legends #71
 * {2}{U} · Enchantment — Aura
 *
 * Enchant artifact an opponent controls
 * Whenever enchanted artifact becomes tapped, choose one —
 * • This Aura deals 1 damage to target player or planeswalker.
 * • Target player gains 1 life.
 *
 * The cause-agnostic `becomesTapped()` attachment trigger fires on every tap of the enchanted
 * artifact (mana abilities included), as on Psychic Venom and Artifact Possession. The mode and its
 * target are chosen when the trigger is put on the stack.
 */
val RelicBind = card("Relic Bind") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant artifact an opponent controls\n" +
        "Whenever enchanted artifact becomes tapped, choose one —\n" +
        "• This Aura deals 1 damage to target player or planeswalker.\n" +
        "• Target player gains 1 life."
    auraTarget = TargetObject(filter = TargetFilter(GameObjectFilter.Artifact.opponentControls()))

    triggeredAbility {
        trigger = Triggers.attached.becomesTapped()
        effect = ModalEffect.chooseOne(
            mode("This Aura deals 1 damage to target player or planeswalker") {
                val victim = target(Targets.PlayerOrPlaneswalker)
                effect = Effects.DealDamage(1, victim)
            },
            mode("Target player gains 1 life") {
                val player = target(Targets.Player)
                effect = Effects.GainLife(1, player)
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9b07dc4-21ad-410b-8f8a-2b034253bfee.jpg?1783948073"
    }
}
