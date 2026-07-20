package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.EachPermanentBecomesCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * The Everflowing Well // The Myriad Pools (The Lost Caverns of Ixalan)
 * {2}{U}
 * Legendary Artifact // Legendary Artifact Land
 *
 * Front — The Everflowing Well ({2}{U} Legendary Artifact)
 *   When The Everflowing Well enters, mill two cards, then draw two cards.
 *   Descend 8 — At the beginning of your upkeep, if there are eight or more permanent cards in your
 *   graveyard, transform The Everflowing Well.
 *
 * Back — The Myriad Pools (Legendary Artifact Land)
 *   {T}: Add {U}.
 *   Whenever you cast a permanent spell using mana produced by The Myriad Pools, up to one other
 *   target permanent you control becomes a copy of that spell until end of turn.
 *
 * Implementation:
 *  - ETB `mill 2, then draw 2` via [Patterns.Library.mill] + [Effects.DrawCards].
 *  - Descend 8 upkeep transform: [Triggers.YourUpkeep] with the
 *    [Conditions.CardsInGraveyardMatchingAtLeast]`(8, Permanent)` intervening-if (the god cycle's
 *    descend-8 idiom).
 *  - The Myriad Pools' cast trigger uses [SpellCastPredicate.PaidWithManaFromSource] (the mana-source
 *    provenance for the mana it produced). The copy uses [EachPermanentBecomesCopyOfTargetEffect]
 *    with `sourceFromAnyZone = true` so the copy source can be the just-cast spell (still on the
 *    stack when the trigger resolves), `affected` the chosen permanent, for [Duration.EndOfTurn].
 *    The target is "up to one other target permanent you control" ([TargetOther] + optional count-1).
 */

private val TheEverflowingWellFront = card("The Everflowing Well") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Artifact"
    oracleText = "When The Everflowing Well enters, mill two cards, then draw two cards.\n" +
        "Descend 8 — At the beginning of your upkeep, if there are eight or more permanent cards in " +
        "your graveyard, transform The Everflowing Well. (You descended if a permanent card was put " +
        "into your graveyard from anywhere.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            Patterns.Library.mill(2),
            Effects.DrawCards(2),
        )
        description = "When The Everflowing Well enters, mill two cards, then draw two cards."
    }

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        triggerCondition = Conditions.CardsInGraveyardMatchingAtLeast(8, GameObjectFilter.Permanent)
        effect = TransformEffect(EffectTarget.Self)
        description = "Descend 8 — At the beginning of your upkeep, if there are eight or more " +
            "permanent cards in your graveyard, transform The Everflowing Well."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "56"
        artist = "David Álvarez"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf573fb7-fa6c-4df7-8e5e-1e071585361e.jpg?1782694564"
    }
}

private val TheMyriadPools = card("The Myriad Pools") {
    manaCost = ""
    colorIdentity = "U"
    typeLine = "Legendary Artifact Land"
    oracleText = "{T}: Add {U}.\n" +
        "Whenever you cast a permanent spell using mana produced by The Myriad Pools, up to one " +
        "other target permanent you control becomes a copy of that spell until end of turn."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE, 1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    triggeredAbility {
        trigger = Triggers.youCastSpell(
            spellFilter = GameObjectFilter.Permanent,
            requires = setOf(SpellCastPredicate.PaidWithManaFromSource),
        )
        val t = target(
            "up to one other target permanent you control",
            TargetOther(
                baseRequirement = TargetPermanent(
                    count = 1,
                    optional = true,
                    filter = TargetFilter(GameObjectFilter.Permanent.youControl()),
                ),
            ),
        )
        effect = EachPermanentBecomesCopyOfTargetEffect(
            target = EffectTarget.TriggeringEntity,
            affected = t,
            duration = Duration.EndOfTurn,
            sourceFromAnyZone = true,
        )
        description = "Whenever you cast a permanent spell using mana produced by The Myriad Pools, " +
            "up to one other target permanent you control becomes a copy of that spell until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "56"
        artist = "David Álvarez"
        imageUri = "https://cards.scryfall.io/normal/back/b/f/bf573fb7-fa6c-4df7-8e5e-1e071585361e.jpg?1782694564"
    }
}

val TheEverflowingWell: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = TheEverflowingWellFront,
    backFace = TheMyriadPools,
)
