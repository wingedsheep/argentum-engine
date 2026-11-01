package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vraska, Swarm's Eminence — War of the Spark #236 (canonical printing)
 * {2}{B/G}{B/G}
 * Legendary Planeswalker — Vraska
 * Loyalty: 5
 *
 * Whenever a creature you control with deathtouch deals damage to a player or planeswalker, put a
 * +1/+1 counter on that creature.
 * −2: Create a 1/1 black Assassin creature token with deathtouch and "Whenever this token deals
 * damage to a planeswalker, destroy that planeswalker."
 *
 * The static trigger is an observer trigger whose source filter reads deathtouch off projected
 * state (Hooded Blightfang's shape), so creatures that gained deathtouch count. Any damage, not just
 * combat. "That creature" is [EffectTarget.DamageSource]. The token's quoted ability is its own
 * self damage trigger; "that planeswalker" is [EffectTarget.DamageRecipient].
 */
val VraskaSwarmsEminence = card("Vraska, Swarm's Eminence") {
    manaCost = "{2}{B/G}{B/G}"
    colorIdentity = "BG"
    typeLine = "Legendary Planeswalker — Vraska"
    startingLoyalty = 5
    oracleText = "Whenever a creature you control with deathtouch deals damage to a player or planeswalker, " +
        "put a +1/+1 counter on that creature.\n" +
        "−2: Create a 1/1 black Assassin creature token with deathtouch and \"Whenever this token deals " +
        "damage to a planeswalker, destroy that planeswalker.\""

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl().withKeyword(Keyword.DEATHTOUCH))
            .dealsDamage(Recipient.AnyPlayerOrPlaneswalker)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.DamageSource)
        description = "Whenever a creature you control with deathtouch deals damage to a player or " +
            "planeswalker, put a +1/+1 counter on that creature."
    }

    loyaltyAbility(-2) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Assassin"),
            keywords = setOf(Keyword.DEATHTOUCH),
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = Triggers.self.dealsDamage(Recipient.Object(GameObjectFilter.Planeswalker)),
                    effect = Effects.Destroy(EffectTarget.DamageRecipient),
                ),
            ),
            imageUri = "https://cards.scryfall.io/normal/front/b/7/b7e997ac-5a4a-4555-9b37-06c25a5db356.jpg?1783933353",
        )
        description = "Create a 1/1 black Assassin creature token with deathtouch and \"Whenever this " +
            "token deals damage to a planeswalker, destroy that planeswalker.\""
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59ee2974-07e9-4506-903c-457cb7327dd8.jpg?1783933375"
    }
}
