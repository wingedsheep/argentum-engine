package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dinrova Horror — Gatecrash #155
 * {4}{U}{B} · Creature — Horror · 4/4
 *
 * When this creature enters, return target permanent to its owner's hand, then that player discards
 * a card.
 *
 * Recoil on a body: "that player" is the bounced permanent's **owner** (`Player.OwnerOf`, read off the
 * card's owner id, which survives the move to hand) — not its controller, and not you.
 */
val DinrovaHorror = card("Dinrova Horror") {
    manaCost = "{4}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Horror"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, return target permanent to its owner's hand, then that player discards a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Permanent)
        effect = Effects.ReturnToHand(t) then
                Effects.Discard(1, EffectTarget.PlayerRef(Player.OwnerOf("target permanent")))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "Johann Bodin"
        flavorText = "\"Welcome to the Dimir Public Offices. Not responsible for death or loss of property. Basement off-limits.\"\n—Sign outside Dinrova Building"
        imageUri = "https://cards.scryfall.io/normal/front/3/9/398df5e6-6bda-467a-81e2-91be7e21d715.jpg?1783940109"
    }
}
