package com.wingedsheep.mtg.sets.definitions.ncc.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect

/**
 * Rose Room Treasurer — Streets of New Capenna Commander #51
 * {3}{R} · Creature — Ogre Warrior · 4/3
 *
 * Alliance — Whenever another creature you control enters, create a Treasure token if this is the
 * first or second time this ability has resolved this turn. Otherwise, you may pay {X}. When you
 * do, this creature deals X damage to any target.
 *
 * The alliance trigger bumps the source's per-turn resolution counter (Omnath, Locus of Creation's
 * shape) and branches: the first and second resolutions make a Treasure; every later one offers
 * the pay-{X} gate. Paying fires a reflexive trigger (CR 603.12) whose "any target" is chosen as
 * it goes on the stack, after X is known, and which reads the paid X (Wildborn Preserver's shape).
 */
val RoseRoomTreasurer = card("Rose Room Treasurer") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Warrior"
    power = 4
    toughness = 3
    oracleText = "Alliance — Whenever another creature you control enters, create a Treasure token if " +
        "this is the first or second time this ability has resolved this turn. Otherwise, you may pay " +
        "{X}. When you do, this creature deals X damage to any target."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = IncrementAbilityResolutionCountEffect then
            Effects.If(
                condition = Conditions.Any(
                    Conditions.SourceAbilityResolvedNTimes(1),
                    Conditions.SourceAbilityResolvedNTimes(2),
                ),
                then = Effects.CreateTreasure(),
                otherwise = Effects.MayPayX(
                    then = Effects.ReflexiveTrigger(
                        action = Effects.Nothing,
                        optional = false,
                        descriptionOverride = "this creature deals X damage to any target",
                    ) {
                        val anyTarget = target(Targets.Any)
                        effect = Effects.DealDamage(DynamicAmounts.xValue(), anyTarget)
                    },
                ),
            )
        description = "Alliance — Whenever another creature you control enters, create a Treasure token " +
            "if this is the first or second time this ability has resolved this turn. Otherwise, you may " +
            "pay {X}. When you do, this creature deals X damage to any target."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "51"
        artist = "David Sladek"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/2167439d-792d-471e-9137-ee46dbf7a82d.jpg?1783923359"
    }
}
