package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessDynamicStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Consuming Blob
 * {3}{G}{G}
 * Creature — Ooze
 * Power/toughness: star / 1+star
 *
 * Consuming Blob's power is equal to the number of card types among cards in your graveyard and
 * its toughness is equal to that number plus 1.
 * At the beginning of your end step, create a green Ooze creature token with "This token's power
 * is equal to the number of card types among cards in your graveyard and its toughness is equal to
 * that number plus 1."
 *
 * The Blob itself is the Lhurgoyf shape (Tarmogoyf) narrowed to [Player.You]'s graveyard:
 * `dynamicStats` with a `toughnessOffset` of 1, working in every zone. The token carries the same
 * characteristic-defining ability as a Layer 7b static on itself (the Voice of Resurgence shape),
 * re-read on every projection rather than snapshotted at creation, so "your" is always the token's
 * own controller.
 */
private val yourGraveyardCardTypes = DynamicAmounts.zone(Player.You, Zone.GRAVEYARD).distinctTypes()

private val oozeToken = Effects.CreateToken(
    power = 0,
    toughness = 1,
    colors = setOf(Color.GREEN),
    creatureTypes = setOf("Ooze"),
    staticAbilities = listOf(
        SetBasePowerToughnessDynamicStatic(
            power = yourGraveyardCardTypes,
            toughness = yourGraveyardCardTypes + 1,
            filter = GroupFilter.source(),
        )
    ),
)

val ConsumingBlob = card("Consuming Blob") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Ooze"
    dynamicStats(yourGraveyardCardTypes, toughnessOffset = 1)
    oracleText = "Consuming Blob's power is equal to the number of card types among cards in your " +
        "graveyard and its toughness is equal to that number plus 1.\n" +
        "At the beginning of your end step, create a green Ooze creature token with \"This token's " +
        "power is equal to the number of card types among cards in your graveyard and its toughness " +
        "is equal to that number plus 1.\""

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = oozeToken
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "177"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/4540fa4b-93e8-4749-a839-9a6e816c1145.jpg?1783925582"
        ruling("2021-09-24", "The card types that can be in a graveyard are artifact, battle, creature, enchantment, instant, kindred, land, planeswalker, and sorcery. Legendary, basic, and snow are supertypes, not card types.")
        ruling("2021-09-24", "Consuming Blob counts card types, not individual cards. If the only card in your graveyard is an artifact creature card, Consuming Blob will be 2/3. If the only cards in your graveyard are ten artifact creature cards, Consuming Blob will still be 2/3.")
        ruling("2021-09-24", "The ability that defines Consuming Blob's power and toughness works in all zones, not just the battlefield. If Consuming Blob is in your graveyard, it will count itself.")
        ruling("2021-09-24", "If an instant or sorcery spell deals damage to Consuming Blob or reduces its toughness to 0, that spell is put into its owner's graveyard before state-based actions are performed. If that card is the first of its type to enter the graveyard of Consuming Blob's controller, it will raise Consuming Blob's toughness before the game checks to see if it dies.")
        ruling("2021-09-24", "The above rulings are also true for Consuming Blob's children (except the first one, as Junior tends to not hang out in graveyards).")
    }
}
