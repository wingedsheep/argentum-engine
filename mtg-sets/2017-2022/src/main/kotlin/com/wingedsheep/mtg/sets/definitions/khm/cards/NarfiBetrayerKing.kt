package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Narfi, Betrayer King
 * {3}{U}{B}
 * Legendary Snow Creature — Zombie Wizard
 * 4/3
 * Other snow and Zombie creatures you control get +1/+1.
 * {S}{S}{S}: Return this card from your graveyard to the battlefield tapped.
 *
 * "Snow and Zombie creatures" is the union — a creature that is snow, a Zombie, or both — so the
 * lord filter is one `CardPredicate.Or` over the creature filter.
 */
val NarfiBetrayerKing = card("Narfi, Betrayer King") {
    manaCost = "{3}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Snow Creature — Zombie Wizard"
    power = 4
    toughness = 3
    oracleText = "Other snow and Zombie creatures you control get +1/+1.\n" +
        "{S}{S}{S}: Return this card from your graveyard to the battlefield tapped. " +
        "({S} can be paid with one mana from a snow source.)"

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.copy(
                    cardPredicates = GameObjectFilter.Creature.cardPredicates + CardPredicate.Or(
                        listOf(CardPredicate.IsSnow, CardPredicate.HasSubtype(Subtype.ZOMBIE))
                    )
                ).youControl(),
                excludeSelf = true
            )
        )
    }

    activatedAbility {
        cost = Costs.Mana("{S}{S}{S}")
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self, tapped = true)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "224"
        artist = "Daarken"
        flavorText = "To serve the god of death, Narfi forfeited the warmth of life."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/421376e4-a4ad-427c-bc9c-d315308dcf68.jpg?1783928191"
    }
}
