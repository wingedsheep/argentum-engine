package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ayara, First of Locthwain
 * {B}{B}{B}
 * Legendary Creature — Elf Noble
 * 2/3
 * Whenever Ayara or another black creature you control enters, each opponent loses 1 life and you
 * gain 1 life.
 * {T}, Sacrifice another black creature: Draw a card.
 *
 * Two triggers: Ayara herself triggers whatever her color; others must be black. The gain is a
 * fixed 1, not "that much" — in multiplayer each opponent loses 1 and you gain 1 once.
 */
val AyaraFirstOfLocthwain = card("Ayara, First of Locthwain") {
    manaCost = "{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Elf Noble"
    power = 2
    toughness = 3
    oracleText = "Whenever Ayara or another black creature you control enters, each opponent loses 1 life " +
        "and you gain 1 life.\n{T}, Sacrifice another black creature: Draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
        description = "Whenever Ayara or another black creature you control enters, each opponent loses " +
            "1 life and you gain 1 life."
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.withColor(Color.BLACK).youControl()).enters()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
        description = "Whenever Ayara or another black creature you control enters, each opponent loses " +
            "1 life and you gain 1 life."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.SacrificeAnother(GameObjectFilter.Creature.withColor(Color.BLACK)),
        )
        effect = Effects.DrawCards(1)
        description = "{T}, Sacrifice another black creature: Draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Ryan Pancoast"
        flavorText = "Mourning shifts seamlessly to celebration as she chooses her next suitor."
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed0ace28-9a33-4f0d-b8c8-f5517f20ccf1.jpg?1783932644"
        ruling("2019-10-04", "In a Two-Headed Giant game, Ayara's first ability causes the opposing team to lose 1 life twice, and you gain 1 life once.")
    }
}
