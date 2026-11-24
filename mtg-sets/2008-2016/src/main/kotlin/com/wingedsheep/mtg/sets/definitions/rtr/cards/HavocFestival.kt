package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventLifeGain
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Havoc Festival
 * {4}{B}{R}
 * Enchantment
 *
 * Players can't gain life.
 * At the beginning of each player's upkeep, that player loses half their life, rounded up.
 *
 * The life loss reads the upkeep player's life total at resolution (so two Festivals halve twice).
 */
val HavocFestival = card("Havoc Festival") {
    manaCost = "{4}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Enchantment"
    oracleText = "Players can't gain life.\n" +
        "At the beginning of each player's upkeep, that player loses half their life, rounded up."

    replacementEffect(PreventLifeGain(appliesTo = EventPattern.LifeGainEvent(player = Player.Each)))

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        effect = Effects.LoseHalfLife(
            roundUp = true,
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            lifePlayer = Player.TriggeringPlayer
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "166"
        artist = "Johannes Voss"
        flavorText = "\"We can't control enemies who have no regard for their own survival.\"\n—Arrester Lavinia, Tenth Precinct"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04560623-c768-4273-a40d-7e3f39e832cf.jpg?1783940339"
        ruling("2012-10-01", "Spells and abilities that would cause a player to gain life still resolve, but the life-gain part has no effect.")
        ruling("2012-10-01", "Effects that would replace gaining life with another effect won't apply because it's impossible for players to gain life.")
        ruling("2012-10-01", "If an effect says to set a player's life total to a certain number and that number is higher than the player's current life total, that part of the effect won't do anything. (If the number is lower than the player's current life total, the effect will work as normal.)")
        ruling("2012-10-01", "The amount of life lost is calculated when the triggered ability resolves. For example, if a player has 20 life and there are two Havoc Festivals on the battlefield, the first ability will cause that player to lose 10 life and then the second ability will cause that player to lose 5 life.")
    }
}
