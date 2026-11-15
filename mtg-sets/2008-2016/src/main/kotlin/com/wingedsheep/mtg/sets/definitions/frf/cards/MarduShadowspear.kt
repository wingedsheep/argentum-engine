package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mardu Shadowspear
 * {B}
 * Creature — Human Warrior
 * 1/1
 * Whenever this creature attacks, each opponent loses 1 life.
 * Dash {1}{B} (You may cast this spell for its dash cost. If you do, it gains haste, and it's returned from the battlefield to its owner's hand at the beginning of the next end step.)
 */
val MarduShadowspear = card("Mardu Shadowspear") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warrior"
    power = 1
    toughness = 1
    oracleText = "Whenever this creature attacks, each opponent loses 1 life.\n" +
        "Dash {1}{B} (You may cast this spell for its dash cost. If you do, it gains haste, and it's returned from the battlefield to its owner's hand at the beginning of the next end step.)"

    dash = "{1}{B}"

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1f55b50-b3b1-4336-914c-1cc2669b8fac.jpg"
    }
}
