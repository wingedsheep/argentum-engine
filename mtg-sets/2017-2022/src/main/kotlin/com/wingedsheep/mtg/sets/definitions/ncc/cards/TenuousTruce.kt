package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tenuous Truce
 * {1}{W}
 * Enchantment — Aura
 * Enchant opponent
 * At the beginning of enchanted opponent's end step, you and that player each draw a card.
 * When you attack enchanted opponent or a planeswalker they control or when they attack you or a
 * planeswalker you control, sacrifice this Aura.
 *
 * The sacrifice trigger is one "any player attacks with a matching creature" trigger whose attacker
 * filter is the union of the two legs, so it fires once per declaration however many attackers
 * qualify:
 *  - **you attack them** — a creature you control attacking the defending side of the Aura's host.
 *    [EffectTarget.EnchantedPermanent] resolves through the source's attachment, which for an
 *    "enchant opponent" Aura is the enchanted player, so `attackingDefenderOf` matches that player
 *    and their planeswalkers; battles are carved out, since "a planeswalker they control" is literal.
 *  - **they attack you** — a creature the enchanted player controls attacking you or a planeswalker
 *    you control.
 * Both legs are scoped by the Aura's *attachment*, so in multiplayer an attack between you and a
 * third player — or between the enchanted player and a third player — leaves the truce standing.
 */
private val youAttackingEnchantedOpponentOrTheirPlaneswalker: GameObjectFilter =
    GameObjectFilter.Creature.youControl()
        .attackingDefenderOf(EffectTarget.EnchantedPermanent)
        .withStatePredicate(StatePredicate.Not(StatePredicate.IsAttackingABattle))

private val enchantedOpponentAttackingYouOrYourPlaneswalker: GameObjectFilter =
    GameObjectFilter.Creature.controlledByEnchantedPlayer().attackingYouOrYourPlaneswalkers()

val TenuousTruce = card("Tenuous Truce") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant opponent\n" +
        "At the beginning of enchanted opponent's end step, you and that player each draw a card.\n" +
        "When you attack enchanted opponent or a planeswalker they control or when they attack you " +
        "or a planeswalker you control, sacrifice this Aura."

    auraTarget = Targets.Opponent

    triggeredAbility {
        trigger = Triggers.player(Player.EnchantedPlayer).beginningOf(Step.END)
        effect = Effects.DrawCards(1) then
            Effects.DrawCards(1, EffectTarget.PlayerRef(Player.EnchantedPlayer))
        description = "At the beginning of enchanted opponent's end step, you and that player each draw a card."
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.attacks(
            with = youAttackingEnchantedOpponentOrTheirPlaneswalker or enchantedOpponentAttackingYouOrYourPlaneswalker
        )
        effect = Effects.SacrificeTarget(EffectTarget.Self)
        description = "When you attack enchanted opponent or a planeswalker they control or when they " +
            "attack you or a planeswalker you control, sacrifice this Aura."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Kari Christensen"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f511555a-09aa-41f8-b240-c21f911a57b4.jpg?1783923346"
    }
}
