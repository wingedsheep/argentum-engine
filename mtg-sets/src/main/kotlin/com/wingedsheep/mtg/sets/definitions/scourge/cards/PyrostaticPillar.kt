package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.DealDamageToPlayersEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val PyrostaticPillar = card("Pyrostatic Pillar") {
    manaCost = "{1}{R}"
    typeLine = "Enchantment"
    oracleText = "Whenever a player casts a spell with mana value 3 or less, Pyrostatic Pillar deals 2 damage to that player."

    triggeredAbility {
        trigger = TriggerSpec(
            event = GameEvent.SpellCastEvent(player = Player.Each, manaValueAtMost = 3),
            binding = TriggerBinding.ANY
        )
        effect = DealDamageToPlayersEffect(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "100"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/5973cd53-f6cd-4edc-b952-f6d3eef97988.jpg?1562529485"
    }
}
