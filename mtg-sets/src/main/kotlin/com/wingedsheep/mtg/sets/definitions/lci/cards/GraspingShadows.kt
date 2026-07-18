package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grasping Shadows // Shadows' Lair (The Lost Caverns of Ixalan)
 * {3}{B}
 * Enchantment // Land — Cave
 *
 * Front — Grasping Shadows (Enchantment, {3}{B})
 *   Whenever a creature you control attacks alone, it gains deathtouch and lifelink until end
 *   of turn. Put a dread counter on this enchantment. Then if there are three or more dread
 *   counters on it, transform it.
 *
 * Back — Shadows' Lair (Land — Cave)
 *   {T}: Add {B}.
 *   {B}, {T}, Remove a dread counter from this land: You draw a card and you lose 1 life.
 *
 * Implementation:
 *  - Attacks-alone trigger via [Triggers.attacks]`(Creature.youControl(), requires =
 *    AttackPredicate.Alone, binding = ANY)`; the lone attacker is [EffectTarget.TriggeringEntity].
 *    Effect grants deathtouch + lifelink (until end of turn), adds a [Counters.DREAD] counter to
 *    Self, then a [ConditionalEffect] on [Conditions.SourceCounterCountAtLeast]`(dread, 3)` flips
 *    it. Same shape as Team Avatar's attacks-alone pump + The Emperor of Palamecia's threshold flip.
 *  - Back land: `{T}: Add {B}` mana ability + a `{B}, {T}, Remove a dread counter` ability
 *    ([Costs.RemoveCounterFromSelf]) that draws a card and loses 1 life.
 */

private val GraspingShadowsFront = card("Grasping Shadows") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control attacks alone, it gains deathtouch and " +
        "lifelink until end of turn. Put a dread counter on this enchantment. Then if there " +
        "are three or more dread counters on it, transform it."

    triggeredAbility {
        trigger = Triggers.attacks(
            filter = GameObjectFilter.Creature.youControl(),
            requires = setOf(AttackPredicate.Alone),
            binding = TriggerBinding.ANY,
        )
        effect = Effects.Composite(
            Effects.GrantKeyword(Keyword.DEATHTOUCH, EffectTarget.TriggeringEntity),
            Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.TriggeringEntity),
            Effects.AddCounters(Counters.DREAD, 1, EffectTarget.Self),
            ConditionalEffect(
                condition = Conditions.SourceCounterCountAtLeast(Counters.DREAD, 3),
                effect = TransformEffect(EffectTarget.Self),
            ),
        )
        description = "Whenever a creature you control attacks alone, it gains deathtouch and " +
            "lifelink until end of turn. Put a dread counter on Grasping Shadows. Then if " +
            "there are three or more dread counters on it, transform it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "108"
        artist = "Sam Wolfe Connelly"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81b8b9c9-725d-476d-a3cf-55e3dc3e433d.jpg?1782694525"
    }
}

private val ShadowsLair = card("Shadows' Lair") {
    manaCost = ""
    colorIdentity = "B"
    typeLine = "Land — Cave"
    oracleText = "{T}: Add {B}.\n" +
        "{B}, {T}, Remove a dread counter from this land: You draw a card and you lose 1 life."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK, 1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{B}"),
            Costs.Tap,
            Costs.RemoveCounterFromSelf(Counters.DREAD, 1),
        )
        effect = Effects.Composite(
            Effects.DrawCards(1),
            Effects.LoseLife(1, EffectTarget.PlayerRef(Player.You)),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "108"
        artist = "Sam Wolfe Connelly"
        flavorText = "The shadows make no mark upon the flesh. It is the mind they claw and strangle."
        imageUri = "https://cards.scryfall.io/normal/back/8/1/81b8b9c9-725d-476d-a3cf-55e3dc3e433d.jpg?1782694525"
    }
}

val GraspingShadows: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = GraspingShadowsFront,
    backFace = ShadowsLair,
)
