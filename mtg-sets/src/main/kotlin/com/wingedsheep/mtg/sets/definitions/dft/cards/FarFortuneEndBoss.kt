package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.maxSpeed
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.events.RecipientFilter
import com.wingedsheep.sdk.scripting.events.SourceFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Far Fortune, End Boss
 * {2}{B}{R}
 * Legendary Creature — Human Mercenary
 * 4/5
 *
 * Start your engines!
 * Whenever you attack, Far Fortune deals 1 damage to each opponent.
 * Max speed — If a source you control would deal damage to an opponent or a permanent an opponent
 * controls, it deals that much damage plus 1 instead.
 *
 * The rider is a replacement effect, so it goes through `maxSpeed { replacementEffect(…) }`, which
 * folds the "your speed is 4" gate into [ModifyDamageAmount]'s own `restrictions` slot. The damage
 * family evaluates those restrictions against the *replacement source's controller*, which is what
 * makes the card work: the damage lands on an opponent while the gate reads **your** speed.
 *
 * Consequences that fall out of modelling it as a plain additive replacement, matching the rulings:
 *  - The extra 1 is dealt by the original source, not by Far Fortune (the replacement adjusts the
 *    would-be amount; it doesn't create new damage from a new source).
 *  - Trample/divided damage is assigned first and each resulting amount is then bumped, because the
 *    replacement runs per damage event at the point the amount is applied.
 *  - Fully prevented damage is never "damage dealt", so nothing is bumped.
 */
val FarFortuneEndBoss = card("Far Fortune, End Boss") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Human Mercenary"
    power = 4
    toughness = 5
    oracleText = "Start your engines! (If you have no speed, it starts at 1. It increases once on " +
        "each of your turns when an opponent loses life. Max speed is 4.)\n" +
        "Whenever you attack, Far Fortune deals 1 damage to each opponent.\n" +
        "Max speed — If a source you control would deal damage to an opponent or a permanent an " +
        "opponent controls, it deals that much damage plus 1 instead."

    startYourEngines()

    triggeredAbility {
        trigger = Triggers.YouAttack
        effect = Effects.DealDamage(
            amount = 1,
            target = EffectTarget.PlayerRef(Player.EachOpponent),
        )
        description = "Whenever you attack, Far Fortune deals 1 damage to each opponent."
    }

    maxSpeed {
        replacementEffect(
            ModifyDamageAmount(
                modifier = 1,
                appliesTo = EventPattern.DamageEvent(
                    source = SourceFilter.YouControl,
                    recipient = RecipientFilter.OpponentOrPermanentTheyControl,
                ),
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "203"
        artist = "Javier Charro"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f523e96d-9df1-4854-accb-9876aef787e5.jpg?1783907858"

        ruling(
            "2025-02-07",
            "The additional 1 damage from Far Fortune's last ability is dealt by the same source as " +
                "the original source of damage. The damage isn't dealt by Far Fortune unless Far " +
                "Fortune is the original source of damage."
        )
        ruling(
            "2025-02-07",
            "If another effect modifies how much damage your sources would deal, including preventing " +
                "some of it, the player being dealt damage or the controller of the permanent being " +
                "dealt damage chooses an order in which to apply those effects. If all of the damage " +
                "is prevented, Far Fortune's last ability no longer applies."
        )
        ruling(
            "2025-02-07",
            "If damage dealt by a source you control is being divided or assigned among multiple " +
                "permanents an opponent controls or among an opponent and one or more permanents they " +
                "control, divide the original amount before adding 1. For example, if you attack with " +
                "a 5/5 creature with trample and your opponent blocks with a 2/2 creature, you can " +
                "assign 2 damage to the blocker and 3 damage to the defending player. These amounts " +
                "are then modified to 3 and 4, respectively."
        )
        ruling("2025-02-07", "A player \"has max speed\" if their speed is 4.")
    }
}
