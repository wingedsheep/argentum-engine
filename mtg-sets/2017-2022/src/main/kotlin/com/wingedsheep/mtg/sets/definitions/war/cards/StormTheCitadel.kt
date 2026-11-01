package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Storm the Citadel
 * {4}{G}
 * Sorcery
 * Until end of turn, creatures you control get +2/+2 and gain "Whenever this creature deals combat
 * damage to a player or planeswalker, destroy target artifact or enchantment defending player
 * controls."
 *
 * The group is locked in at resolution (CR 611.2c): creatures you gain control of later in the turn
 * get neither the bonus nor the trigger. "Defending player" is the player *that* creature attacks —
 * for a creature attacking a planeswalker, the planeswalker's controller — so the target is filtered
 * through the per-attacker `Player.DefendingPlayer` reference, not `defendingPlayerControls()`, which
 * would admit every defending player's permanents in multiplayer.
 */
val StormTheCitadel = card("Storm the Citadel") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Until end of turn, creatures you control get +2/+2 and gain \"Whenever this creature " +
        "deals combat damage to a player or planeswalker, destroy target artifact or enchantment " +
        "defending player controls.\""

    val destroyArtifactOrEnchantment = grantedTriggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayerOrPlaneswalker)
        val permanent = target(
            TargetFilter(
                GameObjectFilter.ArtifactOrEnchantment
                    .targetPlayerControls(EffectTarget.PlayerRef(Player.DefendingPlayer))
            )
        )
        effect = Effects.Destroy(permanent)
        description = "Whenever this creature deals combat damage to a player or planeswalker, destroy " +
            "target artifact or enchantment defending player controls."
    }

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.ModifyStats(2, 2, EffectTarget.IterationEntity) then
                Effects.GrantTriggeredAbility(destroyArtifactOrEnchantment, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "178"
        artist = "Grzegorz Rutkowski"
        flavorText = "\"If we die, we die. But we cannot let him win.\"\n—Ajani Goldmane"
        imageUri = "https://cards.scryfall.io/normal/front/0/4/04b3b12e-bef7-409e-86b3-9e90640e598c.jpg?1783933404"

        ruling(
            "2019-05-03",
            "Storm the Citadel affects only creatures you control at the time it resolves. Creatures " +
                "you begin to control later in the turn won't get +2/+2 or gain the triggered ability."
        )
        ruling(
            "2019-05-03",
            "If a creature is attacking a planeswalker, the controller of the planeswalker is the " +
                "defending player."
        )
    }
}
