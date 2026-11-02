package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Surge Mare
 * {U}{U}
 * Creature — Horse Fish
 * 0/5
 * This creature can't be blocked by green creatures.
 * Whenever this creature deals damage to an opponent, you may draw a card. If you do, discard a card.
 * {1}{U}: This creature gets +2/-2 until end of turn.
 */
val SurgeMare = card("Surge Mare") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Horse Fish"
    power = 0
    toughness = 5
    oracleText = "This creature can't be blocked by green creatures.\n" +
        "Whenever this creature deals damage to an opponent, you may draw a card. If you do, discard a card.\n" +
        "{1}{U}: This creature gets +2/-2 until end of turn."

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.withColor(Color.GREEN))
    }

    triggeredAbility {
        trigger = Triggers.self.dealsDamage(Recipient.Opponent)
        effect = Effects.May(Patterns.Hand.loot())
    }

    activatedAbility {
        cost = Costs.Mana("{1}{U}")
        effect = Effects.ModifyStats(2, -2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "77"
        artist = "Sam Rowan"
        flavorText = "When it passes, waves touch the sky."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/9002d0f1-ff2c-4d1c-a7db-3252ef6bebbd.jpg?1783934580"
    }
}
