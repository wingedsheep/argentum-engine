package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Heliod, the Radiant Dawn // Heliod, the Warped Eclipse (March of the Machine #17)
 *
 * Front — {2}{W}{W} Legendary Enchantment Creature — God 4/4.
 *   "When Heliod enters, return target enchantment card that isn't a God from your graveyard to
 *   your hand." / "{3}{U/P}: Transform Heliod. Activate only as a sorcery."
 * Back — Legendary Enchantment Creature — Phyrexian God 4/6 (white-blue color indicator).
 *   "You may cast spells as though they had flash." / "Spells you cast cost {1} less to cast for
 *   each card your opponents have drawn this turn."
 *
 * The back face's reduction is a battlefield [ModifySpellCost] whose amount is the summed
 * `DynamicAmounts.cardsDrawnThisTurn(EachOpponent)` read at cast time; only generic mana is reduced.
 */
private val HeliodTheRadiantDawnFront = card("Heliod, the Radiant Dawn") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "WU"
    typeLine = "Legendary Enchantment Creature — God"
    power = 4
    toughness = 4
    oracleText = "When Heliod enters, return target enchantment card that isn't a God from your " +
        "graveyard to your hand.\n{3}{U/P}: Transform Heliod. Activate only as a sorcery. " +
        "({U/P} can be paid with either {U} or 2 life.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val card = target(
            TargetFilter(
                baseFilter = GameObjectFilter.Enchantment.notSubtype(Subtype.GOD).ownedByYou(),
                zone = Zone.GRAVEYARD,
            )
        )
        effect = Effects.ReturnToHand(card)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{U/P}")
        effect = Effects.Transform(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform Heliod."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "17"
        artist = "Victor Adame Minguez"
        flavorText = "\"*Compleat* the faith, *compleat* the god.\"\n—Ajani Goldmane"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7113c93-6c6d-410f-aeec-abc5ee121cdf.jpg?1783917074"
    }
}

private val HeliodTheWarpedEclipse = card("Heliod, the Warped Eclipse") {
    manaCost = ""
    colorIndicator = "WU" // Transformed back face, no mana cost (CR 204).
    colorIdentity = "WU"
    typeLine = "Legendary Enchantment Creature — Phyrexian God"
    power = 4
    toughness = 6
    oracleText = "You may cast spells as though they had flash.\nSpells you cast cost {1} less to " +
        "cast for each card your opponents have drawn this turn."

    staticAbility {
        ability = GrantFlashToSpellType(
            filter = GameObjectFilter.Any,
            controllerOnly = true,
        )
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any),
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(
                    DynamicAmounts.cardsDrawnThisTurn(Player.EachOpponent)
                )
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "17"
        artist = "Victor Adame Minguez"
        flavorText = "\"*Compleat* the god, *compleat* the plane.\"\n—Ajani Goldmane"
        imageUri = "https://cards.scryfall.io/normal/back/a/7/a7113c93-6c6d-410f-aeec-abc5ee121cdf.jpg?1783917074"
    }
}

val HeliodTheRadiantDawn: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = HeliodTheRadiantDawnFront,
    backFace = HeliodTheWarpedEclipse,
)
