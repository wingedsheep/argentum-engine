package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Alesha, Who Smiles at Death — Fate Reforged #90 (canonical printing)
 * {2}{R} · Legendary Creature — Human Warrior · 3/2
 *
 * First strike
 * Whenever Alesha attacks, you may pay {W/B}{W/B}. If you do, return target creature card with
 * power 2 or less from your graveyard to the battlefield tapped and attacking.
 *
 * The target is chosen as the trigger goes on the stack; the {W/B}{W/B} is offered once on
 * resolution ([Effects.MayPay]). The power check reads the card in the graveyard only. The return
 * is a bare "tapped and attacking" (CR 508.4), so its controller picks what it attacks
 * ([EnteringAttackDefender.CONTROLLER_CHOOSES]) — it needn't be what Alesha attacks.
 */
val AleshaWhoSmilesAtDeath = card("Alesha, Who Smiles at Death") {
    manaCost = "{2}{R}"
    colorIdentity = "BRW"
    typeLine = "Legendary Creature — Human Warrior"
    power = 3
    toughness = 2
    oracleText = "First strike\n" +
        "Whenever Alesha attacks, you may pay {W/B}{W/B}. If you do, return target creature card " +
        "with power 2 or less from your graveyard to the battlefield tapped and attacking."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        target(TargetFilter.CreatureInYourGraveyard.powerAtMost(2))
        effect = Effects.MayPay(
            cost = ManaCost.parse("{W/B}{W/B}"),
            then = Effects.Pipeline {
                val chosen = gather(CardSource.ChosenTargets)
                move(
                    chosen,
                    CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.TappedAndAttacking),
                    attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
                )
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "90"
        artist = "Anastasia Ovchinnikova"
        flavorText = "\"Greet death with sword in hand.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09ac9669-d0ee-4df8-af77-12dfccac774f.jpg?1783938691"

        ruling("2021-03-19", "While resolving Alesha's triggered ability, you can't pay the cost multiple times to return more than one creature card.")
        ruling("2021-03-19", "Alesha's triggered ability checks the card's power only in your graveyard. The creature will still be tapped and attacking if its power is greater than 2 after it's on the battlefield.")
        ruling("2021-03-19", "You choose which player or planeswalker the returned creature is attacking. It doesn't have to be attacking the same player or planeswalker Alesha is attacking.")
        ruling("2021-03-19", "Although the returned creature is an attacking creature, it was never declared as an attacking creature. This means that abilities that trigger whenever a creature attacks won't trigger when it enters the battlefield attacking.")
        ruling("2021-03-19", "Any effects that say that the returned creature can't attack (such as that of Propaganda, or if the creature has defender) affect only the declaration of attackers. They won't stop the returned creature from entering the battlefield attacking.")
    }
}
