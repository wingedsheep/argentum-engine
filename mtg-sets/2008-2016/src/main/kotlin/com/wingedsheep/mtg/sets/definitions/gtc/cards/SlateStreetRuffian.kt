package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Slate Street Ruffian
 * {2}{B}
 * Creature — Human Warrior
 * 2/2
 * Whenever this creature becomes blocked, defending player discards a card.
 *
 * The bare self `becomesBlocked()` fires once however many creatures block; the defending player is
 * the one the Ruffian is attacking (Abyssal Nightstalker's discard shape).
 */
val SlateStreetRuffian = card("Slate Street Ruffian") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warrior"
    oracleText = "Whenever this creature becomes blocked, defending player discards a card."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked()
        effect = Effects.Discard(1, EffectTarget.PlayerRef(Player.DefendingPlayer))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Jim Murray"
        flavorText = "\"Merciless to the point of psychosis. Let's give him a job.\"\n—Zelinas, Orzhov recruiter"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51528a3e-beba-47f8-a524-ad99b1fec308.jpg?1783940127"
    }
}
