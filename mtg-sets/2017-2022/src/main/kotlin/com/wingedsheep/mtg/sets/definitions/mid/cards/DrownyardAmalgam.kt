package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Drownyard Amalgam (Innistrad: Midnight Hunt #50)
 * {4}{U}
 * Creature — Zombie Horror
 * 3/6
 * When this creature enters, target player mills three cards.
 * {2}{U}: This creature can't be blocked this turn.
 */
val DrownyardAmalgam = card("Drownyard Amalgam") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Horror"
    oracleText = "When this creature enters, target player mills three cards. " +
        "(They put the top three cards of their library into their graveyard.)\n" +
        "{2}{U}: This creature can't be blocked this turn."
    power = 3
    toughness = 6

    triggeredAbility {
        trigger = Triggers.self.enters()
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(3, player)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{U}")
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "50"
        artist = "Alex Brock"
        flavorText = "When human corpses are in short supply, stitchers get creative."
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71b05d44-d987-45e1-bbd9-842dbe58c2b5.jpg"
    }
}
