package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Angrath, Minotaur Pirate
 * {4}{B}{R}
 * Legendary Planeswalker — Angrath
 * Starting Loyalty: 5
 *
 * +2: Angrath deals 1 damage to target opponent or planeswalker and each creature that player or
 * that planeswalker's controller controls.
 * −3: Return target Pirate card from your graveyard to the battlefield.
 * −11: Destroy all creatures target opponent controls. Angrath deals damage to that player equal to
 * their total power.
 *
 * The +2 is Chandra Nalaar's ultimate with an opponent-or-planeswalker target: only the recipient is
 * targeted, so hexproof creatures it covers are still dealt damage (ruling), and an animated target
 * planeswalker is dealt damage once.
 *
 * The −11 snapshots the opponent's total creature power *before* the destroy — the power each had as
 * it last existed on the battlefield. Indestructible survivors are in that total too, and a negative
 * power subtracts from it (the sum keeps signed values). A total of 0 or less deals no damage.
 */
val AngrathMinotaurPirate = card("Angrath, Minotaur Pirate") {
    manaCost = "{4}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Planeswalker — Angrath"
    startingLoyalty = 5
    oracleText = "+2: Angrath deals 1 damage to target opponent or planeswalker and each creature that " +
        "player or that planeswalker's controller controls.\n" +
        "−3: Return target Pirate card from your graveyard to the battlefield.\n" +
        "−11: Destroy all creatures target opponent controls. Angrath deals damage to that player equal " +
        "to their total power."

    loyaltyAbility(+2) {
        val recipient = target(Targets.OpponentOrPlaneswalker)
        val creatures = GameObjectFilter.Creature.targetPlayerControls(recipient) or
            GameObjectFilter.Creature.targetPlayerControls(EffectTarget.TargetController)
        effect = Effects.DealDamage(1, recipient) then
            Patterns.Group.dealDamageToAll(1, GroupFilter(creatures, excludeTarget = true))
    }

    loyaltyAbility(-3) {
        val pirate = target(TargetFilter.CardInGraveyard.withSubtype(Subtype.PIRATE).ownedByYou())
        effect = Effects.Move(pirate, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
    }

    loyaltyAbility(-11) {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val totalPower = storeNumber(
                DynamicAmounts.battlefield(Player.TargetOpponent, GameObjectFilter.Creature).sumPower()
            )
            run(Effects.DestroyAll(GameObjectFilter.Creature.targetOpponentControls()))
            run(Effects.DealDamage(totalPower.amount, opponent))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "201"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f17103dd-f31b-4f6e-b2ea-4ea91815bdd6.jpg?1783935257"
        ruling(
            "2018-01-19",
            "Angrath's first and last abilities target only the player. Creatures with hexproof that player " +
                "controls will be affected.",
        )
        ruling(
            "2018-01-19",
            "Angrath's last ability uses the power of those creatures as they last existed on the " +
                "battlefield to determine their total power. If a creature's power was somehow less than 0, " +
                "it subtracts from the total power of the other creatures. If the total power of those " +
                "creatures is 0 or less, Angrath doesn't deal damage.",
        )
        ruling(
            "2018-01-19",
            "If a creature controlled by the target opponent isn't destroyed as Angrath's last ability " +
                "resolves (most likely because it has indestructible), its power still contributes to the " +
                "amount of damage dealt. Use its power as it currently exists on the battlefield to " +
                "determine the total power of the creatures.",
        )
        ruling(
            "2018-01-19",
            "Any abilities that trigger when creatures die while resolving Angrath's last ability won't be " +
                "put on the stack until after the player is dealt damage. If the player's life total becomes " +
                "0 or less, those triggers won't resolve in time to save that player.",
        )
        ruling(
            "2018-01-19",
            "Any abilities of the destroyed creatures that trigger when the player is dealt damage won't " +
                "trigger.",
        )
    }
}
