package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Havengul Lich
 * {3}{U}{B}
 * Creature — Zombie Wizard
 * 4/4
 *
 * {1}: You may cast target creature card in a graveyard this turn. When you cast it this turn,
 * this creature gains all activated abilities of that card until end of turn.
 *
 * The permission is a per-card "may play" grant that leaves the card where it is (any player's
 * graveyard). The second sentence is a delayed spell-cast trigger whose watched target is the
 * targeted card: it fires only for the spell that card becomes, and the Lich copies the card's
 * activated abilities while it is still a spell on the stack (the 2019-10-04 ruling).
 */
val HavengulLich = card("Havengul Lich") {
    manaCost = "{3}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Zombie Wizard"
    power = 4
    toughness = 4
    oracleText = "{1}: You may cast target creature card in a graveyard this turn. When you cast it this " +
        "turn, this creature gains all activated abilities of that card until end of turn."

    activatedAbility {
        cost = Costs.Mana("{1}")
        val creatureCard = target(TargetFilter.CreatureInGraveyard)
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            run(Effects.GrantMayPlayFromExile(from = chosen, expiry = MayPlayExpiry.EndOfTurn))
            run(
                Effects.CreateDelayedTrigger(
                    trigger = Triggers.you.casts(),
                    watchedTarget = creatureCard,
                    fireOnce = true,
                    effect = Effects.GainAllActivatedAbilitiesOf(donor = EffectTarget.TriggeringEntity),
                )
            )
        }
        description = "{1}: You may cast target creature card in a graveyard this turn. When you cast it " +
            "this turn, this creature gains all activated abilities of that card until end of turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "139"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/321077a4-e468-4e74-94f7-80c83790e0d9.jpg?1783940797"
        ruling("2019-10-04", "Havengul Lich gains the activated abilities of the card as it existed in the graveyard while that card is still a spell on the stack.")
        ruling("2016-06-08", "Activated abilities contain a colon. They're generally written \"[Cost]: [Effect].\" Some keywords are activated abilities and will have colons in their reminder text.")
        ruling("2013-04-15", "Havengul Lich's ability changes the zone you are permitted to cast the card from, not the times you are permitted to cast it. You can cast a creature card that has been targeted by Havengul Lich from the graveyard, but unless it has flash, you can only do so if it's your main phase, the stack is empty, and you have priority.")
        ruling("2011-01-22", "Havengul Lich's activated ability creates a delayed triggered ability that triggers when you cast the creature card. This triggered ability will go on the stack on top of the creature spell and will resolve before the creature spell will.")
        ruling("2011-01-22", "If you don't cast the creature card that turn, Havengul Lich won't gain any of its abilities.")
    }
}
