package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gixian Puppeteer
 * {3}{B}
 * Creature — Phyrexian Warlock
 * 4/3
 * Whenever you draw your second card each turn, each opponent loses 2 life and you gain 2 life.
 * When this creature dies, return another target creature card with mana value 3 or less from
 * your graveyard to the battlefield.
 */
val GixianPuppeteer = card("Gixian Puppeteer") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Warlock"
    power = 4
    toughness = 3
    oracleText = "Whenever you draw your second card each turn, each opponent loses 2 life and you gain 2 life.\n" +
        "When this creature dies, return another target creature card with mana value 3 or less from your " +
        "graveyard to the battlefield."

    triggeredAbility {
        trigger = Triggers.you.drawsNth(2)
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(2)
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        val t = target(
            TargetFilter(
                GameObjectFilter.Creature.ownedByYou().manaValueAtMost(3),
                zone = Zone.GRAVEYARD,
            ).other(),
        )
        effect = Effects.PutOntoBattlefield(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Bastien L. Deharme"
        flavorText = "To some, desecration. To Gix, decoration."
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2cc86e18-ffbc-4c5f-a694-922332b146cc.jpg?1783920087"
    }
}
