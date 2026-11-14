package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mage-Ring Responder
 * {7}
 * Artifact Creature — Golem
 * 7/7
 * This creature doesn't untap during your untap step.
 * {7}: Untap this creature.
 * Whenever this creature attacks, it deals 7 damage to target creature defending player controls.
 *
 * "Doesn't untap" is the [AbilityFlag.DOESNT_UNTAP] self-suppression flag and the pay-to-untap
 * ability a plain [Effects.Untap] on itself (cf. Colossus of Sardia, without the upkeep gate). The
 * damage source of the attack trigger is this creature; the target is a creature controlled by the
 * player this creature attacks (per-attacker [Player.DefendingPlayer], CR 802.2a in multiplayer).
 */
val MageRingResponder = card("Mage-Ring Responder") {
    manaCost = "{7}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Golem"
    power = 7
    toughness = 7
    oracleText = "This creature doesn't untap during your untap step.\n" +
        "{7}: Untap this creature.\n" +
        "Whenever this creature attacks, it deals 7 damage to target creature defending player controls."

    flags(AbilityFlag.DOESNT_UNTAP)

    activatedAbility {
        cost = Costs.Mana("{7}")
        effect = Effects.Untap(EffectTarget.Self)
        description = "{7}: Untap this creature."
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(
            TargetFilter.Creature.targetPlayerControls(EffectTarget.PlayerRef(Player.DefendingPlayer))
        )
        effect = Effects.DealDamage(7, creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "232"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c60473d-5065-4e63-bfc2-a041a8ca48ed.jpg?1783938309"
    }
}
