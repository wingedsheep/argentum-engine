package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blade Juggler
 * {4}{B}
 * Creature — Human Rogue
 * 3/2
 * Spectacle {2}{B} (You may cast this spell for its spectacle cost rather than its mana cost if an
 * opponent lost life this turn.)
 * When this creature enters, it deals 1 damage to you and you draw a card.
 *
 * Spectacle is a [SelfAlternativeCost] gated on [Conditions.OpponentLostLifeThisTurn], the same
 * shape as Light Up the Stage.
 */
val BladeJuggler = card("Blade Juggler") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    oracleText = "Spectacle {2}{B} (You may cast this spell for its spectacle cost rather than its mana cost " +
        "if an opponent lost life this turn.)\n" +
        "When this creature enters, it deals 1 damage to you and you draw a card."
    power = 3
    toughness = 2

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{2}{B}"),
        condition = Conditions.OpponentLostLifeThisTurn,
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You)) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/0/9/099bc474-e656-4167-b105-3a3a36c0b23e.jpg?1783933699"
    }
}
