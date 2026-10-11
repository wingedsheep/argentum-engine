package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Poison-Tip Archer
 * {2}{B}{G}
 * Creature — Elf Archer
 * 2/3
 *
 * Reach
 * Deathtouch
 * Whenever another creature dies, each opponent loses 1 life.
 */
val PoisonTipArcher = card("Poison-Tip Archer") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Elf Archer"
    oracleText = "Reach (This creature can block creatures with flying.)\n" +
        "Deathtouch (Any amount of damage this deals to a creature is enough to destroy it.)\n" +
        "Whenever another creature dies, each opponent loses 1 life."
    power = 2
    toughness = 3

    keywords(Keyword.REACH, Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature).dies()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "220"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e058ff8-043c-498b-8310-0ca45466ac27.jpg"
    }
}
