package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Archmage's Newt
 * {1}{U}
 * Creature — Salamander Mount
 * 2/2
 * Whenever this creature deals combat damage to a player, target instant or sorcery card in
 * your graveyard gains flashback until end of turn. The flashback cost is equal to its mana
 * cost. That card gains flashback {0} until end of turn instead if this creature is saddled.
 * Saddle 3
 *
 * The combat-damage trigger grants Flashback (CR 702.34) at runtime via [Effects.GrantFlashback],
 * whose cost defaults to the targeted card's own mana cost. A [ConditionalEffect] gated on
 * [Conditions.SourceIsSaddled] swaps in a fixed `{0}` flashback cost on the saddled branch. The
 * cast-from-graveyard enumerator, the cast handler, and the stack resolver's exile-on-resolution
 * clause honor the granted flashback exactly like a printed one through the shared
 * `FlashbackGrants` resolver; the grant expires during the cleanup step.
 */
val ArchmagesNewt = card("Archmage's Newt") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Salamander Mount"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature deals combat damage to a player, target instant or " +
        "sorcery card in your graveyard gains flashback until end of turn. The flashback cost is " +
        "equal to its mana cost. That card gains flashback {0} until end of turn instead if this " +
        "creature is saddled. (You may cast that card from your graveyard for its flashback cost. " +
        "Then exile it.)\nSaddle 3"

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        target = TargetObject(
            filter = TargetFilter.InstantOrSorceryInGraveyard.ownedByYou()
        )
        effect = ConditionalEffect(
            condition = Conditions.SourceIsSaddled,
            effect = Effects.GrantFlashback(EffectTarget.ContextTarget(0), cost = ManaCost.parse("{0}")),
            elseEffect = Effects.GrantFlashback(EffectTarget.ContextTarget(0))
        )
        description = "Whenever this creature deals combat damage to a player, target instant or " +
            "sorcery card in your graveyard gains flashback until end of turn. The flashback cost " +
            "is equal to its mana cost. That card gains flashback {0} until end of turn instead " +
            "if this creature is saddled."
    }

    keywordAbility(KeywordAbility.saddle(3))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "Edgar Sánchez Hidalgo"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a440bbd6-8e51-4db1-90d9-7fa9fc327ad5.jpg?1712355386"
    }
}
