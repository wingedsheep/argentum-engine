package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Nissa, Vital Force — Kaladesh #163
 * {3}{G}{G} · Legendary Planeswalker — Nissa · Starting loyalty 5
 *
 * +1: Untap target land you control. Until your next turn, it becomes a 5/5 Elemental creature
 *     with haste. It's still a land.
 * −3: Return target permanent card from your graveyard to your hand.
 * −6: You get an emblem with "Whenever a land you control enters, you may draw a card."
 *
 * The +1 is Nissa, Steward of Elements' untap-and-animate ([Effects.BecomeCreature] keeps the land's
 * existing types and abilities) on Sylvan Awakening's [Duration.UntilYourNextTurn]. The emblem is
 * Huatli, Radiant Champion's emblem trigger narrowed to lands.
 */
val NissaVitalForce = card("Nissa, Vital Force") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Nissa"
    startingLoyalty = 5
    oracleText = "+1: Untap target land you control. Until your next turn, it becomes a 5/5 Elemental " +
        "creature with haste. It's still a land.\n" +
        "−3: Return target permanent card from your graveyard to your hand.\n" +
        "−6: You get an emblem with \"Whenever a land you control enters, you may draw a card.\""

    loyaltyAbility(+1) {
        val land = target(TargetFilter.LandYouControl)
        effect = Effects.Untap(land) then
            Effects.BecomeCreature(
                target = land,
                power = 5,
                toughness = 5,
                keywords = setOf(Keyword.HASTE),
                creatureTypes = setOf("Elemental"),
                duration = Duration.UntilYourNextTurn
            )
        description = "Untap target land you control. Until your next turn, it becomes a 5/5 Elemental " +
            "creature with haste. It's still a land."
    }

    loyaltyAbility(-3) {
        val permanentCard = target(
            TargetFilter(GameObjectFilter.Permanent.ownedByYou(), zone = Zone.GRAVEYARD)
        )
        effect = Effects.ReturnToHand(permanentCard)
        description = "Return target permanent card from your graveyard to your hand."
    }

    loyaltyAbility(-6) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
                effect = Effects.May(Effects.DrawCards(1))
                description = "Whenever a land you control enters, you may draw a card."
            },
            emblem = true,
            descriptionOverride = "Whenever a land you control enters, you may draw a card."
        )
        description = "You get an emblem with \"Whenever a land you control enters, you may draw a card.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "163"
        artist = "Clint Cearley"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbaaa98a-ec40-4ff1-8762-a719cf1c475d.jpg?1783937176"

        ruling("2016-09-20", "You can activate Nissa's first ability targeting a land that's already untapped.")
        ruling("2016-09-20", "After resolving Nissa's first ability, the target land still has any abilities it had before it became a creature and any other types it had.")
        ruling("2016-09-20", "A permanent card is an artifact, creature, enchantment, land, or planeswalker card.")
    }
}
