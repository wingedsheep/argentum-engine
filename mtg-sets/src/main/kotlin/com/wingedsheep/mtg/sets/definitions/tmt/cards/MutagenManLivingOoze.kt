package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Mutagen Man, Living Ooze
 * {X}{G}{G}
 * Legendary Creature — Ooze Mutant
 * 2/3
 *
 * Trample
 * Activated abilities of artifact tokens you control cost {1} less to activate.
 * When Mutagen Man enters, create X Mutagen tokens.
 */
val MutagenManLivingOoze = card("Mutagen Man, Living Ooze") {
    manaCost = "{X}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Ooze Mutant"
    oracleText = "Trample\nActivated abilities of artifact tokens you control cost {1} less to activate.\nWhen Mutagen Man enters, create X Mutagen tokens. (They're artifacts with \"{1}, {T}, Sacrifice this token: Put a +1/+1 counter on target creature. Activate only as a sorcery.\")"
    power = 2
    toughness = 3

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = ReduceActivatedAbilityCost(
            filter = GroupFilter(
                GameObjectFilter(
                    cardPredicates = listOf(CardPredicate.IsArtifact, CardPredicate.IsToken)
                ).youControl()
            ),
            amount = DynamicAmount.Fixed(1)
        )
    }

    // "create X Mutagen tokens" — X is the cast value (CastX, durable onto the permanent).
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateMutagenToken(DynamicAmount.CastX)
        description = "When Mutagen Man enters, create X Mutagen tokens."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Ignatius Budi"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f9085f0-3702-462c-8c81-b4576236792d.jpg?1769006218"
    }
}
