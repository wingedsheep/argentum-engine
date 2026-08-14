package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty

/**
 * Glamdring, Foe-hammer // Gleam of Death
 * {2}
 * Legendary Artifact — Equipment
 *
 * Instant and sorcery spells you cast cost {X} less to cast, where X is equipped creature's power.
 * Equip {2}
 *
 * Adventure: Gleam of Death — {3}{U}, Sorcery — Adventure
 * Mill six cards, then put all instant and sorcery cards from among them into your hand.
 *
 * The cost reduction is the first one in the family that reads a *single* permanent found through
 * the reducing permanent's own attachment rather than aggregating over a group the caster controls,
 * so it needed a new [CostReductionSource.AttachedPermanentProperty] plus the ability's source id
 * threaded into `CostCalculator.evaluateReduction` (every other source only needs the caster).
 * Parameterising it over [EntityNumericProperty] keeps it in line with its neighbours
 * `GreatestPropertyAmongPermanentsYouControl` / `TotalPropertyAmongPermanentsYouControl`.
 *
 * The value is read from projected state at cast time, so the equipped creature's counters and any
 * anthems count, and an *unequipped* Glamdring reduces by 0 — which is what the oracle text means
 * when there is no equipped creature. Negative power floors at 0; a reduction clause can never make
 * a spell cost more.
 *
 * The Adventure is the Cantankerous Keepers shape: mill into a stored collection, then split that
 * collection with [FilterCollectionEffect] and move the matching half to hand. "All instant and
 * sorcery cards" is a filter over the milled six, not a choice, so there is no selection step.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the caster
 * cast the artifact from exile afterwards.)
 */
val GlamdringFoeHammer = card("Glamdring, Foe-hammer") {
    manaCost = "{2}"
    colorIdentity = "U"
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Instant and sorcery spells you cast cost {X} less to cast, where X is equipped " +
        "creature's power.\n" +
        "Equip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(Filters.Unified.instantOrSorcery),
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.AttachedPermanentProperty(EntityNumericProperty.Power)
            ),
        )
    }

    equipAbility("{2}")

    adventure("Gleam of Death") {
        manaCost = "{3}{U}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Mill six cards, then put all instant and sorcery cards from among them into " +
            "your hand. (Then exile this card. You may cast the artifact later from exile.)"
        spell {
            effect = Effects.Composite(
                listOf(
                    // Mill six cards.
                    GatherCardsEffect(
                        source = CardSource.TopOfLibrary(DynamicAmount.Fixed(6), Player.You, isMill = true),
                        storeAs = "milled"
                    ),
                    MoveCollectionEffect(
                        from = "milled",
                        destination = CardDestination.ToZone(Zone.GRAVEYARD)
                    ),
                    // Then put all instant and sorcery cards from among them into your hand.
                    FilterCollectionEffect(
                        from = "milled",
                        filter = CollectionFilter.MatchesFilter(GameObjectFilter.InstantOrSorcery),
                        storeMatching = "spells",
                        storeNonMatching = "rest"
                    ),
                    MoveCollectionEffect(
                        from = "spells",
                        destination = CardDestination.ToZone(Zone.HAND)
                    )
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "174"
        artist = "Chris Cold"
        flavorText = "Suddenly a sword flashed in its own light."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5cfbfde-783e-46ca-b3cf-11f16209d6cb.jpg?1785496394"
    }
}
