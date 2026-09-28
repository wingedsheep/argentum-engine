package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Zhang Liao, Hero of Hefei
 * {4}{B}{B}
 * Legendary Creature — Human Soldier
 * 3/3
 * Whenever Zhang Liao deals damage to an opponent, that opponent discards a card.
 */
val ZhangLiaoHeroOfHefei = card("Zhang Liao, Hero of Hefei") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Soldier"
    oracleText = "Whenever Zhang Liao deals damage to an opponent, that opponent discards a card."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.dealsDamage(Recipient.Opponent)
        effect = Effects.Discard(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Li Youliang"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f42a953-3073-4e74-9bb8-c8c5a45d1dfa.jpg?1783946110"
    }
}
