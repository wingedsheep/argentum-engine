package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nicol Bolas
 * {2}{U}{U}{B}{B}{R}{R}
 * Legendary Creature — Elder Dragon
 * 7/7
 *
 * Flying
 * At the beginning of your upkeep, sacrifice Nicol Bolas unless you pay {U}{B}{R}.
 * Whenever Nicol Bolas deals damage to an opponent, that player discards their hand.
 *
 * The upkeep tax is CR 118.3's "unless": [com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect]
 * asks the controller to pay on resolution and sacrifices the dragon when they decline or cannot.
 * The discard trigger fires on any damage to an opponent, combat or not (2009-10-01 ruling).
 */
val NicolBolas = card("Nicol Bolas") {
    manaCost = "{2}{U}{U}{B}{B}{R}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Creature — Elder Dragon"
    power = 7
    toughness = 7
    oracleText = "Flying\n" +
        "At the beginning of your upkeep, sacrifice Nicol Bolas unless you pay {U}{B}{R}.\n" +
        "Whenever Nicol Bolas deals damage to an opponent, that player discards their hand."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.PayOrSuffer(cost = Costs.pay.Mana("{U}{B}{R}"), suffer = SacrificeSelfEffect)
    }

    triggeredAbility {
        trigger = Triggers.self.dealsDamage(Recipient.Opponent)
        effect = Patterns.Hand.discardHand(EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "246"
        artist = "Edward P. Beard, Jr."
        imageUri = "https://cards.scryfall.io/normal/front/7/2/729feb73-4581-4f9d-ba47-bece72481b86.jpg?1783948035"
        ruling(
            "2009-10-01",
            "Nicol Bolas's third ability triggers whenever it deals damage to an opponent for any reason. " +
                "It's not limited to combat damage.",
        )
    }
}
