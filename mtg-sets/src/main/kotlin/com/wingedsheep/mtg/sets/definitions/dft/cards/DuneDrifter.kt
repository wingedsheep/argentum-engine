package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Dune Drifter
 * {X}{W}{B}
 * Artifact — Vehicle — Uncommon (DFT #200)
 * 3/3
 *
 * When this Vehicle enters, return target artifact or creature card with mana value X or less from
 * your graveyard to the battlefield.
 * Crew 2
 *
 * X is the value chosen as this Vehicle was *cast*, and the enters trigger resolves after the spell
 * is gone — so the cap reads [DynamicAmount.CastX], the durable object-scoped reading that rides the
 * spell's entity onto the battlefield, rather than the resolution-context `XValue`. Expressed as the
 * general [com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueAtMostDynamic] target
 * predicate (`manaValueAtMostDynamic`). A Vehicle that reaches the battlefield without being cast —
 * or cast without paying its mana cost — carries no cast X, so X is 0 and only mana-value-0 cards
 * are legal targets (per the Scryfall ruling below).
 */
val DuneDrifter = card("Dune Drifter") {
    manaCost = "{X}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Artifact — Vehicle"
    power = 3
    toughness = 3
    oracleText = "When this Vehicle enters, return target artifact or creature card with mana value " +
        "X or less from your graveyard to the battlefield.\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val card = target(
            "target artifact or creature card with mana value X or less in your graveyard",
            TargetObject(
                filter = TargetFilter(
                    baseFilter = GameObjectFilter.CreatureOrArtifact
                        .ownedByYou()
                        .manaValueAtMostDynamic(DynamicAmount.CastX),
                    zone = Zone.GRAVEYARD
                )
            )
        )
        effect = Effects.PutOntoBattlefield(card)
        description = "When this Vehicle enters, return target artifact or creature card with mana " +
            "value X or less from your graveyard to the battlefield."
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "200"
        artist = "Simon Dominic"
        flavorText = "It was easy for them to replace lost crew members once they reached home turf."
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36185de4-55c2-4e5d-9bcc-ea12b7052728.jpg?1783907860"
        ruling("2025-02-07", "If this Vehicle enters and wasn't cast, or it was cast without paying its mana cost, X is 0.")
    }
}
