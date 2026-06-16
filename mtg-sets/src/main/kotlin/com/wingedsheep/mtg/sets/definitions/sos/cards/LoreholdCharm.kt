package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Lorehold Charm
 * {R}{W}
 * Instant
 * Choose one —
 * • Each opponent sacrifices a nontoken artifact of their choice.
 * • Return target artifact or creature card with mana value 2 or less from your graveyard to the battlefield.
 * • Creatures you control get +1/+1 and gain trample until end of turn.
 */
val LoreholdCharm = card("Lorehold Charm") {
    manaCost = "{R}{W}"
    colorIdentity = "WR"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Each opponent sacrifices a nontoken artifact of their choice.\n• Return target artifact or creature card with mana value 2 or less from your graveyard to the battlefield.\n• Creatures you control get +1/+1 and gain trample until end of turn."

    spell {
        modal(chooseCount = 1) {
            mode("Each opponent sacrifices a nontoken artifact of their choice") {
                effect = Effects.Sacrifice(
                    filter = GameObjectFilter.Artifact.nontoken(),
                    target = EffectTarget.PlayerRef(Player.EachOpponent)
                )
            }
            mode("Return target artifact or creature card with mana value 2 or less from your graveyard to the battlefield") {
                val t = target(
                    "target artifact or creature card with mana value 2 or less from your graveyard",
                    TargetObject(
                        filter = TargetFilter(
                            GameObjectFilter.Artifact.ownedByYou().manaValueAtMost(2)
                                .or(GameObjectFilter.Creature.ownedByYou().manaValueAtMost(2)),
                            zone = Zone.GRAVEYARD
                        )
                    )
                )
                effect = Effects.PutOntoBattlefield(t)
            }
            mode("Creatures you control get +1/+1 and gain trample until end of turn") {
                effect = Patterns.Group.modifyStatsForAll(1, 1, Filters.Group.creaturesYouControl) then
                    Patterns.Group.grantKeywordToAll(Keyword.TRAMPLE, Filters.Group.creaturesYouControl)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "200"
        artist = "Ksenia Kim"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5fe70295-e550-4577-a341-dab6c25aabfd.jpg?1775938389"
    }
}
