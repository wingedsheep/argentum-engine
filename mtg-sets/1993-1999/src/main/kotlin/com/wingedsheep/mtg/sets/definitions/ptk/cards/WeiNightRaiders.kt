package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wei Night Raiders
 * {2}{B}{B}
 * Creature — Human Soldier
 * 2/2
 * Horsemanship
 * Whenever this creature deals damage to an opponent, that player discards a card.
 */
val WeiNightRaiders = card("Wei Night Raiders") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Horsemanship (This creature can't be blocked except by creatures with horsemanship.)\nWhenever this creature deals damage to an opponent, that player discards a card."

    keywords(Keyword.HORSEMANSHIP)

    triggeredAbility {
        trigger = Triggers.self.dealsDamage(Recipient.Opponent)
        effect = Patterns.Hand.discardCards(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "89"
        artist = "Wang Feng"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5a0292e-ecf4-42df-8abf-f4479f8ad6cb.jpg?1783946112"
    }
}
