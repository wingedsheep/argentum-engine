package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Black Sun's Twilight
 * {X}{B}
 * Instant
 * Up to one target creature gets -X/-X until end of turn. If X is 5 or more, return a creature
 * card with mana value X or less from your graveyard to the battlefield tapped.
 *
 * The returned card is not a target (2023-02-04 ruling): it's chosen on resolution, so it is a
 * gather-from-graveyard → choose-one → move pipeline rather than a second target slot. The
 * -X/-X target is optional ("up to one"), so the spell can be cast with no target purely for
 * the reanimation half.
 */
val BlackSunsTwilight = card("Black Sun's Twilight") {
    manaCost = "{X}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Up to one target creature gets -X/-X until end of turn. If X is 5 or more, return a creature card with mana value X or less from your graveyard to the battlefield tapped."

    spell {
        val creature = target(TargetFilter.Creature, optional = true)
        val negX = -DynamicAmounts.xValue()
        effect = Effects.ModifyStats(negX, negX, creature) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.xValue(),
                    ComparisonOperator.GTE,
                    5
                ),
                then = Effects.Pipeline {
                    val candidates = gather(
                        CardSource.FromZone(
                            Zone.GRAVEYARD,
                            Player.You,
                            GameObjectFilter.Creature.manaValueAtMostX()
                        )
                    )
                    val chosen = chooseExactly(
                        1,
                        from = candidates,
                        prompt = "Choose a creature card to return to the battlefield tapped"
                    )
                    move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
                }
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Jonas De Ro"
        flavorText = "\"Where once there was complacence, Sheoldred brought ambition, and soon the will of Yawgmoth reigned.\"\n—Monument inscription"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5bb4def-c67c-44c2-b3b2-8c53a077432d.jpg?1783918050"
        ruling("2023-02-04", "The creature card you return, if any, isn't a target of Black Sun's Twilight. You choose it as Black Sun's Twilight resolves if X is 5 or more.")
        ruling("2023-02-04", "You can cast Black Sun's Twilight without a target (presumably with X equal to 5 or more) just to return a creature card. However, if you do choose a target, and that target is illegal at the time the spell tries to resolve, the spell won't resolve and none of its effects will happen. You won't get to return a creature card to the battlefield.")
    }
}
