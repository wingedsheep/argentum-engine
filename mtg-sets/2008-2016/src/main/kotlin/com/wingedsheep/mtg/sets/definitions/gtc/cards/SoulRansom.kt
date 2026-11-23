package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.ControlEnchantedPermanent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Soul Ransom — Gatecrash #198 (canonical printing)
 * {2}{U}{B} · Enchantment — Aura
 *
 * Enchant creature
 * You control enchanted creature.
 * Discard two cards: This Aura's controller sacrifices it, then draws two cards. Only your
 * opponents may activate this ability.
 *
 * "Only your opponents" is AnyPlayerMay narrowed by a condition whose "you" is the activating
 * player (Clergy of the Holy Nimbus, Detention Vortex). The ability's controller is the opponent
 * who activated it, so the sacrifice and the draw name the Aura's controller explicitly:
 * [Player.ControllerOfSource] reads the Aura's last-known controller once it has been sacrificed.
 */
val SoulRansom = card("Soul Ransom") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "You control enchanted creature.\n" +
        "Discard two cards: This Aura's controller sacrifices it, then draws two cards. " +
        "Only your opponents may activate this ability."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ControlEnchantedPermanent
    }

    activatedAbility {
        cost = Costs.Discard(count = 2)
        restrictions = listOf(
            ActivationRestriction.All(
                ActivationRestriction.AnyPlayerMay,
                ActivationRestriction.OnlyIfCondition(
                    Conditions.SourceMatches(GameObjectFilter.Any.opponentControls())
                )
            )
        )
        effect = Effects.SacrificeTarget(EffectTarget.Self, sacrificedByItsController = true) then
            Effects.DrawCards(2, EffectTarget.PlayerRef(Player.ControllerOfSource))
        description = "Discard two cards: This Aura's controller sacrifices it, then draws two cards. " +
            "Only your opponents may activate this ability."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "198"
        artist = "Steve Argyle"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd705732-cce9-4d11-85ca-be49381dbaa8.jpg?1783940099"
    }
}
