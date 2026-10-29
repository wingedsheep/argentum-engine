package com.wingedsheep.mtg.sets.definitions.dsc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.PreventLifeGain
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * The Lord of Pain
 * {3}{B}{R}
 * Legendary Creature — Human Assassin
 * 5/5
 *
 * Menace
 * Your opponents can't gain life.
 * Whenever a player casts their first spell each turn, choose another target player. The Lord of
 * Pain deals damage equal to that spell's mana value to the chosen player.
 *
 * "Another target player" excludes the caster: the restriction reads `Player.TriggeringPlayer`,
 * bound both when the trigger's target is chosen and when it is re-checked at resolution. In a
 * two-player game an opponent's first spell makes you the only legal target (per the ruling).
 */
val TheLordOfPain = card("The Lord of Pain") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Assassin"
    power = 5
    toughness = 5
    oracleText = "Menace\n" +
        "Your opponents can't gain life.\n" +
        "Whenever a player casts their first spell each turn, choose another target player. " +
        "The Lord of Pain deals damage equal to that spell's mana value to the chosen player."

    keywords(Keyword.MENACE)

    replacementEffect(PreventLifeGain(appliesTo = EventPattern.LifeGainEvent(player = Player.EachOpponent)))

    triggeredAbility {
        trigger = Triggers.anyPlayer.castsNth(1)
        val chosen = target(
            TargetPlayer(
                restriction = Conditions.Not(Conditions.candidateIs(Player.TriggeringPlayer)),
                descriptionOverride = "another target player",
            )
        )
        effect = Effects.DealDamage(DynamicAmounts.triggeringSpellManaValue(), chosen)
        description = "Whenever a player casts their first spell each turn, choose another target player. " +
            "The Lord of Pain deals damage equal to that spell's mana value to the chosen player."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "3"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/494c244e-dcdc-4169-9743-8a61f58e8763.jpg?1783909663"
        ruling("2024-09-20", "Spells and abilities that cause opponents to gain life still resolve while The Lord of Pain is on the battlefield. They won't gain life, but any other effects of that spell or ability will still happen.")
        ruling("2024-09-20", "If an effect says to set an opponent's life total to a number that's higher than their current life total, that player's life total won't change.")
        ruling("2024-09-20", "The Lord of Pain's last ability must target a player other than the one who cast the spell that caused the ability to trigger. That means that if it's just you and one other player in the game, you'll have to target yourself when that player casts their first spell each turn. (What did you expect? You summoned The Lord of Pain!)")
        ruling("2024-09-20", "If a spell has {X} in its mana cost, use the value chosen for X when determining that spell's mana value.")
    }
}
