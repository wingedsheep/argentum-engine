package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ReplaceLifeGainWith
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tainted Remedy
 * {2}{B}
 * Enchantment
 * If an opponent would gain life, that player loses that much life instead.
 *
 * Lich's `ReplaceLifeGainWith`, scoped to opponents of the enchantment's controller: the gaining
 * player (`TriggeringPlayer`) loses the amount the gain would have been after any earlier life-gain
 * modifications, so an opponent's Alhammarret's Archive applied first doubles the loss.
 */
val TaintedRemedy = card("Tainted Remedy") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "If an opponent would gain life, that player loses that much life instead."

    replacementEffect(
        ReplaceLifeGainWith(
            replacementEffect = Effects.LoseLife(
                DynamicAmounts.replacementLifeGainAmount(),
                EffectTarget.PlayerRef(Player.TriggeringPlayer),
            ),
            appliesTo = EventPattern.LifeGainEvent(Player.EachOpponent),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "120"
        artist = "Izzy"
        flavorText = "\"Drink this, brother. It will bring you rest.\"\n—Liliana Vess"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbb8edd0-4573-4b52-a3ea-83ed19dbf58d.jpg?1783938337"
        ruling("2015-06-22", "If more than one replacement effect tries to apply to a life gain event, the player who would gain life chooses the order in which they apply. For example, if a player who controls Alhammarret's Archive would gain 3 life while Tainted Remedy is on the battlefield, that player may choose to have the 3 life become doubled to 6 life and then lose 6 life. The player may also choose to apply Tainted Remedy first, turning \"gain 3 life\" into \"lose 3 life.\" Alhammarret's Archive would then not apply.")
        ruling("2015-06-22", "Having more than one Tainted Remedy on the battlefield doesn't have any noticeable effect on life gain. Once the effect of one Tainted Remedy applies, there is no life gain for the others to apply to.")
    }
}
