package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Fabrication Foundry — {1}{W}
 * Artifact
 * Rare — The Lost Caverns of Ixalan #12
 *
 * "{T}: Add {W}. Spend this mana only to cast an artifact spell or activate an ability of an
 *  artifact source."
 * "{2}{W}, {T}, Exile one or more other artifacts you control with total mana value X: Return
 *  target artifact card with mana value X or less from your graveyard to the battlefield. Activate
 *  only as a sorcery."
 *
 * The restricted mana reuses [ManaRestriction.CardTypeSpellsOrAbilitiesOnly] (artifact spells *and*
 * abilities of artifact sources — the Steelswarm Operator / Mishra's Workshop shape; per the Oracle,
 * an "artifact source" is any object with the artifact card type in any zone).
 *
 * The reanimation ability's additional cost, [Costs.ExilePermanents], is a variable-count "exile one
 * or more *other* artifacts you control" cost whose **total mana value becomes the ability's X
 * value** (CR 601.2b — a variable defined by a cost choice is announced as the ability is activated).
 * The graveyard target is filtered by [GameObjectFilter.manaValueAtMostX] ("mana value X or less"),
 * which reads that X at target selection (CR 601.2c) and is re-validated against the stored X at
 * resolution (CR 608.2b). Sorcery-speed via [TimingRule.SorcerySpeed] (CR 602.5d). X may be 0 (exile
 * a single mana-value-0 artifact → only a mana-value-0 artifact card can be returned).
 */
val FabricationFoundry = card("Fabrication Foundry") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact"
    oracleText = "{T}: Add {W}. Spend this mana only to cast an artifact spell or activate an " +
        "ability of an artifact source.\n" +
        "{2}{W}, {T}, Exile one or more other artifacts you control with total mana value X: " +
        "Return target artifact card with mana value X or less from your graveyard to the " +
        "battlefield. Activate only as a sorcery."

    // {T}: Add {W}. Spend this mana only to cast an artifact spell or activate an ability of an
    // artifact source.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(
            Color.WHITE, 1,
            restriction = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
                cardType = CardType.ARTIFACT,
                allowSpells = true,
                allowAbilities = true,
            ),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {W}. Spend this mana only to cast an artifact spell or activate " +
            "an ability of an artifact source."
    }

    // {2}{W}, {T}, Exile one or more other artifacts you control with total mana value X: Return
    // target artifact card with mana value X or less from your graveyard to the battlefield.
    // Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{W}"),
            Costs.Tap,
            Costs.ExilePermanents(GameObjectFilter.Artifact, minCount = 1, excludeSelf = true)
        )
        val t = target(
            "target artifact card with mana value X or less from your graveyard",
            TargetObject(
                filter = TargetFilter(
                    baseFilter = GameObjectFilter.Artifact.ownedByYou().manaValueAtMostX(),
                    zone = Zone.GRAVEYARD
                )
            )
        )
        effect = Effects.PutOntoBattlefield(t)
        timing = TimingRule.SorcerySpeed
        description = "{2}{W}, {T}, Exile one or more other artifacts you control with total mana " +
            "value X: Return target artifact card with mana value X or less from your graveyard to " +
            "the battlefield. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "12"
        artist = "Racrufi"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/323a05ee-8296-41c0-94ab-00913d9d84f1.jpg?1782694603"
    }
}
