package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Alacrian Armory — Aetherdrift #2
 * {3}{W} · Artifact
 *
 * Creatures you control get +0/+1 and have vigilance.
 * At the beginning of combat on your turn, choose up to one target Mount or Vehicle you control.
 * Until end of turn, that permanent becomes saddled if it's a Mount and becomes an artifact
 * creature if it's a Vehicle.
 *
 * The two halves of the trigger are gated separately on the chosen permanent's own type, because
 * neither is a safe no-op on the wrong one: saddling is a bare marker with no Mount check in the
 * executor, so an ungated `BecomeSaddled` would stamp a Vehicle as saddled. Both gates read the
 * *chosen* target at resolution (CR 608.2), so a Mount that has since become a Vehicle — or an
 * absent optional target — resolves correctly.
 *
 * "Becomes an artifact creature" is `Effects.AddCardType`, not `BecomeCreature`: a Vehicle already
 * has printed P/T, and this effect only needs to add the CREATURE type. `BecomeCreature` would have
 * to invent a base P/T for a target whose printed stats aren't knowable when the card is authored.
 */
private val MountOrVehicleYouControl = GameObjectFilter(
    cardPredicates = listOf(
        CardPredicate.Or(
            listOf(
                CardPredicate.HasSubtype(Subtype("Mount")),
                CardPredicate.HasSubtype(Subtype.VEHICLE)
            )
        )
    )
).youControl()

private val Mount = GameObjectFilter.Any.withSubtype(Subtype("Mount"))
private val Vehicle = GameObjectFilter.Any.withSubtype(Subtype.VEHICLE)

val AlacrianArmory = card("Alacrian Armory") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "Creatures you control get +0/+1 and have vigilance.\n" +
        "At the beginning of combat on your turn, choose up to one target Mount or Vehicle you " +
        "control. Until end of turn, that permanent becomes saddled if it's a Mount and becomes " +
        "an artifact creature if it's a Vehicle."

    staticAbility {
        ability = ModifyStats(0, 1, GroupFilter.AllCreaturesYouControl)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.AllCreaturesYouControl)
    }

    triggeredAbility {
        trigger = Triggers.BeginCombat
        val permanent = target(
            "up to one target Mount or Vehicle you control",
            TargetPermanent(optional = true, filter = TargetFilter(MountOrVehicleYouControl))
        )
        effect = Effects.Composite(
            ConditionalEffect(
                condition = Conditions.TargetMatchesFilter(Mount),
                effect = Effects.BecomeSaddled(permanent)
            ),
            ConditionalEffect(
                condition = Conditions.TargetMatchesFilter(Vehicle),
                effect = Effects.AddCardType(
                    cardType = "CREATURE",
                    target = permanent,
                    duration = Duration.EndOfTurn
                )
            ),
        )
        description = "At the beginning of combat on your turn, choose up to one target Mount or " +
            "Vehicle you control. Until end of turn, that permanent becomes saddled if it's a " +
            "Mount and becomes an artifact creature if it's a Vehicle."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "2"
        artist = "Artur Nakhodkin"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d39f7f98-ad5e-4e5e-9f7b-abe0984ffe17.jpg?1783907923"
    }
}
