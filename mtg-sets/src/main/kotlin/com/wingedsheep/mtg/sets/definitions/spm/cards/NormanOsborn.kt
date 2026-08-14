package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.GraveyardCardsHaveMayhem
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Norman Osborn // Green Goblin — Marvel's Spider-Man #39 (mythic)
 *
 * Front — Norman Osborn · {1}{U} · Legendary Creature — Human Scientist Villain · 1/1
 *   Norman Osborn can't be blocked.
 *   Whenever Norman Osborn deals combat damage to a player, he connives.
 *   {1}{U}{B}{R}: Transform Norman Osborn. Activate only as a sorcery.
 *
 * Back — Green Goblin · Legendary Creature — Goblin Human Villain · 3/3
 *   Flying, menace
 *   Spells you cast from your graveyard cost {2} less to cast.
 *   Goblin Formula — Each nonland card in your graveyard has mayhem. The mayhem cost is equal to
 *   its mana cost.
 *
 * Modeled as a transforming double-faced creature ([CardDefinition.doubleFacedCreature]); the front
 * owns the sorcery-speed [TransformEffect] flip. The back is a transformed face reached only via the
 * flip, so it carries no castable mana cost — its U/B/R colors come from a color indicator (CR 204).
 *
 *  - Front: [com.wingedsheep.sdk.scripting.CantBeBlocked] (unblockable), a combat-damage
 *    [Effects.Connive] trigger, and the transform ability.
 *  - Back: flying/menace keywords; a `YouCastFromZones(GRAVEYARD)` [ModifySpellCost] `{2}` reduction;
 *    and the Goblin Formula — a [GrantMayhemToGraveyard] static granting mayhem (cost = each card's
 *    own mana value) to every nonland card in your graveyard, consulted by the mayhem cast path.
 */

private val NormanOsbornFront = card("Norman Osborn") {
    manaCost = "{1}{U}"
    colorIdentity = "UBR"
    typeLine = "Legendary Creature — Human Scientist Villain"
    power = 1
    toughness = 1
    oracleText = "Norman Osborn can't be blocked.\n" +
        "Whenever Norman Osborn deals combat damage to a player, he connives. (Draw a card, then " +
        "discard a card. If you discarded a nonland card, put a +1/+1 counter on this creature.)\n" +
        "{1}{U}{B}{R}: Transform Norman Osborn. Activate only as a sorcery."

    // Norman Osborn can't be blocked.
    staticAbility {
        ability = com.wingedsheep.sdk.scripting.CantBeBlocked()
    }

    // Whenever Norman Osborn deals combat damage to a player, he connives.
    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.Connive(EffectTarget.Self)
        description = "Whenever Norman Osborn deals combat damage to a player, he connives."
    }

    // {1}{U}{B}{R}: Transform Norman Osborn. Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Mana("{1}{U}{B}{R}")
        effect = TransformEffect(EffectTarget.Self)
        timing = TimingRule.SorcerySpeed
        description = "Transform Norman Osborn. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "39"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5c53af9-7150-4e78-8771-2de7980aa307.jpg?1783905356"
    }
}

private val GreenGoblinBack = card("Green Goblin") {
    manaCost = ""
    colorIdentity = "UBR"
    colorIndicator = "UBR" // Transformed back face, no mana cost (CR 204).
    typeLine = "Legendary Creature — Goblin Human Villain"
    power = 3
    toughness = 3
    oracleText = "Flying, menace\n" +
        "Spells you cast from your graveyard cost {2} less to cast.\n" +
        "Goblin Formula — Each nonland card in your graveyard has mayhem. The mayhem cost is " +
        "equal to its mana cost. (You may cast a card from your graveyard for its mayhem cost if " +
        "you discarded it this turn. Timing rules still apply.)"

    keywords(Keyword.FLYING, Keyword.MENACE)

    // Spells you cast from your graveyard cost {2} less to cast.
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCastFromZones(setOf(Zone.GRAVEYARD)),
            modification = CostModification.ReduceGeneric(2),
        )
    }

    // Goblin Formula — Each nonland card in your graveyard has mayhem. The mayhem cost is equal to
    // its mana cost.
    staticAbility {
        ability = GraveyardCardsHaveMayhem(filter = GameObjectFilter.Nonland)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "39"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/back/d/5/d5c53af9-7150-4e78-8771-2de7980aa307.jpg?1783905356"
    }
}

val NormanOsborn: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = NormanOsbornFront,
    backFace = GreenGoblinBack,
)
