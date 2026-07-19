package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.GrantKeywordToSpellEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ojer Pakpatiq, Deepest Epoch // Temple of Cyclical Time (The Lost Caverns of Ixalan)
 * {2}{U}{U}
 * Legendary Creature — God // Land
 *
 * Front — Ojer Pakpatiq, Deepest Epoch (4/3, Flying)
 *   Whenever you cast an instant spell from your hand, it gains rebound.
 *   When Ojer Pakpatiq dies, return it to the battlefield tapped and transformed under its
 *   owner's control with three time counters on it.
 *
 * Back — Temple of Cyclical Time (Land)
 *   {T}: Add {U}. Remove a time counter from this land.
 *   {2}{U}, {T}: Transform this land. Activate only if it has no time counters on it and only as
 *   a sorcery.
 *
 * Implementation:
 *  - The grant is a [Triggers.youCastSpell]`(Instant, CastFromZone(HAND))` trigger whose effect is
 *    [GrantKeywordToSpellEffect]`(Keyword.REBOUND, TriggeringEntity)` — it stamps the just-cast
 *    spell with rebound, which the spell-resolution path (CR 702.88) honors by exiling the spell
 *    and arming a next-upkeep free recast.
 *  - Dies-return uses the shared [Effects.ReturnSelfFromGraveyardTransformed]`(tapped = true)`;
 *    because the graveyard→battlefield move preserves the entity id, the "with three time counters"
 *    clause composes as a following [Effects.AddCounters]`(TIME, 3, Self)`.
 *  - Back land: a `{T}: Add {U}` mana ability that also removes a time counter (a legal rider —
 *    no targets/choices, CR 605.1a) + a `{2}{U}, {T}` sorcery-speed [TransformEffect] gated on the
 *    land having no time counters.
 */

private val OjerPakpatiqDeepestEpochFront = card("Ojer Pakpatiq, Deepest Epoch") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — God"
    power = 4
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever you cast an instant spell from your hand, it gains rebound. (Exile it as it " +
        "resolves. At the beginning of your next upkeep, you may cast it from exile without " +
        "paying its mana cost.)\n" +
        "When Ojer Pakpatiq dies, return it to the battlefield tapped and transformed under its " +
        "owner's control with three time counters on it."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.youCastSpell(
            spellFilter = GameObjectFilter.Instant,
            requires = setOf(SpellCastPredicate.CastFromZone(Zone.HAND)),
        )
        effect = GrantKeywordToSpellEffect(Keyword.REBOUND, EffectTarget.TriggeringEntity)
        description = "Whenever you cast an instant spell from your hand, it gains rebound."
    }

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.Composite(
            Effects.ReturnSelfFromGraveyardTransformed(tapped = true),
            Effects.AddCounters(Counters.TIME, 3, EffectTarget.Self),
        )
        description = "When Ojer Pakpatiq dies, return it to the battlefield tapped and " +
            "transformed under its owner's control with three time counters on it."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "67"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a9d71007-bc04-4dff-ad3f-e2c0b5b4400e.jpg?1782694556"
    }
}

private val TempleOfCyclicalTime = card("Temple of Cyclical Time") {
    manaCost = ""
    colorIdentity = "U"
    typeLine = "Land"
    oracleText = "(Transforms from Ojer Pakpatiq, Deepest Epoch.)\n" +
        "{T}: Add {U}. Remove a time counter from this land.\n" +
        "{2}{U}, {T}: Transform this land. Activate only if it has no time counters on it and " +
        "only as a sorcery."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Composite(
            Effects.AddMana(Color.BLUE, 1),
            Effects.RemoveCounters(Counters.TIME, 1, EffectTarget.Self),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}"), Costs.Tap)
        effect = TransformEffect(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.Not(Conditions.SourceCounterCountAtLeast(Counters.TIME, 1))
            )
        )
        description = "Transform this land. Activate only if it has no time counters on it and " +
            "only as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "67"
        artist = "Chris Rahn"
        flavorText = "Chimil gave the Oltec time. Ojer Pakpatiq gave them the tools to learn its lessons."
        imageUri = "https://cards.scryfall.io/normal/back/a/9/a9d71007-bc04-4dff-ad3f-e2c0b5b4400e.jpg?1782694556"
    }
}

val OjerPakpatiqDeepestEpoch: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = OjerPakpatiqDeepestEpochFront,
    backFace = TempleOfCyclicalTime,
)
