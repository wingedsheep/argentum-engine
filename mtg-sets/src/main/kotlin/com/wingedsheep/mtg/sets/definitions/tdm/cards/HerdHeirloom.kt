package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Herd Heirloom — Tarkir: Dragonstorm #144
 * {1}{G} · Artifact
 *
 * {T}: Add one mana of any color. Spend this mana only to cast a creature spell.
 * {T}: Until end of turn, target creature you control with power 4 or greater gains
 *   trample and "Whenever this creature deals combat damage to a player, draw a card."
 *
 * The first ability is a restricted mana ability ([ManaRestriction.CreatureSpellsOnly]).
 * The second taps for a temporary grant: trample plus a granted combat-damage trigger
 * ([GrantTriggeredAbilityEffect] over `DealsCombatDamageToPlayer`, bound to SELF so it
 * fires for the buffed creature). The target filter is creature-you-control with power ≥ 4.
 */
val HerdHeirloom = card("Herd Heirloom") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Artifact"
    oracleText = "{T}: Add one mana of any color. Spend this mana only to cast a creature spell.\n" +
        "{T}: Until end of turn, target creature you control with power 4 or greater gains trample " +
        "and \"Whenever this creature deals combat damage to a player, draw a card.\""

    // {T}: Add one mana of any color, spendable only on creature spells.
    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddAnyColorMana(1, ManaRestriction.CreatureSpellsOnly)
    }

    // {T}: temporary trample + draw-on-combat-damage grant to a power-4+ creature you control.
    activatedAbility {
        cost = Costs.Tap
        val creature = target(
            "creature",
            TargetCreature(filter = TargetFilter.CreatureYouControl.powerAtLeast(4))
        )
        effect = Effects.Composite(listOf(
            Effects.GrantKeyword(Keyword.TRAMPLE, creature, Duration.EndOfTurn),
            GrantTriggeredAbilityEffect(
                ability = TriggeredAbility.create(
                    trigger = Triggers.DealsCombatDamageToPlayer.event,
                    binding = Triggers.DealsCombatDamageToPlayer.binding,
                    effect = Effects.DrawCards(1),
                    descriptionOverride = "Whenever this creature deals combat damage to a player, draw a card."
                ),
                target = creature,
                duration = Duration.EndOfTurn
            )
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "144"
        artist = "Allen Morris"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a88c7713-b3a9-4685-b1d3-623d35b62365.jpg?1743204542"
    }
}
