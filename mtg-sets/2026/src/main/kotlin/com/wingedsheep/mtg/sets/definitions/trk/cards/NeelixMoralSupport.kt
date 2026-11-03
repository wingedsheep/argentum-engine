package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Neelix, Moral Support
 * {1}{G}
 * Legendary Creature — Talaxian Advisor
 * 1/1
 * When Neelix enters, create a Food token.
 * Whenever you gain life, put a +1/+1 counter on Neelix.
 */
val NeelixMoralSupport = card("Neelix, Moral Support") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Talaxian Advisor"
    power = 1
    toughness = 1
    oracleText = "When Neelix enters, create a Food token. " +
        "(It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "Whenever you gain life, put a +1/+1 counter on Neelix."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateFood()
    }

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "209"
        artist = "JB Casacop"
        flavorText = "\"I'm just a cook who sometimes imagines himself to be a diplomat.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09c0daa9-9eca-4817-a427-868d1efcfa20.jpg?1793359554"
    }
}
