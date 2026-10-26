package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rakdos Charm
 * {B}{R}
 * Instant
 * Choose one —
 * • Exile target player's graveyard.
 * • Destroy target artifact.
 * • Each creature deals 1 damage to its controller.
 *
 * The third mode walks every creature on the battlefield; each one is the source of its own
 * damage (`damageSource = IterationEntity`, the Moonlight Hunt shape) and the recipient is that
 * creature's controller ([Player.ControllerOfIterationEntity]), so lifelink, infect and
 * "whenever a creature deals damage" triggers key off each creature individually.
 */
val RakdosCharm = card("Rakdos Charm") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Exile target player's graveyard.\n" +
        "• Destroy target artifact.\n" +
        "• Each creature deals 1 damage to its controller."

    spell {
        modal(chooseCount = 1) {
            mode("Exile target player's graveyard") {
                val player = target(Targets.Player)
                effect = Effects.Pipeline {
                    val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, player.asPlayer))
                    exile(graveyard, player.asPlayer)
                }
            }
            mode("Destroy target artifact") {
                val artifact = target(TargetFilter(GameObjectFilter.Artifact))
                effect = Effects.Destroy(artifact)
            }
            mode("Each creature deals 1 damage to its controller") {
                effect = Effects.ForEachInGroup(
                    filter = GroupFilter(GameObjectFilter.Creature),
                    effect = Effects.DealDamage(
                        amount = 1,
                        target = EffectTarget.PlayerRef(Player.ControllerOfIterationEntity),
                        damageSource = EffectTarget.IterationEntity,
                    ),
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "184"
        artist = "Zoltan Boros"
        flavorText = "\"Let all feel joy in pain.\"\n—Rakdos"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fcd4394-d22d-4eec-ad73-ffaf10ad60de.jpg?1783940335"
    }
}
