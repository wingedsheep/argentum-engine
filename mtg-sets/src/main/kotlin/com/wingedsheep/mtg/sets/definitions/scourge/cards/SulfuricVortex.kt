package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageToPlayersEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.PreventLifeGain

val SulfuricVortex = card("Sulfuric Vortex") {
    manaCost = "{1}{R}{R}"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each player's upkeep, Sulfuric Vortex deals 2 damage to that player.\nIf a player would gain life, that player gains no life instead."

    triggeredAbility {
        trigger = Triggers.EachUpkeep
        effect = DealDamageToPlayersEffect(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    replacementEffect(PreventLifeGain(appliesTo = GameEvent.LifeGainEvent(player = Player.Each)))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "106"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/large/front/7/9/79955e27-eef7-43bd-9895-e9209ed1537f.jpg?1562531138"
    }
}
