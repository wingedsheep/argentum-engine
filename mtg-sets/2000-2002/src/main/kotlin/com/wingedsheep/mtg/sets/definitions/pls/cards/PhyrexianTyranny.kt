package com.wingedsheep.mtg.sets.definitions.pls.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Phyrexian Tyranny
 * {U}{B}{R}
 * Enchantment
 * Whenever a player draws a card, that player loses 2 life unless they pay {2}.
 *
 * `Triggers.anyPlayer.draws()` fires once per card drawn (ruling). The drawing player
 * ([Player.TriggeringPlayer]) is offered {2} on resolution; declining loses them 2 life — the
 * Rhystic Study / Nafs Asp [Effects.PayOrSuffer] shape.
 */
val PhyrexianTyranny = card("Phyrexian Tyranny") {
    manaCost = "{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Enchantment"
    oracleText = "Whenever a player draws a card, that player loses 2 life unless they pay {2}."

    triggeredAbility {
        trigger = Triggers.anyPlayer.draws()
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.Mana("{2}"),
            suffer = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.TriggeringPlayer)),
            player = EffectTarget.PlayerRef(Player.TriggeringPlayer),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "118"
        artist = "Kev Walker"
        flavorText = "\"He is Yawgmoth's reward to me. I shall kill him a hundred times a day.\"\n—Crovax, to Ertai"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8440ca8-73ca-462b-a735-f6fb3d0de603.jpg?1783945604"
        ruling("2004-10-04", "If more than one card is drawn at a time, this triggers for each card drawn.")
    }
}
