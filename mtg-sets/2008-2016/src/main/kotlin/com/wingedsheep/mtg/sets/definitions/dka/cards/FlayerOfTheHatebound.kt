package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flayer of the Hatebound
 * {5}{R}
 * Creature — Devil
 * 4/2
 * Undying
 * Whenever this creature or another creature enters from your graveyard, that creature deals
 * damage equal to its power to any target.
 *
 * "This creature or another creature" is any creature, the source included ([Triggers.a]) — so
 * Flayer's own undying return fires it. "From your graveyard" is a creature *owned by you* entering
 * from a graveyard (a card only sits in its owner's graveyard), whoever ends up controlling it.
 * A creature cast from the graveyard enters from the stack and doesn't trigger it. The entering
 * creature is the damage source, reading its power on resolution (last-known if it's gone).
 */
val FlayerOfTheHatebound = card("Flayer of the Hatebound") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Devil"
    power = 4
    toughness = 2
    oracleText = "Undying (When this creature dies, if it had no +1/+1 counters on it, return it to the battlefield under its owner's control with a +1/+1 counter on it.)\n" +
        "Whenever this creature or another creature enters from your graveyard, that creature deals damage equal to its power to any target."

    keywords(Keyword.UNDYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.ownedByYou()).enters(from = Zone.GRAVEYARD)
        val any = target(Targets.Any)
        effect = Effects.DealDamage(
            amount = DynamicAmounts.triggeringPower(),
            target = any,
            damageSource = EffectTarget.TriggeringEntity
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "89"
        artist = "Jana Schirmer & Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ffb17c3f-0154-49ee-bb5f-cd1df8546871.jpg?1783940817"

        ruling("2011-01-22", "Flayer of the Hatebound's last ability will trigger even if a creature enters from your graveyard under another player's control.")
        ruling("2011-01-22", "The creature that entered from your graveyard deals damage equal to its current power (including any +1/+1 counters it entered with) to the target permanent or player. If it's no longer on the battlefield when the ability resolves, its last known existence on the battlefield is checked to determine its power.")
        ruling("2011-01-22", "If you cast a creature card from your graveyard, that card will be put on the stack before entering. Flayer of the Hatebound won't trigger.")
        ruling("2011-01-22", "Since damage is dealt by the creature, abilities like lifelink and deathtouch are taken into account, even if the creature has left the battlefield by the time it deals damage.")
        ruling("2011-01-22", "Flayer of the Hatebound is the source of the ability, but it may be another creature that is the source of the damage. If a black creature enters from your graveyard, the ability could target a creature with protection from black, although the damage will be prevented. It couldn't target a creature with protection from red.")
    }
}
