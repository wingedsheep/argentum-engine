package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Archangel Elspeth — March of the Machine #6
 * {2}{W}{W} · Legendary Planeswalker — Elspeth · Starting loyalty 4
 *
 * +1: Create a 1/1 white Soldier creature token with lifelink.
 * −2: Put two +1/+1 counters on target creature. It becomes an Angel in addition to its other
 *     types and gains flying.
 * −6: Return all nonland permanent cards with mana value 3 or less from your graveyard to the
 *     battlefield.
 *
 * The −2 has no duration (ruling), so the Angel type and flying are `Duration.Permanent` — they
 * last until the creature leaves the battlefield.
 */
val ArchangelElspeth = card("Archangel Elspeth") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Elspeth"
    startingLoyalty = 4
    oracleText = "+1: Create a 1/1 white Soldier creature token with lifelink.\n" +
        "−2: Put two +1/+1 counters on target creature. It becomes an Angel in addition to its other types and gains flying.\n" +
        "−6: Return all nonland permanent cards with mana value 3 or less from your graveyard to the battlefield."

    loyaltyAbility(+1) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            keywords = setOf(Keyword.LIFELINK),
            imageUri = "https://cards.scryfall.io/normal/front/1/7/1774c68a-3d76-4fe1-b741-e6acf6b9214c.jpg?1783916674"
        )
    }

    loyaltyAbility(-2) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature) then
            Effects.AddCreatureType("Angel", creature, Duration.Permanent) then
            Effects.GrantKeyword(Keyword.FLYING, creature, Duration.Permanent)
    }

    loyaltyAbility(-6) {
        effect = Effects.Pipeline {
            val permanents = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.You,
                    filter = GameObjectFilter.NonlandPermanent.manaValueAtMost(3),
                ),
            )
            move(permanents, CardDestination.ToZone(Zone.BATTLEFIELD))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "6"
        artist = "Cynthia Sheppard"
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35ed6263-bdd7-4013-ac8c-9b652d71a0db.jpg?1783917071"
        ruling("2023-04-14", "The second ability doesn't have a duration. It lasts until the creature leaves the battlefield.")
        ruling("2023-04-14", "If the target of the second ability stops being a creature after the ability has resolved, it will also stop being an Angel until it becomes a creature again, if applicable.")
        ruling("2023-04-14", "For the last ability, if a card in your graveyard has {X} in its mana cost, X is 0.")
    }
}
