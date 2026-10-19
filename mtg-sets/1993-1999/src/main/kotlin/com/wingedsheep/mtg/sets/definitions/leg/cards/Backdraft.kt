package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.div
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Backdraft
 * {1}{R}
 * Instant
 *
 * Choose a player who cast one or more sorcery spells this turn. Backdraft deals damage to that
 * player equal to half the damage dealt by one of those sorcery spells this turn, rounded down.
 *
 * Neither choice targets: the player is picked as Backdraft resolves from those whose cast history
 * this turn holds a sorcery, then you pick one of that player's sorceries. The damage is what that
 * spell dealt as a source this turn, summed over every recipient, read off its cast record — so it
 * counts after the sorcery has resolved and left the stack.
 */
val Backdraft = card("Backdraft") {
    manaCost = "{1}{R}"
    typeLine = "Instant"
    oracleText = "Choose a player who cast one or more sorcery spells this turn. Backdraft deals " +
        "damage to that player equal to half the damage dealt by one of those sorcery spells this " +
        "turn, rounded down."

    spell {
        effect = Effects.Pipeline {
            val player = selectTarget(
                TargetPlayer(
                    restriction = Conditions.candidateCastSpellsThisTurn(GameObjectFilter.Sorcery),
                    descriptionOverride = "a player who cast one or more sorcery spells this turn"
                ),
                nonTargeting = true
            )
            val damage = runStoringNumber {
                Effects.ChooseSpellCastThisTurn(player.asTarget, GameObjectFilter.Sorcery, storeDamageDealtAs = it)
            }
            run(Effects.DealDamage(damage.amount / 2, player.asTarget))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Brian Snõddy"
        imageUri = "https://cards.scryfall.io/normal/front/5/8/58d5b9fe-b66a-48c9-94c4-db783e605f37.jpg?1783948060"
    }
}
