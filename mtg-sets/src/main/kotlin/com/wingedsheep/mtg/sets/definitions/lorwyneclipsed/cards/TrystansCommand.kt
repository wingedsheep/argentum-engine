package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Trystan's Command
 * {4}{B}{G}
 * Kindred Sorcery — Elf
 *
 * Choose two —
 * • Create a token that's a copy of target Elf you control.
 * • Return one or two target permanent cards from your graveyard to your hand.
 * • Destroy target creature or enchantment.
 * • Creatures target player controls get +3/+3 until end of turn. Untap them.
 */
val TrystansCommand = card("Trystan's Command") {
    manaCost = "{4}{B}{G}"
    typeLine = "Kindred Sorcery — Elf"
    oracleText = "Choose two —\n" +
            "• Create a token that's a copy of target Elf you control.\n" +
            "• Return one or two target permanent cards from your graveyard to your hand.\n" +
            "• Destroy target creature or enchantment.\n" +
            "• Creatures target player controls get +3/+3 until end of turn. Untap them."

    spell {
        modal(chooseCount = 2) {
            mode("Create a token that's a copy of target Elf you control") {
                val elf = target(
                    "an Elf you control",
                    TargetObject(filter = TargetFilter(GameObjectFilter.Creature.youControl().withSubtype("Elf")))
                )
                effect = Effects.CreateTokenCopyOfTarget(elf)
            }
            mode("Return one or two target permanent cards from your graveyard to your hand") {
                target = TargetObject(
                    count = 2,
                    minCount = 1,
                    filter = TargetFilter(GameObjectFilter.Permanent.ownedByYou(), zone = Zone.GRAVEYARD)
                )
                effect = ForEachTargetEffect(
                    effects = listOf(MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.HAND))
                )
            }
            mode("Destroy target creature or enchantment") {
                val perm = target(
                    "a creature or enchantment",
                    TargetObject(filter = TargetFilter(GameObjectFilter.CreatureOrEnchantment))
                )
                effect = Effects.Destroy(perm)
            }
            mode("Creatures target player controls get +3/+3 until end of turn. Untap them") {
                val player = target("target player", TargetPlayer())
                effect = EffectPatterns.modifyStatsForAll(
                    power = 3,
                    toughness = 3,
                    filter = GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player))
                ) then EffectPatterns.untapGroup(
                    filter = GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player))
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "247"
        artist = "Sam Guay"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba1d99ca-740a-481a-be89-615e40d56d06.jpg?1767952293"

        ruling("2025-11-17", "The token created by the first mode of Trystan's Command copies exactly what was printed on the original permanent and nothing else (unless that permanent is copying something else or is a token).")
        ruling("2025-11-17", "If all of Trystan's Command's targets are illegal as it tries to resolve, it will do nothing. If at least one target is still legal, it will resolve and do as much as it can.")
    }
}
