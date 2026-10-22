package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Burn Down the House
 * {3}{R}{R}
 * Sorcery
 * Choose one —
 * • Burn Down the House deals 5 damage to each creature and each planeswalker.
 * • Create three 1/1 red Devil creature tokens with "When this token dies, it deals 1 damage to
 *   any target." They gain haste until end of turn.
 *
 * The sweep mode is Brotherhood's End's [GameObjectFilter.CreatureOrPlaneswalker] iteration. The
 * Devils carry their death trigger as Dance with Devils' do; haste is granted until end of turn to
 * exactly the tokens this spell made through the created-tokens pipeline collection (as Sokenzan,
 * Crucible of Defiance does), never baked into the token as printed haste.
 */
val BurnDownTheHouse = card("Burn Down the House") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Burn Down the House deals 5 damage to each creature and each planeswalker.\n" +
        "• Create three 1/1 red Devil creature tokens with \"When this token dies, it deals 1 damage " +
        "to any target.\" They gain haste until end of turn."

    spell {
        modal(chooseCount = 1) {
            mode("Deal 5 damage to each creature and each planeswalker") {
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.CreatureOrPlaneswalker),
                    Effects.DealDamage(5, EffectTarget.IterationEntity),
                )
            }
            mode("Create three 1/1 red Devil tokens with haste until end of turn") {
                effect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.RED),
                    creatureTypes = setOf("Devil"),
                    count = 3,
                    triggeredAbilities = listOf(
                        grantedTriggeredAbility {
                            trigger = Triggers.self.dies()
                            val any = target(Targets.Any)
                            effect = Effects.DealDamage(1, any)
                            description = "When this token dies, it deals 1 damage to any target."
                        }
                    ),
                    imageUri = "https://cards.scryfall.io/normal/front/1/4/14f9c505-a833-4240-83a0-fbd160bdbf0f.jpg?1783925224",
                ) then
                    Effects.ForEachInCollection(
                        CollectionSlot.CreatedTokens,
                        Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity)
                    )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "131"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20ded7af-8086-465e-a980-3099217d324c.jpg?1783925604"
    }
}
