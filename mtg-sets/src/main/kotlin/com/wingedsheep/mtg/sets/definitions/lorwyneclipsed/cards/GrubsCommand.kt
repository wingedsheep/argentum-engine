package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Grub's Command
 * {3}{B}{R}
 * Kindred Sorcery — Goblin
 *
 * Choose two —
 * • Create a token that's a copy of target Goblin you control.
 * • Creatures target player controls get +1/+1 and gain haste until end of turn.
 * • Destroy target artifact or creature.
 * • Target player mills five cards, then puts each Goblin card milled this way into their hand.
 */
val GrubsCommand = card("Grub's Command") {
    manaCost = "{3}{B}{R}"
    typeLine = "Kindred Sorcery — Goblin"
    oracleText = "Choose two —\n" +
            "• Create a token that's a copy of target Goblin you control.\n" +
            "• Creatures target player controls get +1/+1 and gain haste until end of turn.\n" +
            "• Destroy target artifact or creature.\n" +
            "• Target player mills five cards, then puts each Goblin card milled this way into their hand."

    spell {
        modal(chooseCount = 2) {
            mode("Create a token that's a copy of target Goblin you control") {
                val goblin = target(
                    "target Goblin you control",
                    TargetObject(filter = TargetFilter(GameObjectFilter.Creature.youControl().withSubtype("Goblin")))
                )
                effect = Effects.CreateTokenCopyOfTarget(goblin)
            }
            mode("Creatures target player controls get +1/+1 and gain haste until end of turn") {
                val player = target("target player", TargetPlayer())
                effect = EffectPatterns.modifyStatsForAll(
                    power = 1,
                    toughness = 1,
                    filter = GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player))
                ) then EffectPatterns.grantKeywordToAll(
                    keyword = Keyword.HASTE,
                    filter = GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player))
                )
            }
            mode("Destroy target artifact or creature") {
                val perm = target("target artifact or creature", Targets.CreatureOrArtifact)
                effect = Effects.Destroy(perm)
            }
            mode("Target player mills five cards, then puts each Goblin card milled this way into their hand") {
                target("target player", TargetPlayer())
                effect = CompositeEffect(
                    listOf(
                        GatherCardsEffect(
                            source = CardSource.TopOfLibrary(DynamicAmount.Fixed(5), Player.ContextPlayer(0)),
                            storeAs = "milled"
                        ),
                        MoveCollectionEffect(
                            from = "milled",
                            destination = CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0))
                        ),
                        SelectFromCollectionEffect(
                            from = "milled",
                            selection = SelectionMode.All,
                            filter = GameObjectFilter.Any.withSubtype("Goblin"),
                            storeSelected = "milledGoblins"
                        ),
                        MoveCollectionEffect(
                            from = "milledGoblins",
                            destination = CardDestination.ToZone(Zone.HAND, Player.ContextPlayer(0))
                        )
                    )
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Iris Compiet"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73013908-feff-495a-a62e-1e19548ffe6f.jpg?1767662497"

        ruling("2025-11-17", "The token created by the first mode of Grub's Command copies exactly what was printed on the original permanent and nothing else (unless that permanent is copying something else or is a token). It doesn't copy whether that permanent is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, and so on.")
        ruling("2025-11-17", "If the copied permanent is copying something else, then the token enters as whatever that permanent copied.")
        ruling("2025-11-17", "If the copied permanent has {X} in its mana cost, X is 0.")
        ruling("2025-11-17", "If the copied permanent is a token, the token that's created copies the original characteristics of that token as stated by the effect that created that token.")
        ruling("2025-11-17", "Any enters abilities of the copied permanent will trigger when the token enters. Any \"as [this permanent] enters\" or \"[this permanent] enters with\" abilities of the copied permanent will also work.")
        ruling("2025-11-17", "If all of Grub's Command's targets are illegal as it tries to resolve, it will do nothing. If at least one target is still legal, it will resolve and do as much as it can.")
    }
}
