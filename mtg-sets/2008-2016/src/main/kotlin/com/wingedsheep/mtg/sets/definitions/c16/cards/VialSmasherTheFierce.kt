package com.wingedsheep.mtg.sets.definitions.c16.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Vial Smasher the Fierce
 * {1}{B}{R}
 * Legendary Creature — Goblin Berserker
 * 2/3
 * Whenever you cast your first spell each turn, choose an opponent at random. Vial Smasher deals
 * damage equal to that spell's mana value to that player or a planeswalker that player controls.
 * Partner (You can have two commanders if both have partner.)
 *
 * Per the rulings, the opponent is drawn at random while the ability resolves (an engine pick, no
 * decision), and only then does the controller choose between that player and one of their
 * planeswalkers — declining the planeswalker choice sends the damage to the player. Neither choice
 * targets. The mana value is the spell's as cast (its {X} counted at the chosen value), so it still
 * holds if the spell was countered before the ability resolves.
 */
val VialSmasherTheFierce = card("Vial Smasher the Fierce") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Goblin Berserker"
    power = 2
    toughness = 3
    oracleText = "Whenever you cast your first spell each turn, choose an opponent at random. Vial Smasher " +
        "deals damage equal to that spell's mana value to that player or a planeswalker that player controls.\n" +
        "Partner (You can have two commanders if both have partner.)"

    keywordAbility(KeywordAbility.partner())

    triggeredAbility {
        trigger = Triggers.you.castsNth(1)
        effect = Effects.Pipeline {
            val opponent = selectTarget(
                TargetOpponent(descriptionOverride = "an opponent"),
                nonTargeting = true,
                atRandom = true
            )
            val planeswalker = selectTarget(
                TargetObject(
                    optional = true,
                    filter = TargetFilter(
                        GameObjectFilter.Planeswalker.withControllerPredicate(
                            ControllerPredicate.ControlledByReferencedPlayer(opponent.asTarget)
                        )
                    )
                ),
                nonTargeting = true,
                prompt = "Choose a planeswalker that player controls to deal the damage to, " +
                    "or none to deal it to the player"
            )
            val damage = DynamicAmounts.triggeringSpellManaValue()
            ifNotEmpty(planeswalker) {
                run(Effects.DealDamage(damage, planeswalker.asTarget))
            } orElse {
                run(Effects.DealDamage(damage, opponent.asTarget))
            }
        }
        description = "Whenever you cast your first spell each turn, choose an opponent at random. " +
            "Vial Smasher deals damage equal to that spell's mana value to that player or a planeswalker " +
            "that player controls."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "49"
        artist = "Deruchenko Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/722b1e02-2268-4e02-8d09-9b337da2a844.jpg?1783937082"
        ruling("2020-11-10", "Vial Smasher's triggered ability triggers when you cast your first spell each turn, regardless of whose turn it is.")
        ruling("2020-11-10", "Vial Smasher has to be on the battlefield at the moment you cast your first spell. If that spell causes Vial Smasher to leave the battlefield as an additional cost to cast it, Vial Smasher's ability can't trigger. If that spell is Vial Smasher itself, Vial Smasher's ability can't trigger.")
        ruling("2020-11-10", "For spells with {X} in their mana costs, use the value chosen for X to determine the spell's mana value.")
        ruling("2020-11-10", "Vial Smasher's triggered ability resolves before the spell that caused it to trigger. If Vial Smasher's ability resolves and the spell that caused it to trigger has been countered, use that spell's mana value as it last existed on the stack to determine how much damage is dealt.")
        ruling("2020-11-10", "The opponent to be dealt damage is chosen at random while the triggered ability is resolving. After that opponent is chosen, you choose whether damage will be dealt to that player or to a planeswalker they control, and if so, which planeswalker. No player may take any actions between the damage recipient being chosen and the damage being dealt.")
    }
}
